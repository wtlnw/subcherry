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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.status.StatusCacheManager;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;

import com.subcherry.repository.core.LogEntryPath;
import com.subcherry.ui.views.SubcherryMergeEntry;
import com.subcherry.ui.widgets.LogEntryView;
import com.subcherry.ui.widgets.LogEntryView.LogEntryPathsLabelProvider;
import com.subcherry.ui.widgets.LogEntryView.TreeNode;
import com.subcherry.utils.Path;
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
	 * @see #entry()
	 */
	private final SubcherryMergeEntry _entry;

	/**
	 * @see #editor()
	 */
	private LogEntryView _editor;
	
	/**
	 * Create a {@link SubcherryEditDialog}.
	 * 
	 * @param shell
	 *            see {@link #getShell()}
	 * @param entry
	 *            see {@link #entry()}
	 */
	public SubcherryEditDialog(final Shell shell, final SubcherryMergeEntry entry) {
		super(shell);

		// remember the entry itself
		_entry = entry;
		
		// change shell style
		setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
	}
	
	/**
	 * @return the {@link SubcherryMergeEntry} to resolve the conflicts for
	 */
	public SubcherryMergeEntry entry() {
		return _entry;
	}
	
	/**
	 * @return the {@link LogEntryView} or {@code null} if the dialog is not opened.
	 *         The returned {@link LogEntryView} may have been disposed for closed
	 *         dialogs.
	 */
	public LogEntryView editor() {
		return _editor;
	}
	
	@Override
	protected void configureShell(final Shell newShell) {
		super.configureShell(newShell);
		
		newShell.setText("Edit Revision");
	}
	
	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite content = (Composite) super.createDialogArea(parent);
		content.setLayout(new GridLayout());
		
		_editor = new LogEntryView(content, SWT.VERTICAL);
		_editor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// install a custom label provider for the resources viewer
		_editor.resources().setLabelProvider(new LogEntryPathsStatusLabelProvider(entry()));
		
		// initialize the editor with the current dialog's entry
		_editor.setLogEntry(entry().getChange());
		
		// make sure to use the rewritten message (if available)
		_editor.message().setText(entry().getMessage());
		_editor.message().setEditable(entry().getState().isPending());
		
		return content;
	}
	
	@Override
	protected void okPressed() {
		final String message = editor().message().getText();
		entry().setMessage(message.isEmpty() ? null : message);
		
		super.okPressed();
	}
	
	/**
	 * A {@link LogEntryPathsLabelProvider} specialization using
	 * {@link LocalResourceStatus} to compute node items.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	public static final class LogEntryPathsStatusLabelProvider extends LogEntryPathsLabelProvider {
		
		/**
		 * @see #entry()
		 */
		private final SubcherryMergeEntry _entry;
		
		/**
		 * Create a {@link LogEntryPathsStatusLabelProvider}.
		 * 
		 * @param entry
		 *            see {@link #entry()}
		 */
		public LogEntryPathsStatusLabelProvider(final SubcherryMergeEntry entry) {
			_entry = entry;
		}

		/**
		 * @return the {@link SubcherryMergeEntry} to provide labels for
		 */
		public SubcherryMergeEntry entry() {
			return _entry;
		}
		
		@Override
		public Image getImage(final Object element) {
			// for entries not being worked on, display the estimated changes.
			if(!entry().getState().isWorking()) {
				return super.getImage(element);
			}
			
			final String imageId = getImageId(element);
			if (imageId == null) {
				return null;
			}
			
			// check the status of the resource represented by the given node
			final TreeNode node = (TreeNode) element;
			final String path;
			
			if(node.value() instanceof IPath) {
				path = ((IPath) node.value()).toPortableString();
			} else {
				path = ((LogEntryPath) node.value()).getPath();
			}
			
			final PathParser pathParser = entry().getContext().getPathParser();
			final Path parsedPath = pathParser.parsePath(path);
			
			final IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(parsedPath.getResource());
			if(resource != null) {
				final StatusCacheManager states = SVNProviderPlugin.getPlugin().getStatusCacheManager();
				try {
					final LocalResourceStatus state = states.getStatus(resource);
					if(state != null) {
						final SVNStatusKind statusKind = state.getStatusKind();
						final String overlayId;
						
						if(SVNStatusKind.CONFLICTED == statusKind) {
							if (state.hasTreeConflict()) {
								overlayId = ISVNUIConstants.IMG_TREE_CONFLICT;
							} else if (state.isPropConflicted()) {
								overlayId = ISVNUIConstants.IMG_PROPERTY_CONFLICTED;
							} else if(state.isTextConflicted()) {
								overlayId = ISVNUIConstants.IMG_TEXT_CONFLICTED;
							} else {
								overlayId = ISVNUIConstants.IMG_CONFLICTED;
							}
						} else if(SVNStatusKind.MISSING == statusKind) {
							overlayId = ISVNUIConstants.IMG_TREE_CONFLICT;
						} else if(SVNStatusKind.OBSTRUCTED == statusKind) {
							overlayId = ISVNUIConstants.IMG_TREE_CONFLICT;
						} else if(state.isPropModified()) {
							overlayId = ISVNUIConstants.IMG_PROPERTY_CHANGED;
						} else if(SVNStatusKind.NORMAL == statusKind) {
							switch(resource.getType()) {
							case IResource.FILE:
								return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
							default:
								return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
							}
						} else {
							overlayId = null;
						}
						
						if(overlayId != null) {
							final String iconId = imageId.concat(overlayId);
							final ImageRegistry registry = SVNUIPlugin.getPlugin().getImageRegistry();
							
							if(registry.getDescriptor(iconId) == null) {
								final Image image = registry.get(imageId);
								final ImageDescriptor overlay = SVNUIPlugin.getPlugin().getImageDescriptor(overlayId);
								
								registry.put(iconId, new DecorationOverlayIcon(image, new ImageDescriptor[] { overlay, null, null, null, null }));
							}
							
							return registry.get(iconId);
						}
					}
				} catch (final SVNException e) {
					e.printStackTrace();
				}
			}
			
			// default behavior is to display the change type icon
			return SVNUIPlugin.getImage(imageId);
		}
	}
}
