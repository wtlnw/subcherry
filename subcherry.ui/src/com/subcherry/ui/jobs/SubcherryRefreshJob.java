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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.tigris.subversion.subclipse.ui.operations.CleanupOperation;

import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.views.SubcherryMergeContext;
import com.subcherry.ui.views.SubcherryMergeEntry;
import com.subcherry.ui.views.SubcherryMergeView;

/**
 * An {@link AbstractSubcherryJob} which refreshes the workspace in order to reflect changes.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class SubcherryRefreshJob extends AbstractSubcherryJob {
	
	/**
	 * @see #view()
	 */
	private final SubcherryMergeView _view;

	/**
	 * Create a {@link SubcherryRefreshJob}.
	 * 
	 * @param context
	 *            see {@link #getContext()}
	 * @param view 
	 */
	public SubcherryRefreshJob(final SubcherryMergeContext context, final SubcherryMergeView view) {
		super("Refresh Workspace", context);
		
		_view = view;
	}
	
	/**
	 * @return the {@link SubcherryMergeView} to execute this job in
	 */
	public SubcherryMergeView view() {
		return _view;
	}
	
	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		try {
			final SubMonitor progress = SubMonitor.convert(monitor, 100);
			
			final SubcherryMergeContext context = getContext();
			final SubcherryMergeEntry entry = context.getCurrentEntry();
			final Set<String> paths;
			if(entry != null) {
				paths = entry.getChangeset().getTouchedResources();
			} else {
				paths = Stream.of(context.getConfiguration().getModules())
					.collect(Collectors.toSet());
			}
			final Set<IProject> projects = getProjects(paths, progress.split(50));
			refresh(projects, progress.split(50));
		} catch (Throwable ex) {
			return new Status(IStatus.ERROR, SubcherryUI.id(), "Failed to refresh workspace.", ex);
		} finally {
			monitor.done();
		}
		
		return Status.OK_STATUS;
	}
	
	/**
	 * Perform refresh of the given projects only.
	 * 
	 * @param projects
	 *            a {@link Set} of {@link IProject}s to refresh
	 * @param monitor
	 *            the {@link SubMonitor} to report the progress to
	 * @throws Exception
	 *             if an error occurred while refreshing the given resources
	 */
	private void refresh(final Set<IProject> projects, final SubMonitor monitor) throws Exception {
		monitor.setWorkRemaining(projects.size());
		
		final IProject[] resources = projects.toArray(new IProject[projects.size()]);
		final CleanupOperation operation = new CleanupOperation(view(), resources);

		operation.run(monitor);
	}
}
