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
package com.subcherry.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.subcherry.history.Node.Kind;
import com.subcherry.repository.command.log.LogEntryHandler;
import com.subcherry.repository.core.ChangeType;
import com.subcherry.repository.core.LogEntry;
import com.subcherry.repository.core.LogEntryPath;
import com.subcherry.repository.core.RepositoryException;

/**
 * {@link LogEntryHandler} that creates a consolidated history of all nodes that are present in
 * the current view of a repository.
 * 
 * <p>
 * The {@link HistroyBuilder} is feed with a SVN log starting with the current version (for which
 * the history view should be built) back to some former revision. During this scan, it records
 * history for all paths consolidating potential moves. In the generated {@link #getHistory()}, all
 * changes are mapped to current paths.
 * </p>
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 * @version $Revision$ $Author$ $Date$
 */
public class HistroyBuilder implements LogEntryHandler {

	private static final Comparator<LogEntryPath> PATH_ORDER = new Comparator<LogEntryPath>() {
		@Override
		public int compare(LogEntryPath p1, LogEntryPath p2) {
			return p1.getPath().compareTo(p2.getPath());
		}
	};

	private final History _history;

	public HistroyBuilder(long startRevision) {
		_history = new History(startRevision);
	}

	public History getHistory() {
		return _history;
	}

	@Override
	public void handleLogEntry(LogEntry logEntry) throws RepositoryException {
		Change change = createChange(logEntry);

		ArrayList<LogEntryPath> paths = new ArrayList<>(logEntry.getChangedPaths().values());
		Collections.sort(paths, PATH_ORDER);
		for (LogEntryPath pathEntry : paths) {
			String path = pathEntry.getPath();

			ChangeType changeType = pathEntry.getType();
			Kind kind = Kind.fromSvn(pathEntry.getKind());
			switch (changeType) {
				case ADDED: {
					_history.addedNode(kind, path, change, pathEntry.getCopyPath(), pathEntry.getCopyRevision());
					break;
				}
				case DELETED: {
					_history.deletedNode(kind, path, change);
					break;
				}
				case MODIFIED: {
					_history.modifiedNode(kind, path, change);
					break;
				}
				case REPLACED: {
					_history.deletedNode(kind, path, change);
					_history.addedNode(kind, path, change, pathEntry.getCopyPath(), pathEntry.getCopyRevision());
					break;
				}
			}

		}
	}

	private Change createChange(LogEntry logEntry) {
		Change change =
			_history.createChange(logEntry.getRevision(), logEntry.getAuthor(), logEntry.getDate(),
				logEntry.getMessage());
		return change;
	}

}
