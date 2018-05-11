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
import java.util.List;
import java.util.Map;

import com.subcherry.commit.Commit;
import com.subcherry.repository.command.merge.ConflictDescription;
import com.subcherry.repository.command.merge.MergeOperation;
import com.subcherry.repository.core.LogEntry;
import com.subcherry.utils.Utils.TicketMessage;

/**
 * Instances of this class represent a single entry to be merged.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryMergeEntry {
	
	/**
	 * @see #getContext()
	 */
	private final SubcherryMergeContext _context;
	
	/**
	 * @see #getChange()
	 */
	private final LogEntry _change;
	
	/**
	 * @see #getState()
	 */
	private SubcherryMergeState _state = SubcherryMergeState.NEW;
	
	/**
	 * @see #getMessage()
	 */
	private String _message = null;

	/**
	 * @see #getOperation()
	 */
	private MergeOperation _merge;

	/**
	 * @see #getConflicts()
	 */
	private Map<File, List<ConflictDescription>> _conflicts;

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
		_change = revision;
	}
	
	/**
	 * @return the {@link SubcherryMergeContext} of this {@link SubcherryMergeEntry}
	 */
	public SubcherryMergeContext getContext() {
		return _context;
	}
	
	/**
	 * @return the {@link LogEntry} to merge
	 */
	public LogEntry getChange() {
		return _change;
	}
	
	/**
	 * @return the message to be used when committing changes made in this
	 *         {@link #getChange()}
	 */
	public String getMessage() {
		if(_message != null) {
			return _message;
		}
		
		return getChange().getMessage();
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
		final SubcherryMergeState oldState = _state;
		
		_state = newState;
		
		getContext().notifyMergeChanged(this, oldState, newState);
	}

	/**
	 * @return the performed {@link MergeOperation} or {@code null} if
	 *         {@link #merge(SubcherryMergeContext)} has not been called yet
	 */
	public MergeOperation getOperation() {
		return _merge;
	}
	
	/**
	 * @return a (possibly empty) {@link Map} conflicts per file or {@code null} if
	 *         {@link #merge(SubcherryMergeContext)} has not been called yet
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
			_merge = context.getMergeHandler().parseMerge(getChange());
			_conflicts = context.getCommandExecutor().execute(_merge.getCommands());
			
			if(_conflicts.isEmpty()) {
				setState(SubcherryMergeState.MERGED);
			} else {
				setState(SubcherryMergeState.CONFLICT);
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
		final SubcherryMergeContext context = getContext();
		final TicketMessage msg = new TicketMessage(getChange().getRevision(), getMessage(), context.getMessageRewriter());
		final Commit commit = new Commit(context.getConfiguration(), getChange(), msg);
		
		commit.addTouchedResources(getOperation().getTouchedResources());
		
		try {
			commit.run(context.getCommitContext());
			
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
		// reset merge and conflict cache
		_merge = null;
		_conflicts = null;
		_error = null;
		_message = null;

		// update the entry state
		setState(SubcherryMergeState.NEW);
		
		return getState();
	}
}