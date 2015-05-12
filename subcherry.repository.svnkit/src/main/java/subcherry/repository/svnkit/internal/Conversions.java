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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNMergeRange;
import org.tmatesoft.svn.core.SVNMergeRangeList;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.ISVNPropertyHandler;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNConflictAction;
import org.tmatesoft.svn.core.wc.SVNConflictChoice;
import org.tmatesoft.svn.core.wc.SVNConflictDescription;
import org.tmatesoft.svn.core.wc.SVNConflictReason;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNDiffOptions;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNRevisionRange;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.SvnCopySource;
import org.tmatesoft.svn.core.wc2.SvnRevisionRange;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import com.subcherry.repository.command.copy.CopySource;
import com.subcherry.repository.command.diff.DiffOptions;
import com.subcherry.repository.command.log.DirEntryHandler;
import com.subcherry.repository.command.log.LogEntryHandler;
import com.subcherry.repository.command.merge.ConflictAction;
import com.subcherry.repository.command.merge.ConflictDescription;
import com.subcherry.repository.command.merge.ConflictReason;
import com.subcherry.repository.command.merge.PropertyConflict;
import com.subcherry.repository.command.merge.TextConflict;
import com.subcherry.repository.command.merge.TreeConflictDescription;
import com.subcherry.repository.command.status.Status;
import com.subcherry.repository.command.status.StatusHandler;
import com.subcherry.repository.command.status.StatusType;
import com.subcherry.repository.command.wc.PropertyHandler;
import com.subcherry.repository.core.BinaryValue;
import com.subcherry.repository.core.ChangeType;
import com.subcherry.repository.core.CommitInfo;
import com.subcherry.repository.core.Depth;
import com.subcherry.repository.core.DirEntry;
import com.subcherry.repository.core.LogEntry;
import com.subcherry.repository.core.LogEntryPath;
import com.subcherry.repository.core.MergeInfo;
import com.subcherry.repository.core.NodeKind;
import com.subcherry.repository.core.NodeProperties;
import com.subcherry.repository.core.PropertyData;
import com.subcherry.repository.core.PropertyValue;
import com.subcherry.repository.core.PropertyValue.Kind;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.repository.core.RepositoryURL;
import com.subcherry.repository.core.Resolution;
import com.subcherry.repository.core.Revision;
import com.subcherry.repository.core.RevisionRange;
import com.subcherry.repository.core.StringValue;
import com.subcherry.repository.core.Target;
import com.subcherry.repository.core.Target.FileTarget;
import com.subcherry.repository.core.Target.UrlTarget;

public class Conversions {

	private static final Map<SVNConflictAction, ConflictAction> ACTIONS = actions();
	private static final Map<SVNConflictReason, ConflictReason> REASONS = reasons();
	private static final Map<SVNNodeKind, NodeKind> NODE_KINDS = nodeKindMap();

	public static RepositoryException wrap(SVNException ex) {
		return new RepositoryException(ex);
	}

	public static SVNException unwrap(RepositoryException ex) {
		Throwable cause = ex.getCause();
		if (cause instanceof SVNException) {
			return (SVNException) cause;
		} else {
			return new SVNException(SVNErrorMessage.UNKNOWN_ERROR_MESSAGE, ex);
		}
	}

	public static SVNStatusType unwrap(StatusType statusType) {
		switch (statusType) {
		case CONFLICTED:
			return SVNStatusType.CONFLICTED;
		case MERGED:
			return SVNStatusType.MERGED;
		}
		throw new UnsupportedOperationException("No such status type: "
				+ statusType);
	}

	public static SVNRevision unwrap(Revision revision) {
		if (revision == null) {
			return null;
		}
		switch (revision.kind()) {
		case COMMIT:
			return SVNRevision.create(revision.getNumber());
		case BASE:
			return SVNRevision.BASE;
		case HEAD:
			return SVNRevision.HEAD;
		case WORKING:
			return SVNRevision.WORKING;
		case UNDEFINED:
			return SVNRevision.UNDEFINED;
		}
		throw new UnsupportedOperationException("No such revision kind: "
				+ revision.kind());
	}

	public static Collection<SVNRevisionRange> unwrap(
			Collection<RevisionRange> ranges) {
		ArrayList<SVNRevisionRange> result = new ArrayList<>(ranges.size());
		for (RevisionRange range : ranges) {
			result.add(unwrap(range));
		}
		return result;
	}

	public static SVNRevisionRange unwrap(RevisionRange range) {
		return new SVNRevisionRange(unwrap(range.getStart()),
				unwrap(range.getEnd()));
	}

