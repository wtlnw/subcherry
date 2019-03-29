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

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.subcherry.Configuration;
import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.model.SubcherryTree;
import com.subcherry.ui.model.SubcherryTreeRevisionNode;
import com.subcherry.ui.model.SubcherryTreeTicketNode;
import com.subcherry.ui.widgets.LabeledComposite;
import com.subcherry.ui.wizards.SubcherryMergeWizardTicketsPage.ViewerLabelProvider;

/**
 * An {@link WizardPage} implementation for {@link SubcherryMergeWizard} which
 * displays the summary of selected changes along with the desired configuration.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryMergeWizardModePage extends WizardPage {

	/**
	 * The {@link Text} control displaying the selected source branch.
	 */
	private Text _source;
	
	/**
	 * The {@link Text} control displaying the selected target branch.
	 */
	private Text _target;

	/**
	 * The {@link TreeViewer} displaying selected tickets and revisions.
	 */
	private TreeViewer _tickets;
	
	/**
	 * Create a {@link SubcherryMergeWizardModePage}.
	 */
	public SubcherryMergeWizardModePage() {
		super(L10N.SubcherryMergeWizardModePage_name);
		
		setTitle(L10N.SubcherryMergeWizardModePage_title);
		setMessage(L10N.SubcherryMergeWizardModePage_message);
	}
	
	@Override
	public SubcherryMergeWizard getWizard() {
		return (SubcherryMergeWizard) super.getWizard();
	}
	
	@Override
	public void dispose() {
		_source = null;
		_target = null;
		_tickets = null;
		
		// call super implementation
		super.dispose();
	}
	
	@Override
	public void createControl(final Composite parent) {
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(GridLayoutFactory.fillDefaults().create());
		
		createBranchesView(contents);
		
		final Composite container = new Composite(contents, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setLayout(GridLayoutFactory.fillDefaults().margins(5, 0).create());
		createModeView(container);
		createTicketsView(container);
		
		setControl(contents);
		setPageComplete(true);
		
		final WizardDialog dialog = (WizardDialog) getContainer();
		dialog.addPageChangingListener(new IPageChangingListener() {
			@Override
			public void handlePageChanging(final PageChangingEvent event) {
				final IWizardPage thisPage = SubcherryMergeWizardModePage.this;

				// switching from previous to this page -> update viewer 
				if (event.getTargetPage() == thisPage) {
					updatePage();
				}
				
				// ignore all other page changes
			}
		});
	}

	/**
	 * Create {@link Control}s displaying the source and target branch information.
	 * 
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 */
	private void createBranchesView(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		container.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());
		
		final Label sourceLabel = new Label(container, SWT.NONE);
		sourceLabel.setText(L10N.SubcherryMergeWizardModePage_label_source);
		sourceLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		_source = new Text(container, SWT.BORDER | SWT.READ_ONLY);
		_source.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		final Label targetLabel = new Label(container, SWT.NONE);
		targetLabel.setText(L10N.SubcherryMergeWizardModePage_label_target);
		targetLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		_target = new Text(container, SWT.BORDER | SWT.READ_ONLY);
		_target.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}
	
	/**
	 * Create {@link Control}s allowing users to select the merge mode.
	 * 
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 */
	private void createModeView(final Composite parent) {
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		contents.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
		
		createModeSelector(contents);
		createMiscView(contents);
	}
	
	/**
	 * Create {@link Control}s allowing users to select the commit message rewriting
	 * mode.
	 * 
	 * @param parent
	 *            the {@link Composite} to create the {@link Control}s in
	 */
	private void createModeSelector(final Composite parent) {
		final LabeledComposite group = new LabeledComposite(parent, SWT.NONE);
		group.setLabelText(L10N.SubcherryMergeWizardModePage_mode_title);
		group.setLabelFont(SubcherryUI.getBoldDefault());
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		group.setLayout(GridLayoutFactory.swtDefaults().create());
		
		// default behavior is porting (which is not reflected in the configuration)
		final Button port = new Button(group, SWT.RADIO);
		port.setText(L10N.SubcherryMergeWizardModePage_label_mode_port);
		port.setToolTipText(L10N.SubcherryMergeWizardModePage_tooltip_mode_port);
		port.setSelection(true);
		
		final Button rebase = new Button(group, SWT.RADIO);
		rebase.setText(L10N.SubcherryMergeWizardModePage_label_mode_rebase);
		rebase.setToolTipText(L10N.SubcherryMergeWizardModePage_tooltip_mode_rebase);
		rebase.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				getWizard().getConfiguration().setRebase(((Button) event.widget).getSelection());
			}
		});
		
		final Button preview = new Button(group, SWT.RADIO);
		preview.setText(L10N.SubcherryMergeWizardModePage_label_mode_preview);
		preview.setToolTipText(L10N.SubcherryMergeWizardModePage_tooltip_mode_preview);
		preview.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				getWizard().getConfiguration().setPreview(((Button) event.widget).getSelection());
			}
		});
		
		final Button reintegrate = new Button(group, SWT.RADIO);
		reintegrate.setText(L10N.SubcherryMergeWizardModePage_label_mode_reintegrate);
		reintegrate.setToolTipText(L10N.SubcherryMergeWizardModePage_tooltip_mode_reintegrate);
		reintegrate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				getWizard().getConfiguration().setReintegrate(((Button) event.widget).getSelection());
			}
		});
	}
	
	/**
	 * Create {@link Control}s allowing users to provide additional settings.
	 * 
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 */
	private void createMiscView(final Composite parent) {
		final LabeledComposite group = new LabeledComposite(parent, SWT.NONE);
		group.setLabelText(L10N.SubcherryMergeWizardModePage_title_additional);
		group.setLabelFont(SubcherryUI.getBoldDefault());
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		group.setLayout(GridLayoutFactory.swtDefaults().create());
		
		final Button button = new Button(group, SWT.CHECK);
		button.setText(L10N.SubcherryMergeWizardModePage_label_nocommit);
		button.setToolTipText(L10N.SubcherryMergeWizardModePage_tooltip_nocommit);
		button.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				getWizard().getConfiguration().setNoCommit(((Button) e.widget).getSelection());
			}
		});
	}
	
	/**
	 * Create a view displaying selected tickets along with their selected revision
	 * to be ported.
	 * 
	 * @param parent
	 *            the {@link Composite} to create the view in
	 */
	private void createTicketsView(final Composite parent) {
		final LabeledComposite group = new LabeledComposite(parent, SWT.NONE);
		group.setLabelText(L10N.SubcherryMergeWizardModePage_title_selection);
		group.setLabelFont(SubcherryUI.getBoldDefault());
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		group.setLayout(GridLayoutFactory.fillDefaults().margins(0, 5).create());
		
		_tickets = new TreeViewer(group);
		_tickets.getTree().setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		_tickets.getTree().setLinesVisible(true);
		_tickets.setLabelProvider(new TicketViewerLabelProvider());
		_tickets.setContentProvider(new TicketViewerContentProvider());
	}
	
	/**
	 * Update page {@link Control}s with current data from {@link #getWizard()}.
	 */
	private void updatePage() {
		final SubcherryMergeWizard wizard = getWizard();
		final SubcherryTree tree = wizard.getSubcherryTree();
		final Configuration config = wizard.getConfiguration();
		
		_source.setText(config.getSourceBranch());
		_target.setText(config.getTargetBranch());
		_tickets.setInput(tree);
	}
	
	/**
	 * A {@link ViewerLabelProvider} specialization displaying the number of
	 * selected revisions for a ticket.
	 */
	private final class TicketViewerLabelProvider extends ViewerLabelProvider {
		@Override
		public String getText(final Object element) {
			final String label = super.getText(element);
			
			if(element instanceof SubcherryTreeTicketNode) {
				final SubcherryTreeTicketNode node = (SubcherryTreeTicketNode) element;
				
				return NLS.bind(L10N.SubcherryMergeWizardModePage_label_ticket, new Object[] {node.getSelectedChanges().size(), node.getChanges().size(), label});
			}
			
			return label;
		}
	}

	/**
	 * An {@link ITreeContentProvider} implementation providing selected tickets
	 * with selected revisions.
	 */
	private final class TicketViewerContentProvider implements ITreeContentProvider {
		
		@Override
		public boolean hasChildren(final Object element) {
			// only ticket nodes may have children
			return element instanceof SubcherryTreeTicketNode;
		}

		@Override
		public Object getParent(final Object element) {
			if(element instanceof SubcherryTreeRevisionNode) {
				return ((SubcherryTreeRevisionNode) element).getTicket();
			}
			
			return null;
		}

		@Override
		public Object[] getElements(final Object model) {
			if(model instanceof SubcherryTree) {
				return ((SubcherryTree) model).getSelectedTickets().toArray();
			}
			
			return new Object[0];
		}

		@Override
		public Object[] getChildren(final Object parent) {
			if(parent instanceof SubcherryTreeTicketNode) {
				return ((SubcherryTreeTicketNode) parent).getSelectedChanges().toArray();
			}
			
			return new Object[0];
		}
	}
}
