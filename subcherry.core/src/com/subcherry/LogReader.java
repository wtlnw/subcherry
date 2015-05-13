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

import com.subcherry.repository.command.Client;
import com.subcherry.repository.command.log.LogEntryHandler;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.repository.core.RepositoryURL;
import com.subcherry.repository.core.Revision;

public class LogReader {

	private final Client _logClient;

	private final RepositoryURL _url;

	private Revision _startRevision;

	private Revision _endRevision;

	private Revision _pegRevision;

	private boolean _stopOnCopy;

	private boolean _discoverChangedPaths;

	private long _limit;

	public LogReader(Client logClient, RepositoryURL url) {
		_logClient = logClient;
		_url = url;
	}

	public Revision getStartRevision() {
		return _startRevision;
	}

	public void setStartRevision(Revision startRevision) {
		_startRevision = startRevision;
	}

	public Revision getEndRevision() {
		return _endRevision;
	}

	public void setEndRevision(Revision endRevision) {
		_endRevision = endRevision;
	}

	public Revision getPegRevision() {
		return _pegRevision;
	}

	public void setPegRevision(Revision pegRevision) {
		_pegRevision = pegRevision;
	}

	public boolean isStopOnCopy() {
		return _stopOnCopy;
	}

	public void setStopOnCopy(boolean stopOnCopy) {
		_stopOnCopy = stopOnCopy;
	}

	public boolean isDiscoverChangedPaths() {
		return _discoverChangedPaths;
	}

	public void setDiscoverChangedPaths(boolean discoverChangedPaths) {
		_discoverChangedPaths = discoverChangedPaths;
	}

	public long getLimit() {
		return _limit;
	}

	public void setLimit(long limit) {
		_limit = limit;
	}

	public void readLog(String[] paths, LogEntryHandler logTarget) throws RepositoryException {
		_logClient.log(_url, paths, _pegRevision, _startRevision, _endRevision, _stopOnCopy, _discoverChangedPaths,
			_limit, logTarget);
	}

}
