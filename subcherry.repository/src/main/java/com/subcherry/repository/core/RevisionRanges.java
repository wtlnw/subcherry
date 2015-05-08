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

import java.util.List;

public class RevisionRanges {

	public static boolean containsAll(List<RevisionRange> ranges, List<RevisionRange> values) {
		for (RevisionRange value : values) {
			if (!containsAll(ranges, value)) {
				return false;
			}
		}
		return true;
	}

	public static boolean containsAll(List<RevisionRange> ranges, RevisionRange value) {
		// TODO: Optimize:
		for (long rev = value.getStart().getNumber(), end = value.getEnd().getNumber(); rev <= end; rev++) {
			if (!contains(ranges, rev)) {
				return false;
			}
		}
		return true;
	}

	public static boolean contains(List<RevisionRange> ranges, long rev) {
		// TODO: Optimize:
		for (RevisionRange range : ranges) {
			if (contains(range, rev)) {
				return true;
			}
		}
		return false;
	}

	public static boolean contains(RevisionRange range, long rev) {
		return range.getStart().getNumber() <= rev && rev <= range.getEnd().getNumber();
	}

}
