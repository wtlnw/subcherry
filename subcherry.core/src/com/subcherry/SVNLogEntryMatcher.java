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

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;

/**
 * @version   $Revision$  $Author$  $Date$
 */
public abstract class SVNLogEntryMatcher implements ISVNLogEntryHandler {
	
	private final class AllEntries extends SVNLogEntryMatcher {
		@Override
		public boolean matches(SVNLogEntry logEntry) {
			return true;
		}
	}

	private List<SVNLogEntry> _entries = new ArrayList<SVNLogEntry>();
	
	protected abstract boolean matches(SVNLogEntry logEntry);

	public List<SVNLogEntry> getEntries() {
		return _entries;
	}
	
	@Override
	public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
		if (matches(logEntry)) {
			_entries.add(logEntry);
		}
	}

	public void forward(ISVNLogEntryHandler mergeCommitHandler) throws SVNException {
		for (SVNLogEntry entry : getEntries()) {
			mergeCommitHandler.handleLogEntry(entry);
		}
	}
}

