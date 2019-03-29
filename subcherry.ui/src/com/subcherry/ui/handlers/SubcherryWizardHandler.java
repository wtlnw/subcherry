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
package com.subcherry.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.preferences.SubcherryPreferenceConstants;
import com.subcherry.ui.wizards.SubcherryMergeWizard;

/**
 * An {@link AbstractHandler} implementation for {@link SubcherryUI} which opens the merge wizard.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryWizardHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		
		// check if trac connection has been specified
		final IPreferenceStore prefs = SubcherryUI.getInstance().getPreferenceStore();
		final String url = prefs.getString(SubcherryPreferenceConstants.TRAC_URL);
		if(url == null) {
			MessageDialog.openError(window.getShell(), L10N.SubcherryWizardHandler_error_title, L10N.SubcherryWizardHandler_error_message);
		} else {
			final SubcherryMergeWizard wizard = new SubcherryMergeWizard();
			final WizardDialog dialog = new WizardDialog(window.getShell(), wizard);
			dialog.open();
		}

		return null;
	}
}
