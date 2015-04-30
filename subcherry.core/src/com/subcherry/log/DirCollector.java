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

import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * @version   $Revision$  $Author$  $Date$
 */
public class DirCollector implements ISVNDirEntryHandler {
	private List<String> dirs = new ArrayList<String>();

	@Override
	public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {
		if (dirEntry.getKind() == SVNNodeKind.DIR) {
			String path = dirEntry.getRelativePath();
			if (path.isEmpty()) {
				return;
			}
			
			dirs.add(path);
			TicketsOnBranch.LOG.fine("Branch module: " + path);
		}
	}

	public List<String> listDirs(SVNLogClient logClient, SVNURL url, SVNRevision revision) throws SVNException {
		logClient.doList(url, revision, revision, false, SVNDepth.IMMEDIATES, SVNDirEntry.DIRENT_KIND, this);
		return getDirs();
	}
	
	private List<String> getDirs() {
		return dirs;
	}
	
	public static Set<String> getBranchModules(SVNLogClient logClient,
			String[] configuredModules, SVNURL branchUrl1, SVNRevision revision)
			throws SVNException {
		List<String> moduleDirs1 = new DirCollector().listDirs(logClient, branchUrl1, revision);
		Set<String> modules = new HashSet<String>(moduleDirs1);
		if (configuredModules.length > 0) {
			modules.retainAll(Arrays.asList(configuredModules));
		}
		return modules;
	}
	
}
