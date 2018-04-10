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

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.jface.wizard.Wizard;
import org.osgi.service.prefs.Preferences;

import com.subcherry.Configuration;
import com.subcherry.repository.command.ClientManager;
import com.subcherry.repository.javahl.HLRepositoryFactory;
import com.subcherry.trac.TracConnection;
import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.preferences.SubcherryPreferenceConstants;

import de.haumacher.common.config.ValueFactory;

/**
 * An {@link Wizard} implementation for {@link SubcherryUI} which allows users to
 * initialize the merge process.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryMergeWizard extends Wizard {

	/**
	 * @see #getClientManager()
	 */
	private ClientManager _manager;
	
	/**
	 * @see #getTracConnection();
	 */
	private TracConnection _trac;
	
	/**
	 * @see #getConfiguration()
	 */
	private Configuration _config;
	
	/**
	 * Create a {@link SubcherryMergeWizard}.
	 */
	public SubcherryMergeWizard() {
		setWindowTitle("Subcherry Merge");
	}
	
	/**
	 * @return the {@link ClientManager} to be used for SVN repository access
	 */
	public ClientManager getClientManager() {
		if (_manager == null) {
			// use SVN internal authentication mechanics!
			_manager = new HLRepositoryFactory().createClientManager(null);
		}
		
		return _manager;
	}
	
	/**
	 * @return the {@link TracConnection} to be used for accessing trac tickets
	 */
	public TracConnection getTracConnection() {
		if(_trac == null) {
			try {
				final Preferences defPrefs = SubcherryUI.getInstance().getPreferences();
				final String url = defPrefs.get(SubcherryPreferenceConstants.TRAC_URL, null);

				final ISecurePreferences secPrefs = SubcherryUI.getInstance().getSecurePreferences();
				final String username = secPrefs.get(SubcherryPreferenceConstants.TRAC_USERNAME, null);
				final String password = secPrefs.get(SubcherryPreferenceConstants.TRAC_PASSWORD, null);

				_trac = new TracConnection(url, username, password);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		return _trac;
	}
	
	/**
	 * @return the {@link Configuration} for the merge process
	 */
	public Configuration getConfiguration() {
		if(_config == null) {
			_config = ValueFactory.newInstance(Configuration.class);
		}
		
		return _config;
	}
	
	@Override
	public void addPages() {
		addPage(new SubcherryMergeWizardSourcePage());
		addPage(new SubcherryMergeWizardTicketsPage());
	}
	
	@Override
	public boolean canFinish() {
		// disable Finish button when entering merge source and revision
		if(getContainer().getCurrentPage() instanceof SubcherryMergeWizardSourcePage) {
			return false;
		}
		
		return super.canFinish();
	}
	
	@Override
	public boolean performFinish() {
		// 1. Open/Display the SubcherryMergeView
		// 2. Initialize the SubcherryMergeView with the selected revision
		// 3. Start the merge process
		return true;
	}
}
