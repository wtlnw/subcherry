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

import java.util.List;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.subcherry.ui.model.SubcherryTree;
import com.subcherry.ui.model.SubcherryTreeRevisionNode;
import com.subcherry.ui.model.SubcherryTreeTicketNode;

/**
 * An {@link WizardPage} implementation for {@link SubcherryMergeWizard} which
 * displays the summary of selected changes along with the desired configuration.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryMergeWizardSummaryPage extends WizardPage {

	/**
	 * The {@link Text} control displaying the selected source branch.
	 */
	private Text _branch;
	
	/**
	 * The {@link Text} control displaying the number of tickets to merge.
	 */
	private Text _tickets;
	
	/**
	 * The {@link Text} control displaying the number of revisions to merge.
	 */
	private Text _revisions;

	/**
	 * Create a {@link SubcherryMergeWizardSummaryPage}.
	 */
	public SubcherryMergeWizardSummaryPage() {
		super("Summary");
		
		setTitle("SVN Cherry Picking With Subcherry");
		setMessage("Please review the summary.");
	}
	
	@Override
	public SubcherryMergeWizard getWizard() {
		return (SubcherryMergeWizard) super.getWizard();
	}
	
	@Override
	public void dispose() {
		_branch = null;
		_tickets = null;
		_revisions = null;
		
		// call super implementation
		super.dispose();
	}
	
	@Override
	public void createControl(final Composite parent) {
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(new GridLayout(2, false));
	
		/* create selected branch info controls */
		final Label branchLabel = new Label(contents, SWT.NONE);
		branchLabel.setLayoutData(new GridData());
		branchLabel.setText("Branch:");
		
		_branch = new Text(contents, SWT.READ_ONLY);
		_branch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		/* create selected tickets info controls */
		final Label ticketsLabel = new Label(contents, SWT.NONE);
		ticketsLabel.setLayoutData(new GridData());
		ticketsLabel.setText("Tickets:");
		
		_tickets = new Text(contents, SWT.READ_ONLY);
		_tickets.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		/* create selected revisions info controls */
		final Label revsLabel = new Label(contents, SWT.NONE);
		revsLabel.setLayoutData(new GridData());
		revsLabel.setText("Revisions:");
		
		_revisions = new Text(contents, SWT.READ_ONLY);
		_revisions.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		setControl(contents);
		setPageComplete(true);
		
		final WizardDialog dialog = (WizardDialog) getContainer();
		dialog.addPageChangingListener(new IPageChangingListener() {
			@Override
			public void handlePageChanging(final PageChangingEvent event) {
				final IWizardPage thisPage = SubcherryMergeWizardSummaryPage.this;

				// switching from previous to this page -> update viewer 
				if (event.getTargetPage() == thisPage) {
					updatePage();
				}
				
				// ignore all other page changes
			}
		});
	}
	
	/**
	 * Update page {@link Control}s with current data from {@link #getWizard()}.
	 */
	private void updatePage() {
		final SubcherryMergeWizard wizard = getWizard();
		final SubcherryTree tree = wizard.getSubcherryTree();
		final List<SubcherryTreeTicketNode> tickets = tree.getSelectedTickets();
		
		int revisions = 0;
		for (final SubcherryTreeTicketNode ticket : tickets) {
			for (final SubcherryTreeRevisionNode revision : ticket.getChanges()) {
				switch(revision.getState()) {
				case CHECKED: // fall through
				case GRAYED:
					revisions++;
					break;
				default:
					break;
				}
			}
		}
		
		_branch.setText(wizard.getConfiguration().getSourceBranch());
		_tickets.setText(String.valueOf(tickets.size()));
		_revisions.setText(String.valueOf(revisions));
	}
}
