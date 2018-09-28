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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

import com.subcherry.Configuration;
import com.subcherry.repository.command.ClientManager;
import com.subcherry.repository.core.LogEntry;
import com.subcherry.repository.core.LogEntryPath;
import com.subcherry.trac.TracConnection;
import com.subcherry.trac.TracTicket;
import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.model.SubcherryTree;
import com.subcherry.ui.model.SubcherryTreeNode;
import com.subcherry.ui.model.SubcherryTreeNode.Check;
import com.subcherry.ui.model.SubcherryTreeRevisionNode;
import com.subcherry.ui.model.SubcherryTreeTicketNode;

/**
 * An {@link WizardPage} implementation for {@link SubcherryMergeWizard} which
 * allows users to select the tickets to merge the changes for.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryMergeWizardTicketsPage extends WizardPage {
	
	/**
	 * @see #createTicketViewer()
	 */
	private FilteredTree _tree;

	/**
	 * The {@link Text} control displaying the selected {@link SubcherryTreeRevisionNode}'s
	 * revision number.
	 */
	private Text _revision;
	
	/**
	 * The {@link Text} control displaying the selected {@link SubcherryTreeRevisionNode}'s
	 * revision timestamp.
	 */
	private Text _timestamp;
	
	/**
	 * The {@link Text} control displaying the selected {@link SubcherryTreeRevisionNode}'s author.
	 */
	private Text _author;
	
	/**
	 * The {@link Text} control displaying the selected {@link SubcherryTreeRevisionNode}'s commit
	 * message.
	 */
	private Text _message;
	
	/**
	 * The {@link Text} control displaying the selected {@link SubcherryTreeRevisionNode}'s changed
	 * paths.
	 */
	private Text _paths;
	
	/**
	 * Create a {@link SubcherryMergeWizardTicketsPage}.
	 */
	public SubcherryMergeWizardTicketsPage() {
		super("Tickets");
		
		setTitle("SVN Cherry Picking With Subcherry");
		setMessage("Please select the revisions to merge.");
	}
	
	@Override
	public SubcherryMergeWizard getWizard() {
		return (SubcherryMergeWizard) super.getWizard();
	}
	
	@Override
	public void dispose() {
		// release all handles for finalization
		_tree = null;
		_revision = null;
		_timestamp = null;
		_author = null;
		_message = null;
		_paths = null;
		
		// call super implementation
		super.dispose();
	}
	
	@Override
	public void createControl(final Composite parent) {
		final SashForm contents = new SashForm(parent, SWT.HORIZONTAL);
		
		createTicketsView(contents);
		createDetailsView(contents);
		
		setControl(contents);
		setPageComplete(true);
		
		final WizardDialog dialog = (WizardDialog) getContainer();
		dialog.addPageChangingListener(new IPageChangingListener() {
			@Override
			public void handlePageChanging(final PageChangingEvent event) {
				final IWizardPage thisPage = SubcherryMergeWizardTicketsPage.this;
				final IWizardPage prevPage = SubcherryMergeWizardTicketsPage.this.getPreviousPage();
				final IWizardPage nextPage = SubcherryMergeWizardTicketsPage.this.getNextPage();

				// switching from previous to this page -> update viewer 
				if (event.getCurrentPage() == prevPage && event.getTargetPage() == thisPage) {
					updatePage();
					return;
				}
				
				// switching from this page to next page -> store subcherry tree
				if(event.getCurrentPage() == thisPage && event.getTargetPage() == nextPage) {
					storePage();
					return;
				}
				
				// ignore all other page changes
			}
		});
	}

	/**
	 * Store data from this page to {@link #getWizard()}.
	 */
	private void storePage() {
		final SubcherryMergeWizard wizard = SubcherryMergeWizardTicketsPage.this.getWizard();
		wizard.setSubcherryTree((SubcherryTree) _tree.getViewer().getInput());
	}

	/**
	 * Update this page with current data from {@link #getWizard()}.
	 */
	private void updatePage() {
		final SubcherryMergeWizard wizard = getWizard();
		final ClientManager mgr = wizard.getClientManager();
		final TracConnection trac = wizard.getTracConnection();
		final Configuration config = wizard.getConfiguration();
		final SubcherryTree tree = new SubcherryTree(mgr, trac, config);

		_tree.getViewer().setInput(tree);
	}
	
	/**
	 * Create {@link Control}s for the details view displaying the selected
	 * {@link SubcherryTreeRevisionNode}'s details.
	 * 
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 */
	private void createDetailsView(final Composite parent) {
		final SashForm contents = new SashForm(parent, SWT.VERTICAL);
		
		createRevisionDetailsView(contents);
		createPathDetailsView(contents);
	}

	/**
	 * Create {@link Control}s displaying detailed information for
	 * {@link SubcherryTreeRevisionNode}s.
	 * 
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 */
	private Composite createRevisionDetailsView(final SashForm contents) {
		final Composite ticketsView = new Composite(contents, SWT.NONE);
		ticketsView.setLayout(new GridLayout(2, false));
		
		/* revision information */
		final Label labelRev = new Label(ticketsView, SWT.NONE);
		labelRev.setLayoutData(new GridData());
		labelRev.setText("Revision:");
		
		_revision = new Text(ticketsView, SWT.BORDER | SWT.READ_ONLY);
		_revision.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		/* date information */
		final Label labelTimestamp = new Label(ticketsView, SWT.NONE);
		labelTimestamp.setLayoutData(new GridData());
		labelTimestamp.setText("Date:");
		
		_timestamp = new Text(ticketsView, SWT.BORDER | SWT.READ_ONLY);
		_timestamp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		/* author information */
		final Label labelAuthor = new Label(ticketsView, SWT.NONE);
		labelAuthor.setLayoutData(new GridData());
		labelAuthor.setText("Author:");
		
		_author = new Text(ticketsView, SWT.BORDER | SWT.READ_ONLY);
		_author.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		/* affected paths information */
		final Label labelMsg = new Label(ticketsView, SWT.NONE);
		labelMsg.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		labelMsg.setText("Message:");
		
		_message = new Text(ticketsView, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
		_message.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return ticketsView;
	}

	/**
	 * Create {@link Control}s displaying a {@link SubcherryTreeRevisionNode}'s affected paths.
	 * 
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 */
	private void createPathDetailsView(final Composite parent) {
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(new GridLayout());
		
		final Label labelPaths = new Label(contents, SWT.NONE);
		labelPaths.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		labelPaths.setText("Affected paths:");
		
		_paths = new Text(contents, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
		_paths.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	/**
	 * Create {@link Control}s for the tickets view.
	 * 
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 */
	private void createTicketsView(final Composite parent) {
		final Composite ticketsView = new Composite(parent, SWT.NONE);
		ticketsView.setLayout(new GridLayout());
		
		_tree = createTicketViewer(ticketsView);
		createTicketTableButtons(ticketsView);
	}
	
	/**
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 * @return the {@link FilteredTree} displaying available tickets
	 */
	private FilteredTree createTicketViewer(final Composite parent) {
		final PatternFilter filter = new PatternFilter() {
			@Override
			protected boolean isLeafMatch(final Viewer viewer, final Object element) {
				// the revision node is always visible if the ticket node itself matches
				if(element instanceof SubcherryTreeRevisionNode) {
					if(super.isLeafMatch(viewer, ((SubcherryTreeRevisionNode) element).getTicket())) {
						return true;
					}
				}
				
				return super.isLeafMatch(viewer, element);
			}
		};
		filter.setIncludeLeadingWildcard(true);
		
		final FilteredTree tree = new FilteredTree(parent, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION, filter, true) {
			@Override
			public CheckboxTreeViewer doCreateTreeViewer(final Composite treeParent, final int style) {
				final CheckboxTreeViewer viewer = new CheckboxTreeViewer(treeParent, style);
				final Tree table = viewer.getTree();
				table.setLinesVisible(true);
				table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				
				viewer.setLabelProvider(new ViewerLabelProvider());
				viewer.setContentProvider(new ViewerContentProvider());
				viewer.setCheckStateProvider(new ViewerCheckStateProvider());
				viewer.addCheckStateListener(new ViewerCheckStateListener());
				
				viewer.addSelectionChangedListener(new ViewerSelectionChangedListener());
				
				return viewer;
			}
		};
		tree.setInitialText("type ticket or revision filter text");
		
		return tree;
	}
	
	/**
	 * Create convenience {@link Button}s for the
	 * {@link #createTicketViewer(Composite) table of tickets} allowing users to
	 * quickly select/deselect the currently displayed entries.
	 * 
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 */
	private void createTicketTableButtons(final Composite parent) {
		final Composite buttons = new Composite(parent, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		final GridLayout buttonsLayout = new GridLayout(2, false);
		buttonsLayout.marginWidth = 0;
		buttonsLayout.marginHeight = 0;
		buttons.setLayout(buttonsLayout);
		
		final Button all = new Button(buttons, SWT.NONE);
		all.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		all.setText("Select All");
		all.setToolTipText("Select all tickets (including all revisions) accepted by the current filter.");
		all.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final TreeViewer viewer = _tree.getViewer();
				
				for (final TreeItem item : viewer.getTree().getItems()) {
					final Object element = item.getData();
					if(element instanceof SubcherryTreeTicketNode) {
						final SubcherryTreeTicketNode ticket = (SubcherryTreeTicketNode) element;
						ticket.setState(Check.CHECKED);
					}
				}
				
				viewer.refresh();
			}
		});
		
		final Button none = new Button(buttons, SWT.NONE);
		none.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		none.setText("Deselect All");
		none.setToolTipText("Deselect all tickets (including all revisions) accepted by the current filter.");
		none.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final TreeViewer viewer = _tree.getViewer();
				
				for (final TreeItem item : viewer.getTree().getItems()) {
					final Object element = item.getData();
					if(element instanceof SubcherryTreeTicketNode) {
						final SubcherryTreeTicketNode ticket = (SubcherryTreeTicketNode) element;
						ticket.setState(Check.UNCHECKED);
					}
				}
				
				viewer.refresh();
			}
		});
	}
	
	/**
	 * An {@link Comparator} implementation for {@link LogEntryPath}s which compares
	 * by the {@link LogEntryPath#getType()} first and then by
	 * {@link LogEntryPath#getPath()}.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	protected static class LogEntryPathComparator implements Comparator<LogEntryPath> {
		
		/**
		 * The {@link Collator} instance to be used for comparing
		 * {@link LogEntryPath#getPath()}s.
		 */
		private final Collator _collator = Collator.getInstance();
		
		@Override
		public int compare(final LogEntryPath o1, final LogEntryPath o2) {
			// sort by change type first
			int result = o1.getType().compareTo(o2.getType());
			
			// sort by change path after that
			if(result == 0) {
				result = _collator.compare(o1.getPath(), o2.getPath());
			}
			
			return result;
		}
	}
	
	/**
	 * An {@link ISelectionChangedListener} implementation which updates {@link Control}s
	 * displaying detail information on the selected {@link SubcherryTreeRevisionNode}.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	protected class ViewerSelectionChangedListener implements ISelectionChangedListener {

		@Override
		public void selectionChanged(final SelectionChangedEvent event) {
			final IStructuredSelection selection = event.getStructuredSelection();

			final StringBuilder revisionText = new StringBuilder();
			final StringBuilder timestampText = new StringBuilder();
			final StringBuilder authorText = new StringBuilder();
			final StringBuilder messageText = new StringBuilder();
			final StringBuilder pathText = new StringBuilder();
			
			// append changed paths to the text field
			if(!selection.isEmpty()) {
				final Object element = selection.getFirstElement();
				
				if(element instanceof SubcherryTreeRevisionNode) {
					final SubcherryTreeRevisionNode node = (SubcherryTreeRevisionNode) element;
					final LogEntry change = node.getChange();
					
					revisionText.append(change.getRevision());
					timestampText.append(change.getDate());
					authorText.append(change.getAuthor());
					messageText.append(change.getMessage());
					
					final List<LogEntryPath> entries = new ArrayList<>(change.getChangedPaths().values());
					Collections.sort(entries, new LogEntryPathComparator());
					
					final Iterator<LogEntryPath> iterator = entries.iterator();
					while(iterator.hasNext()) {
						pathText.append(iterator.next().toString());
						if(iterator.hasNext()) {
							pathText.append("\n");
						}
					}
				}
			}
			
			_revision.setText(revisionText.toString());
			_timestamp.setText(timestampText.toString());
			_author.setText(authorText.toString());
			_message.setText(messageText.toString());
			_paths.setText(pathText.toString());
		}
	}

	/**
	 * An {@link ICheckStateListener} implementation which updates the check state
	 * of {@link SubcherryTreeNode}s recursively and updates only affected nodes of
	 * the specified {@link TreeViewer}.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	protected class ViewerCheckStateListener implements ICheckStateListener {
		
		@Override
		public void checkStateChanged(final CheckStateChangedEvent event) {
			final SubcherryTreeNode element = (SubcherryTreeNode) event.getElement();
			final Check state;
			
			if(event.getChecked()) {
				state = Check.CHECKED;
			} else {
				state = Check.UNCHECKED;
			}
			element.setState(state);
			
			if(element instanceof SubcherryTreeTicketNode) {
				final SubcherryTreeTicketNode ticket = (SubcherryTreeTicketNode) element;
				
				// force the viewer to update both, the parent node AND children
				_tree.getViewer().update(ticket, null);
				_tree.getViewer().update(ticket.getChanges().toArray(), null);
			} else {
				final SubcherryTreeRevisionNode change = (SubcherryTreeRevisionNode) element;
				
				// force the viewer to update both, the child node and parent
				_tree.getViewer().update(change, null);
				_tree.getViewer().update(change.getTicket(), null);
			}
		}
	}

	/**
	 * An {@link ICheckStateProvider} implementation which delegates
	 * {@link ICheckStateProvider#isGrayed(Object)} and
	 * {@link ICheckStateProvider#isChecked(Object)} to {@link SubcherryTreeNode}s
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	protected static class ViewerCheckStateProvider implements ICheckStateProvider {
		
		@Override
		public boolean isGrayed(final Object element) {
			if(element instanceof SubcherryTreeNode) {
				return ((SubcherryTreeNode) element).getState() == Check.GRAYED;
			}
			
			return false;
		}

		@Override
		public boolean isChecked(final Object element) {
			if(element instanceof SubcherryTreeNode) {
				return ((SubcherryTreeNode) element).getState() != Check.UNCHECKED;
			}
			
			return true;
		}
	}
	
	/**
	 * An {@link ColumnLabelProvider} implementation for {@link SubcherryTreeNode}s.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	protected static class ViewerLabelProvider extends ColumnLabelProvider {
		
		/**
		 * The label text to be used for {@link SubcherryTreeTicketNode}s without actual
		 * {@link TracTicket}s.
		 */
		public static final String TICKET_NONE = "NONE";

		@Override
		public String getText(final Object element) {
			if (element instanceof SubcherryTreeTicketNode) {
				final SubcherryTreeTicketNode node = (SubcherryTreeTicketNode) element;
				final TracTicket ticket = node.getTicket();
				
				if (ticket != null) {
					return String.format("Ticket #%d: %s", ticket.getNumber(), ticket.getSummary());
				}
			} else if(element instanceof SubcherryTreeRevisionNode) {
				final SubcherryTreeRevisionNode node = (SubcherryTreeRevisionNode) element;
				final LogEntry change = node.getChange();
				
				return String.format("Revision [%d]: %s", change.getRevision(), change.getMessage());
			}
			
			return TICKET_NONE;
		}
		
		@Override
		public Font getFont(final Object element) {
			if(element instanceof SubcherryTreeTicketNode) {
				return SubcherryUI.getInstance().getFontRegistry().getBold(SubcherryUI.DEFAULT);
			}
			
			return super.getFont(element);
		}
	}
	
	/**
	 * An {@link ITreeContentProvider} implementation providing access to the
	 * {@link SubcherryTree}.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	protected static class ViewerContentProvider implements ITreeContentProvider {
		
		@Override
		public Object[] getElements(final Object model) {
			if(model instanceof SubcherryTree) {
				return ((SubcherryTree) model).getTickets().toArray();
			}
			
			return new Object[0];
		}
		
		@Override
		public Object[] getChildren(final Object parent) {
			if(parent instanceof SubcherryTreeTicketNode) {
				return ((SubcherryTreeTicketNode) parent).getChanges().toArray();
			}
			
			return new Object[0];
		}
		
		@Override
		public boolean hasChildren(final Object parent) {
			if(parent instanceof SubcherryTreeTicketNode) {
				return !((SubcherryTreeTicketNode) parent).getChanges().isEmpty();
			}
			
			return false;
		}
		
		@Override
		public Object getParent(final Object child) {
			if(child instanceof SubcherryTreeRevisionNode) {
				return ((SubcherryTreeRevisionNode) child).getTicket();
			}
			
			return null;
		}
	}
}
