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
package com.subcherry.ui.widgets;

import org.eclipse.osgi.util.NLS;

/**
 * An {@link NLS} specialization for widget message localization.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class L10N extends NLS {

	/**
	 * The bundle location to load the {@code .properties} file from.
	 */
	private static final String BUNDLE_NAME = "OSGI-INF.l10n.widgets"; //$NON-NLS-1$

	public static String LogEntryView_label_author;
	public static String LogEntryView_label_copied_from;
	public static String LogEntryView_label_date;
	public static String LogEntryView_label_message;
	public static String LogEntryView_label_resources;
	public static String LogEntryView_label_revision;
	public static String LogEntryView_title_revision;
	
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
