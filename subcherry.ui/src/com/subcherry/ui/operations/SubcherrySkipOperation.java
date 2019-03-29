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
import org.tigris.subversion.subclipse.core.SVNException;

import com.subcherry.ui.views.SubcherryMergeContext;
import com.subcherry.ui.views.SubcherryMergeEntry;
import com.subcherry.ui.views.SubcherryMergeState;
import com.subcherry.ui.views.SubcherryMergeView;

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
	public SubcherrySkipOperation(final SubcherryMergeView part) {
		super(part);
	}

	@Override
	protected void executeOperation(final IProgressMonitor monitor) throws SVNException, InterruptedException {
		final SubcherryMergeContext context = getContext();
		final SubcherryMergeEntry current = context.getCurrentEntry();
		
		if (current != null) {
			current.setState(SubcherryMergeState.SKIPPED);
			
			final SubcherryMergeEntry next = context.getCurrentEntry();
			if (next != null) {
				updateViewer(current, next);
			} else {
				updateViewer(current);
			}
		}
	}

	@Override
	protected String getTaskName() {
		return L10N.SubcherrySkipOperation_name;
	}
}
