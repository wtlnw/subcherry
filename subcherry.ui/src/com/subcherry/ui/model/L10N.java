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
package com.subcherry.ui.model;

import org.eclipse.osgi.util.NLS;

/**
 * An {@link NLS} specialization for model message localization.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class L10N extends NLS {

	/**
	 * The bundle location to load the {@code .properties} file from.
	 */
	private static final String BUNDLE_NAME = "OSGI-INF.l10n.models"; //$NON-NLS-1$
	
	public static String SubcherryTree_progress_analyze;
	public static String SubcherryTree_progress_compute;
	public static String SubcherryTree_progress_error_svn_message;
	public static String SubcherryTree_progress_error_svn_status;
	public static String SubcherryTree_progress_error_svn_title;
	public static String SubcherryTree_progress_error_trac_message;
	public static String SubcherryTree_progress_error_trac_status;
	public static String SubcherryTree_progress_error_trac_title;
	public static String SubcherryTree_progress_init;
	public static String SubcherryTree_progress_parse;
	public static String SubcherryTree_progress_paths;
	public static String SubcherryTree_progress_reading;
	
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
