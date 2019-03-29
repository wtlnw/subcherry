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
package com.subcherry.ui.views;

import org.eclipse.osgi.util.NLS;

/**
 * An {@link NLS} specialization for view message localization.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class L10N extends NLS {

	/**
	 * The bundle location to load the {@code .properties} file from.
	 */
	private static final String BUNDLE_NAME = "OSGI-INF.l10n.views"; //$NON-NLS-1$
	
	public static String SubcherryMergeView_author_column_label;
	public static String SubcherryMergeView_column_label_message;
	public static String SubcherryMergeView_revision_column_label;
	public static String SubcherryMergeView_state_column_label;
	public static String SubcherryMergeView_state_column_tooltip_committed;
	public static String SubcherryMergeView_state_column_tooltip_conflict;
	public static String SubcherryMergeView_state_column_tooltip_error;
	public static String SubcherryMergeView_state_column_tooltip_merged;
	public static String SubcherryMergeView_state_column_tooltip_new;
	public static String SubcherryMergeView_state_column_tooltip_nocommit;
	public static String SubcherryMergeView_state_column_tooltip_skipped;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, L10N.class);
	}

	/**
	 * Create a {@link L10N}s.
	 */
	private L10N() {
		// no instantiation
	}
}
