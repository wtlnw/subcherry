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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;

import com.subcherry.Configuration;
import com.subcherry.LogReader;
import com.subcherry.MergeInfoTester;
import com.subcherry.repository.command.Client;
import com.subcherry.repository.command.ClientManager;
import com.subcherry.repository.command.log.LogEntryHandler;
import com.subcherry.repository.core.LogEntry;
import com.subcherry.repository.core.MergeInfo;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.repository.core.RepositoryURL;
import com.subcherry.repository.core.Revision;
import com.subcherry.repository.core.RevisionRange;
import com.subcherry.repository.core.RevisionRanges;
import com.subcherry.trac.TracConnection;
import com.subcherry.trac.TracTicket;
import com.subcherry.ui.SubcherryUI;
import com.subcherry.utils.Path;
import com.subcherry.utils.PathParser;
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
	 * @param progress the {@link IProgressMonitor} instance to report the progress
	 *                to
	 * @return a (possibly empty) {@link List} of top-level
	 *         {@link SubcherryTreeTicketNode}s
	 */
	public List<SubcherryTreeTicketNode> getTickets(final IProgressMonitor progress) {
		if(_nodes == null) {
			try {
				progress.beginTask(L10N.SubcherryTree_progress_compute, IProgressMonitor.UNKNOWN);
				
				final LogReader log = newLogReader(progress);
				final List<String> paths = computePaths(progress);
				final List<LogEntry> entries = readHistory(log, paths, progress);
			
				_nodes = groupByTicket(entries, progress);
			} catch(Exception e) {
				SubcherryUI.error(L10N.SubcherryTree_progress_error_trac_status, L10N.SubcherryTree_progress_error_trac_title, L10N.SubcherryTree_progress_error_trac_message, e);
				
				// no data upon failure
				_nodes = Collections.emptyList();
			} finally {
				progress.done();
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
		
		for (final SubcherryTreeTicketNode ticket : getTickets(new NullProgressMonitor())) {
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
	 * @param progress 
	 *            the {@link IProgressMonitor} to report the progress to
	 * @return a (possibly empty) {@link List} of {@link SubcherryTreeTicketNode}s representing
	 *         {@link TracTicket}s
	 */
	private List<SubcherryTreeTicketNode> groupByTicket(final List<LogEntry> entries, final IProgressMonitor progress) {
		final Map<Integer, SubcherryTreeTicketNode> tickets = new HashMap<>();
		for (final LogEntry entry : entries) {
			final String msg = entry.getMessage();
			final String id = Utils.getTicketId(msg);

			SubcherryTreeTicketNode ticket = null;
			if(id != null) {
				final int number = Integer.parseInt(id);
				
				ticket = tickets.get(number);
				if(ticket == null) {
					ticket = new SubcherryTreeTicketNode(_trac.getTicket(number));
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
	 * @param progress
	 *            the {@link IProgressMonitor} to report the progress to
	 * @return a (possibly empty) {@link List} of {@link LogEntry}s for the given
	 *         paths
	 */
	private List<LogEntry> readHistory(final LogReader log, final List<String> paths, final IProgressMonitor progress) {
		progress.subTask(L10N.SubcherryTree_progress_reading);
		
		final Configuration config = getConfiguration();
		final MergeInfoPredicate filter;
		if (config.getIgnoreMergeInfo()) {
			filter = null;
		} else {
			filter = new MergeInfoPredicate(getClientManager(), config, progress);
		}
		
		final FilteredLogEntryHandler handler = new FilteredLogEntryHandler(filter, progress);
		try {
			log.readLog(paths.toArray(new String[paths.size()]), handler);
		} catch (RepositoryException e) {
			SubcherryUI.error(L10N.SubcherryTree_progress_error_svn_status, L10N.SubcherryTree_progress_error_svn_title, L10N.SubcherryTree_progress_error_svn_message, e);
		}
		
		// convert from descending to ascending order
		final List<LogEntry> entries = handler.entries();
		Collections.reverse(entries);
		
		return entries;
	}
	
	/**
	 * Compute the remove SVN paths to read the history for based on the current
	 * workspace modules.
	 * 
	 * @param progress the {@link IProgressMonitor} instance to report the progress
	 *                 to
	 * @return a (possibly empty) {@link List} of repository local SVN paths to read
	 *         the history for
	 */
	private List<String> computePaths(final IProgressMonitor progress) {
		progress.subTask(L10N.SubcherryTree_progress_paths);
		
		// list all paths in the client workspace
		final String source = getConfiguration().getSourceBranch();
		final List<String> paths = new ArrayList<>();
		
		for (final String module : getConfiguration().getModules()) {
			final StringBuilder path = new StringBuilder();
			path.append(source);
			if (!source.endsWith("/")) { //$NON-NLS-1$
				path.append('/');
			}
			path.append(module);
			paths.add(path.toString());
		}
		
		// do not append the branch itself since adding/removing modules
		// is not supported when working with subclipse.
		// paths.add(source);
		
		return paths;
	}
	
	/**
	 * @param progress the {@link IProgressMonitor} instance to report progress to
	 * @return a new {@link LogReader} instance
	 */
	private LogReader newLogReader(final IProgressMonitor progress) {
		progress.subTask(L10N.SubcherryTree_progress_init);
		
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
	private static Revision toRevision(final long commitNumber) {
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
	
	/**
	 * A {@link LogEntryHandler} implementation which filters out already merged
	 * {@link LogEntry}s.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	protected static class FilteredLogEntryHandler implements LogEntryHandler {
		
		/**
		 * @see #filter()
		 */
		private final Predicate<LogEntry> _filter;
		
		/**
		 * @see #progress()
		 */
		private final IProgressMonitor _progress;
		
		/**
		 * @see #entries()
		 */
		private final List<LogEntry> _entries = new ArrayList<LogEntry>();
		
		/**
		 * Create a {@link FilteredLogEntryHandler}.
		 * 
		 * @param filter   see {@link #filter()}
		 * @param progress see {@link #progress()}
		 */
		public FilteredLogEntryHandler(final Predicate<LogEntry> filter, final IProgressMonitor progress) {
			_filter = filter;
			_progress = progress;
		}
		
		/**
		 * @return the {@link Predicate} to be used for {@link LogEntry} filtering or
		 *         {@code null} to accept all {@link LogEntry}s.
		 */
		public Predicate<LogEntry> filter() {
			return _filter;
		}
		
		/**
		 * @return the {@link IProgressMonitor} to report the progress to
		 */
		public IProgressMonitor progress() {
			return _progress;
		}
		
		/**
		 * @return a (possibly empty) {@link List} of accumulated {@link LogEntry}s.
		 */
		public List<LogEntry> entries() {
			return _entries;
		}
		
		@Override
		public void handleLogEntry(final LogEntry entry) throws RepositoryException {
			_progress.subTask(NLS.bind(L10N.SubcherryTree_progress_parse, entry.getRevision()));
			
			if (_filter == null || _filter.test(entry)) {
				_entries.add(entry);
			}
		}
	}
	
	/**
	 * A {@link Predicate} implementation which accepts only {@link LogEntry}
	 * instances which have not been merged into the target branch yet.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	protected static class MergeInfoPredicate implements Predicate<LogEntry> {

		/**
		 * The {@link MergeInfoTester} to be used for checking the merge information of
		 * {@link LogEntry} instances.
		 */
		private final MergeInfoTester _tester;
		
		/**
		 * The {@link RepositoryURL} pointing to the source branch.
		 */
		private final RepositoryURL _source;

		/**
		 * The {@link PathParser} instance for convenient path computation.
		 */
		private final PathParser _paths;

		/**
		 * The {@link Set} of modules to evaluate the merge information for.
		 */
		private final Set<String> _modules;

		/**
		 * @see #progress()
		 */
		private final IProgressMonitor _progress;
		
		/**
		 * Create a {@link MergeInfoPredicate}.
		 * 
		 * @param clients  the {@link ClientManager} to be used for merge information
		 *                 access
		 * @param config   the {@link Configuration} to be used for initialization
		 * @param progress see {@link #progress()}
		 */
		public MergeInfoPredicate(final ClientManager clients, final Configuration config, final IProgressMonitor progress) {
			final RepositoryURL url = RepositoryURL.parse(config.getSvnURL());
			final File root = config.getWorkspaceRoot();
			final Revision peg = SubcherryTree.toRevision(config.getPegRevision());
			
			_tester = new MergeInfoTester(clients, url, root, peg);
			_source = RepositoryURL.parse(config.getSvnURL()).appendPath(config.getSourceBranch());
			_paths = new PathParser(config);
			_modules = Stream.of(config.getModules()).collect(Collectors.toSet());
			_progress = progress;
		}
		
		/**
		 * @return the {@link IProgressMonitor} to report the progress to
		 */
		public IProgressMonitor progress() {
			return _progress;
		}
		
		@Override
		public boolean test(final LogEntry entry) {
			_progress.subTask(NLS.bind(L10N.SubcherryTree_progress_analyze, entry.getRevision()));

			try {
				final long mergedRevision = entry.getRevision();
				final Set<String> touchedModules = new HashSet<>();
				
				boolean alreadyMerged = false;
				for (final String changedPath : entry.getChangedPaths().keySet()) {
					final Path parsedPath = _paths.parsePath(changedPath);
					
					final String changedModuleName = parsedPath.getModule();
					touchedModules.add(changedModuleName);
					
					// Merge info is only recorded at module level. Therefore, checks on all
					// other paths can be skipped.
					if (!parsedPath.getResource().equals(parsedPath.getModule())) {
						continue;
					}
					
					// Skip changes to ignored modules.
					if (!_modules.contains(changedModuleName)) {
						continue;
					}
					
					alreadyMerged = _tester.isAlreadyMerged(mergedRevision, changedPath, changedModuleName);
					if (alreadyMerged) {
						break;
					}
				}
				
				if (!alreadyMerged) {
					for (final String touchedModule : touchedModules) {
						// Skip changes to ignored modules.
						if (!_modules.contains(touchedModule)) {
							continue;
						}
						
						final MergeInfo moduleMergeInfo = _tester.lookupMergeInfo(touchedModule);
						final RepositoryURL mergeSrcUrl = _source.appendPath(touchedModule);
						final List<RevisionRange> mergedRevisions = moduleMergeInfo.getRevisions(mergeSrcUrl);
						if (mergedRevisions == null) {
							continue;
						}
						
						if (RevisionRanges.contains(mergedRevisions, mergedRevision)) {
							alreadyMerged = true;
							break;
						}
					}
				}
				
				// accept only log entries which have not been merged yet
				return !alreadyMerged;
				
			} catch (RepositoryException e) {
				throw new IllegalStateException(e);
			}
		}
	}
}
