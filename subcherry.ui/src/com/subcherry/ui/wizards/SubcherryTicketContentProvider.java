/*
 * SubCherry - Cherry Picking with Trac and Subversion
 * Copyright (C) 2018 Bernhard Haumacher and others
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.subcherry.ui.wizards;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;

import com.subcherry.Configuration;
import com.subcherry.LogReader;
import com.subcherry.repository.command.Client;
import com.subcherry.repository.command.ClientManager;
import com.subcherry.repository.command.log.LogEntryHandler;
import com.subcherry.repository.core.LogEntry;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.repository.core.RepositoryURL;
import com.subcherry.repository.core.Revision;
import com.subcherry.trac.TracConnection;
import com.subcherry.trac.TracTicket;
import com.subcherry.ui.SubcherryUI;
import com.subcherry.utils.Utils;

/**
 * An {@link ITreeContentProvider} implementation computing all available
 * tickets.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
final class SubcherryTicketContentProvider implements ITreeContentProvider {
	
	/**
	 * The {@link ClientManager} to be used for accessing the remote repository.
	 */
	private final ClientManager _mgr;

	/**
	 * The {@link TracConnection} to be used for accessing trac tickets.
	 */
	private final TracConnection _trac;
	
	/**
	 * @see #getElements(Object)
	 */
	private Object[] _elements;
	
	/**
	 * Create a {@link SubcherryTicketContentProvider}.
	 * 
	 * @param mgr
	 *            see {@link #_mgr}
	 * @param trac
	 *            see {@link #_trac}
	 */
	public SubcherryTicketContentProvider(final ClientManager mgr, final TracConnection trac) {
		_mgr = mgr;
		_trac = trac;
	}
	
	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		_elements = null;
	}
	
	@Override
	public void dispose() {
		_elements = null;
	}
	
	@Override
	public Object[] getElements(final Object model) {
		if(_elements == null) {
			try {
				final Configuration config = (Configuration) model;
				final LogReader log = newLogReader(config);
				final List<String> paths = computePaths(config);
				final List<LogEntry> entries = readHistory(log, paths);
			
				_elements = groupByTicket(config, entries).toArray();
			} catch(Exception e) {
				final Throwable cause;
				if(e instanceof UndeclaredThrowableException) {
					cause = e.getCause();
				} else {
					cause = e;
				}
				final Status status = new Status(IStatus.ERROR, SubcherryUI.getInstance().getBundle().getSymbolicName(), "Trac access failed.", cause);
				ErrorDialog.openError(Display.getCurrent().getActiveShell(), "Subcherry Merge", "Failed to resolve trac tickets.", status);
				
				// no data upon failure
				_elements = new Object[0];
			}
		}
		
		return _elements;
	}

	/**
	 * Group the given changes by the ticket they have been committed for.
	 * 
	 * @param config
	 *            the {@link Configuration} providing common user settings
	 * @param entries
	 *            a (possibly empty) {@link List} of {@link LogEntry}s to group
	 * @return a (possibly empty) {@link List} of {@link TicketNode}s representing
	 *         {@link TracTicket}s
	 */
	private List<TicketNode> groupByTicket(final Configuration config, final List<LogEntry> entries) {
		final Map<Integer, TicketNode> tickets = new HashMap<>();
		for (final LogEntry entry : entries) {
			final String msg = entry.getMessage();
			final String id = Utils.getTicketId(msg);

			TicketNode ticket = null;
			if(id != null) {
				final int number = Integer.parseInt(id);
				
				ticket = tickets.get(number);
				if(ticket == null) {
					ticket = new TicketNode(TracTicket.getTicket(_trac, number));
					tickets.put(number, ticket);
				}
			} else {
				ticket = tickets.get(null);
				if(ticket == null) {
					ticket = new TicketNode(null);
					tickets.put(null, ticket);
				}
			}
			
			ticket.addChange(entry);
		}
		
		// sort by ticket number
		final ArrayList<TicketNode> result = new ArrayList<>(tickets.values());
		Collections.sort(result, new Comparator<TicketNode>() {
			@Override
			public int compare(final TicketNode n1, final TicketNode n2) {
				final TracTicket t1 = n1.getTicket();
				final TracTicket t2 = n2.getTicket();
				
				if(t1 != null) {
					if(t2 != null) {
						return t1.getNumber().compareTo(t2.getNumber());
					} else {
						return 1;
					}
				} else {
					if(t2 != null) {
						return -1;
					} else {
						return 0;
					}
				}
			}
		});
		
		return result;
	}

	/**
	 * Read the SVN history for the given paths using the given log.
	 * 
	 * @param log
	 *            the {@link LogReader} to be used for accessing SVN
	 * @param paths
	 *            the {@link List} of paths in the remote repository to read the
	 *            history for
	 * @return a (possibly empty) {@link List} of {@link LogEntry}s for the given
	 *         paths
	 */
	private List<LogEntry> readHistory(final LogReader log, final List<String> paths) {
		final List<LogEntry> entries = new ArrayList<>();
		
		try {
			log.readLog(paths.toArray(new String[paths.size()]), new LogEntryHandler() {
				@Override
				public void handleLogEntry(final LogEntry logEntry) throws RepositoryException {
					entries.add(logEntry);
				}
			});
		} catch (RepositoryException e) {
			final Status status = new Status(IStatus.ERROR, SubcherryUI.getInstance().getBundle().getSymbolicName(), "Failed to access remote location.", e);
			ErrorDialog.openError(Display.getCurrent().getActiveShell(), "Subcherry Merge", "No revision logs available.", status);
		}
		
		// convert from descending to ascending order
		Collections.reverse(entries);
		
		return entries;
	}

	/**
	 * Compute the remove SVN paths to read the history for based on the current
	 * workspace modules.
	 * 
	 * @param config
	 *            the {@link Configuration} providing common user settings
	 * @return a (possibly empty) {@link List} of repository local SVN paths to read
	 *         the history for
	 */
	private List<String> computePaths(final Configuration config) {
		final SVNProviderPlugin svn = SVNProviderPlugin.getPlugin();
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final String source = config.getSourceBranch();
		final List<String> paths = new ArrayList<>();
		
		for (final IProject module : workspace.getRoot().getProjects()) {
			if(module.isAccessible() && svn.isManagedBySubversion(module)) {
				final StringBuilder path = new StringBuilder();
				path.append(source);
				if(!source.endsWith("/")) {
					path.append('/');
				}
				path.append(module.getName());
				
				paths.add(path.toString());
			}
		}
		
		// append the branch itself to handle adding/removing modules
		paths.add(source);
		
		return paths;
	}

	/**
	 * @param config
	 *            the {@link Configuration} providing common user settings
	 * @return a new {@link LogReader} instance
	 */
	private LogReader newLogReader(final Configuration config) {
		final Client client = _mgr.getClient();
		final RepositoryURL url = RepositoryURL.parse(config.getSvnURL());

		final LogReader log = new LogReader(client, url);
		log.setStartRevision(toRevision(config.getStartRevision()));
		log.setEndRevision(toRevision(config.getEndRevision()));
		log.setPegRevision(toRevision(config.getPegRevision()));
		log.setStopOnCopy(false);
		log.setDiscoverChangedPaths(true);
		log.setLimit(0);
	
		return log;
	}
	
	@Override
	public Object[] getChildren(final Object parent) {
		if(parent instanceof TicketNode) {
			return ((TicketNode) parent).getChanges().toArray();
		}
		
		return new Object[0];
	}
	
	@Override
	public boolean hasChildren(final Object parent) {
		if(parent instanceof TicketNode) {
			return !((TicketNode) parent).getChanges().isEmpty();
		}
		
		return false;
	}
	
	@Override
	public Object getParent(final Object child) {
		if(child instanceof ChangeNode) {
			return ((ChangeNode) child).getTicket();
		}
		
		return null;
	}
	
	/**
	 * @param commitNumber
	 *            the commit number to return the revision for
	 * @return the {@link Revision} matching the given commit number or
	 *         {@link Revision#HEAD} if the given number is less than 1
	 */
	private Revision toRevision(final long commitNumber) {
		if(commitNumber < 1) {
			return Revision.HEAD;
		} else {
			return Revision.create(commitNumber);
		}
	}
	
	/**
	 * Implementing classes provide functionality for checking/unchecking instances.
	 * 
	 * @author wta
	 */
	public static interface Checkable {
		
		/**
		 * This enumeration defines all available states for {@link Checkable}s.
		 * 
		 * @author wta
		 */
		enum Check {
			CHECKED,
			GRAYED,
			UNCHECKED
		}
		
		/**
		 * @return the {@link Check} state of this node
		 */
		Check getState();

		/**
		 * Setter for {@link #getState()}.
		 * 
		 * @param state
		 *            see {@link #getState()}
		 */
		void setState(Check state);
	}
	
	/**
	 * Instances of this class represent {@link TracTicket}s displayed to the user.
	 * 
	 * @author wta
	 */
	public static class TicketNode implements Checkable {
		
		/**
		 * @see #getTicket()
		 */
		private final TracTicket _ticket;
		
		/**
		 * @see #getChanges()
		 */
		private final List<ChangeNode> _changes = new ArrayList<>();
		
		/**
		 * Create a {@link TicketNode}.
		 * 
		 * @param ticket
		 *            see {@link #getTicket()}
		 */
		public TicketNode(final TracTicket ticket) {
			_ticket = ticket;
		}

		/**
		 * @return the {@link TracTicket} this {@link TicketNode} represents or
		 *         {@code null} if {@link #getChanges()} were committed without a ticket
		 */
		public TracTicket getTicket() {
			return _ticket;
		}
		
		/**
		 * @return a (possibly empty) {@link List} of {@link ChangeNode} committed for
		 *         {@link #getTicket()}
		 */
		public List<ChangeNode> getChanges() {
			return Collections.unmodifiableList(_changes);
		}
		
		/**
		 * Add the given {@link LogEntry} to the {@link List} of {@link #getChanges()}
		 * committed for {@link #getTicket()}.
		 * 
		 * @param entry
		 *            the {@link LogEntry} to add
		 * @return a new {@link ChangeNode} representing the given entr<
		 */
		public ChangeNode addChange(final LogEntry entry) {
			final ChangeNode change = new ChangeNode(this, entry);
			
			_changes.add(change);
			
			Collections.sort(_changes, new Comparator<ChangeNode>() {
				@Override
				public int compare(final ChangeNode o1, final ChangeNode o2) {
					final Long r1 = Long.valueOf(o1.getChange().getRevision());
					final Long r2 = Long.valueOf(o2.getChange().getRevision());

					return r1.compareTo(r2);
				}
			});
			
			return change;
		}
		
		@Override
		public Check getState() {
			final List<ChangeNode> changes = getChanges();
			int checked = 0;
			
			for (final ChangeNode change : changes) {
				if(Check.CHECKED == change.getState()) {
					checked++;
				}
			}
			
			if(checked == 0) {
				return Check.UNCHECKED;
			} else if(checked == changes.size()) {
				return Check.CHECKED;
			} else {
				return Check.GRAYED; 
			}
		}
		
		@Override
		public void setState(final Check state) {
			// this state does not change anything
			if(Check.GRAYED == state) {
				return;
			}
			
			// propagate the new state to all changes
			for(final ChangeNode change : getChanges()) {
				change.setState(state);
			}
		}
	}
	
	/**
	 * Instances of this class represent {@link LogEntry}s displayed to the user.
	 * 
	 * @author wta
	 */
	public static class ChangeNode implements Checkable {
		
		/**
		 * @see #getTicket()
		 */
		private final TicketNode _ticket;
		
		/**
		 * @see #getChange()
		 */
		private final LogEntry _change;
		
		/**
		 * @see #getState()
		 */
		private Check _state;
		
		/**
		 * Create a {@link ChangeNode}.
		 * 
		 * @param ticket
		 *            see {@link #getTicket()}
		 * @param change
		 *            see {@link #getChange()}
		 */
		protected ChangeNode(final TicketNode ticket, final LogEntry change) {
			_ticket = ticket;
			_change = change;
			_state = Check.CHECKED;
		}
		
		/**
		 * @return the {@link TicketNode} this {@link ChangeNode} belongs to
		 */
		public TicketNode getTicket() {
			return _ticket;
		}
		
		/**
		 * @return the {@link LogEntry} this {@link ChangeNode} represents
		 */
		public LogEntry getChange() {
			return _change;
		}
		
		@Override
		public Check getState() {
			return _state;
		}
		
		@Override
		public void setState(final Check state) {
			_state = state;
		}
	}
}