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

import com.subcherry.repository.command.log.LogEntryHandler;
import com.subcherry.repository.core.LogEntry;
import com.subcherry.repository.core.RepositoryException;

public class CombinedLogEntryHandler implements LogEntryHandler {

	private LogEntryHandler _handler1;

	private LogEntryHandler _handler2;

	public CombinedLogEntryHandler(LogEntryHandler handler1, LogEntryHandler handler2) {
		_handler1 = handler1;
		_handler2 = handler2;
	}

	@Override
	public void handleLogEntry(LogEntry logEntry) throws RepositoryException {
		_handler1.handleLogEntry(logEntry);
		_handler2.handleLogEntry(logEntry);
	}

}
