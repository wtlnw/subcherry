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
package com.subcherry.ui.dialogs;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;

import com.subcherry.repository.command.merge.ConflictDescription;
import com.subcherry.repository.core.LogEntryPath;
import com.subcherry.ui.views.SubcherryMergeEntry;
import com.subcherry.ui.views.SubcherryMergeState;
import com.subcherry.utils.PathParser;

/**
 * A {@link TrayDialog} implementation which allows users to edit contents of a
 * {@link SubcherryMergeEntry}.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryEditDialog extends TrayDialog {

	/**
	 * @see #getEntry()
	 */
	private final SubcherryMergeEntry _entry;

	/**
	 * @see #getPaths()
	 */
	private final Set<String> _paths;
	
	/**
	 * @see #createRevisionControls(Composite)
	 */
	private Text _message;
	
	/**
	 * @see #createResourcesControls(Composite)
	 */
	private TableViewer _resources;

	/**
	 * Create a {@link SubcherryEditDialog}.
	 * 
	 * @param shell
	 *            see {@link #getShell()}
	 * @param entry
	 *            see {@link #getEntry()}
	 */
	public SubcherryEditDialog(final Shell shell, final SubcherryMergeEntry entry) {
		super(shell);

		_entry = entry;

		if(entry.getState() == SubcherryMergeState.NEW) {
			final PathParser parser = entry.getContext().getPathParser();
			_paths = entry.getChange().getChangedPaths().keySet().stream()
				.map(path -> parser.parsePath(path).getResource())
				.collect(Collectors.toCollection(TreeSet::new));
		} else {
			_paths = new TreeSet<String>(entry.getChangeset().getTouchedResources());
		}
		
		// change shell style
		setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
	}
	
	/**
	 * @return the {@link SubcherryMergeEntry} to resolve the conflicts for
	 */
	public SubcherryMergeEntry getEntry() {
		return _entry;
	}
	
	/**
	 * @return a (possibly empty) {@link Set} of {@link String}s representing
	 *         affected paths
	 */
	public Set<String> getPaths() {
		return _paths;
	}
	
	@Override
	protected void configureShell(final Shell newShell) {
		super.configureShell(newShell);
		
		newShell.setText("Edit Revision");
	}
	
	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite content = (Composite) super.createDialogArea(parent);
		
		final SashForm sash = new SashForm(content, SWT.VERTICAL | SWT.SMOOTH);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createRevisionControls(sash);
		createResourcesControls(sash);
		
		return content;
	}

	/**
	 * Create {@link Control}s displaying revision information.
	 * 
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 */
	private void createRevisionControls(final Composite parent) {
		final Composite content = new Composite(parent, SWT.NONE);
		content.setLayout(new GridLayout(2, false));
		
		/* revision information */
		final Label labelRev = new Label(content, SWT.NONE);
		labelRev.setLayoutData(new GridData());
		labelRev.setText("Revision:");
		
		final Text revision = new Text(content, SWT.BORDER | SWT.READ_ONLY);
		revision.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		revision.setText(String.valueOf(getEntry().getChange().getRevision()));
		
		/* date information */
		final Label labelTimestamp = new Label(content, SWT.NONE);
		labelTimestamp.setLayoutData(new GridData());
		labelTimestamp.setText("Date:");
		
		final Text timestamp = new Text(content, SWT.BORDER | SWT.READ_ONLY);
		timestamp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		timestamp.setText(String.valueOf(getEntry().getChange().getDate()));
		
		/* author information */
		final Label labelAuthor = new Label(content, SWT.NONE);
		labelAuthor.setLayoutData(new GridData());
		labelAuthor.setText("Author:");
		
		final Text author = new Text(content, SWT.BORDER | SWT.READ_ONLY);
		author.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		author.setText(getEntry().getChange().getAuthor());
		
		/* message information */
		final Label labelMsg = new Label(content, SWT.NONE);
		labelMsg.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		labelMsg.setText("Message:");
		
		_message = new Text(content, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		_message.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		_message.setText(getEntry().getMessage());
		_message.setEditable(getEntry().getState().isPending());
		_message.setFocus();
	}
	
	/**
	 * Create the {@link Control}s displaying the resources changed by
	 * {@link #getEntry()}.
	 * 
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 */
	private void createResourcesControls(final Composite parent) {
		final Composite content = new Composite(parent, SWT.NONE);
		content.setLayout(new GridLayout(2, false));
		
		_resources = new TableViewer(content, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		_resources.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		_resources.getTable().setHeaderVisible(true);
		_resources.getTable().setLinesVisible(true);
		_resources.setContentProvider(ArrayContentProvider.getInstance());
		
		final TableViewerColumn colType = new TableViewerColumn(_resources, SWT.NONE);
		colType.getColumn().setText("Type");
		colType.getColumn().setWidth(128);
		colType.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				final String branch = getEntry().getContext().getConfiguration().getSourceBranch();
				
				final StringBuilder resource = new StringBuilder();
				resource.append(branch);
				resource.append(branch.endsWith("/") ? "" : "/");
				resource.append(element);
				
				final Map<String, LogEntryPath> changes = getEntry().getChange().getChangedPaths();
				final LogEntryPath path = changes.get(resource.toString());
				if (path != null) {
					return path.getType().toString() + " " + path.getKind().toString();
				}
				
				return null;
			}
		});
		
		final TableViewerColumn colState = new TableViewerColumn(_resources, SWT.NONE);
		colState.getColumn().setText("State");
		colState.getColumn().setWidth(64);
		colState.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				final File workspace = getEntry().getContext().getConfiguration().getWorkspaceRoot();
				final File resource = new File(workspace, String.valueOf(element));
				
				final Map<File, List<ConflictDescription>> conflicts = getEntry().getConflicts();
				final List<ConflictDescription> conflict = conflicts.get(resource);
				if(conflict != null) {
					final StringBuilder label = new StringBuilder();
					final Iterator<ConflictDescription> i = conflict.iterator();
					while(i.hasNext()) {
						label.append(i.next());
						if(i.hasNext()) {
							label.append(", ");
						}
					}
					
					return label.toString();
				}
				
				return null;
			}
		});
		
		final TableViewerColumn colResource = new TableViewerColumn(_resources, SWT.NONE);
		colResource.getColumn().setText("Resource");
		colResource.getColumn().setWidth(256);
		colResource.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				return String.valueOf(element);
			}
		});
		
		_resources.setInput(getPaths());
		
		final Button add = new Button(content, SWT.PUSH);
		add.setText("Add Resources...");
		add.setEnabled(getEntry().getState().isPending());
		add.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			final ResourceListSelectionDialog dialog = new ResourceListSelectionDialog(
					event.display.getActiveShell(), 
					ResourcesPlugin.getWorkspace().getRoot(), 
					IResource.PROJECT | IResource.FOLDER | IResource.FILE) {
				
				@Override
				protected boolean select(final IResource resource) {
					final String path = resource.getFullPath().makeRelative().toPortableString();
					
					return !_paths.contains(path);
				}
			};
			dialog.setTitle("Select Additional Resources");
			
			if(ResourceListSelectionDialog.OK == dialog.open()) {
				for (final Object result : dialog.getResult()) {
					final IResource resource = (IResource) result;
					final String path = resource.getFullPath().makeRelative().toPortableString();
					
					_paths.add(path);
				}
				_resources.refresh();
			}
		}));
		
		final Button remove = new Button(content, SWT.PUSH);
		remove.setText("Remove Resources");
		remove.setEnabled(false);
		remove.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			final IStructuredSelection selection = _resources.getStructuredSelection();
			_paths.removeAll(selection.toList());
			_resources.refresh();
		}));
		if(getEntry().getState().isPending()) {
			// enable the remove button upon valid selection in the resources view
			_resources.addSelectionChangedListener(event -> remove.setEnabled(!event.getSelection().isEmpty()));
		}
	}
	
	@Override
	protected void okPressed() {
		// apply the new message text
		getEntry().setMessage(_message.getText());
		
		// apply new changed resources
		getEntry().getChangeset().getTouchedResources().clear();
		getEntry().getChangeset().getTouchedResources().addAll(getPaths());
		
		// update conflict paths (file system absolute)
		final IPath root = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		final Map<File, List<ConflictDescription>> conflicts = getEntry().getConflicts();
		final boolean hadConflicts = !conflicts.isEmpty();
		final Iterator<File> i = conflicts.keySet().iterator();
		while(i.hasNext()) {
			final File key = i.next();
			final IPath resource = Path.fromOSString(key.getPath()).makeRelativeTo(root);
			
			if(!getPaths().contains(resource.toPortableString())) {
				i.remove();
			}
		}
		
		// update the conflict state after changes
		if(hadConflicts && conflicts.isEmpty()) {
			getEntry().setState(SubcherryMergeState.MERGED);
		}
		
		super.okPressed();
	}
}