	public static SvnRevisionRange unwrap2(RevisionRange range) {
		return SvnRevisionRange.create(unwrap(range.getStart()),
				unwrap(range.getEnd()));
	}

	public static SVNDepth unwrap(Depth depth) {
		switch (depth) {
		case EMPTY:
			return SVNDepth.EMPTY;
		case FILES:
			return SVNDepth.FILES;
		case IMMEDIATES:
			return SVNDepth.IMMEDIATES;
		case INFINITY:
			return SVNDepth.INFINITY;
		case UNKNOWN:
			return SVNDepth.UNKNOWN;
		}
		throw new UnsupportedOperationException("No such depth: " + depth);
	}

	public static SvnTarget unwrap(Target target) {
		Target.Kind kind = target.kind();
		switch (kind) {
		case FILE:
				return SvnTarget.fromFile(unwrapFile(target), unwrap(target.getPegRevision()));
		case URL:
				return SvnTarget.fromURL(unwrap(unwrapUrl(target)), unwrap(target.getPegRevision()));
		}
		throw new UnsupportedOperationException("No such target kind: " + kind);
	}

	public static RepositoryURL unwrapUrl(Target target) {
		return ((UrlTarget) target).getUrl();
	}

	public static File unwrapFile(Target target) {
		return ((FileTarget) target).getFile();
	}

	public static SVNConflictAction unwrap(ConflictAction value) {
		switch (value) {
		case ADDED:
			return SVNConflictAction.ADD;
		case EDITED:
			return SVNConflictAction.EDIT;
			case DELETED:
				return SVNConflictAction.DELETE;
		}
		throw new UnsupportedOperationException("No such action: " + value);
	}

	public static SVNConflictReason unwrap(ConflictReason value) {
		switch (value) {
			case MISSING:
				return SVNConflictReason.MISSING;
			case OBSTRUCTED:
				return SVNConflictReason.OBSTRUCTED;
		}
		throw new UnsupportedOperationException("No such reason: " + value);
	}

	public static RepositoryURL wrap(SVNURL url) {
		return new RepositoryURL(url.getProtocol(), url.getHost(),
				url.getPort(), url.getPath());
	}

