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
import java.util.regex.Pattern;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.subcherry.repository.core.LogEntry;
import com.subcherry.repository.core.LogEntryPath;
import com.subcherry.trac.TracTicket;
import com.subcherry.ui.DelayedModifyListener;
import com.subcherry.ui.wizards.SubcherryTicketContentProvider.ChangeNode;
import com.subcherry.ui.wizards.SubcherryTicketContentProvider.Checkable;
import com.subcherry.ui.wizards.SubcherryTicketContentProvider.Checkable.Check;
import com.subcherry.ui.wizards.SubcherryTicketContentProvider.TicketNode;

/**
 * An {@link WizardPage} implementation for {@link SubcherryMergeWizard} which
 * allows users to select the tickets to merge the changes for.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryMergeWizardTicketsPage extends WizardPage {

	/**
	 * The name of the data property to be used for annotating the
	 * {@link TreeViewer} with the filter {@link Pattern}.
	 */
	private static final String FILTER_PATTERN = "FILTER_EXPRESSION";
	
	/**
	 * The regular expression indicating all possible characters.
	 */
	private static final String REGEX_ANY = ".*";
	
	/**
	 * @see #createTicketTable()
	 */
	private CheckboxTreeViewer _viewer;

	/**
	 * The {@link Text} control displaying the selected {@link ChangeNode}'s
	 * revision number.
	 */
	private Text _revision;
	
	/**
	 * The {@link Text} control displaying the selected {@link ChangeNode}'s
	 * revision timestamp.
	 */
	private Text _timestamp;
	
	/**
	 * The {@link Text} control displaying the selected {@link ChangeNode}'s author.
	 */
	private Text _author;
	
	/**
	 * The {@link Text} control displaying the selected {@link ChangeNode}'s commit
	 * message.
	 */
	private Text _message;
	
	/**
	 * The {@link Text} control displaying the selected {@link ChangeNode}'s changed
	 * paths.
	 */
	private Text _paths;
	
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
	public void setVisible(final boolean visible) {
		// update the ticket table when this page becomes visible
		if(visible) {
			_viewer.setInput(getWizard().getConfiguration());
		}
		
		// change visibility after table update
		super.setVisible(visible);
	}
	
	@Override
	public void dispose() {
		// release all handles for finalization
		_viewer = null;
		_revision = null;
		_timestamp = null;
		_author = null;
		_message = null;
		_paths = null;
	}
	
	@Override
	public void createControl(final Composite parent) {
		final SashForm contents = new SashForm(parent, SWT.HORIZONTAL);
		
		createTicketsView(contents);
		createDetailsView(contents);
		
		setControl(contents);
		setPageComplete(true);
	}

	/**
	 * Create {@link Control}s for the details view displaying the selected
	 * {@link ChangeNode}'s details.
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
	 * {@link ChangeNode}s.
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
	 * Create {@link Control}s displaying a {@link ChangeNode}'s affected paths.
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
		
		_paths = new Text(contents, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
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
		
		createTicketFilter(ticketsView);
		_viewer = createTicketViewer(ticketsView);
		createTicketTableButtons(ticketsView);
	}

	/**
	 * Create the {@link Control}s allowing users to filter the
	 * {@link #createTicketViewer(Composite) table of tickets}
	 * 
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 */
	private void createTicketFilter(final Composite parent) {
		final Text filter = new Text(parent, SWT.BORDER);
		filter.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		filter.setMessage("Enter ticket filter expression");
		filter.addModifyListener(new DelayedModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				final String expr = filter.getText();
				final Pattern pattern;
				
				if(expr.isEmpty()) {
					pattern = null;
				} else {
					pattern = Pattern.compile(REGEX_ANY + Pattern.quote(expr) + REGEX_ANY, Pattern.CASE_INSENSITIVE); 
				}
				_viewer.setData(FILTER_PATTERN, pattern);
				_viewer.refresh();
			}
		}));
	}
	
	/**
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 * @return the {@link CheckboxTreeViewer} displaying available tickets
	 */
	private CheckboxTreeViewer createTicketViewer(final Composite parent) {
		final CheckboxTreeViewer viewer = new CheckboxTreeViewer(parent, SWT.BORDER|SWT.FULL_SELECTION);
		final Tree table = viewer.getTree();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// initialize columns
		final TreeViewerColumn colSummary = new TreeViewerColumn(viewer, SWT.NONE);
		colSummary.getColumn().setMoveable(false);
		colSummary.getColumn().setWidth(256);
		colSummary.setLabelProvider(new SubcherryDescriptionLabelProvider());
		
		final TreeViewerColumn colTicket = new TreeViewerColumn(viewer, SWT.RIGHT);
		colTicket.getColumn().setText("Id");
		colTicket.getColumn().setWidth(64);
		colTicket.setLabelProvider(new SubcherryIdLabelProvider());

		viewer.addFilter(new SubcherryTicketFilter());
		viewer.setContentProvider(new SubcherryTicketContentProvider(getWizard().getClientManager(), getWizard().getTracConnection()));
		viewer.setCheckStateProvider(new ICheckStateProvider() {
			@Override
			public boolean isGrayed(final Object element) {
				if(element instanceof Checkable) {
					return ((Checkable) element).getState() == Check.GRAYED;
				}
				
				return false;
			}
			
			@Override
			public boolean isChecked(final Object element) {
				if(element instanceof Checkable) {
					return ((Checkable) element).getState() != Check.UNCHECKED;
				}
				
				return true;
			}
		});
		viewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(final CheckStateChangedEvent event) {
				final Checkable element = (Checkable) event.getElement();
				final Check state;
				
				if(event.getChecked()) {
					state = Check.CHECKED;
				} else {
					state = Check.UNCHECKED;
				}
				element.setState(state);
				
				if(element instanceof TicketNode) {
					final TicketNode ticket = (TicketNode) element;
					
					// force the viewer to update both, the parent node AND children
					_viewer.update(ticket, null);
					_viewer.update(ticket.getChanges().toArray(), null);
				} else {
					final ChangeNode change = (ChangeNode) element;
					
					// force the viewer to update both, the child node and parent
					_viewer.update(change, null);
					_viewer.update(change.getTicket(), null);
				}
			}
		});
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
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
					
					if(element instanceof ChangeNode) {
						final ChangeNode node = (ChangeNode) element;
						final LogEntry change = node.getChange();
						
						revisionText.append(change.getRevision());
						timestampText.append(change.getDate());
						authorText.append(change.getAuthor());
						messageText.append(change.getMessage());
						
						final List<LogEntryPath> entries = new ArrayList<>(change.getChangedPaths().values());
						Collections.sort(entries, new Comparator<LogEntryPath>() {
							final Collator _collator = Collator.getInstance();
							
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
						});
						
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
		});
		
		return viewer;
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
		all.setText("Select All Visible");
		all.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				for (final TreeItem item : _viewer.getTree().getItems()) {
					final Object element = item.getData();
					if(element instanceof TicketNode) {
						final TicketNode ticket = (TicketNode) element;
						ticket.setState(Check.CHECKED);
					}
				}
				
				_viewer.refresh();
			}
		});
		
		final Button none = new Button(buttons, SWT.NONE);
		none.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		none.setText("Deselect All Visible");
		none.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				for (final TreeItem item : _viewer.getTree().getItems()) {
					final Object element = item.getData();
					if(element instanceof TicketNode) {
						final TicketNode ticket = (TicketNode) element;
						ticket.setState(Check.UNCHECKED);
					}
				}
				
				_viewer.refresh();
			}
		});
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
			final Pattern filter = (Pattern) viewer.getData(FILTER_PATTERN);
			
			// fast path return upon empty filter input
			if(filter == null) {
				return true;
			}
			
			// filter tickets using their ticket summary
			if(element instanceof TicketNode) {
				final TreeViewer tree = (TreeViewer) viewer;
				final ColumnLabelProvider labels = (ColumnLabelProvider) tree.getLabelProvider(0);
				final String label = labels.getText(element);
				
				if(label != null) {
					return filter.matcher(label).matches();
				}
			}
			
			return true;
		}
	}
	
	/**
	 * An {@link ColumnLabelProvider} implementation which displays the ticket or revision number.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	private static final class SubcherryIdLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText(final Object element) {
			if (element instanceof TicketNode) {
				final TicketNode node = (TicketNode) element;
				final TracTicket ticket = node.getTicket();

				if (ticket != null) {
					return "#" + String.valueOf(ticket.getNumber());
				}
			} else if (element instanceof ChangeNode) {
				final ChangeNode node = (ChangeNode) element;

				return "[" + String.valueOf(node.getChange().getRevision()) + "]";
			}

			return null;
		}
	}
	
	/**
	 * An {@link ColumnLabelProvider} implementation which displays the ticket
	 * summary or the revision message.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	private static final class SubcherryDescriptionLabelProvider extends ColumnLabelProvider {
		
		/**
		 * The label text to be used for {@link TicketNode}s without actual
		 * {@link TracTicket}s.
		 */
		public static final String TICKET_NONE = "NONE";
		
		/**
		 * The {@link Font} to be used for rendering {@link TicketNode}s.
		 */
		private Font _ticketFont;

		@Override
		public String getText(final Object element) {
			if (element instanceof TicketNode) {
				final TicketNode node = (TicketNode) element;
				final TracTicket ticket = node.getTicket();
				
				if (ticket != null) {
					return ticket.getSummary();
				}
			} else if(element instanceof ChangeNode) {
				final ChangeNode node = (ChangeNode) element;
				
				return node.getChange().getMessage();
			}
			
			return TICKET_NONE;
		}
		
		@Override
		public Font getFont(final Object element) {
			if(element instanceof TicketNode) {
				if(_ticketFont == null) {
					final Display device = Display.getCurrent();
					_ticketFont = FontDescriptor.createFrom(device.getSystemFont()).setStyle(SWT.BOLD).createFont(device);
				}
				
				return _ticketFont;
			}
			
			return super.getFont(element);
		}
		
		@Override
		public void dispose() {
			if(_ticketFont != null && !_ticketFont.isDisposed()) {
				_ticketFont.dispose();
			}
			
			super.dispose();
		}
	}
}
