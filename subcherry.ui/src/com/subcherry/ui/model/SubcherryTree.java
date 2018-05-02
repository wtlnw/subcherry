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
package com.subcherry.ui.model;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;

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
 * Instances of this class represent the SVN tree to pick the cherries from.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryTree {

	/**
	 * @see #getClientManager()
	 */
	private final ClientManager _mgr;

	/**
	 * @see #getTracConnection()
	 */
	private final TracConnection _trac;
	
	/**
	 * @see #getConfiguration()
	 */
	private final Configuration _config;
	
	/**
	 * @see #getTickets()
	 */
	private List<SubcherryTreeTicketNode> _nodes;
	
	/**
	 * Create a {@link SubcherryTree}.
	 * 
	 * @param mgr
	 *            see {@link #getClientManager()}
	 * @param trac
	 *            see {@link #getTracConnection()}
	 * @param config
	 *            see {@link #getConfiguration()}
	 */
	public SubcherryTree(final ClientManager mgr, final TracConnection trac, final Configuration config) {
		_mgr = mgr;
		_trac = trac;
		_config = config;
	}
	
	/**
	 * @return the {@link ClientManager} to be used for accessing the remote
	 *         repository
	 */
	public ClientManager getClientManager() {
		return _mgr;
	}
	
	/**
	 * @return the {@link TracConnection} to be used for accessing trac tickets
	 */
	public TracConnection getTracConnection() {
		return _trac;
	}
	
	/**
	 * @return the {@link Configuration} containing user preferences
	 */
	public Configuration getConfiguration() {
		return _config;
	}
	
	/**
	 * @return a (possibly empty) {@link List} of top-level
	 *         {@link SubcherryTreeTicketNode}s
	 */
	public List<SubcherryTreeTicketNode> getTickets() {
		if(_nodes == null) {
			try {
				final LogReader log = newLogReader();
				final List<String> paths = computePaths();
				final List<LogEntry> entries = readHistory(log, paths);
			
				_nodes = groupByTicket(entries);
			} catch(Exception e) {
				final Throwable cause;
				if(e instanceof UndeclaredThrowableException) {
					cause = e.getCause();
				} else {
					cause = e;
				}
				final Status status = new Status(IStatus.ERROR, SubcherryUI.id(), "Trac access failed.", cause);
				ErrorDialog.openError(Display.getCurrent().getActiveShell(), "Subcherry Merge", "Failed to resolve trac tickets.", status);
				
				// no data upon failure
				_nodes = Collections.emptyList();
			}
		}
		
		return _nodes;
	}
	
	/**
	 * @return a (possibly empty) {@link List} of {@link SubcherryTreeTicketNode}s
	 *         with at least one selected revision
	 */
	public List<SubcherryTreeTicketNode> getSelectedTickets() {
		final List<SubcherryTreeTicketNode> tickets = new ArrayList<>();
		
		for (final SubcherryTreeTicketNode ticket : getTickets()) {
			switch(ticket.getState()) {
			case CHECKED: // fall through
			case GRAYED:
				tickets.add(ticket);
				break;
			default:
				// ignore
				break;
			}
		}
		
		return tickets;
	}
	
	/**
	 * Group the given changes by the ticket they have been committed for.
	 * 
	 * @param entries
	 *            a (possibly empty) {@link List} of {@link LogEntry}s to group
	 * @return a (possibly empty) {@link List} of {@link SubcherryTreeTicketNode}s representing
	 *         {@link TracTicket}s
	 */
	private List<SubcherryTreeTicketNode> groupByTicket(final List<LogEntry> entries) {
		final Map<Integer, SubcherryTreeTicketNode> tickets = new HashMap<>();
		for (final LogEntry entry : entries) {
			final String msg = entry.getMessage();
			final String id = Utils.getTicketId(msg);

			SubcherryTreeTicketNode ticket = null;
			if(id != null) {
				final int number = Integer.parseInt(id);
				
				ticket = tickets.get(number);
				if(ticket == null) {
					ticket = new SubcherryTreeTicketNode(TracTicket.getTicket(_trac, number));
					tickets.put(number, ticket);
				}
			} else {
				ticket = tickets.get(null);
				if(ticket == null) {
					ticket = new SubcherryTreeTicketNode(null);
					tickets.put(null, ticket);
				}
			}
			
			ticket.addChange(entry);
		}
		
		// sort by ticket number
		final List<SubcherryTreeTicketNode> result = new ArrayList<>(tickets.values());
		Collections.sort(result, new SubcherryTreeTicketNodeComparator());
		
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
			final Status status = new Status(IStatus.ERROR, SubcherryUI.id(), "Failed to access remote location.", e);
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
	 * @return a (possibly empty) {@link List} of repository local SVN paths to read
	 *         the history for
	 */
	private List<String> computePaths() {
		// list all paths in the client workspace
		final String source = getConfiguration().getSourceBranch();
		final List<String> paths = new ArrayList<>();
		
		for (final String module : getConfiguration().getModules()) {
			final StringBuilder path = new StringBuilder();
			path.append(source);
			if (!source.endsWith("/")) {
				path.append('/');
			}
			path.append(module);
			paths.add(path.toString());
		}
		
		// append the branch itself to handle adding/removing modules
		paths.add(source);
		
		return paths;
	}
	
	/**
	 * @return a new {@link LogReader} instance
	 */
	private LogReader newLogReader() {
		final Configuration config = getConfiguration();
		final Client client = getClientManager().getClient();
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
	 * A {@link Comparator} implementation for {@link SubcherryTreeTicketNode}s.
	 * 
	 * <p>
	 * This implementation sorts {@link SubcherryTreeTicketNode}s by their ticket
	 * number in ascending order.
	 * </p>
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	private static class SubcherryTreeTicketNodeComparator implements Comparator<SubcherryTreeTicketNode> {
		
		@Override
		public int compare(final SubcherryTreeTicketNode n1, final SubcherryTreeTicketNode n2) {
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
	}
}
