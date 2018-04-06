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

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

/**
 * An {@link WizardPage} implementation for {@link SubcherryMergeWizard} which
 * allows users to select the tickets to merge the changes for.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryMergeWizardTicketsPage extends WizardPage {

	/**
	 * Create a {@link SubcherryMergeWizardTicketsPage}.
	 */
	public SubcherryMergeWizardTicketsPage() {
		super("Tickets");
		
		setTitle("SVN Cherry Picking With Subcherry");
		setMessage("Please select the tickets to merge.");
	}
	
	@Override
	public SubcherryMergeWizard getWizard() {
		return (SubcherryMergeWizard) super.getWizard();
	}
	
	@Override
	public void createControl(final Composite parent) {
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(new GridLayout());
		
		final Text filter = new Text(contents, SWT.BORDER);
		filter.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		filter.setMessage("Enter ticket filter expression");
		
		final TableViewer viewer = new TableViewer(contents, SWT.BORDER|SWT.FULL_SELECTION);
		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// initialize columns
		final TableViewerColumn colSelection = new TableViewerColumn(viewer, SWT.CENTER);
		colSelection.getColumn().setResizable(false);
		colSelection.getColumn().setWidth(32);
		colSelection.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				return String.valueOf(element);
			}
		});
		
		final TableViewerColumn colTicket = new TableViewerColumn(viewer, SWT.NONE);
		colTicket.getColumn().setText("Ticket");
		colTicket.getColumn().setWidth(128);
		colTicket.setLabelProvider(new SubcherryTicketNumberLabelProvider());
		
		final TableViewerColumn colSummary = new TableViewerColumn(viewer, SWT.NONE);
		colSummary.getColumn().setText("Summary");
		colSummary.getColumn().setWidth(256);
		colSummary.setLabelProvider(new SubcherryTicketSummaryLabelProvider());

		viewer.addFilter(new SubcherryTicketFilter());
		viewer.setContentProvider(new SubcherryTicketContentProvider());
		viewer.setInput(getWizard().getBranch());
		
		final Composite buttons = new Composite(contents, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		final GridLayout buttonsLayout = new GridLayout(2, false);
		buttonsLayout.marginWidth = 0;
		buttonsLayout.marginHeight = 0;
		buttons.setLayout(buttonsLayout);
		
		final Button all = new Button(buttons, SWT.NONE);
		all.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		all.setText("Select All");
		
		final Button none = new Button(buttons, SWT.NONE);
		none.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		none.setText("Deselect All");
		
		setControl(contents);
		setPageComplete(false);
	}
	
	/**
	 * An {@link IStructuredContentProvider} implementation computing all available
	 * tickets.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	private static final class SubcherryTicketContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(final Object model) {
			return new Object[] {"Ticket 1", "Ticket 2", "Ticket 3"};
		}
	}
	
	/**
	 * An {@link ViewerFilter} implementation which accepts only tickets matching
	 * the filter pattern.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	private static final class SubcherryTicketFilter extends ViewerFilter {
		@Override
		public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
			return true;
		}
	}
	
	/**
	 * An {@link ColumnLabelProvider} implementation which displays the ticket number.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	private static final class SubcherryTicketNumberLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText(final Object element) {
			return String.valueOf(element);
		}
	}
	
	/**
	 * An {@link ColumnLabelProvider} implementation which displays the ticket summary.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	private static final class SubcherryTicketSummaryLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText(final Object element) {
			return String.valueOf(element);
		}
	}
}
