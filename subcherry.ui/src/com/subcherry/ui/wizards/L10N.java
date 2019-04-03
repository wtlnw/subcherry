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
package com.subcherry.ui.wizards;

import org.eclipse.osgi.util.NLS;

/**
 * An {@link NLS} specialization for wizard message localization.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class L10N extends NLS {

	/**
	 * The bundle location to load the {@code .properties} file from.
	 */
	private static final String BUNDLE_NAME = "OSGI-INF.l10n.wizards"; //$NON-NLS-1$

	public static String SubcherryMergeWizard_error_message;
	public static String SubcherryMergeWizard_error_status;
	public static String SubcherryMergeWizard_error_title;
	public static String SubcherryMergeWizard_title;

	public static String SubcherryMergeWizardModePage_label_mode_port;
	public static String SubcherryMergeWizardModePage_label_mode_preview;
	public static String SubcherryMergeWizardModePage_label_mode_rebase;
	public static String SubcherryMergeWizardModePage_label_mode_reintegrate;
	public static String SubcherryMergeWizardModePage_label_nocommit;
	public static String SubcherryMergeWizardModePage_label_source;
	public static String SubcherryMergeWizardModePage_label_target;
	public static String SubcherryMergeWizardModePage_label_ticket;
	public static String SubcherryMergeWizardModePage_message;
	public static String SubcherryMergeWizardModePage_mode_title;
	public static String SubcherryMergeWizardModePage_name;
	public static String SubcherryMergeWizardModePage_title;
	public static String SubcherryMergeWizardModePage_title_additional;
	public static String SubcherryMergeWizardModePage_title_selection;
	public static String SubcherryMergeWizardModePage_tooltip_mode_port;
	public static String SubcherryMergeWizardModePage_tooltip_mode_preview;
	public static String SubcherryMergeWizardModePage_tooltip_mode_rebase;
	public static String SubcherryMergeWizardModePage_tooltip_mode_reintegrate;
	public static String SubcherryMergeWizardModePage_tooltip_nocommit;

	public static String SubcherryMergeWizardSourcePage_error_message_branch_invalid;
	public static String SubcherryMergeWizardSourcePage_error_message_no_branch;
	public static String SubcherryMergeWizardSourcePage_error_message_repository_malformed_url;
	public static String SubcherryMergeWizardSourcePage_error_message_repository_invalid;
	public static String SubcherryMergeWizardSourcePage_error_message_repository_no_access;
	public static String SubcherryMergeWizardSourcePage_error_message_revision_invalid;
	public static String SubcherryMergeWizardSourcePage_error_message_revision_range;
	public static String SubcherryMergeWizardSourcePage_error_message_svn;
	public static String SubcherryMergeWizardSourcePage_error_status_svn;
	public static String SubcherryMergeWizardSourcePage_error_title_svn;
	public static String SubcherryMergeWizardSourcePage_hint_source;
	public static String SubcherryMergeWizardSourcePage_hint_start;
	public static String SubcherryMergeWizardSourcePage_label_include_merged;
	public static String SubcherryMergeWizardSourcePage_label_source;
	public static String SubcherryMergeWizardSourcePage_label_source_select;
	public static String SubcherryMergeWizardSourcePage_label_start;
	public static String SubcherryMergeWizardSourcePage_label_start_select;
	public static String SubcherryMergeWizardSourcePage_message;
	public static String SubcherryMergeWizardSourcePage_name;
	public static String SubcherryMergeWizardSourcePage_title;

	public static String SubcherryMergeWizardTargetPage_error_filter_invalid;
	public static String SubcherryMergeWizardTargetPage_error_message_multiple_targets;
	public static String SubcherryMergeWizardTargetPage_error_message_no_module;
	public static String SubcherryMergeWizardTargetPage_error_message_no_target;
	public static String SubcherryMergeWizardTargetPage_hint_filter;
	public static String SubcherryMergeWizardTargetPage_label_add;
	public static String SubcherryMergeWizardTargetPage_label_add_all;
	public static String SubcherryMergeWizardTargetPage_label_remove;
	public static String SubcherryMergeWizardTargetPage_label_remove_all;
	public static String SubcherryMergeWizardTargetPage_label_target;
	public static String SubcherryMergeWizardTargetPage_message;
	public static String SubcherryMergeWizardTargetPage_name;
	public static String SubcherryMergeWizardTargetPage_title;

	public static String SubcherryMergeWizardTicketsPage_error_message;
	public static String SubcherryMergeWizardTicketsPage_error_status;
	public static String SubcherryMergeWizardTicketsPage_error_title;
	public static String SubcherryMergeWizardTicketsPage_hint_filter;
	public static String SubcherryMergeWizardTicketsPage_label_deselect_all;
	public static String SubcherryMergeWizardTicketsPage_label_revision;
	public static String SubcherryMergeWizardTicketsPage_label_select_all;
	public static String SubcherryMergeWizardTicketsPage_label_ticket;
	public static String SubcherryMergeWizardTicketsPage_message;
	public static String SubcherryMergeWizardTicketsPage_name;
	public static String SubcherryMergeWizardTicketsPage_name_ticket_none;
	public static String SubcherryMergeWizardTicketsPage_title;
	public static String SubcherryMergeWizardTicketsPage_tooltip_deselect_all;
	public static String SubcherryMergeWizardTicketsPage_tooltip_select_all;
	
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
