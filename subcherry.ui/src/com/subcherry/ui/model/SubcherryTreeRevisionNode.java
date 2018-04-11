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

import com.subcherry.repository.core.LogEntry;

/**
 * Instances of this class represent {@link LogEntry}s displayed to the user.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryTreeRevisionNode implements SubcherryTreeNode {
	
	/**
	 * @see #getTicket()
	 */
	private final SubcherryTreeTicketNode _ticket;
	
	/**
	 * @see #getChange()
	 */
	private final LogEntry _change;
	
	/**
	 * @see #getState()
	 */
	private SubcherryTreeNode.Check _state;
	
	/**
	 * Create a {@link SubcherryTreeRevisionNode}.
	 * 
	 * @param ticket
	 *            see {@link #getTicket()}
	 * @param change
	 *            see {@link #getChange()}
	 */
	public SubcherryTreeRevisionNode(final SubcherryTreeTicketNode ticket, final LogEntry change) {
		_ticket = ticket;
		_change = change;
		_state = Check.CHECKED;
	}
	
	/**
	 * @return the {@link SubcherryTreeTicketNode} this {@link SubcherryTreeRevisionNode} belongs to
	 */
	public SubcherryTreeTicketNode getTicket() {
		return _ticket;
	}
	
	/**
	 * @return the {@link LogEntry} this {@link SubcherryTreeRevisionNode} represents
	 */
	public LogEntry getChange() {
		return _change;
	}
	
	@Override
	public SubcherryTreeNode.Check getState() {
		return _state;
	}
	
	@Override
	public void setState(final SubcherryTreeNode.Check state) {
		_state = state;
	}
}