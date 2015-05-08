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
package com.subcherry.repository.javahl.internal;

import static com.subcherry.repository.javahl.internal.Conversions.*;

import java.io.File;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.subversion.javahl.ClientException;
import org.apache.subversion.javahl.ISVNClient;
import org.apache.subversion.javahl.ISVNRepos;
import org.apache.subversion.javahl.SVNClient;
import org.apache.subversion.javahl.SVNRepos;
import org.apache.subversion.javahl.callback.CommitCallback;
import org.apache.subversion.javahl.callback.CommitMessageCallback;
import org.apache.subversion.javahl.callback.ListCallback;
import org.apache.subversion.javahl.callback.LogMessageCallback;
import org.apache.subversion.javahl.callback.StatusCallback;
import org.apache.subversion.javahl.types.ChangePath;
import org.apache.subversion.javahl.types.DirEntry;
import org.apache.subversion.javahl.types.Lock;
import org.apache.subversion.javahl.types.LogDate;
import org.apache.subversion.javahl.types.Status;

import com.subcherry.repository.LoginCredential;
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
import com.subcherry.repository.core.LogEntry;
import com.subcherry.repository.core.NodeProperties;
import com.subcherry.repository.core.PropertyData;
import com.subcherry.repository.core.PropertyValue;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.repository.core.RepositoryRuntimeException;
import com.subcherry.repository.core.RepositoryURL;
import com.subcherry.repository.core.Revision;
import com.subcherry.repository.core.RevisionRange;

public class HLClient implements Client {

	private static final String SVN_LOG = "svn:log";

	private static final String SVN_AUTHOR = "svn:author";

	private static final String SVN_DATE = "svn:date";

	private static final String[] DEFAULT_REVPROPS_ARRAY = { SVN_DATE, SVN_AUTHOR, SVN_LOG };

	private static final HashSet<String> DEFAULT_REVPROPS = new HashSet<String>(Arrays.asList(DEFAULT_REVPROPS_ARRAY));

	private ISVNClient _client;

	private ISVNRepos _repos;

	private HLClientManager _clientManager;

	public HLClient(HLClientManager clientManager, LoginCredential credentials) {
		_clientManager = clientManager;
		_client = new SVNClient();
		if (credentials != null) {
			_client.username(credentials.getUser());
			_client.password(credentials.getPasswd());
		}

		_repos = new SVNRepos();
	}

	public ISVNClient impl() {
		return _client;
	}

	@Override
	public OperationFactory getOperationsFactory() {
		return _clientManager.getOperationsFactory();
	}

	@Override
	public RepositoryURL createRepository(File path, String uuid,
			boolean enableRevisionProperties, boolean force)
			throws RepositoryException {
		boolean disableFsyncCommit = true;
		boolean keepLog = false;
		File configPath = null;
		String fstype = SVNRepos.FSFS;

		try {
			_repos.create(path, disableFsyncCommit, keepLog, configPath, fstype);
		} catch (ClientException ex) {
			throw wrap(ex);
		}

		return new RepositoryURL("file", "", -1, path.getAbsolutePath().replace(File.separatorChar, '/'));
	}

	@Override
	public CommitInfo commit(File[] paths, boolean keepLocks,
			final String commitMessage, NodeProperties revisionProperties,
			String[] changelists, boolean keepChangelist, boolean force,
			Depth depth) throws RepositoryException {

		boolean noUnlock = false;
		Map<String, String> revpropTable = unwrap(revisionProperties);
		LastCommitInfo callback = new LastCommitInfo();
		try {
			_client.commit(pathSet(paths), unwrap(depth), noUnlock,
				keepChangelist, wrap(changelists), revpropTable,
				wrapMessage(commitMessage), callback);
		} catch (ClientException ex) {
			throw wrap(ex);
		}
		return callback.getInfo();
	}

	@Override
	public CommitInfo mkDir(RepositoryURL[] urls, String commitMessage,
			NodeProperties revisionProperties, boolean makeParents)
			throws RepositoryException {
		LastCommitInfo callback = new LastCommitInfo();
		try {
			_client.mkdir(pathSet(urls), makeParents,
				unwrap(revisionProperties), wrapMessage(commitMessage),
				callback);
		} catch (ClientException ex) {
			throw wrap(ex);
		}
		return callback.getInfo();
	}

	@Override
	public CommitInfo importResource(File path, RepositoryURL dstURL,
			String commitMessage, NodeProperties revisionProperties,
			boolean useGlobalIgnores, boolean ignoreUnknownNodeTypes,
			Depth depth) throws RepositoryException {
		boolean noIgnore = false;
		CommitMessageCallback handler = wrapMessage(commitMessage);
		LastCommitInfo callback = new LastCommitInfo();
		try {
			_client.doImport(unwrap(path), unwrap(dstURL), unwrap(depth),
				noIgnore, ignoreUnknownNodeTypes,
				unwrap(revisionProperties), handler, callback);
		} catch (ClientException ex) {
			throw wrap(ex);
		}
		return callback.getInfo();
	}

