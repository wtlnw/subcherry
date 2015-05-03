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
package subcherry.repository.svnkit.internal;

import static subcherry.repository.svnkit.internal.Conversions.*;

import java.io.File;
import java.io.OutputStream;
import java.util.Collection;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNCopyClient;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;

import com.subcherry.repository.command.Client;
import com.subcherry.repository.command.OperationFactory;
import com.subcherry.repository.command.copy.CopySource;
import com.subcherry.repository.command.log.DirEntryHandler;
import com.subcherry.repository.command.log.LogEntryHandler;
import com.subcherry.repository.command.status.StatusHandler;
import com.subcherry.repository.command.wc.PropertyHandler;
import com.subcherry.repository.core.CommitInfo;
import com.subcherry.repository.core.Depth;
import com.subcherry.repository.core.DirEntry.Kind;
import com.subcherry.repository.core.NodeProperties;
import com.subcherry.repository.core.PropertyData;
import com.subcherry.repository.core.PropertyValue;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.repository.core.RepositoryURL;
import com.subcherry.repository.core.Revision;
import com.subcherry.repository.core.RevisionRange;

public class SKClient implements Client {

	private SKClientManager _clientManager;
	
	public SKClient(SKClientManager clientManager) {
		_clientManager = clientManager;
	}

	@Override
	public OperationFactory getOperationsFactory() {
		return _clientManager.getOperationsFactory();
	}

