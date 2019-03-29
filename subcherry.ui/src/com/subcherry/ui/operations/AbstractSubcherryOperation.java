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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.ui.operations.SVNOperation;

import com.subcherry.ui.expressions.SubcherryStateTester;
import com.subcherry.ui.views.SubcherryMergeContext;
import com.subcherry.ui.views.SubcherryMergeEntry;
import com.subcherry.ui.views.SubcherryMergeView;

/**
 * An abstract {@link SVNOperation} specialization providing common
 * functionality for cherry picking operations.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public abstract class AbstractSubcherryOperation extends SVNOperation {

	/**
	 * Create an {@link AbstractSubcherryOperation}.
	 * 
	 * @param part
	 *            see {@link #getPart()}
	 */
	protected AbstractSubcherryOperation(final SubcherryMergeView part) {
		super(part);
	}

	@Override
	protected final void execute(final IProgressMonitor monitor) throws SVNException, InterruptedException {
		try {
			evaluateVariables();
			
			executeOperation(monitor);
		} finally {
			evaluateVariables();
		}
	}
	
	/**
	 * Execute this {@link AbstractSubcherryOperation}.
	 * 
	 * @param monitor
	 *            the {@link IProgressMonitor} to report the operation progress to
	 * @throws SVNException
	 * @throws InterruptedException
	 */
	protected abstract void executeOperation(final IProgressMonitor monitor) throws SVNException, InterruptedException;

	@Override
	public boolean belongsTo(final Object family) {
		return family == AbstractSubcherryOperation.class;
	}

	@Override
	public SubcherryMergeView getPart() {
		return (SubcherryMergeView) super.getPart();
	}
	
	/**
	 * Asynchronously update the {@link #getPart()} to reflect changes to the given
	 * entries and set the viewer's selection to the last entry.
	 * 
	 * @param entries
	 *            a (possibly empty) {@link List} of {@link SubcherryMergeEntry}s to
	 *            update the viewer for
	 */
	protected void updateViewer(final SubcherryMergeEntry... entries) {
		if(entries.length < 1) {
			return;
		}
		
		final SubcherryMergeView part = getPart();
		part.getSite().getShell().getDisplay().asyncExec(() -> {
			final TableViewer viewer = part.getViewer();
			
			// update the entries first
			viewer.update(entries, null);
			
			// now select the last entry
			viewer.setSelection(new StructuredSelection(entries[entries.length - 1]), true);
		});
	}
	
	/**
	 * @return the {@link SubcherryMergeContext} to execute the operation in
	 */
	protected SubcherryMergeContext getContext() {
		final SubcherryMergeView view = getPart();

		return (SubcherryMergeContext) view.getViewer().getInput();
	}

	/**
	 * @param resources
	 *            a (possibly empty) {@link Collection} of {@link IResource}s to
	 *            return as array
	 * @return a (possibly empty) array of {@link IResource}s contained in the given
	 *         {@link Collection}
	 */
	public static IResource[] array(final Collection<IResource> resources) {
		return resources.toArray(new IResource[resources.size()]);
	}

	/**
	 * @param paths
	 *            a (possibly empty) {@link Set} of paths being part of a change set
	 * @return a (possibly empty) array of {@link IResource}s representing changed
	 *         {@link IProject}s
	 */
	public static IResource[] computeChangedProjects(final Set<String> paths) {
		return paths.stream()
			.map(path -> Path.forPosix(path).segment(0))
			.filter(module -> module != null)
			.distinct()
			.map(module -> ResourcesPlugin.getWorkspace().getRoot().getProject(module))
			.filter(project -> project != null)
			.toArray(IResource[]::new);
	}

	/**
	 * @param projectsByProvider
	 *            a (possibly empty) {@link Map} of {@link IResource}s representing
	 *            {@link IProject}s managed by an {@link SVNTeamProvider}
	 * @param sync
	 *            the {@link SyncInfoSet} to be updated with the changes to the
	 *            given projects
	 * @param monitor
	 *            the {@link IProgressMonitor} to report the progress to
	 */
	public static void computeChangedResources(final Map<SVNTeamProvider, List<IResource>> projectsByProvider, final SyncInfoSet sync, final IProgressMonitor monitor) {
		final SubMonitor progress = SubMonitor.convert(monitor, projectsByProvider.size());
		
		projectsByProvider.forEach((provider, projects) -> 
			provider.getSubscriber().collectOutOfSync(array(projects), IResource.DEPTH_INFINITE,
				sync, progress.split(1)));
	}

	/**
	 * @param resource
	 *            the {@link IResource} to resolve the team provider for
	 * @return the {@link SVNTeamProvider} managing the given resource or
	 *         {@code null} if none could be resolved
	 */
	public static SVNTeamProvider getRepositoryProvider(final IResource resource) {
		return (SVNTeamProvider) RepositoryProvider.getProvider(
			resource.getProject(),
			SVNProviderPlugin.getTypeId());
	}

	/**
	 * @param resources
	 *            a (possibly empty) array of {@link IResource}s to be grouped by
	 *            their managing {@link SVNTeamProvider}
	 * @return a (possibly empty) {@link Map} of {@link IResource}s managed by an
	 *         {@link SVNTeamProvider}
	 */
	public static Map<SVNTeamProvider, List<IResource>> groupByProvider(final IResource[] resources) {
		return Stream.of(resources)
			.collect(Collectors.groupingBy(AbstractSubcherryOperation::getRepositoryProvider));
	}
	
	/**
	 * Request evaluation of subcherry variables using the
	 * {@link IEvaluationService}.
	 */
	public static void evaluateVariables() {
		PlatformUI.getWorkbench().getService(IEvaluationService.class).requestEvaluation(
				SubcherryStateTester.NAMESPACE + SubcherryStateTester.PROPERTY_STATE);
	}
}