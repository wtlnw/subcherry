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

import com.subcherry.ui.views.SubcherryMergeContext;
import com.subcherry.ui.views.SubcherryMergeEntry;
import com.subcherry.ui.views.SubcherryMergeView;

/**
 * A {@link PropertyTester} extension which provides access to
 * {@link SubcherryMergeEntry} instances of the {@link SubcherryMergeView}'s
 * {@link SubcherryMergeContext}.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryEntryTester extends PropertyTester {
	
	/**
	 * The namespace of properties supported by this {@link PropertyTester}
	 * implementation.
	 */
	public static final String NAMESPACE = "com.subcherry.ui.merge"; //$NON-NLS-1$
	
	/**
	 * The name of the property indicating the state of the current {@link SubcherryMergeEntry}.
	 */
	public static final String PROPERTY_ENTRY = "entry"; //$NON-NLS-1$
	
	/**
	 * A wildcard representing any state.
	 */
	public static final String STATE_ANY = "*"; //$NON-NLS-1$
	
	@Override
	public boolean test(final Object receiver, final String property, final Object[] args, final Object expected) {
		switch(property) {
		case PROPERTY_ENTRY:
			final SubcherryMergeView view = (SubcherryMergeView) receiver;
			final SubcherryMergeContext context = (SubcherryMergeContext) view.getViewer().getInput();
			if(context != null) {
				final SubcherryMergeEntry entry = context.getCurrentEntry();
				if(entry != null) {
					return STATE_ANY.equals(expected) || entry.getState().toString().equals(expected);
				}
			}
			
			return false;
		default:
			return true;
		}
	}
}
