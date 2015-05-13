/*
 * SubCherry - Cherry Picking with Trac and Subversion
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

public abstract class ConflictDescription {

	public enum Kind {
		TEXT, TREE, PROPERTY;
	}

	public abstract Kind kind();

	public final boolean isPropertyConflict() {
		return kind() == Kind.PROPERTY;
	}

	public final boolean isTextConflict() {
		return kind() == Kind.TEXT;
	}

	public boolean isTreeConflict() {
		return kind() == Kind.TREE;
	}

	@Override
	public String toString() {
		return kind() + " conflict";
	}

}
