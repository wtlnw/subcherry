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
package com.subcherry.merge;

import com.subcherry.repository.core.LogEntry;
import com.subcherry.utils.Path;

/**
 * Description of the change of a single resource (file or directory) in a certain change set.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 * @version $Revision$ $Author$ $Date$
 */
public class ResourceChange {

	private final LogEntry _changeSet;

	private final Path _change;

	public ResourceChange(LogEntry logEntry, Path change) {
		_changeSet = logEntry;
		_change = change;
	}

	/**
	 * The change set.
	 */
	public LogEntry getChangeSet() {
		return _changeSet;
	}

	/**
	 * The change within the {@link #getChangeSet()}.
	 */
	public Path getChange() {
		return _change;
	}

}