	public static SVNURL unwrap(RepositoryURL url) {
		try {
			String protocol = url.getProtocol();
			String userInfo = null;
			String host = url.getHost();
			int port = url.getPort();
			String path = "/" + url.getPath();
			boolean uriEncoded = true;
			return SVNURL.create(protocol, userInfo, host, port, path,
					uriEncoded);
		} catch (SVNException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	public static DiffOptions wrap(SVNDiffOptions options) {
		return new DiffOptions() {
		};
	}

	public static SVNProperties unwrap(NodeProperties properties) {
		SVNProperties result = new SVNProperties();
		// TODO: Convert.
		return result;
	}

	public static CommitInfo wrap(final SVNCommitInfo info) {
		return new CommitInfo() {
			@Override
			public long getNewRevision() {
				return info.getNewRevision();
			}
		};
	}

	public static SVNURL[] unwrap(RepositoryURL[] values) throws SVNException {
		SVNURL[] result = new SVNURL[values.length];
		for (int n = 0, cnt = values.length; n < cnt; n++) {
			result[n] = unwrap(values[n]);
		}
		return result;
	}

	public static SVNCopySource[] unwrap(CopySource[] values)
			throws SVNException {
		SVNCopySource[] result = new SVNCopySource[values.length];
		for (int n = 0, cnt = values.length; n < cnt; n++) {
			result[n] = unwrap(values[n]);
		}
		return result;
	}

	public static SVNCopySource unwrap(CopySource value) {
		Target target = value.getTarget();
		SVNRevision pegRevision = unwrap(target.getPegRevision());
		SVNRevision revision = unwrap(value.getRevision());
		Target.Kind kind = target.kind();
		switch (kind) {
		case FILE:
			File path = unwrapFile(target);
			return new SVNCopySource(pegRevision, revision, path);
		case URL:
				RepositoryURL url = unwrapUrl(target);
			return new SVNCopySource(pegRevision, revision, unwrap(url));
		}
		throw new UnsupportedOperationException("No such target kind: " + kind);
	}

	public static SvnCopySource unwrap2(CopySource value) {
		return SvnCopySource.create(unwrap(value.getTarget()),
				unwrap(value.getRevision()));
	}

	public static int unwrap(DirEntry.Kind entryField) {
		switch (entryField) {
		case DIRENT:
			return SVNDirEntry.DIRENT_KIND;
		}
		throw new UnsupportedOperationException("No such dir entry kind: "
				+ entryField);
	}

	public static PropertyData wrap(SVNPropertyData value) {
		if (value == null) {
			return null;
		}
		return new PropertyData(value.getName(), wrap(value.getValue()));
	}

	private static PropertyValue wrap(SVNPropertyValue value) {
		if (value.isBinary()) {
			return PropertyValue.create(value.getBytes());
		} else {
			return PropertyValue.create(value.getString());
		}
	}

	public static SVNPropertyValue unwrap(PropertyValue value) {
		Kind kind = value.kind();
		switch (kind) {
		case BINARY:
			byte[] bytes = ((BinaryValue) value).getBytes();
			return SVNPropertyValue.create(null, bytes, 0, bytes.length);
		case STRING:
			return SVNPropertyValue.create(((StringValue) value).getString());
		}
		throw new UnsupportedOperationException("No such property value kind: "
				+ kind);
	}

	public static DirEntry wrap(final SVNDirEntry dirEntry) {
		return new DirEntry() {

			@Override
			public NodeKind getNodeKind() {
				return wrap(dirEntry.getKind());
			}

			@Override
			public String getRelativePath() {
				return dirEntry.getRelativePath();
			}

		};
	}

	public static NodeKind wrap(SVNNodeKind kind) {
		return NODE_KINDS.get(kind);
	}

	private static HashMap<SVNNodeKind, NodeKind> nodeKindMap() {
		HashMap<SVNNodeKind, NodeKind> result = new HashMap<SVNNodeKind, NodeKind>();
		result.put(SVNNodeKind.DIR, NodeKind.DIR);
		result.put(SVNNodeKind.FILE, NodeKind.FILE);
		result.put(SVNNodeKind.NONE, NodeKind.NONE);
		result.put(SVNNodeKind.UNKNOWN, NodeKind.UNKNOWN);
		return result;
	}

	public static List<ConflictDescription> wrapConflicts(
			List<SVNConflictDescription> values) {
		ArrayList<ConflictDescription> result = new ArrayList<>();
		for (SVNConflictDescription value : values) {
			result.add(wrap(value));
		}
		return result;
	}

	private static ConflictDescription wrap(final SVNConflictDescription value) {
		if (value.isTreeConflict()) {
			return new TreeConflictDescription(wrap(value.getConflictAction()), wrap(value.getConflictReason()));
		} else if (value.isTextConflict()) {
			return new TextConflict();
		} else if (value.isPropertyConflict()) {
			return new PropertyConflict();
		} else {
			throw new UnsupportedOperationException("No such conflict: " + value);
		}
	}

	private static ConflictReason wrap(SVNConflictReason conflictReason) {
		return REASONS.get(conflictReason);
	}

	private static Map<SVNConflictReason, ConflictReason> reasons() {
		HashMap<SVNConflictReason, ConflictReason> result = new HashMap<>();
		result.put(SVNConflictReason.MISSING, ConflictReason.MISSING);
		result.put(SVNConflictReason.OBSTRUCTED, ConflictReason.OBSTRUCTED);
		return result;
	}

	private static ConflictAction wrap(SVNConflictAction conflictAction) {
		return ACTIONS.get(conflictAction);
	}

	private static Map<SVNConflictAction, ConflictAction> actions() {
		HashMap<SVNConflictAction, ConflictAction> result = new HashMap<>();
		result.put(SVNConflictAction.ADD, ConflictAction.ADDED);
		result.put(SVNConflictAction.EDIT, ConflictAction.EDITED);
		result.put(SVNConflictAction.DELETE, ConflictAction.DELETED);
		return result;
	}

	public static Status wrap(final SVNStatus status) {
		return new Status() {
			@Override
			public File getFile() {
				return status.getFile();
			}
		};
	}

	public static LogEntry wrap(SVNLogEntry logEntry) {
		Map<String, LogEntryPath> changedPaths = wrap(logEntry
				.getChangedPaths());
		long revision = logEntry.getRevision();
		String author = logEntry.getAuthor();
		Date date = logEntry.getDate();
		String message = logEntry.getMessage();
		return new LogEntry(changedPaths, revision, author, date, message, logEntry.hasChildren());
	}

	public static Map<String, LogEntryPath> wrap(
			Map<String, SVNLogEntryPath> changedPaths) {
		HashMap<String, LogEntryPath> result = new HashMap<>();
		for (Entry<String, SVNLogEntryPath> entry : changedPaths.entrySet()) {
			result.put(entry.getKey(), wrap(entry.getValue()));
		}
		return result;
	}

	private static LogEntryPath wrap(SVNLogEntryPath value) {
		String path = value.getPath();
		ChangeType changeType = wrap(value.getType());
		String copyPath = value.getCopyPath();
		long copyRevision = value.getCopyRevision();
		return new LogEntryPath(wrap(value.getKind()), path, changeType, copyPath, copyRevision);
	}

	private static ChangeType wrap(char type) {
		switch (type) {
		case SVNLogEntryPath.TYPE_ADDED:
			return ChangeType.ADDED;
		case SVNLogEntryPath.TYPE_DELETED:
			return ChangeType.DELETED;
		case SVNLogEntryPath.TYPE_MODIFIED:
			return ChangeType.MODIFIED;
		case SVNLogEntryPath.TYPE_REPLACED:
			return ChangeType.REPLACED;
		}
		throw new UnsupportedOperationException("No such change type: " + type);
	}

	public static ISVNLogEntryHandler adapt(final LogEntryHandler handler) {
		if (handler == null) {
			return null;
		}
		return new ISVNLogEntryHandler() {
			@Override
			public void handleLogEntry(SVNLogEntry logEntry)
					throws SVNException {
				try {
					handler.handleLogEntry(wrap(logEntry));
				} catch (RepositoryException ex) {
					throw unwrap(ex);
				}
			}
		};
	}

	public static ISVNDirEntryHandler adapt(final DirEntryHandler handler) {
		if (handler == null) {
			return null;
		}
		return new ISVNDirEntryHandler() {
			@Override
			public void handleDirEntry(SVNDirEntry dirEntry)
					throws SVNException {
				try {
					handler.handleDirEntry(wrap(dirEntry));
				} catch (RepositoryException ex) {
					throw unwrap(ex);
				}
			}
		};
	}

	public static ISVNStatusHandler adapt(final StatusHandler handler) {
		if (handler == null) {
			return null;
		}
		return new ISVNStatusHandler() {
			@Override
			public void handleStatus(SVNStatus status) throws SVNException {
				try {
					handler.handleStatus(wrap(status));
				} catch (RepositoryException ex) {
					throw unwrap(ex);
				}
			}
		};
	}

	public static ISVNPropertyHandler adapt(final PropertyHandler handler) {
		if (handler == null) {
			return null;
		}
		return new ISVNPropertyHandler() {

			@Override
			public void handleProperty(long revision, SVNPropertyData property)
					throws SVNException {
				handler.handleProperty(revision, wrap(property));
			}

			@Override
			public void handleProperty(SVNURL url, SVNPropertyData property)
					throws SVNException {
				handler.handleProperty(wrap(url), wrap(property));
			}

			@Override
			public void handleProperty(File path, SVNPropertyData property)
					throws SVNException {
				handler.handleProperty(path, wrap(property));
			}
		};
	}

	public static MergeInfo wrapMergeInfo(final Map<SVNURL, SVNMergeRangeList> info) {
		return new MergeInfo() {
			@Override
			public List<RevisionRange> getRevisions(RepositoryURL path) {
				return wrapRanges(info.get(unwrap(path)));
			}

			@Override
			public Set<RepositoryURL> getPaths() {
				return wrapURLs(info.keySet());
			}
		};
	}

	protected static Set<RepositoryURL> wrapURLs(Set<SVNURL> urls) {
		HashSet<RepositoryURL> result = new HashSet<>();
		for (SVNURL url : urls) {
			result.add(wrap(url));
		}
		return result;
	}

	protected static List<RevisionRange> wrapRanges(SVNMergeRangeList svnMergeRangeList) {
		ArrayList<RevisionRange> result = new ArrayList<>(svnMergeRangeList.getSize());
		for (SVNMergeRange range : svnMergeRangeList.getRanges()) {
			result.add(wrap(range));
		}
		return result;
	}

	private static RevisionRange wrap(SVNMergeRange range) {
		return new RevisionRange(Revision.create(range.getStartRevision()), Revision.create(range.getEndRevision()));
	}

	public static SVNConflictChoice unwrap(Resolution resolution) {
		switch (resolution) {
			case CHOOSE_BASE:
				return SVNConflictChoice.BASE;
			case CHOOSE_MERGED:
				return SVNConflictChoice.MERGED;
			case CHOOSE_MINE_CONFLICT:
				return SVNConflictChoice.MINE_CONFLICT;
			case CHOOSE_MINE_FULL:
				return SVNConflictChoice.MINE_FULL;
			case CHOOSE_THEIRS_CONFLICT:
				return SVNConflictChoice.THEIRS_CONFLICT;
			case CHOOSE_THEIRS_FULL:
				return SVNConflictChoice.THEIRS_FULL;
			case POSTPONE:
				return SVNConflictChoice.POSTPONE;
		}
		throw new UnsupportedOperationException("No such resolution: "
			+ resolution);
	}

}
