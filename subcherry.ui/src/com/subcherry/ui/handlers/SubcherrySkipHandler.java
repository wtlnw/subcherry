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

import com.subcherry.ui.jobs.SubcherryRevertJob;
import com.subcherry.ui.jobs.SubcherrySkipJob;
import com.subcherry.ui.views.SubcherryMergeContext;
import com.subcherry.ui.views.SubcherryMergeEntry;

/**
 * An {@link AbstractSubcherryHandler} implementation which reverts uncommitted changes
 * and skips the current {@link SubcherryMergeEntry}.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class SubcherrySkipHandler extends AbstractSubcherryHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final SubcherryMergeContext context = getContext(event);
		final SubcherryMergeEntry entry = context.getCurrentEntry();
		
		if (entry != null) {
			final SubcherrySkipJob skip = new SubcherrySkipJob(context);
			
			// schedule revert for work-in-progress entries
			if (entry.getState().isWorking()) {
				new SubcherryRevertJob(context).next(skip).schedule();
			} else {
				skip.schedule();
			}
		}
		
		return null;
	}
}
