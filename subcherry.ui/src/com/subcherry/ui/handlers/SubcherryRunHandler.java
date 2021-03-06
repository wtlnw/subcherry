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
import org.eclipse.jface.dialogs.ProgressMonitorDialog;

import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.operations.SubcherryMergeOperation;
import com.subcherry.ui.views.SubcherryMergeView;

/**
 * An {@link AbstractSubcherryHandler} implementation for {@link SubcherryUI} which
 * merges all revisions automatically and stops only upon merge conflicts or
 * commit errors.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class SubcherryRunHandler extends AbstractSubcherryHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		try {
			final SubcherryMergeView view = getView(event);
			final boolean fork = true;
			final boolean cancelable = true;

			new ProgressMonitorDialog(view.getSite().getShell()).run(fork, cancelable, progress -> {
				new SubcherryMergeOperation(view).run(progress);
			});
		} catch(final InterruptedException e) {
			// user cancellation -> ignore
		} catch (Throwable e) {
			throw new ExecutionException(e.getMessage(), e);
		}
		
		return null;
	}
}
