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

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.subcherry.ui.views.SubcherryMergeEntry;
import com.subcherry.ui.widgets.LogEntryView;

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
		_editor.setLogEntry(entry().getChange());
		_editor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
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
}
