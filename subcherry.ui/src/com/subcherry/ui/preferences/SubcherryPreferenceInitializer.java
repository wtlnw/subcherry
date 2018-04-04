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

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;

import com.subcherry.ui.SubcherryUI;

/**
 * An {@link AbstractPreferenceInitializer} implementation for {@link SubcherryUI}.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryPreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		final Preferences node = DefaultScope.INSTANCE.getNode(SubcherryUI.getInstance().getBundle().getSymbolicName());
		
		// automatically detect common modules
		node.putBoolean("detectCommonModules", true);

		// detect semantic moves by default
		node.putBoolean("semanticMoves", true);
		
		// do not wait for the timestamp
		node.putBoolean("skipWaitForTimestamp", true);
		
		// assume default SVN layout
		node.put("trunkPattern", "/trunk/[^/\\._]+/|/trunk/");
		node.put("branchPattern", "/branches/[^/]+/[^/]+/|/tags/[^/]+/[^/]+/");
	}
}
