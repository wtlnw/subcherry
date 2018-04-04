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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.subcherry.ui.SubcherryUI;

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
		addField(new BooleanFieldEditor("detectCommonModules", "&Detect common modules", container));
		
		// detect semantic moves by default
		addField(new BooleanFieldEditor("semanticMoves", "&Enable semantic moves", container));
		
		// do not wait for the timestamp
		addField(new BooleanFieldEditor("skipWaitForTimestamp", "&Skip waiting for timestamps", container));
		
		// assume default SVN layout
		addField(new StringFieldEditor("trunkPattern", "&Trunk pattern:", container));
		addField(new StringFieldEditor("branchPattern", "&Brunch pattern:", container));
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(SubcherryUI.getInstance().getPreferenceStore());
		setDescription("General Cherry Picking Settings:");
	}
}