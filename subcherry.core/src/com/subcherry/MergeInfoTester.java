/*
 * SubCherry - Cherry Picking with Trac and Subversion
 * Copyright (C) 2016 Bernhard Haumacher and others
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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.subcherry.repository.command.ClientManager;
import com.subcherry.repository.core.MergeInfo;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.repository.core.RepositoryURL;
import com.subcherry.repository.core.Revision;
import com.subcherry.repository.core.RevisionRange;
import com.subcherry.repository.core.RevisionRanges;
import com.subcherry.repository.core.Target;

public class MergeInfoTester {

	private final ClientManager _clientManager;

	private final RepositoryURL _url;

	private final File _workspaceRoot;

	private final Revision _pegRevision;

	private final Map<String, MergeInfo> _moduleMergeInfos = new HashMap<>();

	public MergeInfoTester(ClientManager clientManager, RepositoryURL url, File workspaceRoot, Revision pegRevision) {
		_clientManager = clientManager;
		_url = url;
		_workspaceRoot = workspaceRoot;
		_pegRevision = pegRevision;
	}

	public boolean isAlreadyMerged(long revision, String modulePath, String moduleName) throws RepositoryException {
		MergeInfo moduleMergeInfo = lookupMergeInfo(moduleName);
		Target moduleUrl = Target.fromURL(_url.appendPath(modulePath), _pegRevision);
		Map<String, List<RevisionRange>> mergeInfoDiff =
			_clientManager.getClient().mergeInfoDiff(moduleUrl, revision);

		for (Entry<String, List<RevisionRange>> mergeEntry : mergeInfoDiff.entrySet()) {
			String mergedModulePath = mergeEntry.getKey();
			RepositoryURL mergedModuleUrl = _url.appendPath(mergedModulePath);

			List<RevisionRange> transitivelyMergedRevisions = moduleMergeInfo.getRevisions(mergedModuleUrl);
			if (transitivelyMergedRevisions == null) {
				continue;
			}
			if (RevisionRanges.containsAll(transitivelyMergedRevisions, mergeEntry.getValue())) {
				// This module has already been merged.
				return true;
			}
		}
		return false;
	}

	public MergeInfo lookupMergeInfo(String moduleName) throws RepositoryException {
		MergeInfo moduleMergeInfo;
		{
			File targetModuleFile = new File(_workspaceRoot, moduleName);
			moduleMergeInfo = _moduleMergeInfos.get(moduleName);
			if (moduleMergeInfo == null) {
				moduleMergeInfo =
					_clientManager.getClient().getMergeInfo(Target.fromFile(targetModuleFile));
				_moduleMergeInfos.put(moduleName, moduleMergeInfo);
			}
		}
		return moduleMergeInfo;
	}

}
