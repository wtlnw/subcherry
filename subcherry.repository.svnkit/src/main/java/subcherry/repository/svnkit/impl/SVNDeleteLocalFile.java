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
package subcherry.repository.svnkit.impl;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.wc.SVNConflictAction;
import org.tmatesoft.svn.core.wc.SVNConflictReason;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNOperation;
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
public class SVNDeleteLocalFile extends SVNCustomOperation {

	/**
	 * Creates a new {@link SVNDeleteLocalFile}.
	 */
	public SVNDeleteLocalFile(SvnOperationFactory factory) {
		super(factory);
	}

	@Override
	protected SVNEvent internalRun() throws SVNException {
		File firstTarget = getFile();
		if (firstTarget == null) {
			throw new IllegalArgumentException("Only local files may be deleted: "
				+ getFirstTarget().getPathOrUrlString());
		}
		if (!firstTarget.exists()) {
			return reportTreeConflict(firstTarget, SVNNodeKind.DIR, SVNConflictAction.DELETE,
				SVNConflictReason.MISSING);
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

