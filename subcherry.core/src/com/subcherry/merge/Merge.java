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
package com.subcherry.merge;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.wc.SVNConflictVersion;
import org.tmatesoft.svn.core.wc.SVNConflictAction;
import org.tmatesoft.svn.core.wc.SVNConflictDescription;
import org.tmatesoft.svn.core.wc.SVNConflictReason;
import org.tmatesoft.svn.core.wc.SVNOperation;
import org.tmatesoft.svn.core.wc.SVNTreeConflictDescription;
import org.tmatesoft.svn.core.wc2.SvnOperation;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

/**
 * @version    $Revision$  $Author$  $Date$
 */
public class Merge {
	
	private final Collection<SvnOperation<?>> _operations;

	private final long _revision;

	private Set<String> _touchedResources;

	public Merge(long revision, Collection<SvnOperation<?>> operations, Set<String> touchedResources) {
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
	public Collection<SvnOperation<?>> getOperations() {
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
		return getOperations().isEmpty();
	}

	public Map<File, List<SVNConflictDescription>> run(MergeContext context) throws SVNException {
		Map<File, List<SVNConflictDescription>> allConflicts = Collections.emptyMap();
		for (SvnOperation<?> operation : this.getOperations()) {
			Map<File, List<SVNConflictDescription>> moduleConflicts = execute(operation);

			allConflicts = addAll(allConflicts, moduleConflicts);
		}
		return allConflicts;
	}

	private Map<File, List<SVNConflictDescription>> execute(SvnOperation<?> op) throws SVNException {
		System.out.println(OperationToString.toString(op));

		SvnOperationFactory operationFactory = op.getOperationFactory();

		TouchCollector touchedFilesHandler = new TouchCollector(operationFactory.getEventHandler());
		operationFactory.setEventHandler(touchedFilesHandler);
		try {
			MergeConflictCollector conclictCollector =
				new MergeConflictCollector(
					operationFactory.getOperationHandler(),
					touchedFilesHandler.getTouchedFiles());
			operationFactory.setOperationHandler(conclictCollector);
			try {
				try {
					op.run();
				} catch (SVNException ex) {
					SVNErrorCode errorCode = ex.getErrorMessage().getErrorCode();
					boolean missingTarget = errorCode == SVNErrorCode.WC_PATH_NOT_FOUND;
					boolean alreadyExists = errorCode == SVNErrorCode.ENTRY_EXISTS;
					if (missingTarget || alreadyExists) {
						File path = (File) ex.getErrorMessage().getRelatedObjects()[0];
						SVNNodeKind nodeKind = SVNNodeKind.UNKNOWN;
						SVNConflictAction conflictAction;
						SVNConflictReason conflictReason;
						if (missingTarget) {
							conflictAction = SVNConflictAction.EDIT;
							conflictReason = SVNConflictReason.MISSING;
						} else {
							conflictAction = SVNConflictAction.ADD;
							conflictReason = SVNConflictReason.OBSTRUCTED;
						}
						SVNOperation operation = SVNOperation.MERGE;
						SVNConflictVersion sourceLeftVersion = null;
						SVNConflictVersion sourceRightVersion = null;
						List<SVNConflictDescription> conflicts =
							Arrays.<SVNConflictDescription> asList(new SVNTreeConflictDescription(path, nodeKind,
								conflictAction, conflictReason, operation, sourceLeftVersion, sourceRightVersion));
						// Like a tree conflict, where the target of the merge does not exist in the current working copy.
						conclictCollector.addConflict(path, conflicts);
					} else {
						throw ex;
					}
				}
				return conclictCollector.getMergeConflicts();
			} finally {
				operationFactory.setOperationHandler(conclictCollector.getDelegate());
			}
		} finally {
			operationFactory.setEventHandler(touchedFilesHandler.getDelegate());
		}
	}

	private static <K, V> Map<K, V> addAll(Map<K, V> allConflicts, Map<K, V> moduleConflicts) {
		if (allConflicts.isEmpty()) {
			// allConflicts is unmodifiable.
			allConflicts = moduleConflicts;
		} else {
			allConflicts.putAll(moduleConflicts);
		}
		return allConflicts;
	}

}
