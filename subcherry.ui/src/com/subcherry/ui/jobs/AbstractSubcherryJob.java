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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.views.SubcherryMergeContext;

/**
 * An abstract {@link Job} extension which provides common functionality
 * for merge jobs.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public abstract class AbstractSubcherryJob extends Job {
	
	/**
	 * @see #getContext()
	 */
	private final SubcherryMergeContext _context;

	/**
	 * Create an {@link AbstractSubcherryJob}.
	 * 
	 * @param string
	 *            see {@link #getName()}
	 * @param context
	 *            see {@link #getContext()}
	 */
	public AbstractSubcherryJob(final String string, final SubcherryMergeContext context) {
		super(string);
		
		_context = context;
	}
	
	/**
	 * @return the {@link SubcherryMergeContext} to execute the job in
	 */
	public SubcherryMergeContext getContext() {
		return _context;
	}
	
	@Override
	public boolean belongsTo(final Object family) {
		return family == AbstractSubcherryJob.class;
	}
	
	/**
	 * Schedule the given job when this one has finished execution.
	 * 
	 * @param job
	 *            the {@link Job} to schedule execution for upon finishing this one
	 * @return this instance for convenient call chain
	 */
	public AbstractSubcherryJob next(final Job job) {
		addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
				job.schedule();
			}
		});
		
		return this;
	}
	
	/**
	 * @param paths
	 *            a (possibly empty) {@link Set} of workspace relative paths to be
	 *            resolved to {@link IResource}s and grouped by their
	 *            {@link IProject}
	 * @param monitor
	 *            the {@link SubMonitor} to report the progress to
	 * @return a (possibly empty) {@link Map} of {@link IResource}s by the
	 *         {@link IProject} they belong to
	 */
	public Map<IProject, Set<IResource>> groupByProject(final Set<String> paths, final SubMonitor monitor) {
		monitor.subTask("Resolve modules to revert");
		monitor.setWorkRemaining(paths.size());
		
		final Map<IProject, Set<IResource>> resourcesByProject = new LinkedHashMap<>();
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		
		for (final String path : paths) {
			final IResource resource = root.findMember(path, true);
			if (resource != null) {
				final IProject project = resource.getProject();

				Set<IResource> resources = resourcesByProject.get(project);
				if (resources == null) {
					resources = new LinkedHashSet<>();
					resourcesByProject.put(project, resources);
				}
				resources.add(resource);
			} else {
				SubcherryUI.getInstance().getLog().log(new Status(IStatus.ERROR, SubcherryUI.id(), String.format("Resource not found: %s", path)));
			}

			monitor.worked(1);
		}
		
		return resourcesByProject;
	}
}
