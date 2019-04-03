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
package com.subcherry.ui.help;

import com.subcherry.ui.SubcherryUI;

/**
 * An interface declaring help context identifiers for F1 help.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public interface IHelpContextIds {

	String MERGE_WIZARD_SOURCE = "merge_wizard_source"; //$NON-NLS-1$
	String MERGE_WIZARD_TARGET = "merge_wizard_target"; //$NON-NLS-1$
	String MERGE_WIZARD_TICKETS = "merge_wizard_tickets"; //$NON-NLS-1$
	String MERGE_WIZARD_MODE = "merge_wizard_mode"; //$NON-NLS-1$
	
	String MERGE_VIEW = "merge_view"; //$NON-NLS-1$
	String MERGE_VIEW_DETAILS = "merge_view_details"; //$NON-NLS-1$
	
	String MERGE_PREFERENCES = "merge_preferences"; //$NON-NLS-1$
	String MERGE_PREFERENCES_CREDENTIALS = "merge_preferences_credentials"; //$NON-NLS-1$
	
	static String id(final String id) {
		return SubcherryUI.id() + '.' + id;
	}
}
