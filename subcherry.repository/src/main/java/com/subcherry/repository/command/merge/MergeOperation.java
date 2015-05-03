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
package com.subcherry.repository.command.merge;

import java.util.Collection;
import java.util.Set;

import com.subcherry.repository.command.Command;

/**
 * @version $Revision$ $Author$ $Date$
 */
public class MergeOperation {

	private final Collection<Command> _operations;

	private final long _revision;

	private Set<String> _touchedResources;

	public MergeOperation(long revision, Collection<Command> operations, Set<String> touchedResources) {
		_revision = revision;
		_operations = operations;
		_touchedResources = touchedResources;
	}

	/**
	 * The single revision to merge.
	 */
	public long getRevision() {
		return _revision;
	}

	/**
	 * The resources for which a merge is required.
	 */
	public Collection<Command> getCommands() {
		return _operations;
	}

	/**
	 * The resources (workspace-relative paths) that must be committed after applying this merge.
	 */
	public Set<String> getTouchedResources() {
		return _touchedResources;
	}

	/**
	 * Whether this is a no-op.
	 */
	public boolean isEmpty() {
		return getCommands().isEmpty();
	}

}
