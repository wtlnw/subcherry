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

import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;

import com.subcherry.repository.core.LogEntry;
import com.subcherry.utils.Utils.TicketMessage;

/**
 * Instances of this class represent a single entry to be merged.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
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
	 * @see #getMessage()
	 */
	private final TicketMessage _message;
	
	/**
	 * @see #getChangeSet()
	 */
	private ActiveChangeSet _changeset;
	
	/**
	 * @see #getState()
	 */
	private SubcherryMergeState _state = SubcherryMergeState.NEW;

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
		_message = new TicketMessage(revision.getRevision(), revision.getMessage(), context.getMessageRewriter());
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
	 * @return the {@link TicketMessage} to be used when committing changes made in this
	 *         {@link #getChange()}
	 */
	public TicketMessage getMessage() {
		return _message;
	}
	
	/**
	 * @return this {@link SubcherryMergeEntry}'s {@link SubcherryMergeState}
	 */
	public SubcherryMergeState getState() {
		return _state;
	}
	
	/**
	 * @return the {@link ActiveChangeSet} describing changed resources or {@code null}
	 *         if this entry is not being merged
	 */
	public ActiveChangeSet getChangeSet() {
		return _changeset;
	}
	
	/**
	 * Setter for {@link #getChangeSet()}.
	 * 
	 * @param set
	 *            see {@link #getChangeSet()}
	 */
	public void setChangeSet(final ActiveChangeSet set) {
		_changeset = set;
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

		// cleanup after switching to non-working state
		if (!newState.isWorking()) {
			// reset the error message
			_error = null;
			_changeset = null;
		}
		
		// notify registered listeners
		getContext().notifyStateChanged(this, oldState, newState);
	}
	
	/**
	 * @return the {@link Throwable} occurred during the previous merge execution or
	 *         {@code null}
	 */
	public Throwable getError() {
		return _error;
	}

	/**
	 * Setter for {@link #getError()}.
	 * 
	 * @param error
	 *            see {@link #getError()}
	 */
	public void setError(final Throwable error) {
		_error = error;
	}
}