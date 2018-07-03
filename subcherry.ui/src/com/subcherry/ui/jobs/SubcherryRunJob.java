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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.views.SubcherryMergeContext;
import com.subcherry.ui.views.SubcherryMergeEntry;
import com.subcherry.ui.views.SubcherryMergeState;

/**
 * An {@link AbstractSubcherryJob} implementation which performs the actual merge steps
 * for the current {@link SubcherryMergeEntry}.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryRunJob extends AbstractSubcherryJob {

	/**
	 * Create an {@link SubcherryRunJob}.
	 * 
	 * @param context
	 *            see {@link #getContext()}
	 */
	public SubcherryRunJob(final SubcherryMergeContext context) {
		super("Run Merge", context);
	}
	
	@Override
	public IStatus run(final IProgressMonitor monitor) {
		final SubMonitor progress = SubMonitor.convert(monitor, 100);
		
		try {
			return process(progress);
		} catch(Throwable ex) {
			return new Status(IStatus.ERROR, SubcherryUI.id(), "Merge execution failed", ex);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Merge all pending {@link SubcherryMergeEntry}s in {@link #getView()}'s
	 * {@link SubcherryMergeContext}.
	 * 
	 * @param monitor
	 *            the {@link SubMonitor} to report the progress to
	 * @return the {@link IStatus} indicating the merge result
	 */
	private IStatus process(final SubMonitor monitor) {
		final SubcherryMergeContext context = getContext();
		final List<SubcherryMergeEntry> pending = context.getPendingEntries();
		final int total = context.getAllEntries().size();
		
		// non-final: will be used for reporting progress
		int progress = total - pending.size();
		
		// initialize the monitor (consider resuming)
		monitor.setWorkRemaining(total);
		monitor.worked(progress);
		
		// merge pending entries
		for (final SubcherryMergeEntry entry : pending) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			monitor.subTask(String.format("Entry %d/%d: Revision [%d]\n%s", ++progress, total, entry.getChange().getRevision(), entry.getMessage()));

			processEntry(entry, monitor.split(1));

			// terminate processing if the entry needs further processing
			if (entry.getState().isPending()) {
				break;
			}
		}
		
		return Status.OK_STATUS;
	}

	/**
	 * Process the given entry completely.
	 * 
	 * @param entry
	 *            the {@link SubcherryMergeEntry} to process
	 * @param monitor
	 *            the {@link SubMonitor} to report the progress to
	 */
	private void processEntry(final SubcherryMergeEntry entry, final SubMonitor monitor) {
		// there are two work units for each entry
		monitor.setWorkRemaining(2);
		
		// merge new entries
		if (SubcherryMergeState.NEW == entry.getState()) {
			entry.merge();
			monitor.worked(1);
		}
		monitor.setWorkRemaining(1);
		
		// commit merged entries (allow commit retry for entries with an ERROR)
		if(SubcherryMergeState.MERGED == entry.getState() ||
		   SubcherryMergeState.ERROR == entry.getState()) {
			
			entry.commit();
			monitor.worked(1);
		}
		monitor.setWorkRemaining(0);
	}
}