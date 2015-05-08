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

import java.util.Date;
import java.util.Map;

public class LogEntry {

	private Map<String, LogEntryPath> _changedPaths;

	private long _revision;

	private String _author;

	private Date _date;

	private String _message;

	private final boolean _hasChildren;

	public LogEntry(Map<String, LogEntryPath> changedPaths, long revision, String author, Date date,
			String message, boolean hasChildren) {
		_changedPaths = changedPaths;
		_revision = revision;
		_author = author;
		_date = date;
		_message = message;
		_hasChildren = hasChildren;
	}

	public Map<String, LogEntryPath> getChangedPaths() {
		return _changedPaths;
	}

	public long getRevision() {
		return _revision;
	}

	public String getMessage() {
		return _message;
	}

	public String getAuthor() {
		return _author;
	}

	public Date getDate() {
		return _date;
	}

	@Override
	public String toString() {
		return getRevision() + " (" + getDate() + ", " + getAuthor() + "): " + getMessage();
	}
}
