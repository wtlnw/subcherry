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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.subversion.javahl.ClientException;
import org.apache.subversion.javahl.types.ChangePath;
import org.apache.subversion.javahl.types.ChangePath.Action;

import com.subcherry.repository.command.copy.CopySource;
import com.subcherry.repository.command.merge.ConflictDescription;
import com.subcherry.repository.command.status.Status;
import com.subcherry.repository.core.BinaryValue;
import com.subcherry.repository.core.ChangeType;
import com.subcherry.repository.core.CommitInfo;
import com.subcherry.repository.core.Depth;
import com.subcherry.repository.core.DirEntry;
import com.subcherry.repository.core.LogEntryPath;
import com.subcherry.repository.core.NodeKind;
import com.subcherry.repository.core.NodeProperties;
import com.subcherry.repository.core.PropertyData;
import com.subcherry.repository.core.PropertyValue;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.repository.core.RepositoryURL;
import com.subcherry.repository.core.Revision;
import com.subcherry.repository.core.RevisionRange;
import com.subcherry.repository.core.StringValue;
import com.subcherry.repository.core.Target;
import com.subcherry.repository.core.Target.FileTarget;
import com.subcherry.repository.core.Target.UrlTarget;

public class Conversions {

	public static RepositoryException wrap(ClientException ex) {
		return new RepositoryException(ex);
	}

	public static Set<String> pathSet(File[] paths) {
		HashSet<String> result = new HashSet<String>();
		for (File path : paths) {
			result.add(path.getAbsolutePath());
		}
		return result;
	}

	public static CommitInfo wrap(final org.apache.subversion.javahl.CommitInfo info) {
		return new CommitInfo() {
			@Override
			public long getNewRevision() {
				return info.getRevision();
			}
		};
	}

	public static org.apache.subversion.javahl.types.Depth unwrap(Depth depth) {
		switch (depth) {
		case EMPTY: return org.apache.subversion.javahl.types.Depth.empty;
		case FILES: return org.apache.subversion.javahl.types.Depth.files;
		case IMMEDIATES: return org.apache.subversion.javahl.types.Depth.immediates;
		case INFINITY: return org.apache.subversion.javahl.types.Depth.infinity;
		case UNKNOWN: return org.apache.subversion.javahl.types.Depth.unknown;
		}
		throw unsupported("No such depth: " + depth);
	}

	private static UnsupportedOperationException unsupported(String message) {
		return new UnsupportedOperationException(message);
	}

	public static Map<String, String> unwrap(NodeProperties revisionProperties) {
		// TODO: Fill with info.
		return Collections.emptyMap();
	}

	public static Set<String> pathSet(RepositoryURL[] urls) {
		HashSet<String> result = new HashSet<String>();
		for (RepositoryURL url : urls) {
			result.add(url.toString());
		}
		return result;
	}

	public static PredefinedCommitMessage wrapMessage(final String commitMessage) {
		return new PredefinedCommitMessage(commitMessage);
	}

	public static String unwrap(RepositoryURL dstURL) {
		return dstURL.toString();
	}

	public static String unwrap(File path) {
		return path.getAbsolutePath();
	}

	public static org.apache.subversion.javahl.types.Revision unwrap(Revision revision) {
		switch (revision.kind()) {
		case COMMIT:
			return org.apache.subversion.javahl.types.Revision.getInstance(revision.getNumber());
		case BASE:
			return org.apache.subversion.javahl.types.Revision.BASE;
		case HEAD:
			return org.apache.subversion.javahl.types.Revision.HEAD;
		case WORKING:
			return org.apache.subversion.javahl.types.Revision.WORKING;
		case UNDEFINED:
			return null;
		}
		throw unsupported("No such revision kind: " + revision.kind());
	}

	public static List<org.apache.subversion.javahl.types.CopySource> unwrapSources(
			CopySource[] sources) {
		ArrayList<org.apache.subversion.javahl.types.CopySource> result = new ArrayList<org.apache.subversion.javahl.types.CopySource>(sources.length);
		for (CopySource source : sources) {
			result.add(unwrap(source));
		}
		return result;
	}

	private static org.apache.subversion.javahl.types.CopySource unwrap(CopySource source) {
		Target target = source.getTarget();
		return new org.apache.subversion.javahl.types.CopySource(unwrap(target), unwrap(source.getRevision()), unwrap(target.getPegRevision()));
	}

