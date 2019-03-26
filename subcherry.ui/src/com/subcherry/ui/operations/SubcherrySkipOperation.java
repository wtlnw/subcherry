/*
 * SubCherry - Cherry Picking with Trac and Subversion
 * Copyright (C) 2018 Bernhard Haumacher and others
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
package com.subcherry.ui.operations;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.SVNException;

import com.subcherry.ui.views.SubcherryMergeContext;
import com.subcherry.ui.views.SubcherryMergeEntry;
import com.subcherry.ui.views.SubcherryMergeState;

/**
 * An {@link AbstractSubcherryOperation} skipping the current merge entry.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class SubcherrySkipOperation extends AbstractSubcherryOperation {

	/**
	 * Create a {@link SubcherrySkipOperation}.
	 * 
	 * @param part
	 *            see {@link #getPart()}
	 */
	public SubcherrySkipOperation(final IWorkbenchPart part) {
		super(part);
	}

	@Override
	protected void execute(final IProgressMonitor monitor) throws SVNException, InterruptedException {
		final SubcherryMergeContext context = getContext();
		final SubcherryMergeEntry entry = context.getCurrentEntry();
		
		if (entry != null) {
			entry.setState(SubcherryMergeState.SKIPPED);
		}
	}

	@Override
	protected String getTaskName() {
		return "Skip Revision";
	}
}
