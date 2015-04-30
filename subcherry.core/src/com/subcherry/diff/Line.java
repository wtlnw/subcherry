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
package com.subcherry.diff;

import java.io.PrintWriter;

/**
 * @version   $Revision$  $Author$  $Date$
 */
public class Line {

	private final Operation operation;
	private final String content;

	public Line(Operation operation, String content) {
		this.operation = operation;
		this.content = content;
	}

	public Operation getOperation() {
		return operation;
	}

	public String getContent() {
		return content;
	}

	public void print(PrintWriter out) {
		switch (operation) {
			case TAKE: {
				out.print(" ");
				break;
			}
			case ADD: {
				out.print("+");
				break;
			}
			case DELETE: {
				out.print("-");
				break;
			}
		}
		out.println(content);
	}
}

