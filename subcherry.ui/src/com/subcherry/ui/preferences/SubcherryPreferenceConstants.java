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

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.osgi.service.prefs.Preferences;

import com.subcherry.ui.SubcherryUI;

/**
 * An interface defining preference constant names for {@link SubcherryUI}.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public interface SubcherryPreferenceConstants {

	/**
	 * The name of the key in {@link Preferences} defining whether semantic moves
	 * should be detected or not.
	 */
	String SEMANTIC_MOVES = "semanticMoves"; //$NON-NLS-1$
	
	/**
	 * The name of the key in {@link Preferences} containing the trunk name pattern.
	 */
	String TRUNK_PATTERN = "trunkPattern"; //$NON-NLS-1$
	
	/**
	 * The name of the key in {@link Preferences} containing the branch name
	 * pattern.
	 */
	String BRANCH_PATTERN = "branchPattern"; //$NON-NLS-1$
	
	/**
	 * The name of the key in {@link Preferences} containing the trac URL.
	 */
	String TRAC_URL = "trac.url"; //$NON-NLS-1$
	
	/**
	 * The name of the key in {@link ISecurePreferences} containing the trac user
	 * name.
	 */
	String TRAC_USERNAME = "trac.username"; //$NON-NLS-1$

	/**
	 * The name of the key in {@link ISecurePreferences} containing the trac
	 * password.
	 */
	String TRAC_PASSWORD = "trac.password"; //$NON-NLS-1$
}
