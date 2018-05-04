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

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;

import com.subcherry.repository.command.merge.MergeOperation;
import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.views.SubcherryMergeContext;
import com.subcherry.ui.views.SubcherryMergeEntry;

/**
 * An {@link AbstractSubcherryJob} which refreshes the workspace in order to reflect changes.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryRefreshJob extends AbstractSubcherryJob {

	/**
	 * @see isIncremental()
	 */
	private boolean _incremental = true;
	
	/**
	 * Create a {@link SubcherryRefreshJob}.
	 * 
	 * @param context
	 *            see {@link #getContext()}
	 */
	public SubcherryRefreshJob(final SubcherryMergeContext context) {
		super("Refresh Workspace", context);
	}
	
	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		try {
			final SubMonitor progress = SubMonitor.convert(monitor, 100);
			
			if(isIncremental()) {
				final SubcherryMergeContext context = getContext();
				final SubcherryMergeEntry entry = context.getCurrentEntry();
				final MergeOperation operation = entry.getOperation();
				
				if(operation != null) {
					final Set<IProject> projects = getProjects(operation.getTouchedResources(), progress.split(50));
					refreshIncremental(projects, progress.split(50));
				}
			} else {
				refreshGlobal(progress);
			}
		} catch (Throwable ex) {
			return new Status(IStatus.ERROR, SubcherryUI.id(), "Failed to refresh workspace.", ex);
		} finally {
			monitor.done();
		}
		
		return Status.OK_STATUS;
	}
	
	/**
	 * Perform incremental refresh of the given projects only.
	 * 
	 * @param projects
	 *            a {@link Set} of {@link IProject}s to refresh
	 * @param monitor
	 *            the {@link SubMonitor} to report the progress to
	 */
	private void refreshIncremental(final Set<IProject> projects, final SubMonitor monitor) {
		monitor.setWorkRemaining(projects.size());
		
		projects.forEach(project -> {
			monitor.subTask(String.format("Refreshing: %s", project.getName()));
			try {
				// refresh the workspace first
				project.refreshLocal(IResource.DEPTH_INFINITE, monitor.split(1));
				
				// refresh the SVN status cache
				SVNProviderPlugin.getPlugin().getStatusCacheManager().refreshStatus(project, true);
			} catch (Throwable ex) {
				SubcherryUI.getInstance().getLog().log(new Status(IStatus.ERROR, SubcherryUI.id(), String.format("Failed to refresh resource: %s", project.getLocation()), ex));
			}
		});
	}

	/**
	 * Refresh the entire workspace.
	 * 
	 * @param progress
	 *            the {@link SubMonitor} to report the progress to
	 * @throws CoreException
	 *             if an error occurred while refreshing the workspace
	 */
	private void refreshGlobal(final SubMonitor progress) throws CoreException {
		ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, progress);
	}
	
	/**
	 * @return {@code true} to refresh only resources touched by the
	 *         {@link SubcherryMergeContext#getCurrentEntry()}, {@code false}
	 *         indicates that the entire workspace is refreshed
	 */
	public boolean isIncremental() {
		return _incremental;
	}
	
	/**
	 * Setter for {@link #isIncremental()}.
	 * 
	 * @param incremental
	 *            see {@link #isIncremental()}
	 * @return {@link SubcherryRefreshJob} for convenient call chaining
	 */
	public SubcherryRefreshJob setIncremental(final boolean incremental) {
		_incremental = incremental;
		
		return this;
	}
}
