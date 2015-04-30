/*
 * SubCherry - Cherry Picking with Trac and Subversion
 * Copyright (C) 2014 Bernhard Haumacher and others
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
package com.subcherry.utils;

import java.util.Iterator;

/**
 * @version   $Revision$  $Author$  $Date$
 */
public class StringSource {

	private Iterator<String> it;
	private int line;
	private String content;
	
	public StringSource(Iterable<String> lines) {
		it = lines.iterator();
	}
	
	public String next() {
		if (it.hasNext()) {
			content = it.next();
			line++;
		} else {
			content = null;
		}
		return content;
	}
	
	public String last() {
		return content;
	}

	public boolean has() {
		return content != null;
	}

	public int getLine() {
		return line;
	}
}

