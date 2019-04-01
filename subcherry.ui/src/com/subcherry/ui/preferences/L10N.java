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

import org.eclipse.osgi.util.NLS;

/**
 * An {@link NLS} specialization for preference message localization.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class L10N extends NLS {
	
	/**
	 * The bundle location to load the {@code .properties} file from.
	 */
	private static final String BUNDLE_NAME = "OSGI-INF.l10n.preferences"; //$NON-NLS-1$
	
	public static String SubcherryPreferencePage_description;
	public static String SubcherryPreferencePage_label_branch_pattern;
	public static String SubcherryPreferencePage_label_credentials;
	public static String SubcherryPreferencePage_label_semantic_moves;
	public static String SubcherryPreferencePage_label_trac_settings;
	public static String SubcherryPreferencePage_label_trac_url;
	public static String SubcherryPreferencePage_label_trunk_pattern;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, L10N.class);
	}

	/**
	 * Create a {@link L10N}.
	 */
	private L10N() {
		// no instantiation
	}
}
