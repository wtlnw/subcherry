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
package com.subcherry.repository.core;


public class RevisionRange {

	private Revision _start;

	private Revision _end;

	public RevisionRange(Revision start, Revision end) {
		_start = start;
		_end = end;
	}

	/**
	 * Start revision of the range (exclusive).
	 */
	public Revision getStart() {
		return _start;
	}

	/**
	 * Last revision of the range (inclusive).
	 */
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_end == null) ? 0 : _end.hashCode());
		result = prime * result + ((_start == null) ? 0 : _start.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RevisionRange other = (RevisionRange) obj;
		if (_end == null) {
			if (other._end != null)
				return false;
		} else if (!_end.equals(other._end))
			return false;
		if (_start == null) {
			if (other._start != null)
				return false;
		} else if (!_start.equals(other._start))
			return false;
		return true;
	}

}
