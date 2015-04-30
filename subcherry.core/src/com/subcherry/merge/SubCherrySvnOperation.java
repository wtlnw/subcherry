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

/**
 * 
 * 
 * @author     <a href="mailto:jst@top-logic.com">Jan Stolzenburg</a>
 * @version    $Revision$  $Author$  $Date$
 */
public abstract class SubCherrySvnOperation extends SvnOperation<Void> {

	protected SubCherrySvnOperation(SvnOperationFactory factory) {
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

	protected abstract SVNEvent internalRun() throws SVNException;

	protected SVNEvent reportTreeConflict(File file, SVNNodeKind nodeKind, SVNConflictAction action,
			SVNConflictReason reason) throws SVNException {
		SVNWCContext wcContext = getOperationFactory().getWcContext();

		ISVNWCDb wcDb = wcContext.getDb();
		SVNTreeConflictDescription treeConflict =
				new SVNTreeConflictDescription(file, nodeKind, action,
					reason, SVNOperation.MERGE,
					null, null);
		wcDb.opSetTreeConflict(file, treeConflict);

		return SVNEventFactory.createSVNEvent(file, nodeKind, null, -1,
			SVNEventAction.TREE_CONFLICT, SVNEventAction.TREE_CONFLICT, null, null);
	}

}