	@Override
	public void diff(RepositoryURL url, Revision pegRevision, Revision startRev,
			Revision stopRev, Depth depth, boolean useAncestry,
			OutputStream result) throws RepositoryException {
		SVNDiffClient diffClient = _clientManager.impl().getDiffClient();
		try {
			diffClient.doDiff(
				unwrap(url), 
				unwrap(pegRevision), unwrap(startRev), unwrap(stopRev), 
				unwrap(depth), useAncestry, result);
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void merge(RepositoryURL url, Revision pegRevision,
			Collection<RevisionRange> rangesToMerge, File dstPath,
			Depth depth, boolean useAncestry, boolean force, boolean dryRun, boolean recordOnly) throws RepositoryException {
		try {
			SVNDiffClient diffClient = _clientManager.impl().getDiffClient();
			diffClient.doMerge(
				unwrap(url), 
				unwrap(pegRevision), 
				unwrap(rangesToMerge), dstPath, unwrap(depth), 
				useAncestry, force, dryRun, recordOnly);
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public RepositoryURL createRepository(File path, String uuid, boolean enableRevisionProperties,
			boolean force) throws RepositoryException {
		SVNAdminClient adminClient = _clientManager.impl().getAdminClient();
		try {
			return wrap(adminClient.doCreateRepository(path, uuid, enableRevisionProperties, force));
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public CommitInfo commit(File[] paths, boolean keepLocks,
			String commitMessage, NodeProperties revisionProperties,
			String[] changelists, boolean keepChangelist, boolean force,
			Depth depth) throws RepositoryException {
		SVNCommitClient commitClient = _clientManager.impl().getCommitClient();
		try {
			return wrap(commitClient.doCommit(paths, keepLocks, commitMessage, unwrap(revisionProperties), changelists, keepChangelist, force, unwrap(depth)));
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public CommitInfo mkDir(RepositoryURL[] urls, String commitMessage,
			NodeProperties revisionProperties, boolean makeParents) throws RepositoryException {
		try {
			SVNCommitClient commitClient = _clientManager.impl().getCommitClient();
			return wrap(commitClient.doMkDir(unwrap(urls), commitMessage, unwrap(revisionProperties), makeParents));
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public CommitInfo importResource(File path, RepositoryURL dstURL,
			String commitMessage, NodeProperties revisionProperties, boolean useGlobalIgnores, boolean ignoreUnknownNodeTypes,
			Depth depth) throws RepositoryException {
		try {
			SVNCommitClient commitClient = _clientManager.impl().getCommitClient();
			return wrap(commitClient.doImport(path, unwrap(dstURL), commitMessage, unwrap(revisionProperties), useGlobalIgnores, ignoreUnknownNodeTypes, unwrap(depth)));
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void update(File[] paths, Revision revision, Depth depth,
			boolean allowUnversionedObstructions, boolean depthIsSticky) throws RepositoryException {
		try {
			SVNUpdateClient updateClient = _clientManager.impl().getUpdateClient();
			updateClient.doUpdate(paths, unwrap(revision), unwrap(depth), allowUnversionedObstructions, depthIsSticky);
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void checkout(RepositoryURL url, File dstPath, Revision pegRevision,
			Revision revision, Depth depth, boolean allowUnversionedObstructions) throws RepositoryException {
		try {
			SVNUpdateClient updateClient = _clientManager.impl().getUpdateClient();
			updateClient.doCheckout(unwrap(url), dstPath, unwrap(pegRevision), unwrap(revision), unwrap(depth), allowUnversionedObstructions);
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public CommitInfo copy(CopySource[] sources, RepositoryURL dst,
			boolean isMove, boolean makeParents, boolean failWhenDstExists, String commitMessage, NodeProperties revisionProperties) throws RepositoryException {
		try {
			SVNCopyClient copyClient = _clientManager.impl().getCopyClient();
			return wrap(copyClient.doCopy(unwrap(sources), unwrap(dst), isMove, makeParents, failWhenDstExists, commitMessage, unwrap(revisionProperties)));
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void copy(CopySource[] sources, File dst, boolean isMove, boolean makeParents,
			boolean failWhenDstExists) throws RepositoryException {
		try {
			SVNCopyClient copyClient = _clientManager.impl().getCopyClient();
			copyClient.doCopy(unwrap(sources), dst, isMove, makeParents, failWhenDstExists);
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void log(RepositoryURL url, String[] paths, Revision pegRevision,
			Revision startRevision, Revision endRevision, boolean stopOnCopy,
			boolean discoverChangedPaths, long limit, LogEntryHandler handler) throws RepositoryException {
		try {
			_clientManager.impl().getLogClient().doLog(unwrap(url), paths, unwrap(pegRevision), unwrap(startRevision), unwrap(endRevision), stopOnCopy, discoverChangedPaths, limit, adapt(handler));
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void log(RepositoryURL url, String[] paths,
			Revision pegRevision, Revision startRevision,
			Revision endRevision, boolean stopOnCopy, boolean discoverChangedPaths, boolean includeMergedRevisions,
			long limit, String[] revisionProperties, LogEntryHandler handler) throws RepositoryException {
		try {
			SVNLogClient logClient = _clientManager.impl().getLogClient();
			logClient.doLog(unwrap(url), paths, unwrap(pegRevision), unwrap(startRevision), unwrap(endRevision), stopOnCopy, discoverChangedPaths, includeMergedRevisions, limit, revisionProperties, adapt(handler));
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void list(RepositoryURL url, Revision pegRevision,
			Revision revision, boolean fetchLocks, Depth depth, Kind entryFields,
			DirEntryHandler handler) throws RepositoryException {
		try {
			SVNLogClient logClient = _clientManager.impl().getLogClient();
			logClient.doList(unwrap(url), unwrap(pegRevision), unwrap(revision), fetchLocks, unwrap(depth), unwrap(entryFields), adapt(handler));
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void status(File path, Revision revision, Depth depth,
			boolean remote, boolean reportAll, boolean includeIgnored, StatusHandler handler, Collection<String> changeLists) throws RepositoryException {
		try {
			SVNStatusClient statusClient = _clientManager.impl().getStatusClient();
			boolean collectParentExternals = false;
			statusClient.doStatus(path, unwrap(revision), unwrap(depth), remote, reportAll, includeIgnored, collectParentExternals, adapt(handler), changeLists);
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void add(File path, boolean force, boolean mkdir, boolean climbUnversionedParents, Depth depth,
			boolean includeIgnored, boolean makeParents) throws RepositoryException {
		try {
			SVNWCClient wcClient = _clientManager.impl().getWCClient();
			wcClient.doAdd(path, force, mkdir, climbUnversionedParents, unwrap(depth), includeIgnored, makeParents);
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void delete(File path, boolean force, boolean deleteFiles, boolean dryRun) throws RepositoryException {
		try {
			SVNWCClient wcClient = _clientManager.impl().getWCClient();
			wcClient.doDelete(path, force, deleteFiles, dryRun);
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public PropertyData getProperty(File path, String propName,
			Revision pegRevision, Revision revision) throws RepositoryException {
		try {
			SVNWCClient wcClient = _clientManager.impl().getWCClient();
			return wrap(wcClient.doGetProperty(path, propName, unwrap(pegRevision), unwrap(revision)));
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void setProperty(File path, String propName, PropertyValue propValue,
			boolean skipChecks, Depth depth, PropertyHandler handler, Collection<String> changeLists) throws RepositoryException {
		try {
			SVNWCClient wcClient = _clientManager.impl().getWCClient();
			wcClient.doSetProperty(path, propName, unwrap(propValue), skipChecks, unwrap(depth), adapt(handler), changeLists);
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

}
