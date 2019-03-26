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
package com.subcherry.ui.expressions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.jobs.Job;

import com.subcherry.ui.operations.AbstractSubcherryOperation;
import com.subcherry.ui.views.SubcherryMergeView;

/**
 * A {@link PropertyTester} extension which provides access to the
 * {@link SubcherryMergeView}'s state.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryStateTester extends PropertyTester {
	
	/**
	 * The namespace of properties supported by this {@link PropertyTester}
	 * implementation.
	 */
	public static final String NAMESPACE = "com.subcherry.ui.merge";
	
	/**
	 * The name of the boolean property indicating whether one of the merge jobs is
	 * being executed or not.
	 */
	public static final String PROPERTY_STATE = "state";
	
	/**
	 * The name of the state indicating that the merge process is running.
	 */
	public static final String STATE_RUNNING = "running";
	
	@Override
	public boolean test(final Object receiver, final String property, final Object[] args, final Object expected) {
		switch(property) {
		case PROPERTY_STATE: {
			return STATE_RUNNING.equals(expected) && Job.getJobManager().find(AbstractSubcherryOperation.class).length > 0;
		} default:
			return false;
		}
	}
}
