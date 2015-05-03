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
package com.subcherry.repository.core;


public class RevisionRange {

	private Revision _start;

	private Revision _end;

	public RevisionRange(Revision start, Revision end) {
		_start = start;
		_end = end;
	}

	public Revision getStart() {
		return _start;
	}

	public Revision getEnd() {
		return _end;
	}

	public static RevisionRange create(Revision start, Revision end) {
		return new RevisionRange(start, end);
	}

	@Override
	public String toString() {
		return getStart() + ":" + getEnd();
	}
}
