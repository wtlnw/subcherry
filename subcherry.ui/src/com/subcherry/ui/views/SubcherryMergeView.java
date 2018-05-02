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
package com.subcherry.ui.views;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.subcherry.repository.command.merge.ConflictDescription;
import com.subcherry.ui.SubcherryUI;

/**
 * An {@link ViewPart} implementation for {@link SubcherryUI} which allows
 * performing the actual merge process.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryMergeView extends ViewPart {

	/**
	 * The identifier of this view as registered in the {@code plugin.xml}.
	 */
	public static final String ID = "com.subcherry.ui.views.SubcherryMergeView";
	
	/**
	 * @see #getViewer()
	 */
	private TableViewer _viewer;
	
	@Override
	public void createPartControl(final Composite parent) {
		_viewer = createViewer(parent);
	}

	/**
	 * @param parent
	 *            the parent {@link Composite} to create the viewer in
	 * @return the new {@link TableViewer}
	 */
	protected TableViewer createViewer(final Composite parent) {
		final TableViewer viewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		final Table table = viewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);
		
		final TableViewerColumn stateCol = new TableViewerColumn(viewer, SWT.CENTER);
		stateCol.getColumn().setText("State");
		stateCol.getColumn().setWidth(48);
		stateCol.setLabelProvider(new SubcherryMergeViewLabelProvider() {
			@Override
			public String getText(final Object element) {
				final SubcherryMergeEntry entry = (SubcherryMergeEntry) element;
				
				return entry.getState().toString();
			}
			
			@Override
			public String getToolTipText(final Object element) {
				final SubcherryMergeEntry entry = (SubcherryMergeEntry) element;
				
				switch(entry.getState()) {
				case CONFLICT: {
					final StringBuilder text = new StringBuilder("Conflicts detected:\n");
					
					for (final Entry<File, List<ConflictDescription>> conflict : entry.getConflicts().entrySet()) {
						text.append("\n");
						text.append(conflict.getKey().getAbsolutePath()).append(":");

						for (final ConflictDescription description : conflict.getValue()) {
							text.append("\n\t- ");
							text.append(description.toString());
						}
					}
					
					return text.toString();
				}
				case ERROR: {
					return entry.getError().getLocalizedMessage();
				}
				case MERGED: {
					final StringBuilder text = new StringBuilder("Touched resources:\n");
					
					for (final String resource : entry.getOperation().getTouchedResources()) {
						text.append("\n");
						text.append(resource);
					}
					
					return text.toString();
				}
				default:
					return null;
				}
			}
		});
		
		final TableViewerColumn revCol = new TableViewerColumn(viewer, SWT.RIGHT);
		revCol.getColumn().setText("Revision");
		revCol.getColumn().setWidth(70);
		revCol.setLabelProvider(new SubcherryMergeViewLabelProvider() {
			@Override
			public String getText(final Object element) {
				final SubcherryMergeEntry entry = (SubcherryMergeEntry) element;
				
				return String.valueOf(entry.getChange().getRevision());
			}
		});
		
		final TableViewerColumn authorCol = new TableViewerColumn(viewer, SWT.NONE);
		authorCol.getColumn().setText("Author");
		authorCol.getColumn().setWidth(60);
		authorCol.setLabelProvider(new SubcherryMergeViewLabelProvider() {
			@Override
			public String getText(final Object element) {
				final SubcherryMergeEntry entry = (SubcherryMergeEntry) element;
				
				return entry.getChange().getAuthor();
			}
		});
		
		final TableViewerColumn msgCol = new TableViewerColumn(viewer, SWT.NONE);
		msgCol.getColumn().setText("Message");
		msgCol.getColumn().setWidth(256);
		msgCol.setLabelProvider(new SubcherryMergeViewLabelProvider() {
			@Override
			public String getText(final Object element) {
				final SubcherryMergeEntry entry = (SubcherryMergeEntry) element;
				
				return entry.getMessage();
			}
		});
		
		viewer.setContentProvider(new SubcherryMergeViewContentProvider());
		
		return viewer;
	}
	
	@Override
	public void setFocus() {
		// does nothing
	}
	
	@Override
	public void dispose() {
		_viewer = null;
		
		super.dispose();
	}
	
	/**
	 * @return the {@link TableViewer} displaying the {@link SubcherryMergeEntry}s
	 *         to be merged as well as their current status
	 */
	public TableViewer getViewer() {
		return _viewer;
	}

	
	/**
	 * A {@link IStructuredContentProvider} implementation which uses the
	 * {@link SubcherryMergeContext} as input in order to resolve the entries to be
	 * displayed in the view.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	public static class SubcherryMergeViewContentProvider implements IStructuredContentProvider {
		
		/**
		 * The {@link SubcherryMergeListener} which will update the viewer upon merge
		 * changes.
		 */
		private SubcherryMergeListener _listener;

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
			// unregister listener from the old context
			if (oldInput instanceof SubcherryMergeContext) {
				((SubcherryMergeContext) oldInput).removeMergeListener(_listener);

				// reset the old listener
				_listener = null;
			}

			// register a new listener instance with the new context
			if (newInput instanceof SubcherryMergeContext) {
				_listener = new SubcherryMergeListener() {
					@Override
					public void onStateChanged(final SubcherryMergeEntry entry, final SubcherryMergeState oldState, final SubcherryMergeState newState) {
						PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
							@Override
							public void run() {
								if (viewer instanceof StructuredViewer) {
									final StructuredViewer structuredViewer = (StructuredViewer) viewer;
									
									structuredViewer.update(entry, null);
									structuredViewer.reveal(entry);
								} else {
									viewer.refresh();
								}
							}
						});
					}
				};

				((SubcherryMergeContext) newInput).addMergeListener(_listener);
			}
		}

		@Override
		public Object[] getElements(final Object input) {
			if(input instanceof SubcherryMergeContext) {
				final List<SubcherryMergeEntry> entries = ((SubcherryMergeContext) input).getAllEntries();
				
				return entries.toArray();
			}
			
			return new Object[0];
		}

		@Override
		public void dispose() {
			_listener = null;
		}
	}
	
	/**
	 * A special {@link ColumnLabelProvider} which emphasizes all pending
	 * {@link SubcherryMergeEntry} instances.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	public static class SubcherryMergeViewLabelProvider extends ColumnLabelProvider {
		
		@Override
		public Font getFont(final Object element) {
			final SubcherryMergeEntry entry = (SubcherryMergeEntry) element;
			
			if(entry.getState().isWorking()) {
				return SubcherryUI.getInstance().getFontRegistry().getBold(SubcherryUI.DEFAULT);
			}
			
			return super.getFont(element);
		}
	}
}