	@Override
	public void update(File[] paths, Revision revision, Depth depth,
			boolean allowUnversionedObstructions, boolean depthIsSticky)
			throws RepositoryException {
		boolean makeParents = false;
		boolean ignoreExternals = false;
		try {
			_client.update(pathSet(paths), unwrap(revision), unwrap(depth),
				depthIsSticky, makeParents, ignoreExternals,
				allowUnversionedObstructions);
		} catch (ClientException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void checkout(RepositoryURL url, File dstPath, Revision pegRevision,
			Revision revision, Depth depth, boolean allowUnversionedObstructions)
			throws RepositoryException {
		boolean ignoreExternals = false;
		String moduleName = unwrap(url);
		try {
			_client.checkout(moduleName, unwrap(dstPath), unwrap(revision),
				unwrap(pegRevision), unwrap(depth), ignoreExternals,
				allowUnversionedObstructions);
		} catch (ClientException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public CommitInfo copy(CopySource[] sources, RepositoryURL dst,
			boolean isMove, boolean makeParents, boolean failWhenDstExists,
			String commitMessage, NodeProperties revisionProperties)
			throws RepositoryException {
		boolean copyAsChild = false;
		boolean ignoreExternals = false;
		CommitMessageCallback handler = wrapMessage(commitMessage);
		LastCommitInfo callback = new LastCommitInfo();
		try {
			_client.copy(unwrapSources(sources), unwrap(dst), copyAsChild,
				makeParents, ignoreExternals, unwrap(revisionProperties),
				handler, callback);
		} catch (ClientException ex) {
			throw wrap(ex);
		}
		return callback.getInfo();
	}

	@Override
	public void copy(CopySource[] sources, File dst, boolean isMove,
			boolean makeParents, boolean failWhenDstExists)
			throws RepositoryException {
		boolean copyAsChild = false;
		boolean ignoreExternals = false;
		try {
			_client.copy(unwrapSources(sources), unwrap(dst), copyAsChild,
				makeParents, ignoreExternals, null, null, null);
		} catch (ClientException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void diff(Target target, Revision startRev,
			Revision stopRev, Depth depth, boolean useAncestry,
			OutputStream result)
			throws RepositoryException {
		String relativeToDir = null;
		Collection<String> changelists = null;
		boolean ignoreAncestry = !useAncestry;
		boolean noDiffDeleted = false;
		boolean force = false;
		boolean copiesAsAdds = true;
		boolean ignoreProps = false;
		boolean propsOnly = false;
		try {
			_client.diff(unwrap(target), unwrap(target.getPegRevision()), unwrap(startRev),
				unwrap(stopRev), relativeToDir, result, unwrap(depth),
				changelists, ignoreAncestry, noDiffDeleted, force,
				copiesAsAdds, ignoreProps, propsOnly);
		} catch (ClientException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void merge(RepositoryURL url, Revision pegRevision,
			Collection<RevisionRange> rangesToMerge, File dstPath, Depth depth,
			boolean useAncestry, boolean force, boolean dryRun,
			boolean recordOnly) throws RepositoryException {
		boolean ignoreAncestry = !useAncestry;
		try {
			_client.merge(unwrap(url), unwrap(pegRevision),
				unwrapRanges(rangesToMerge), unwrap(dstPath), force,
				unwrap(depth), ignoreAncestry, dryRun, recordOnly);
		} catch (ClientException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void log(RepositoryURL url, String[] paths, Revision pegRevision,
			Revision startRevision, Revision endRevision, boolean stopOnCopy,
			boolean discoverChangedPaths, long limit,
			final LogEntryHandler handler) throws RepositoryException {
		log(url, paths, pegRevision, startRevision, endRevision, stopOnCopy, discoverChangedPaths, false, limit,
			DEFAULT_REVPROPS_ARRAY, handler);
	}

	@Override
	public void log(RepositoryURL url, String[] paths, Revision pegRevision,
			Revision startRevision, Revision endRevision, boolean stopOnCopy,
			boolean discoverChangedPaths, boolean includeMergedRevisions,
			long limit, String[] revisionProperties, final LogEntryHandler handler)
			throws RepositoryException {
		final LogFilter logFilter = new LogFilter(unwrap(url), paths);
		LogMessageCallback callback = new LogMessageCallback() {
			@Override
			public void singleMessage(Set<ChangePath> changedPaths,
					long revision, Map<String, byte[]> revprops,
					boolean hasChildren) {

				if (revprops == null) {
					revprops = Collections.emptyMap();
				}
				String author = binaryToString(revprops.get(SVN_AUTHOR));
				String message = binaryToString(revprops.get(SVN_LOG));
				Date date = binaryToDate(revprops.get(SVN_DATE));

				LogEntry logEntry = new LogEntry(wrap(changedPaths, logFilter), revision, author, date, message);
				try {
					handler.handleLogEntry(logEntry);
				} catch (RepositoryException ex) {
					throw new RepositoryRuntimeException(ex);
				}
			}

			private Date binaryToDate(byte[] data) {
				if (data != null) {
					long timeMicros;
					try {
						LogDate logDate = new LogDate(new String(data));
						timeMicros = logDate.getTimeMicros();
					} catch (ParseException ex) {
						timeMicros = 0;
					}
					return new Date(timeMicros / 1000);
				} else {
					return null;
				}
			}

			private String binaryToString(byte[] data) {
				if (data != null) {
					try {
						return new String(data, "utf-8");
					} catch (UnsupportedEncodingException e) {
						return new String(data);
					}
				} else {
					return null;
				}
			}
		};
		try {
			_client.logMessages(logFilter.getPrefixUrl(), unwrap(pegRevision),
				range(startRevision, endRevision), stopOnCopy,
				discoverChangedPaths, includeMergedRevisions, properties(revisionProperties),
				limit, callback);
		} catch (RepositoryRuntimeException ex) {
			throw ex.getCause();
		} catch (ClientException ex) {
			throw wrap(ex);
		}
	}

	private static Set<String> properties(String[] revisionProperties) {
		if (revisionProperties == null) {
			return DEFAULT_REVPROPS;
		}
		return set(revisionProperties);
	}

	@Override
	public void list(RepositoryURL url, Revision pegRevision, Revision revision, boolean fetchLocks, Depth depth,
			Kind entryFields, final DirEntryHandler handler) throws RepositoryException {
		ListCallback callback = new ListCallback() {
			@Override
			public void doEntry(DirEntry dirent, Lock lock) {
				try {
					handler.handleDirEntry(wrap(dirent));
				} catch (RepositoryException ex) {
					throw new RepositoryRuntimeException(ex);
				}
			}
		};
		try {
			_client.list(unwrap(url), unwrap(revision), unwrap(pegRevision), unwrap(depth), unwrap(entryFields),
				fetchLocks, callback);
		} catch (RepositoryRuntimeException ex) {
			throw ex.getCause();
		} catch (ClientException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void status(File path, Revision revision, Depth depth,
			boolean remote, boolean reportAll, boolean includeIgnored,
			final StatusHandler handler, Collection<String> changeLists) throws RepositoryException {
		boolean noIgnore = includeIgnored;
		boolean ignoreExternals = false;
		StatusCallback callback = new StatusCallback() {
			@Override
			public void doStatus(String statusPath, Status status) {
				try {
					handler.handleStatus(wrap(status));
				} catch (RepositoryException ex) {
					throw new RepositoryRuntimeException(ex);
				}
			}
		};
		try {
			_client.status(unwrap(path), unwrap(depth), remote, reportAll, noIgnore, ignoreExternals, changeLists,
				callback);
		} catch (RepositoryRuntimeException ex) {
			throw ex.getCause();
		} catch (ClientException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void add(File path, boolean force, boolean mkdir,
			boolean climbUnversionedParents, Depth depth,
			boolean includeIgnored, boolean makeParents)
			throws RepositoryException {
		try {
			_client.add(unwrap(path), unwrap(depth), force, includeIgnored, climbUnversionedParents);
		} catch (ClientException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void delete(File path, boolean force, boolean deleteFiles,
			boolean dryRun) throws RepositoryException {
		boolean keepLocal = !deleteFiles;
		Map<String, String> revpropTable = null;
		CommitMessageCallback handler = null;
		CommitCallback callback = null;
		try {
			_client.remove(Collections.singleton(unwrap(path)), force, keepLocal, revpropTable, handler, callback);
		} catch (ClientException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public PropertyData getProperty(File path, String propName,
			Revision pegRevision, Revision revision) throws RepositoryException {
		try {
			return wrap(propName, _client.propertyGet(unwrap(path), propName, unwrap(revision), unwrap(pegRevision)));
		} catch (ClientException ex) {
			throw wrap(ex);
		}
	}

	@Override
	public void setProperty(File path, String propName,
			PropertyValue propValue, boolean skipChecks, Depth depth,
			PropertyHandler handler, Collection<String> changeLists)
			throws RepositoryException {
		boolean force = false;
		try {
			_client.propertySetLocal(Collections.singleton(unwrap(path)), propName, unwrap(propValue), unwrap(depth),
				changeLists, force);
		} catch (ClientException ex) {
			throw wrap(ex);
		}
	}

}
