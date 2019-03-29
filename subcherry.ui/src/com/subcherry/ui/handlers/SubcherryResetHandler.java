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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.subcherry.ui.operations.SubcherryResetOperation;
import com.subcherry.ui.views.SubcherryMergeContext;
import com.subcherry.ui.views.SubcherryMergeEntry;
import com.subcherry.ui.views.SubcherryMergeView;

/**
 * An {@link AbstractSubcherryHandler} which resets the
 * {@link SubcherryMergeContext#getCurrentEntry()}.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class SubcherryResetHandler extends AbstractSubcherryHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		try {
			final SubcherryMergeEntry entry = getContext(event).getCurrentEntry();
			final SubcherryMergeView view = getView(event);
			final Shell shell = view.getSite().getShell();
			
			final String title = L10N.SubcherryResetHandler_confirm_title;
			final StringBuilder message = new StringBuilder();
			message.append(L10N.SubcherryResetHandler_confirm_message_1);
			message.append(entry.getMessage().getLogEntryMessage());
			message.append(L10N.SubcherryResetHandler_confirm_message_2);
			message.append(L10N.SubcherryResetHandler_confirm_message_3);
			message.append(L10N.SubcherryResetHandler_confirm_message_4);
			
			final MessageDialog dialog = new MessageDialog(shell, title, null, message.toString(), MessageDialog.CONFIRM, 0, L10N.SubcherryResetHandler_button_reset_label, IDialogConstants.CANCEL_LABEL);
			
			if (dialog.open() == IDialogConstants.OK_ID) {
				new SubcherryResetOperation(view).run();
			}
		} catch (Throwable e) {
			throw new ExecutionException(null, e);
		}
		
		return null;
	}
}
