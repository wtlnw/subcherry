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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.RevertResourcesCommand;

import com.subcherry.repository.command.merge.MergeOperation;
import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.views.SubcherryMergeContext;
import com.subcherry.ui.views.SubcherryMergeEntry;

/**
 * An {@link AbstractSubcherryJob} implementation which reverts changes made in
 * the context of the current {@link SubcherryMergeEntry}.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryRevertJob extends AbstractSubcherryJob {

	/**
	 * Create a {@link SubcherryRevertJob}.
	 * 
	 * @param context
	 *            see {@link #getContext()}
	 */
	public SubcherryRevertJob(final SubcherryMergeContext context) {
		super("Revert Revision", context);
	}
	
	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		final SubcherryMergeContext context = getContext();
		final SubcherryMergeEntry entry = context.getCurrentEntry();
		
		final SubMonitor progress = SubMonitor.convert(monitor, 100);
		
		try {
			// group changed resources by the module they belong to
			final Map<IProject, Set<IResource>> changesByModule = getChangesByModule(entry, progress.split(10));
			
			// perform revert operation for each module separately
			final IStatus state = revertChangesByModule(changesByModule, progress.split(90));
			
			// reset the entry
			entry.reset();
			
			return state;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Revert the given changes.
	 * 
	 * @param changesByModule
	 *            see
	 *            {@link #getChangesByModule(SubcherryMergeEntry, SubcherryMergeContext, SubMonitor)}
	 * @param monitor
	 *            the {@link SubMonitor} to report progress to
	 * @return the {@link IStatus} indicating the operation result
	 */
	private IStatus revertChangesByModule(final Map<IProject, Set<IResource>> changesByModule, final SubMonitor monitor) {
		monitor.setWorkRemaining(changesByModule.size() + 1);
		
		for (final Entry<IProject, Set<IResource>> moduleChanges : changesByModule.entrySet()) {
			final IProject project = moduleChanges.getKey();
			final IResource[] resources = moduleChanges.getValue().toArray(new IResource[0]);
			
			final SVNTeamProvider provider = (SVNTeamProvider) RepositoryProvider.getProvider(project, SVNProviderPlugin.getTypeId());
			final RevertResourcesCommand revertCommand = new RevertResourcesCommand(provider.getSVNWorkspaceRoot(), resources);
			revertCommand.setRecurse(false);
			
			try {
				revertCommand.run(monitor.split(1));
			} catch (Exception ex) {
				return new Status(IStatus.ERROR, SubcherryUI.id(), String.format("Failed to revert %s", project.getLocation()), ex);
			}
		}
		
		return Status.OK_STATUS;
	}

	/**
	 * Group resources changed by the given entry by the module they belong to.
	 * 
	 * @param entry
	 *            the {@link SubcherryMergeEntry} to group the changes of
	 * @param monitor
	 *            the {@link SubMonitor} to report the progress to
	 * @return a (possibly empty) {@link Map} of changed {@link IResource}s by the
	 *         {@link IProject} they belong to
	 */
	private Map<IProject, Set<IResource>> getChangesByModule(final SubcherryMergeEntry entry, final SubMonitor monitor) {
		final MergeOperation operation = entry.getOperation();
		final Set<String> paths = operation.getTouchedResources();
		final Map<IProject, Set<IResource>> changesByModule = groupByProject(paths, monitor);
		
		return changesByModule;
	}
}
