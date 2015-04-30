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

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.wc.SVNConflictAction;
import org.tmatesoft.svn.core.wc.SVNConflictReason;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc2.SvnCat;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

/**
 * 
 * 
 * @author     <a href="mailto:jst@top-logic.com">Jan Stolzenburg</a>
 * @version    $Revision$  $Author$  $Date$
 */
public class ScheduledTreeConflict extends SubCherrySvnOperation {

	private SVNNodeKind _nodeKind = SVNNodeKind.DIR;

	private SVNConflictAction _action = SVNConflictAction.EDIT;

	private SVNConflictReason _reason = SVNConflictReason.MISSING;

	protected ScheduledTreeConflict(SvnOperationFactory factory) {
		super(factory);
	}

	public void setNodeKind(SVNNodeKind nodeKind) {
		_nodeKind = nodeKind;
	}

	public void setAction(SVNConflictAction action) {
		_action = action;
	}

	public void setReason(SVNConflictReason reason) {
		_reason = reason;
	}

	@Override
	protected SVNEvent internalRun() throws SVNException {
		ensureThatWorkinCopyContextHasBeenSetUp();

		return reportTreeConflict(getFile(), _nodeKind, _action, _reason);
	}

	private void ensureThatWorkinCopyContextHasBeenSetUp() {
		if (getOperationFactory().getWcContext() == null) {
			// Dirty hack to set up the working copy context: Fire an almost useless operation.
			try {
				SvnCat cat = getOperationFactory().createCat();
				cat.setDepth(SVNDepth.EMPTY);
				cat.addTarget(SvnTarget.fromFile(getFile()));
				cat.run();
			} catch (SVNException ex) {
				// Ignore.
			}
		}
	}

	private File getFile() {
		return getOperationalWorkingCopy();
	}

}
