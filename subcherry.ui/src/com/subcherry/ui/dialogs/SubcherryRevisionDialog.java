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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.views.SubcherryMergeEntry;
import com.subcherry.ui.widgets.LabeledComposite;
import com.subcherry.ui.widgets.LogEntryView;

/**
 * A {@link TrayDialog} implementation displaying revision details for a
 * {@link SubcherryMergeEntry}.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class SubcherryRevisionDialog extends TitleAreaDialog {

	/**
	 * @see #entry()
	 */
	private final SubcherryMergeEntry _entry;
	
	/**
	 * Create a {@link SubcherryRevisionDialog}.
	 * 
	 * @param shell
	 *            see {@link #getShell()}
	 * @param entry
	 *            see {@link #entry()}
	 */
	public SubcherryRevisionDialog(final Shell shell, final SubcherryMergeEntry entry) {
		super(shell);
		
		// remember the entry itself
		_entry = entry;
		
		// change shell style
		setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
		setHelpAvailable(false);
	}
	
	/**
	 * @return the {@link SubcherryMergeEntry} to resolve the conflicts for
	 */
	public SubcherryMergeEntry entry() {
		return _entry;
	}
	
	@Override
	protected void configureShell(final Shell newShell) {
		super.configureShell(newShell);
		
		newShell.setText("Revision Details");
	}
	
	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
	@Override
	protected Control createDialogArea(final Composite parent) {
		// configure the title area with title and message
		configureTitleArea();
		
		final Composite container = (Composite) super.createDialogArea(parent);
		final Composite content = new Composite(container, SWT.NONE);
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		content.setLayout(GridLayoutFactory.swtDefaults().margins(10, 5).create());
		
		final LogEntryView view = new LogEntryView(content, SWT.VERTICAL);
		view.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		view.setLogEntry(entry().getChange());
		view.message().setText(entry().getMessage().getMergeMessage());
		
		final Throwable error = entry().getError();
		if (error != null) {
			final LabeledComposite errorContents = new LabeledComposite(content, SWT.NONE);
			errorContents.setLabelText("Error details");
			errorContents.setLabelFont(SubcherryUI.getBoldDefault());
			errorContents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			errorContents.setLayout(GridLayoutFactory.fillDefaults().margins(0, 5).create());
			
			final StyledText errorText = new StyledText(errorContents, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
			errorText.setForeground(errorText.getDisplay().getSystemColor(SWT.COLOR_RED));
			errorText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			if(error instanceof InvocationTargetException) {
				errorText.setText(((InvocationTargetException) error).getTargetException().getLocalizedMessage());
			} else {
				errorText.setText(error.getLocalizedMessage());
			}
		}
		
		return container;
	}

	/**
	 * Configure the dialog's title and message according to the {@link #entry()}'s
	 * state.
	 */
	private void configureTitleArea() {
		setTitle(String.format("Revision details for r%d.", entry().getChange().getRevision()));

		switch(entry().getState()) {
		case COMMITTED:
			setMessage("Successfully merged and committed.", IMessageProvider.NONE);
			break;
		case CONFLICT:
			setMessage("Conflicting changes detected.", IMessageProvider.WARNING);
			break;
		case ERROR:
			setMessage("Errors detected. See below for details.", IMessageProvider.ERROR);
			break;
		case MERGED:
			setMessage("Successfully merged, commit pending.", IMessageProvider.INFORMATION);
			break;
		case NEW:
			setMessage("Merge pending.", IMessageProvider.NONE);
			break;
		case NO_COMMIT:
			setMessage("Merged without committing changes.", IMessageProvider.INFORMATION);
			break;
		case SKIPPED:
			setMessage("Skipped without merging.", IMessageProvider.INFORMATION);
			break;
		default:
			break;
		}
	}
}
