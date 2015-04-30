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

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNConflictAction;
import org.tmatesoft.svn.core.wc.SVNConflictReason;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNOperation;
import org.tmatesoft.svn.core.wc.SVNTreeConflictDescription;
import org.tmatesoft.svn.core.wc2.ISvnOperationHandler;
import org.tmatesoft.svn.core.wc2.SvnOperation;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnScheduleForRemoval;

/**
 * {@link SVNOperation} that deletes a local file. In contrast to {@link SvnScheduleForRemoval} the
 * operation does not throw a {@link SVNException} if the file to delete does not exist, it marks
 * the file as tree conflict.
 * 
 * @history 14.07.2014 dbusche created
 * 
 * @version $Revision$ $Author$ $Date$
 */
public class DeleteLocalFile extends SvnOperation<Void> {

	/**
	 * Creates a new {@link DeleteLocalFile}.
	 */
	public DeleteLocalFile(SvnOperationFactory factory) {
		super(factory);
	}

	@Override
	public Void run() throws SVNException {
		ensureArgumentsAreValid();

		ISvnOperationHandler operationHandler = getOperationFactory().getOperationHandler();
		operationHandler.beforeOperation(this);
		try {
			SVNEvent event = internalRun();
			getOperationFactory().getWcContext().getEventHandler().handleEvent(event, ISVNEventHandler.UNKNOWN);
			operationHandler.afterOperationSuccess(this);
		} catch (SVNException ex) {
			operationHandler.afterOperationFailure(this);
			throw ex;
		}
		return null;
	}

	private SVNEvent internalRun() throws SVNException {
		File firstTarget = getFile();
		if (firstTarget == null) {
			throw new IllegalArgumentException("Only local files may be deleted: "
				+ getFirstTarget().getPathOrUrlString());
		}
		if (!firstTarget.exists()) {
			// Report conflict.
			SVNWCContext wcContext = getOperationFactory().getWcContext();
			ISVNWCDb wcDb = wcContext.getDb();
			SVNTreeConflictDescription treeConflict =
				new SVNTreeConflictDescription(firstTarget, SVNNodeKind.DIR, SVNConflictAction.DELETE,
					SVNConflictReason.MISSING, SVNOperation.MERGE,
					null, null);
			wcDb.opSetTreeConflict(firstTarget, treeConflict);
			return SVNEventFactory.createSVNEvent(firstTarget, SVNNodeKind.NONE, null, -1,
				SVNEventAction.TREE_CONFLICT, SVNEventAction.TREE_CONFLICT, null, null);
		}

		SvnScheduleForRemoval remove = getOperationFactory().createScheduleForRemoval();
		remove.setSingleTarget(getFirstTarget());
		remove.setForce(true);
		remove.setDeleteFiles(true);
		remove.setDryRun(false);
		remove.run();
		SVNNodeKind kind = firstTarget.isDirectory() ? SVNNodeKind.DIR: firstTarget.isFile() ? SVNNodeKind.FILE: SVNNodeKind.UNKNOWN;
		return SVNEventFactory.createSVNEvent(firstTarget, kind, null, -1, SVNEventAction.DELETE,
			SVNEventAction.DELETE, null, null);
	}

	private File getFile() {
		return getOperationalWorkingCopy();
	}

}

