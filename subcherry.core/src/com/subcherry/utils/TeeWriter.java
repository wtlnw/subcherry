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

import java.io.IOException;
import java.io.Writer;

public class TeeWriter extends Writer {

	private final Writer _writer1;
	private final Writer _writer2;

	public TeeWriter(Writer writer1, Writer writer2) {
		_writer1 = writer1;
		_writer2 = writer2;
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		_writer1.write(cbuf, off, len);
		_writer2.write(cbuf, off, len);
	}

	@Override
	public void flush() throws IOException {
		_writer1.flush();
		_writer2.flush();
	}

	@Override
	public void close() throws IOException {
		try {
			_writer1.close();
		} finally {
			_writer2.close();
		}
		
	}

}