	public static String unwrap(Target target) {
		switch (target.kind()) {
		case FILE:
			return unwrap(((FileTarget) target).getFile());
		case URL:
			return unwrap(((UrlTarget) target).getUrl());
		}
		throw unsupported("No such target kind: " + target.kind());
	}

	public static List<org.apache.subversion.javahl.types.RevisionRange> unwrapRanges(Collection<RevisionRange> ranges) {
		ArrayList<org.apache.subversion.javahl.types.RevisionRange> result = new ArrayList<org.apache.subversion.javahl.types.RevisionRange>(ranges.size());
		for (RevisionRange range : ranges) {
			result.add(unwrap(range));
		}
		return result;
	}

	public static org.apache.subversion.javahl.types.RevisionRange unwrap(RevisionRange range) {
		return new org.apache.subversion.javahl.types.RevisionRange(unwrap(range.getStart()), unwrap(range.getEnd()));
	}

	public static Map<String, LogEntryPath> wrap(Set<ChangePath> changedPaths, LogFilter filter) {
		HashMap<String, LogEntryPath> result = new HashMap<String, LogEntryPath>();
		for (ChangePath path : changedPaths) {
			if (!filter.accept(path.getPath())) {
				continue;
			}
			LogEntryPath entry = wrap(path);
			result.put(entry.getPath(), entry);
		}
		return result;
	}
	
	public static LogEntryPath wrap(ChangePath path) {
		return new LogEntryPath(wrap(path.getNodeKind()), path.getPath(), wrap(path.getAction()), path.getCopySrcPath(), path.getCopySrcRevision());
	}

	public static ChangeType wrap(Action action) {
		switch (action) {
		case add:
			return ChangeType.ADDED;
		case delete:
			return ChangeType.DELETED;
		case modify:
			return ChangeType.MODIFIED;
		case replace:
			return ChangeType.REPLACED;
		}
		throw unsupported("No such action: " + action);
	}

	public static NodeKind wrap(org.apache.subversion.javahl.types.NodeKind nodeKind) {
		switch (nodeKind) {
		case dir:
			return NodeKind.DIR;
		case file:
			return NodeKind.FILE;
		case none:
			return NodeKind.NONE;
		case unknown:
			return NodeKind.UNKNOWN;
		case symlink:
			return NodeKind.SYMLINK;
		}
		throw unsupported("No such action: " + nodeKind);
	}

	public static List<org.apache.subversion.javahl.types.RevisionRange> range(Revision start, Revision end) {
		return Collections.singletonList(new org.apache.subversion.javahl.types.RevisionRange(unwrap(start), unwrap(end)));
	}

	public static <T> Set<T> set(T[] values) {
		return new HashSet<T>(Arrays.asList(values));
	}

	public static int unwrap(DirEntry.Kind kind) {
		switch (kind) {
			case DIRENT:
				return org.apache.subversion.javahl.types.DirEntry.Fields.nodeKind;
		}
		throw unsupported("No such dir entry kind: " + kind);
	}

	public static DirEntry wrap(final org.apache.subversion.javahl.types.DirEntry dirent) {
		return new DirEntry() {

			@Override
			public NodeKind getNodeKind() {
				return wrap(dirent.getNodeKind());
			}

			@Override
			public String getRelativePath() {
				return dirent.getPath();
			}

		};
	}

	public static Status wrap(final org.apache.subversion.javahl.types.Status status) {
		return new Status() {
			@Override
			public File getFile() {
				return wrapFile(status.getPath());
			}
		};
	}

	public static File wrapFile(String path) {
		return new File(path);
	}

	public static PropertyData wrap(String name, byte[] value) {
		if (value == null) {
			return null;
		}
		return new PropertyData(name, PropertyValue.create(value));
	}

	public static byte[] unwrap(PropertyValue propValue) {
		switch (propValue.kind()) {
			case BINARY:
				return ((BinaryValue) propValue).getBytes();
			case STRING:
				String string = ((StringValue) propValue).getString();
				try {
					return string.getBytes("utf-8");
				} catch (UnsupportedEncodingException ex) {
					return string.getBytes();
				}
		}
		throw unsupported("No such value kind: " + propValue.kind());
	}

	public static List<ConflictDescription> list(ConflictDescription conflictDescription) {
		ArrayList<ConflictDescription> result = new ArrayList<ConflictDescription>(
				1);
		result.add(conflictDescription);
		return result;
	}

	public static Collection<String> wrap(String[] changelists) {
		if (changelists == null) {
			return null;
		}
		return Arrays.asList(changelists);
	}

}
