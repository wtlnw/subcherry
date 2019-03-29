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
package com.subcherry.ui.wizards;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.text.TableView;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.model.WorkbenchViewerComparator;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.svnclientadapter.SVNUrl;

import com.subcherry.Configuration;
import com.subcherry.ui.SubcherryUI;

/**
 * An {@link Wizard} implementation for {@link SubcherryUI} which allows users to
 * select the target modules.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class SubcherryMergeWizardTargetPage extends WizardPage {

	/**
	 * The {@link Text} displaying the target branch.
	 */
	private Text _branch;
	
	/**
	 * The {@link TableViewer} displaying selectable {@link IProject}s.
	 */
	private TableViewer _options;
	
	/**
	 * The {@link TableViewer} displaying selected {@link IProject}s.
	 */
	private TableViewer _selection;
	
	/**
	 * The {@link Button} selecting marked {@link IProject}s.
	 */
	private Button _add;
	
	/**
	 * The {@link Button} selecting all _visible_ {@link IProject}s.
	 */
	private Button _addAll;
	
	/**
	 * The {@link Button} unselecting marked {@link IProject}s. 
	 */
	private Button _remove;
	
	/**
	 * The {@link Button} unselecting all {@link IProject}s.
	 */
	private Button _removeAll;

	/**
	 * Create a {@link SubcherryMergeWizardTargetPage}.
	 */
	public SubcherryMergeWizardTargetPage() {
		super(L10N.SubcherryMergeWizardTargetPage_name);
		
		setTitle(L10N.SubcherryMergeWizardTargetPage_title);
		setMessage(L10N.SubcherryMergeWizardTargetPage_message);
	}
	
	@Override
	public void createControl(final Composite parent) {
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(new FormLayout());
		
		final IProject[] options = getOptions();
		final Composite top = createBranchView(contents);
		final Composite left = createOptionsView(contents, options);
		final Composite center = createButtonsView(contents, options);
		final Composite right = createSelectionView(contents);
		
		final FormData topData = new FormData();
		topData.top = new FormAttachment(0);
		topData.left = new FormAttachment(0);
		topData.right = new FormAttachment(100);
		top.setLayoutData(topData);
		
		final FormData leftData = new FormData();
		leftData.top = new FormAttachment(top);
		leftData.left = new FormAttachment(0);
		leftData.bottom = new FormAttachment(100);
		leftData.right = new FormAttachment(center);
		left.setLayoutData(leftData);
		
		final FormData rightData = new FormData();
		rightData.top = new FormAttachment(top);
		rightData.left = new FormAttachment(center);
		rightData.bottom = new FormAttachment(100);
		rightData.right = new FormAttachment(100);
		right.setLayoutData(rightData);
		
		final Point centerSize = center.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		final FormData centerData = new FormData();
		centerData.top = new FormAttachment(top);
		centerData.left = new FormAttachment(50, -centerSize.x / 2);
		centerData.bottom = new FormAttachment(100);
		centerData.right = new FormAttachment(50, centerSize.x / 2);
		center.setLayoutData(centerData);
		
		setControl(contents);
		setPageComplete(false);
		
		final WizardDialog dialog = (WizardDialog) getContainer();
		dialog.addPageChangingListener(new IPageChangingListener() {
			@Override
			public void handlePageChanging(final PageChangingEvent event) {
				final IWizardPage thisPage = SubcherryMergeWizardTargetPage.this;
				final IWizardPage nextPage = SubcherryMergeWizardTargetPage.this.getNextPage();
				
				// switching from this page to next page -> store subcherry tree
				if(event.getCurrentPage() == thisPage && event.getTargetPage() == nextPage) {
					final Configuration config = getWizard().getConfiguration();
					
					// update modules
					config.setModules(Arrays.stream((IProject[]) _selection.getInput())
						.map(module -> module.getName())
						.toArray(String[]::new));
					
					// update target branch
					config.setTargetBranch(_branch.getText());
				}
				
				// ignore all other page changes
			}
		});
	}

	/**
	 * Create a view displaying the target branch.
	 * 
	 * @param parent the {@link Composite} to create the view in
	 * @return the created {@link Composite} view
	 */
	private Composite createBranchView(final Composite parent) {
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());
		
		final Label label = new Label(contents, SWT.NONE);
		label.setText(L10N.SubcherryMergeWizardTargetPage_label_target);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		
		_branch = new Text(contents, SWT.BORDER | SWT.READ_ONLY);
		_branch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		return contents;
	}

	/**
	 * Create a view displaying selectable {@link IProject}s using a
	 * {@link TableView}.
	 * 
	 * @param parent  the {@link Composite} to create the view in
	 * @param options a (possibly empty) array of available {@link IProject}s
	 * @return the created {@link Composite} view
	 */
	private Composite createOptionsView(final Composite parent, final IProject[] options) {
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(GridLayoutFactory.swtDefaults().create());
		
		final Text filterText = new Text(contents, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		filterText.setMessage(L10N.SubcherryMergeWizardTargetPage_hint_filter);
		filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		final ControlDecoration filterDeco = new ControlDecoration(filterText, SWT.LEFT | SWT.TOP);
		filterDeco.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		filterDeco.hide();
		
		filterText.addModifyListener(event -> {
			for (final ViewerFilter filter : _options.getFilters()) {
				if (filter instanceof ProjectPatternViewerFilter) {
					final Text widget = (Text) event.widget;
					final String text = widget.getText();
					final ProjectPatternViewerFilter patternFilter = (ProjectPatternViewerFilter) filter;
					
					if (text.isEmpty()) {
						patternFilter.setPattern(null);
						filterDeco.hide();
					} else {
						try {
							patternFilter.setPattern(Pattern.compile(text));
							filterDeco.hide();
						} catch (PatternSyntaxException e) {
							filterDeco.setDescriptionText(L10N.SubcherryMergeWizardTargetPage_error_filter_invalid);
							filterDeco.show();
						}
					}

					// refresh the table after filter update
					_options.refresh(true);
					
					// update the pattern filter only
					break;
				}
			}
		});
		
		_options = new TableViewer(contents);
		_options.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		_options.setLabelProvider(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
		_options.setContentProvider(ArrayContentProvider.getInstance());
		_options.setComparator(new WorkbenchViewerComparator());
		_options.setInput(options);
		_options.addSelectionChangedListener(event -> _add.setEnabled(!event.getSelection().isEmpty()));
		_options.addDoubleClickListener(event -> select(getContents(event.getSelection())));
		_options.addFilter(new ProjectPatternViewerFilter());
		
		return contents;
	}

	/**
	 * @return a (possibly empty) array of all selectable {@link IProject}s
	 */
	private IProject[] getOptions() {
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final SVNProviderPlugin svn = SVNProviderPlugin.getPlugin();
		
		// add only accessible modules which are managed by SVN
		return Stream.of(workspace.getRoot().getProjects())
			.filter(project -> project.isAccessible() && svn.isManagedBySubversion(project))
			.toArray(IProject[]::new);
	}

	/**
	 * Create a view displaying {@link Button}s for selection/unselection.
	 * 
	 * @param parent  the {@link Composite} to create the view in
	 * @param options a (possibly empty) array of available {@link IProject}s
	 * @return the created {@link Composite} view
	 */
	private Composite createButtonsView(final Composite parent, final IProject[] options) {
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(GridLayoutFactory.swtDefaults().create());
		
		_add = new Button(contents, SWT.PUSH);
		_add.setText(L10N.SubcherryMergeWizardTargetPage_label_add);
		_add.setEnabled(false);
		_add.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		_add.addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("unchecked")
			@Override
			public void widgetSelected(final SelectionEvent e) {
				select((List<IProject>) _options.getStructuredSelection().toList());
			}
		});
		
		_addAll = new Button(contents, SWT.PUSH);
		_addAll.setText(L10N.SubcherryMergeWizardTargetPage_label_add_all);
		_addAll.setEnabled(options.length > 0);
		_addAll.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		_addAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				select(getVisibleElements(_options));
			}
		});
		
		_remove = new Button(contents, SWT.PUSH);
		_remove.setText(L10N.SubcherryMergeWizardTargetPage_label_remove);
		_remove.setEnabled(false);
		_remove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		_remove.addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("unchecked")
			@Override
			public void widgetSelected(final SelectionEvent e) {
				unselect((List<IProject>) _selection.getStructuredSelection().toList());
			}
		});
		
		_removeAll = new Button(contents, SWT.PUSH);
		_removeAll.setText(L10N.SubcherryMergeWizardTargetPage_label_remove_all);
		_removeAll.setEnabled(false);
		_removeAll.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		_removeAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				unselect(getVisibleElements(_selection));
			}
		});
		
		return contents;
	}

	/**
	 * Create a view displaying the selected {@link IProject}s.
	 * 
	 * @param parent the {@link Composite} to create the view in
	 * @return the created {@link Composite} view
	 */
	private Composite createSelectionView(final Composite parent) {
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(GridLayoutFactory.swtDefaults().create());
		
		_selection = new TableViewer(contents);
		_selection.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		_selection.setLabelProvider(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
		_selection.setContentProvider(ArrayContentProvider.getInstance());
		_selection.setComparator(new WorkbenchViewerComparator());
		_selection.setInput(new IProject[0]);
		_selection.addSelectionChangedListener(event -> _remove.setEnabled(!event.getSelection().isEmpty()));
		_selection.addDoubleClickListener(event -> unselect(getContents(event.getSelection())));
		
		return contents;
	}
	
	@Override
	public SubcherryMergeWizard getWizard() {
		return (SubcherryMergeWizard) super.getWizard();
	}
	
	@Override
	public void dispose() {
		// release all handles for finalization
		_options = null;
		_selection = null;
		_add = null;
		_addAll = null;
		_remove = null;
		_removeAll = null;
		
		// call super implementation
		super.dispose();
	}
	
	/**
	 * @param viewer the {@link TableViewer} to return the visible elements from
	 * @return a (possibly empty) {@link List} of {@link IProject}s displayed by the
	 *         given {@link TableViewer}
	 */
	private List<IProject> getVisibleElements(final TableViewer viewer) {
		return Arrays.stream(viewer.getTable().getItems())
			.map(item -> item.getData())
			.map(data -> (IProject) data)
			.collect(Collectors.toList());
	}
	
	/**
	 * Add the given {@link IProject}s to module selection.
	 * 
	 * @param projects a (possibly empty) {@link List} of {@link IProject}s to
	 *                 select
	 */
	private void select(final List<IProject> projects) {
		// update the selection input by adding the new projects
		final Set<IProject> selection = new HashSet<>(Arrays.asList((IProject[]) _selection.getInput()));
		selection.addAll(projects);
		_selection.setInput(selection.toArray(new IProject[selection.size()]));

		// update the options input by removing the new projects
		final Set<IProject> options = new HashSet<>(Arrays.asList((IProject[]) _options.getInput()));
		options.removeAll(projects);
		_options.setInput(options.toArray(new IProject[options.size()]));
		
		// update "Add All ->" and "<- Remove All" buttons
		updateButtons(options, selection);
		
		// update the target branch selection
		updateBranch(selection);
		
		// validate page input
		validate();
	}
	
	/**
	 * Remove the given {@link IProject}s from module selection.
	 * 
	 * @param projects a (possibly empty) {@link List} of {@link IProject}s to
	 *                 unselect
	 */
	private void unselect(final List<IProject> projects) {
		// update the selection input by removing the new projects
		final Set<IProject> selection = new HashSet<>(Arrays.asList((IProject[]) _selection.getInput()));
		selection.removeAll(projects);
		_selection.setInput(selection.toArray(new IProject[selection.size()]));

		// update the options input by adding the new projects
		final Set<IProject> options = new HashSet<>(Arrays.asList((IProject[]) _options.getInput()));
		options.addAll(projects);
		_options.setInput(options.toArray(new IProject[options.size()]));
		
		// update "Add All ->" and "<- Remove All" buttons
		updateButtons(options, selection);
		
		// update the target branch selection
		updateBranch(selection);
		
		// validate page input
		validate();
	}
	
	/**
	 * Update the buttons according to the available options and selection items.
	 * 
	 * @param options   a (possibly empty) {@link Set} of available
	 *                  {@link IProject}s
	 * @param selection a (possibly empty) {@link Set} of selected {@link IProject}s
	 */
	private void updateButtons(final Set<IProject> options, final Set<IProject> selection) {
		_addAll.setEnabled(!options.isEmpty());
		_removeAll.setEnabled(!selection.isEmpty());
	}
	
	/**
	 * Update the {@link Text} control displaying the target branch.
	 * 
	 * @param selection a (possibly empty) {@link Set} of selected {@link IProject}s
	 */
	private void updateBranch(final Set<IProject> selection) {
		final SVNProviderPlugin svn = SVNProviderPlugin.getPlugin();
		final String branches = selection.stream()
			.map(module -> {
				final RepositoryProvider provider = SVNTeamProvider.getProvider(module);
				if(provider != null) {
					try {
						final SVNUrl modulePath = svn.getStatusCacheManager().getStatus(module).getUrl();
						final SVNUrl branchPath = modulePath.getParent();
						final SVNUrl repoPath = svn.getRepository(branchPath.toString()).getRepositoryRoot();
						final String branchName = branchPath.toString().replace(repoPath.toString(), ""); //$NON-NLS-1$
						
						return branchName;
					} catch (final SVNException e) {
						// ignore for now and try with the next project
					}
				}
				return null;
			})
			.filter(branch -> branch != null)
			.distinct()
			.collect(Collectors.joining("; ")); //$NON-NLS-1$
		
		_branch.setText(branches);
	}
	
	/**
	 * Validate {@link IProject} selection and update page completion status.
	 */
	private void validate() {
		// validate target branch selection
		final String branch = _branch.getText();
		switch(branch.split(";").length) { //$NON-NLS-1$
		case 0:
			setErrorMessage(L10N.SubcherryMergeWizardTargetPage_error_message_no_target);
			break;
		case 1:
			setErrorMessage(null);
			break;
		default:
			setErrorMessage(L10N.SubcherryMergeWizardTargetPage_error_message_multiple_targets);
			break;
		}
		
		// validate module selection
		if(getErrorMessage() == null) {
			final IProject[] modules = (IProject[]) _selection.getInput();
			switch(modules.length) {
			case 0:
				setErrorMessage(L10N.SubcherryMergeWizardTargetPage_error_message_no_module);
				break;
			default:
				setErrorMessage(null);
				break;
			}
		}
		
		// the page is complete when no error messages are shown
		setPageComplete(getErrorMessage() == null);
	}
	
	/**
	 * @param selection the {@link ISelection} to return the contents of
	 * @return a (possibly empty) {@link List} of {@link IProject}s from the given
	 *         {@link ISelection}
	 */
	private List<IProject> getContents(final ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			final List<?> contents = ((IStructuredSelection) selection).toList();
			
			return contents.stream()
				.filter(element -> element instanceof IProject)
				.map(element -> (IProject) element)
				.collect(Collectors.toList());
		}
		
		return Collections.emptyList();
	}
	
	/**
	 * A {@link ViewerFilter} implementation which uses a regular expression {@link Pattern}
	 * for filtering {@link IProject} names.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	private static final class ProjectPatternViewerFilter extends ViewerFilter {
		
		/**
		 * @see #setPattern(Pattern)
		 */
		private Pattern _pattern;

		@Override
		public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
			final IProject module = Adapters.adapt(element, IProject.class);
			
			return _pattern == null || _pattern.matcher(module.getName()).find();
		}

		/**
		 * Set the regular expression {@link Pattern} to be used for matching
		 * {@link IProject} names.
		 * 
		 * @param pattern the new {@link Pattern} or {@code null} to accept all
		 *                {@link IProject} names
		 */
		public void setPattern(final Pattern pattern) {
			_pattern = pattern;
		}
	}
}
