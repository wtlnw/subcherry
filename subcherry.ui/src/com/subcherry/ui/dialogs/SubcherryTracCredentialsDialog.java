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

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.preferences.SubcherryPreferenceConstants;

/**
 * An {@link TrayDialog} implementation allowing users to enter their trac
 * credentials.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryTracCredentialsDialog extends TrayDialog {

	/**
	 * The {@link Text} field for user name input
	 */
	private Text _username;
	
	/**
	 * The {@link Text} field for password input
	 */
	private Text _password;
	
	/**
	 * Create a {@link SubcherryTracCredentialsDialog}.
	 * 
	 * @param shell
	 *            see {@link #getShell()}
	 */
	public SubcherryTracCredentialsDialog(final Shell shell) {
		super(shell);
		
		setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
		setHelpAvailable(false);
	}

	@Override
	protected void configureShell(final Shell newShell) {
		super.configureShell(newShell);

		newShell.setText(L10N.SubcherryTracCredentialsDialog_title);
	}
	
	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite contents = (Composite) super.createDialogArea(parent);
		final GridLayout layout = (GridLayout) contents.getLayout();
		layout.numColumns = 2;
		
		new Label(contents, SWT.NONE).setText(L10N.SubcherryTracCredentialsDialog_username);
		_username = new Text(contents, SWT.BORDER);
		_username.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		new Label(contents, SWT.NONE).setText(L10N.SubcherryTracCredentialsDialog_password);
		_password = new Text(contents, SWT.BORDER | SWT.PASSWORD);
		_password.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// initialize the input field with current values
		final String username = getUsername();
		if(username != null) {
			_username.setText(username);
		}
		
		final String password = getPassword();
		if(password != null) {
			_password.setText(password);
		}
		
		return contents;
	}
	
	@Override
	protected void okPressed() {
		final ISecurePreferences prefs = SubcherryUI.getInstance().getSecurePreferences();
		
		try {
			prefs.put(SubcherryPreferenceConstants.TRAC_USERNAME, _username.getText(), true);
			prefs.put(SubcherryPreferenceConstants.TRAC_PASSWORD, _password.getText(), true);
		} catch (StorageException e) {
			throw new RuntimeException(e);
		}

		super.okPressed();
	}
	
	/**
	 * @return the current {@link SubcherryPreferenceConstants#TRAC_USERNAME}
	 */
	private String getUsername() {
		try {
			return SubcherryUI.getInstance().getSecurePreferences().get(SubcherryPreferenceConstants.TRAC_USERNAME, null);
		} catch (StorageException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @return the current {@link SubcherryPreferenceConstants#TRAC_PASSWORD}
	 */
	private String getPassword() {
		try {
			return SubcherryUI.getInstance().getSecurePreferences().get(SubcherryPreferenceConstants.TRAC_PASSWORD, null);
		} catch (StorageException e) {
			throw new RuntimeException(e);
		}
	}
}
