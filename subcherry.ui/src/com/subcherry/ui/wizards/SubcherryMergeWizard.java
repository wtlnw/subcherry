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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.svnclientadapter.SVNUrl;

import com.subcherry.Configuration;
import com.subcherry.repository.command.ClientManager;
import com.subcherry.repository.javahl.HLRepositoryFactory;
import com.subcherry.trac.TracConnection;
import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.model.SubcherryTree;
import com.subcherry.ui.preferences.SubcherryPreferenceConstants;
import com.subcherry.ui.views.SubcherryMergeContext;
import com.subcherry.ui.views.SubcherryMergeView;

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
	 * @see #getSubcherryTree()
	 */
	private SubcherryTree _tree;
	
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
				final IPreferenceStore defPrefs = SubcherryUI.getInstance().getPreferenceStore();
				final String url = defPrefs.getString(SubcherryPreferenceConstants.TRAC_URL);

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
			_config.setWorkspaceRoot(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile());
			
			final IPreferenceStore prefs = SubcherryUI.getInstance().getPreferenceStore();
			_config.setSemanticMoves(prefs.getBoolean(SubcherryPreferenceConstants.SEMANTIC_MOVES));
			_config.setBranchPattern(prefs.getString(SubcherryPreferenceConstants.BRANCH_PATTERN));
			_config.setSkipWaitForTimestamp(prefs.getBoolean(SubcherryPreferenceConstants.SKIP_WAIT_FOR_TIMESTAMP));
			_config.setTrunkPattern(prefs.getString(SubcherryPreferenceConstants.TRUNK_PATTERN));
			
			final IWorkspace workspace = ResourcesPlugin.getWorkspace();
			final SVNProviderPlugin svn = SVNProviderPlugin.getPlugin();
			final List<String> paths = new ArrayList<>();
			
			for (final IProject module : workspace.getRoot().getProjects()) {
				// add only accessible modules which are managed by SVN
				if(module.isAccessible() && svn.isManagedBySubversion(module)) {
					paths.add(module.getName());
					
					// assume that all projects have been checked out from the same branch
					// in order to initialize the target branch configuration
					if(_config.getTargetBranch() == null || _config.getTargetBranch().isEmpty()) {
						final RepositoryProvider provider = SVNTeamProvider.getProvider(module);
						if(provider != null) {
							try {
								final SVNUrl modulePath = svn.getStatusCacheManager().getStatus(module).getUrl();
								final SVNUrl branchPath = modulePath.getParent();
								final SVNUrl repoPath = svn.getRepository(branchPath.toString()).getRepositoryRoot();
								final String branchName = branchPath.toString().replace(repoPath.toString(), "");
								
								_config.setTargetBranch(branchName);
							} catch (final SVNException e) {
								// ignore for now and try with the next project
							}
						}
					}
				}
			}
			_config.setModules(paths.toArray(new String[paths.size()]));
		}
		
		return _config;
	}
	
	/**
	 * @return the {@link SubcherryTree} with the picked cherries or {@code null} if
	 *         no cherries have been picked yet
	 */
	public SubcherryTree getSubcherryTree() {
		return _tree;
	}
	
	/**
	 * Setter for {@link #getSubcherryTree()}.
	 * 
	 * @param tree
	 *            see {@link #getSubcherryTree()}
	 */
	public void setSubcherryTree(final SubcherryTree tree) {
		_tree = tree;
	}
	
	@Override
	public void addPages() {
		addPage(new SubcherryMergeWizardSourcePage());
		addPage(new SubcherryMergeWizardTicketsPage());
		addPage(new SubcherryMergeWizardAdvancedPage());
		addPage(new SubcherryMergeWizardSummaryPage());
	}
	
	@Override
	public boolean canFinish() {
		// Finish button is only enabled for the last page
		return getContainer().getCurrentPage() instanceof SubcherryMergeWizardSummaryPage;
	}
	
	@Override
	public boolean performFinish() {
		try {
			final IWorkbench workbench = PlatformUI.getWorkbench();
			final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			final IWorkbenchPage page = window.getActivePage();
			final SubcherryMergeView view = (SubcherryMergeView) page.showView(SubcherryMergeView.ID);
			final SubcherryMergeContext context = new SubcherryMergeContext(getSubcherryTree());
			
			view.getViewer().setInput(context);
		} catch (WorkbenchException e) {
			final IStatus status = new Status(IStatus.ERROR, SubcherryUI.id(), "Failed to open merge view", e);
			ErrorDialog.openError(getShell(), "Subcherry Merge Wizard", "Wizard cannot be finished.", status);
			
			return false;
		}
		
		return true;
	}
	
	@Override
	public void dispose() {
		_manager = null;
		_config = null;
		_trac = null;
		_tree = null;
		
		super.dispose();
	}
}
