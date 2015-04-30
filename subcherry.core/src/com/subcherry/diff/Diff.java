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
import java.util.ArrayList;
import java.util.List;

/**
 * @version   $Revision$  $Author$  $Date$
 */
public class Diff {

	public static final String HEADER_SEPARATOR = "===================================================================";
	
	private final String name1;
	private final long revision1;
	private final String name2;
	private final long revision2;
	private final List<Chunk> chunks = new ArrayList<Chunk>();

	public Diff(String name1, long revision1, String name2, long revision2) {
		this.name1 = name1;
		this.revision1 = revision1;
		this.name2 = name2;
		this.revision2 = revision2;
	}

	public void add(Chunk chunk) {
		chunks.add(chunk);
	}
	
	public List<Chunk> getChunks() {
		return chunks;
	}

	public void print(PrintWriter out) {
		out.println(HEADER_SEPARATOR);
		
		out.print("--- ");
		out.print(name1);
		out.print("\t(revision ");
		out.print(revision1);
		out.println(")");
		
		out.print("+++ ");
		out.print(name2);
		out.print("\t(revision ");
		out.print(revision2);
		out.println(")");
		
		for (Chunk chunk : chunks) {
			chunk.print(out);
		}
	}

}

