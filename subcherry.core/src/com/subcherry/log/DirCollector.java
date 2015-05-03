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
package com.subcherry.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.subcherry.repository.command.Client;
import com.subcherry.repository.command.log.DirEntryHandler;
import com.subcherry.repository.core.Depth;
import com.subcherry.repository.core.DirEntry;
import com.subcherry.repository.core.NodeKind;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.repository.core.RepositoryURL;
import com.subcherry.repository.core.Revision;

/**
 * @version   $Revision$  $Author$  $Date$
 */
public class DirCollector implements DirEntryHandler {
	private List<String> dirs = new ArrayList<String>();

	@Override
	public void handleDirEntry(DirEntry dirEntry) throws RepositoryException {
		if (dirEntry.getNodeKind() == NodeKind.DIR) {
			String path = dirEntry.getRelativePath();
			if (path.isEmpty()) {
				return;
			}
			
			dirs.add(path);
		}
	}

	public List<String> listDirs(Client logClient, RepositoryURL url, Revision revision) throws RepositoryException {
		logClient.list(url, revision, revision, false, Depth.IMMEDIATES, DirEntry.Kind.DIRENT, this);
		return getDirs();
	}
	
	private List<String> getDirs() {
		return dirs;
	}
	
	public static Set<String> getBranchModules(Client logClient,
			String[] configuredModules, RepositoryURL branchUrl1, Revision revision)
			throws RepositoryException {
		List<String> moduleDirs1 = new DirCollector().listDirs(logClient, branchUrl1, revision);
		Set<String> modules = new HashSet<String>(moduleDirs1);
		if (configuredModules.length > 0) {
			modules.retainAll(Arrays.asList(configuredModules));
		}
		return modules;
	}
	
}
