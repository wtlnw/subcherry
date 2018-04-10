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
package com.subcherry.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.dialogs.SubcherryTracCredentialsDialog;

/**
 * An {@link IWorkbenchPreferencePage} implementation for {@link SubcherryUI}.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/**
	 * Create a new {@link SubcherryPreferencePage}.
	 */
	public SubcherryPreferencePage() {
		super(GRID);
	}

	@Override
	public void createFieldEditors() {
		final Composite container = getFieldEditorParent();
		
		// automatically detect common modules
		addField(new BooleanFieldEditor(SubcherryPreferenceConstants.DETECT_COMMON_MODULES, "&Detect common modules", container));
		
		// detect semantic moves by default
		addField(new BooleanFieldEditor(SubcherryPreferenceConstants.SEMANTIC_MOVES, "&Enable semantic moves", container));
		
		// do not wait for the timestamp
		addField(new BooleanFieldEditor(SubcherryPreferenceConstants.SKIP_WAIT_FOR_TIMESTAMP, "&Skip waiting for timestamps", container));
		
		// assume default SVN layout
		addField(new StringFieldEditor(SubcherryPreferenceConstants.TRUNK_PATTERN, "&Trunk pattern:", container));
		addField(new StringFieldEditor(SubcherryPreferenceConstants.BRANCH_PATTERN, "&Brunch pattern:", container));
		
		// trac preferences
		addTracFields(container);
	}

	/**
	 * Add {@link Control}s allowing users to edit trac settings.
	 * 
	 * @param parent
	 *            the parent {@link Composite}
	 */
	private void addTracFields(final Composite parent) {
		final Group container = new Group(parent, SWT.NONE);
		container.setText("Trac Connection Settings");
		
		final GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		layoutData.verticalIndent = 8;
		container.setLayoutData(layoutData);
		
		// add trac URL preference
		addField(new StringFieldEditor(SubcherryPreferenceConstants.TRAC_URL, "Trac &URL:", container));
		
		// allow users to change their credentials
		final Button tracCredentials = new Button(container, SWT.PUSH);
		tracCredentials.setText("Credentials...");
		tracCredentials.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				new SubcherryTracCredentialsDialog(getShell()).open();
			}
		});
		
		// update the layout AFTER adding controls to the container
		final GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 5;
		layout.marginHeight = 5;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 5;
		container.setLayout(layout);
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(SubcherryUI.getInstance().getPreferenceStore());
		setDescription("General Cherry Picking Settings:");
	}
}