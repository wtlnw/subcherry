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
package com.subcherry;

import java.util.ArrayList;
import java.util.List;

import com.subcherry.repository.command.log.LogEntryHandler;
import com.subcherry.repository.core.LogEntry;
import com.subcherry.repository.core.RepositoryException;

/**
 * @version   $Revision$  $Author$  $Date$
 */
public abstract class SVNLogEntryMatcher implements LogEntryHandler {
	
	private final class AllEntries extends SVNLogEntryMatcher {
		@Override
		public boolean matches(LogEntry logEntry) {
			return true;
		}
	}

	private List<LogEntry> _entries = new ArrayList<LogEntry>();
	
	protected abstract boolean matches(LogEntry logEntry);

	public List<LogEntry> getEntries() {
		return _entries;
	}
	
	@Override
	public void handleLogEntry(LogEntry logEntry) throws RepositoryException {
		if (matches(logEntry)) {
			_entries.add(logEntry);
		}
	}

	public void forward(LogEntryHandler mergeCommitHandler) throws RepositoryException {
		for (LogEntry entry : getEntries()) {
			mergeCommitHandler.handleLogEntry(entry);
		}
	}
}

