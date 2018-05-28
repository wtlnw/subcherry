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
package com.subcherry.ui.views;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.ISubscriberChangeEvent;
import org.eclipse.team.core.subscribers.ISubscriberChangeListener;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.sync.SVNWorkspaceSubscriber;

import com.subcherry.commit.Commit;
import com.subcherry.repository.command.merge.ConflictDescription;
import com.subcherry.repository.command.merge.MergeOperation;
import com.subcherry.repository.core.LogEntry;
import com.subcherry.ui.SubcherryUI;
import com.subcherry.utils.Utils.TicketMessage;

/**
 * Instances of this class represent a single entry to be merged.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryMergeEntry implements ISubscriberChangeListener {
	
	/**
	 * @see #getContext()
	 */
	private final SubcherryMergeContext _context;
	
	/**
	 * @see getChangeset()
	 */
	private final Commit _changeset;
	
	/**
	 * @see #getState()
	 */
	private SubcherryMergeState _state = SubcherryMergeState.NEW;

	/**
	 * @see #getConflicts()
	 */
	private final Map<File, List<ConflictDescription>> _conflicts = new LinkedHashMap<>();

	/**
	 * @see #getError()
	 */
	private Throwable _error;
	
	/**
	 * Create a {@link SubcherryMergeEntry}.
	 * 
	 * @param context
	 *            see {@link #getContext()}
	 * @param revision
	 *            see {@link #getChange()}
	 */
	public SubcherryMergeEntry(final SubcherryMergeContext context, final LogEntry revision) {
		_context = context;
		_changeset = new Commit(context.getConfiguration(), revision, new TicketMessage(revision.getRevision(), revision.getMessage(), context.getMessageRewriter()));
	}
	
	/**
	 * @return the {@link SubcherryMergeContext} of this {@link SubcherryMergeEntry}
	 */
	public SubcherryMergeContext getContext() {
		return _context;
	}
	
	/**
	 * @return the {@link Commit} defining the changes for this
	 *         {@link SubcherryMergeEntry}
	 */
	public Commit getChangeset() {
		return _changeset;
	}
	
	/**
	 * @return the {@link LogEntry} to merge
	 */
	public LogEntry getChange() {
		return getChangeset().getLogEntry();
	}
	
	/**
	 * @return the message to be used when committing changes made in this
	 *         {@link #getChange()}
	 */
	public String getMessage() {
		return getChangeset().getCommitMessage();
	}
	
	/**
	 * Setter for {@link #getMessage()}.
	 * 
	 * @param message
	 *            see {@link #getMessage()}
	 */
	public void setMessage(final String message) {
		getChangeset().setCommitMessage(message);
		
		getContext().notifyEntryChanged(this);
	}
	
	/**
	 * @return this {@link SubcherryMergeEntry}'s {@link SubcherryMergeState}
	 */
	public SubcherryMergeState getState() {
		return _state;
	}
	
	/**
	 * Setter for {@link #getState()}.
	 * 
	 * @param newState
	 *            see {@link #getState()}
	 */
	public void setState(final SubcherryMergeState newState) {
		// stop listening for resolved conflicts
		if(SubcherryMergeState.CONFLICT != newState) {
			SVNWorkspaceSubscriber.getInstance().removeListener(this);
		}
		
		final SubcherryMergeState oldState = _state;
		
		_state = newState;
		
		getContext().notifyStateChanged(this, oldState, newState);
	}
	
	/**
	 * @return a (possibly empty) {@link Map} conflicts per file
	 */
	public Map<File, List<ConflictDescription>> getConflicts() {
		return _conflicts;
	}
	
	/**
	 * @return the {@link Throwable} occurred during the previous merge execution or
	 *         {@code null}
	 */
	public Throwable getError() {
		return _error;
	}
	
	/**
	 * Merge {@link #getChange()}.
	 * 
	 * @return {@link #getState()} for convenience
	 */
	public SubcherryMergeState merge() {
		final SubcherryMergeContext context = getContext();
		
		try {
			final MergeOperation merge = context.getMergeHandler().parseMerge(getChange());
			_changeset.addTouchedResources(merge.getTouchedResources());
			_conflicts.putAll(context.getCommandExecutor().execute(merge.getCommands()));
			
			if(_conflicts.isEmpty()) {
				setState(SubcherryMergeState.MERGED);
			} else {
				setState(SubcherryMergeState.CONFLICT);
				
				// listen for resolved conflicts
				SVNWorkspaceSubscriber.getInstance().addListener(this);
			}
		} catch (final Throwable e) {
			_error = e;
			
			setState(SubcherryMergeState.ERROR);
		}
		
		return getState();
	}
	
	/**
	 * Commit {@link #getChange()}.
	 * 
	 * @return {@link #getState()} for convenience
	 */
	public SubcherryMergeState commit() {
		try {
			// commit the changes
			getChangeset().run(getContext().getCommitContext());
			
			// reset conflicts and error
			_conflicts.clear();
			_error = null;
			
			// finally, update the state
			setState(SubcherryMergeState.COMMITTED);
		} catch (Throwable e) {
			_error = e;
			
			setState(SubcherryMergeState.ERROR);
		}
		
		return getState();
	}

	/**
	 * Skip {@link #getChange()}.
	 * 
	 * @return {@link #getState()} for convenience
	 */
	public SubcherryMergeState skip() {
		setState(SubcherryMergeState.SKIPPED);
		
		return getState();
	}
	
	/**
	 * Reset the state to {@link SubcherryMergeState#NEW} and clear cached data.
	 * 
	 * @return {@link #getState()} for convenience
	 */
	public SubcherryMergeState reset() {
		// clear the changeset
		_changeset.clear();
		
		// reset merge and conflict cache
		_conflicts.clear();
		_error = null;

		// update the entry state
		setState(SubcherryMergeState.NEW);
		
		return getState();
	}
	
	@Override
	public void subscriberResourceChanged(final ISubscriberChangeEvent[] events) {
		final Map<File, List<ConflictDescription>> conflicts = getConflicts();
		
		for (final ISubscriberChangeEvent event : events) {
			final IResource resource = event.getResource();

			final File file = resource.getLocation().toFile();
			if (conflicts.containsKey(file)) {
				try {
					final SyncInfo info = event.getSubscriber().getSyncInfo(resource);
					// the resource is properly tracked by SVN
					if (info != null) {
						
						// conflict has been resolved
						if ((info.getKind() & SyncInfo.CONFLICTING) != SyncInfo.CONFLICTING) {
							conflicts.remove(file);
						}
					}
				} catch (TeamException ex) {
					final Status status = new Status(IStatus.ERROR, SubcherryUI.id(), "Failed to resolve synchronizatio info for " + file, ex);
					SubcherryUI.getInstance().getLog().log(status);
					ErrorDialog.openError(PlatformUI.getWorkbench().getDisplay().getActiveShell(), "Subcherry Merge", "Revision information not available.", status);
				}
			}
		}
		
		// all conflicts have been resolved, mark the entry as merged
		if(conflicts.isEmpty()) {
			setState(SubcherryMergeState.MERGED);
		}
	}
}