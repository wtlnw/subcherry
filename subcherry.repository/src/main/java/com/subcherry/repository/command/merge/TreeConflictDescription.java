/*
 * TimeCollect records time you spent on your development work.
 * Copyright (C) 2015 Bernhard Haumacher and others
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
package com.subcherry.repository.command.merge;


public class TreeConflictDescription extends ConflictDescription {

	private final ConflictAction _action;

	private final ConflictReason _reason;

	public TreeConflictDescription(ConflictAction action, ConflictReason reason) {
		_action = action;
		_reason = reason;
	}

	public ConflictAction getConflictAction() {
		return _action;
	}

	public ConflictReason getConflictReason() {
		return _reason;
	}

	@Override
	public Kind kind() {
		return Kind.TREE;
	}

	@Override
	public String toString() {
		return super.toString() + " (" + getConflictAction() + " but locally " + getConflictReason() + ")";
	}

}
