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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.subcherry.repository.core.LogEntry;
import com.subcherry.trac.TracTicket;

/**
 * Instances of this class represent {@link TracTicket}s displayed to the user.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryTreeTicketNode implements SubcherryTreeNode {
	
	/**
	 * @see #getTicket()
	 */
	private final TracTicket _ticket;
	
	/**
	 * @see #getChanges()
	 */
	private final List<SubcherryTreeRevisionNode> _changes = new ArrayList<>();
	
	/**
	 * Create a {@link SubcherryTreeTicketNode}.
	 * 
	 * @param ticket
	 *            see {@link #getTicket()}
	 */
	public SubcherryTreeTicketNode(final TracTicket ticket) {
		_ticket = ticket;
	}

	/**
	 * @return the {@link TracTicket} this {@link SubcherryTreeTicketNode} represents or
	 *         {@code null} if {@link #getChanges()} were committed without a ticket
	 */
	public TracTicket getTicket() {
		return _ticket;
	}
	
	/**
	 * @return a (possibly empty) {@link List} of {@link SubcherryTreeRevisionNode} committed for
	 *         {@link #getTicket()}
	 */
	public List<SubcherryTreeRevisionNode> getChanges() {
		return Collections.unmodifiableList(_changes);
	}
	
	/**
	 * Add the given {@link LogEntry} to the {@link List} of {@link #getChanges()}
	 * committed for {@link #getTicket()}.
	 * 
	 * @param entry
	 *            the {@link LogEntry} to add
	 * @return a new {@link SubcherryTreeRevisionNode} representing the given entr<
	 */
	public SubcherryTreeRevisionNode addChange(final LogEntry entry) {
		final SubcherryTreeRevisionNode change = new SubcherryTreeRevisionNode(this, entry);
		
		_changes.add(change);
		
		Collections.sort(_changes, new Comparator<SubcherryTreeRevisionNode>() {
			@Override
			public int compare(final SubcherryTreeRevisionNode o1, final SubcherryTreeRevisionNode o2) {
				final Long r1 = Long.valueOf(o1.getChange().getRevision());
				final Long r2 = Long.valueOf(o2.getChange().getRevision());

				return r1.compareTo(r2);
			}
		});
		
		return change;
	}
	
	@Override
	public SubcherryTreeNode.Check getState() {
		final List<SubcherryTreeRevisionNode> changes = getChanges();
		int checked = 0;
		
		for (final SubcherryTreeRevisionNode change : changes) {
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
	public void setState(final SubcherryTreeNode.Check state) {
		// this state does not change anything
		if(Check.GRAYED == state) {
			return;
		}
		
		// propagate the new state to all changes
		for(final SubcherryTreeRevisionNode change : getChanges()) {
			change.setState(state);
		}
	}
}