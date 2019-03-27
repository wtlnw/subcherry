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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.part.ViewPart;

import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.dialogs.SubcherryRevisionDialog;

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
		final TableViewer viewer = new TableViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		final Table table = viewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);
		
		viewer.addDoubleClickListener(event -> {
			final ISelection selection = event.getSelection();
			if (selection instanceof IStructuredSelection) {
				final Object entry = ((IStructuredSelection) selection).getFirstElement();
				if (entry instanceof SubcherryMergeEntry) {
					new SubcherryRevisionDialog(event.getViewer().getControl().getShell(), (SubcherryMergeEntry) entry).open();
				}
			}
		});
		
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
				case ERROR: {
					final Throwable error = entry.getError();
					if (error instanceof InvocationTargetException) {
						return ((InvocationTargetException) error).getTargetException().getLocalizedMessage();
					} else {
						return error.getLocalizedMessage();
					}
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
				
				return entry.getMessage().getMergeMessage();
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

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
			// dispose the old input (if necessary)
			if (oldInput instanceof SubcherryMergeContext) {
				((SubcherryMergeContext) oldInput).dispose();
			}
		}

		@Override
		public Object[] getElements(final Object input) {
			if (input instanceof SubcherryMergeContext) {
				return ((SubcherryMergeContext) input).getAllEntries().toArray();
			}
			
			return new Object[0];
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
				return SubcherryUI.getBoldDefault();
			}
			
			return super.getFont(element);
		}
	}
}
