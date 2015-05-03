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

public class LogEntryPath {

	private String _path;

	private ChangeType _changeType;

	private String _copyPath;

	private long _copyRevision;

	private NodeKind _nodeKind;

	public LogEntryPath(NodeKind nodeKind, String path, ChangeType changeType, String copyPath, long copyRevision) {
		_nodeKind = nodeKind;
		_path = path;
		_changeType = changeType;
		_copyPath = copyPath;
		_copyRevision = copyRevision;
	}

	public long getCopyRevision() {
		return _copyRevision;
	}

	public ChangeType getType() {
		return _changeType;
	}

	public NodeKind getKind() {
		return _nodeKind;
	}

	public String getPath() {
		return _path;
	}

	public String getCopyPath() {
		return _copyPath;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getType());
		result.append(" ");
		result.append(getKind());
		result.append(" ");
		result.append(getPath());
		if (getCopyPath() != null) {
			result.append(" (copied from ");
			result.append(getCopyPath());
			result.append("@");
			result.append(getCopyRevision());
			result.append(")");
		}
		return result.toString();
	}

}
