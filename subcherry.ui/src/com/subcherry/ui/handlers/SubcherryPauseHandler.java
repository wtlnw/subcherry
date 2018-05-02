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
import org.eclipse.core.runtime.jobs.Job;

import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.jobs.AbstractSubcherryJob;

/**
 * An {@link AbstractSubcherryHandler} implementation for {@link SubcherryUI} which
 * pauses automatic merge execution.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryPauseHandler extends AbstractSubcherryHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		// terminate all currently scheduled SubcherryMergeJobs
		for (final Job job : Job.getJobManager().find(AbstractSubcherryJob.class)) {
			job.cancel();
		}
		
		return null;
	}
}
