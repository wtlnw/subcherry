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
package com.subcherry.ui.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.views.SubcherryMergeContext;
import com.subcherry.ui.views.SubcherryMergeEntry;

/**
 * An {@link AbstractSubcherryJob} which merges the next {@link SubcherryMergeEntry}.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryMergeJob extends AbstractSubcherryJob {

	/**
	 * Create a {@link SubcherryMergeJob}.
	 * 
	 * @param context
	 *            see {@link #getContext()}
	 */
	public SubcherryMergeJob(final SubcherryMergeContext context) {
		super("Merge Revision", context);
	}
	
	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		final SubcherryMergeEntry entry = getContext().getCurrentEntry();
		
		try {
			entry.merge();
		} catch(Throwable ex) {
			return new Status(IStatus.ERROR, SubcherryUI.id(), String.format("Failed merging revision: %d", entry.getChange().getRevision()), ex);
		} finally {
			monitor.done();
		}
		
		return Status.OK_STATUS;
	}
}
