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

import org.eclipse.jface.wizard.Wizard;

import com.subcherry.ui.SubcherryUI;

/**
 * An {@link Wizard} implementation for {@link SubcherryUI} which allows users to
 * initialize the merge process.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryMergeWizard extends Wizard {

	/**
	 * @see #getUrl();
	 */
	private String _branch;
	
	/**
	 * @see #getRevision();
	 */
	private String _revision;
	
	/**
	 * Create a {@link SubcherryMergeWizard}.
	 */
	public SubcherryMergeWizard() {
		setWindowTitle("Subcherry Merge");
	}
	
	/**
	 * @return the source branch URL or {@code null} if none was selected yet
	 */
	public String getBranch() {
		return _branch;
	}

	/**
	 * Setter for {@link #getBranch()}.
	 * 
	 * @param url
	 *            see {@link #getBranch()}
	 */
	public void setBranch(final String url) {
		_branch = url;
	}
	
	/**
	 * @return the start revision number (as {@link String}) or {@code null} to use
	 *         the very first revision of {@link #getBranch()}
	 */
	public String getRevision() {
		return _revision;
	}
	
	/**
	 * Setter for {@link #getRevision()}.
	 * 
	 * @param revision
	 *            see {@link #getRevision()}
	 */
	public void setRevision(final String revision) {
		_revision = revision;
	}
	
	@Override
	public void addPages() {
		addPage(new SubcherryMergeWizardSourcePage());
		addPage(new SubcherryMergeWizardTicketsPage());
	}
	
	@Override
	public boolean performFinish() {
		// 1. Open/Display the SubcherryMergeView
		// 2. Initialize the SubcherryMergeView with the selected revision
		// 3. Start the merge process
		return true;
	}
}
