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
package com.subcherry.ui.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.IResourceStateChangeListener;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.CleanupResourcesCommand;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.status.StatusCacheManager;
import org.tigris.subversion.subclipse.ui.operations.CommitOperation;
import org.tigris.subversion.subclipse.ui.operations.SVNOperation;
import org.tigris.subversion.subclipse.ui.operations.UpdateOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;

import com.subcherry.repository.command.merge.MergeOperation;
import com.subcherry.repository.core.LogEntry;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.ui.views.SubcherryMergeContext;
import com.subcherry.ui.views.SubcherryMergeEntry;
import com.subcherry.ui.views.SubcherryMergeListener;
import com.subcherry.ui.views.SubcherryMergeState;
import com.subcherry.ui.views.SubcherryMergeView;
import com.subcherry.utils.Utils.TicketMessage;

/**
 * An {@link AbstractSubcherryOperation} performing cherry picking using subcherry.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class SubcherryMergeOperation extends AbstractSubcherryOperation {

	/**
	 * Create a {@link SubcherryMergeOperation}.
	 * 
	 * @param part
	 *            see {@link #getPart()}
	 */
	public SubcherryMergeOperation(final SubcherryMergeView part) {
		super(part);
	}

	@Override
	public String getTaskName() {
		return L10N.SubcherryMergeOperation_name;
	}
	
	@Override
	protected void executeOperation(final IProgressMonitor monitor) throws SVNException, InterruptedException {
		final SubMonitor progress = SubMonitor.convert(monitor, 100);
		
		try {
			final List<SubcherryMergeEntry> processed = process(progress);
			
			updateViewer(processed.toArray(new SubcherryMergeEntry[processed.size()]));
		} finally {
			monitor.done();
		}
	}

	/**
	 * Merge all pending {@link SubcherryMergeEntry}s in {@link #getView()}'s
	 * {@link SubcherryMergeContext}.
	 * 
	 * @param monitor
	 *            the {@link SubMonitor} to report the progress to
	 * @return a (possibly empty) {@link List} of processed
	 *         {@link SubcherryMergeEntry}s
	 */
	private List<SubcherryMergeEntry> process(final SubMonitor monitor) {
		final SubcherryMergeContext context = getContext();
		final List<SubcherryMergeEntry> pending = context.getPendingEntries();
		final int total = context.getAllEntries().size();
		
		// non-final: will be used for reporting progress
		int progress = total - pending.size();
		
		// initialize the monitor (consider resuming)
		monitor.setWorkRemaining(total);
		monitor.worked(progress);
		
		// merge pending entries
		final List<SubcherryMergeEntry> changed = new ArrayList<>();
		for (final SubcherryMergeEntry entry : pending) {
			if (monitor.isCanceled()) {
				break;
			}

			// provide progress feedback to users
			monitor.subTask(NLS.bind(L10N.SubcherryMergeOperation_progress_process, new Object[] {++progress, total, entry.getChange().getRevision(), entry.getMessage().getLogEntryMessage()}));

			// process the current entry
			processEntry(entry, monitor.split(1));
			changed.add(entry);

			// terminate processing if the entry needs further processing
			if (entry.getState().isPending()) {
				break;
			}
		}
		
		return changed;
	}
	
	/**
	 * Process the given entry completely.
	 * 
	 * @param entry
	 *            the {@link SubcherryMergeEntry} to process
	 * @param monitor
	 *            the {@link SubMonitor} to report the progress to
	 */
	private void processEntry(final SubcherryMergeEntry entry, final SubMonitor monitor) {
		final boolean commitChanges = !getContext().getConfiguration().getNoCommit();
		
		try {
			switch(entry.getState()) {
			case NEW:
				processNewEntry(entry, monitor, commitChanges);
				break;
			case MERGED: // try committing changes
			case ERROR:
				processMerged(entry, monitor, commitChanges);
				break;
			default:
				break;
			}
		} catch (final Throwable e) {
			e.printStackTrace();
			entry.setError(e);
			entry.setState(SubcherryMergeState.ERROR);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Process the given entry assuming the {@link SubcherryMergeState#NEW} state.
	 * 
	 * @param entry
	 *            the {@link SubcherryMergeEntry} to process
	 * @param monitor
	 *            the {@link SubMonitor} instance to report the progress to
	 * @param commit
	 *            {@code true} to commit changes, {@code false} to skip committing
	 * @throws Throwable
	 *             if an error occurred during processing
	 */
	private void processNewEntry(final SubcherryMergeEntry entry, final SubMonitor monitor, final boolean commit) throws Throwable {
		monitor.setWorkRemaining(2);
		
		// perform the merge operation
		final SyncInfoSet sync = merge(entry.getChange(), monitor.split(1));
		
		// no changes -> continue execution
		if (sync.isEmpty()) {
			entry.setState(SubcherryMergeState.NO_COMMIT);
		}
		// conflicts detected -> break execution
		else if (hasConflicts(sync)) {
			// create a change set for user processing
			if (commit) {
				entry.setChangeSet(newChangeSet(sync, entry.getMessage()));
			}
			
			// install a listener to the sync set for conflict resolution
			new ConflictResolveListener(entry, sync);
			entry.setState(SubcherryMergeState.CONFLICT);
		}
		// commit changes -> continue execution
		else if(commit) {
			try {
				commit(entry.getMessage().getMergeMessage(), sync.getResources(), monitor.split(1));
				entry.setState(SubcherryMergeState.COMMITTED);
			} catch (Throwable e) {
				e.printStackTrace();
				entry.setError(e);
				entry.setChangeSet(newChangeSet(sync, entry.getMessage()));
				entry.setState(SubcherryMergeState.ERROR);
			}
		}
		// no commit requested
		else {
			entry.setState(SubcherryMergeState.NO_COMMIT);
		}
	}

	/**
	 * Process the given entry assuming the {@link SubcherryMergeState#MERGED}
	 * state.
	 * 
	 * @param entry
	 *            the {@link SubcherryMergeEntry} to process
	 * @param monitor
	 *            the {@link SubMonitor} instance to report the progress to
	 * @param commit
	 *            {@code true} to commit changes, {@code false} to skip committing
	 * @throws Throwable
	 *             if an error occurred during processing
	 */
	private void processMerged(final SubcherryMergeEntry entry, final SubMonitor monitor, final boolean commit) throws Throwable {
		if (commit) {
			monitor.setWorkRemaining(1);
			final ActiveChangeSet set = entry.getChangeSet();
			commit(set.getComment(), set.getResources(), monitor.split(1));
			SVNProviderPlugin.getPlugin().getChangeSetManager().remove(set);
			entry.setState(SubcherryMergeState.COMMITTED);
		} else {
			// no-commit mode -> just update the state
			entry.setState(SubcherryMergeState.NO_COMMIT);
		}
	}
	
	/**
	 * @param sync
	 *            the {@link SyncInfoSet} containing the changes to be grouped into
	 *            a new changeset
	 * @param message
	 *            the {@link TicketMessage} to be used for resolving the changeset's
	 *            title and comment
	 * @return a new {@link ActiveChangeSet} for containing the given changes
	 * @throws CoreException
	 */
	private ActiveChangeSet newChangeSet(final SyncInfoSet sync, final TicketMessage message) throws CoreException {
		final ActiveChangeSetManager mgr = SVNProviderPlugin.getPlugin().getChangeSetManager();
		final String title = NLS.bind(L10N.SubcherryMergeOperation_changeset_title, message.getOriginalRevision());
		final IDiff[] diff = null;
		
		final ActiveChangeSet set = mgr.createSet(title, diff);
		set.setComment(message.getMergeMessage());
		set.add(sync.getResources());
		mgr.add(set);
		
		return set;
	}
	
	/**
	 * Commit changes to the given resources.
	 * 
	 * @param message
	 *            the commit message
	 * @param resources
	 *            the {@link IResource}s to commit the changes for
	 * @param monitor
	 *            the {@link IProgressMonitor} to report the progress to
	 * @throws Throwable
	 *             if an error occurred while committing the changes
	 */
	private void commit(final String message, final IResource[] resources, final IProgressMonitor monitor) throws Throwable {
		final SubMonitor progress = SubMonitor.convert(monitor);
		progress.setWorkRemaining(2);

		try {
			final List<IResource> addedResources = new ArrayList<>();
			final List<IResource> removedResources = new ArrayList<>();

			for (final IResource resource : resources) {
				final ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
				
				if (svnResource.exists() && !svnResource.isManaged()) {
					addedResources.add(resource);
				}

				if (svnResource.getStatus().isMissing()) {
					removedResources.add(resource);
				}
			}
			
			final IWorkbenchPart part = getPart();
			final Display display = part.getSite().getShell().getDisplay();

			// commit resources
			final CommitOperation commit = new CommitOperation(
					part, 
					resources,
					array(addedResources),
					array(removedResources),
					resources,
					message,
					false);
			commit.setCanRunAsJob(false);
			final SVNOperationRunnable commitRunnable = new SVNOperationRunnable(commit, progress.split(1));
			display.syncExec(commitRunnable);
			if (commitRunnable.error() != null) {
				throw commitRunnable.error();
			}

			// update committed resources
			final UpdateOperation update = new UpdateOperation(part, resources, SVNRevision.HEAD);
			update.setCanRunAsJob(false);
			update.setForce(true);
			final SVNOperationRunnable updateRunnable = new SVNOperationRunnable(update, progress.split(1));
			display.syncExec(updateRunnable);
			if (updateRunnable.error() != null) {
				throw updateRunnable.error();
			}
		} finally {
			progress.done();
		}
	}

	/**
	 * Merge the given {@link LogEntry}.
	 * 
	 * @param mergeEntry
	 *            the {@link LogEntry} to merge
	 * @param monitor
	 *            the {@link IProgressMonitor} instance to report the progress to
	 * @return the {@link SyncInfoSet} containing changed resources
	 * @throws RepositoryException
	 * @throws CoreException
	 */
	private SyncInfoSet merge(final LogEntry mergeEntry, final IProgressMonitor monitor) throws RepositoryException, CoreException {
		final SubMonitor progress = SubMonitor.convert(monitor, 6);
		
		try {
			final SyncInfoSet sync = new SyncInfoSet();
			final SubcherryMergeContext context = getContext();
			
			final MergeOperation merge = context.getMergeHandler().parseMerge(mergeEntry);
			progress.worked(1);
			
			context.getCommandExecutor().execute(merge.getCommands());
			progress.worked(1);

			// resolve touched projects
			final Set<String> paths = merge.getTouchedResources();
			final IResource[] projects = computeChangedProjects(paths);
			progress.worked(1);

			// map touched projects by their TeamProvider
			final Map<SVNTeamProvider, List<IResource>> providerProjects = groupByProvider(projects);
			progress.worked(1);

			// refresh/cleanup
			final SubMonitor subprogress = progress.split(1);
			subprogress.setWorkRemaining(providerProjects.size());
			try {
				for (final Entry<SVNTeamProvider, List<IResource>> entry : providerProjects.entrySet()) {
					new CleanupResourcesCommand(entry.getKey().getSVNWorkspaceRoot(), array(entry.getValue()))
					.run(subprogress.split(1));
				}
			} finally {
				subprogress.done();
			}
			
			// update the given SyncInfoSet instance with computed changes
			computeChangedResources(providerProjects, sync, progress.split(1));
			sync.removeIncomingNodes();
			
			return sync;
		} finally {
			progress.done();
		}
	}
	
	/**
	 * @param set
	 *            the {@link SyncInfoSet} to be checked for conflicts
	 * @return {@code true} if the given set has at least one conflict
	 * @throws SVNException
	 */
	public static boolean hasConflicts(final SyncInfoSet set) throws SVNException {
		final StatusCacheManager cache = SVNProviderPlugin.getPlugin().getStatusCacheManager();
		
		for (final IResource resource : set.getResources()) {
			final LocalResourceStatus status = cache.getStatus(resource);
			if (status != null && (status.isPropConflicted() || status.isTextConflicted() || status.hasTreeConflict())) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * An {@link IResourceStateChangeListener} implementation which changes the
	 * {@link SubcherryMergeEntry} state when all conflicts of a {@link SyncInfoSet}
	 * have been resolved.
	 */
	class ConflictResolveListener implements IResourceStateChangeListener, SubcherryMergeListener {
		
		/**
		 * @see #entry()
		 */
		private final SubcherryMergeEntry _entry;

		/**
		 * @see #changes()
		 */
		private final SyncInfoSet _changes;
		
		/**
		 * Create a {@link ConflictResolveListener}.
		 * 
		 * @param entry
		 *            see {@link #entry()}
		 */
		ConflictResolveListener(final SubcherryMergeEntry entry, final SyncInfoSet changes) {
			_entry = entry;
			_changes = changes;
			
			// attach itself to the entry's context to be notified when the entry's
			// state changes from CONFLICT to any other state.
			_entry.addMergeListener(this);
			
			// attach itself to the SVNProviderPlugin to be notified when resources
			// change their synchronize state.
			SVNProviderPlugin.addResourceStateChangeListener(this);
		}
		
		/**
		 * @return the {@link SubcherryMergeEntry} to change the state for when all
		 *         conflicts have been resolved
		 */
		SubcherryMergeEntry entry() {
			return _entry;
		}
		
		/**
		 * @return the {@link SyncInfoSet} containing changed resources upon changes to
		 *         which the {@link #entry()} state is to be updated
		 */
		SyncInfoSet changes() {
			return _changes;
		}
		
		@Override
		public void onStateChanged(final SubcherryMergeEntry entry, final SubcherryMergeState oldState, final SubcherryMergeState newState) {
			// detach itself from the entry's context and SyncInfoSet when the 
			// entry has switched from CONFLICT to any other state.
			if (entry() == entry && SubcherryMergeState.CONFLICT != newState) {
				entry().removeMergeListener(this);
				SVNProviderPlugin.removeResourceStateChangeListener(this);
				updateViewer(entry());
			}
		}
		
		@Override
		public void initialize() {
			// does nothing
		}
		
		@Override
		public void projectConfigured(final IProject project) {
			// does nothing
		}
		
		@Override
		public void projectDeconfigured(final IProject project) {
			// does nothing
		}
		
		@Override
		public void resourceModified(final IResource[] changedResources) {
			update(changedResources);
		}
		
		@Override
		public void resourceSyncInfoChanged(final IResource[] changedResources) {
			update(changedResources);
		}
		
		/**
		 * Update {@link #entry()} upon changes to the given resources.
		 * 
		 * @param changes
		 *            the {@link IResource}s which were changed (either {@link SyncInfo}
		 *            or modification)
		 */
		private void update(final IResource[] changes) {
			try {
				if (isRelated(changes) && !hasConflicts(changes())) {
					entry().setState(SubcherryMergeState.MERGED);
				}
			} catch (SVNException e) {
				e.printStackTrace();
			}
		}

		/**
		 * @param changes
		 *            a (possibly empty) array of modified {@link IResource}s
		 * @return {@code true} if the given resources are part of {@link #changes()},
		 *         {@code false} otherwise
		 */
		private boolean isRelated(final IResource[] changes) {
			return !Collections.disjoint(Arrays.asList(changes), Arrays.asList(changes().getResources()));
		}
	}

	/**
	 * A {@link Runnable} implementation running an {@link SVNOperation} with an
	 * {@link IProgressMonitor} instance.
	 */
	static final class SVNOperationRunnable implements Runnable {

		/**
		 * @see #operation()
		 */
		private final SVNOperation _operation;
		
		/**
		 * @see #monitor()
		 */
		private final IProgressMonitor _monitor;
		
		/**
		 * @see #error()
		 */
		private Throwable _error;
		
		/**
		 * Create a {@link SVNOperationRunnable}.
		 * 
		 * @param operation
		 *            see {@link #operation()}
		 * @param monitor
		 *            see {@link #monitor()}
		 */
		SVNOperationRunnable(final SVNOperation operation, final IProgressMonitor monitor) {
			_operation = operation;
			_monitor = monitor;
		}
		
		/**
		 * @return the {@link SVNOperation} to run
		 */
		SVNOperation operation() {
			return _operation;
		}

		/**
		 * @return the {@link IProgressMonitor} instance to run the {@link #operation()}
		 *         with
		 */
		IProgressMonitor monitor() {
			return _monitor;
		}
		
		/**
		 * @return the {@link Throwable} occurred while running {@link #operation()} or
		 *         {@code null} if this instance was not run yet or no error occurred
		 */
		Throwable error() {
			return _error;
		}
		
		@Override
		public void run() {
			try {
				operation().run(monitor());
			} catch (Throwable e) {
				_error = e;
			}
		}
	}
}
