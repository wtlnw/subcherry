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

import java.util.ResourceBundle.Control;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.ui.dialogs.ChooseUrlDialog;
import org.tigris.subversion.subclipse.ui.dialogs.HistoryDialog;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;
import org.tigris.subversion.svnclientadapter.SVNUrl;

import com.subcherry.ui.DelayedModifyListener;
import com.subcherry.ui.SubcherryUI;

/**
 * An {@link WizardPage} implementation for {@link SubcherryMergeWizard} which
 * allows users to select the source branch and revision to merge the changes
 * from.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryMergeWizardSourcePage extends WizardPage {
	
	/**
	 * @see #createBranchSelector(Composite)
	 */
	private Text _branch;
	
	/**
	 * @see #createRevisionSelector(Composite)
	 */
	private Text _revision;
	
	/**
	 * Create a {@link SubcherryMergeWizardSourcePage}.
	 */
	public SubcherryMergeWizardSourcePage() {
		super("Branch");
		
		setTitle("SVN Cherry Picking With Subcherry");
		setMessage("Please select the source branch with an optional start revision.");
	}
	
	@Override
	public SubcherryMergeWizard getWizard() {
		return (SubcherryMergeWizard) super.getWizard();
	}
	
	@Override
	public void setErrorMessage(final String newMessage) {
		super.setErrorMessage(newMessage);
	}
	
	@Override
	public void createControl(final Composite parent) {
		final Composite contents = new Composite(parent, SWT.None);
		contents.setLayout(new GridLayout(3, false));
	
		_branch = createBranchSelector(contents);
		_revision = createRevisionSelector(contents);
		
		setControl(contents);
		setPageComplete(false);
	}

	/**
	 * Create {@link Control}s allowing users to select the source branch.
	 * 
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 * @return the {@link Text} control displaying the user input
	 */
	private Text createBranchSelector(final Composite parent) {
		final Label label = new Label(parent, SWT.NONE);
		label.setText("Branch:");
		
		final Text input = new Text(parent, SWT.BORDER);
		input.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		input.setMessage("<Enter branch name>");
		input.addModifyListener(new DelayedModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				getWizard().setBranch(((Text)e.widget).getText());
				validate();
			}
		}));
		
		final Button select = new Button(parent, SWT.NONE);
		select.setText("Select...");
		select.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final ChooseUrlDialog dialog = new ChooseUrlDialog(e.display.getActiveShell(), null);
				
				// open the dialog and modify the input upon successful selection
				if(dialog.open() == ChooseUrlDialog.OK) {
					final String url = dialog.getUrl();
					
					if(url != null) {
						input.setText(url);
					}
				}
			}
		});
		
		return input;
	}
	
	/**
	 * Create {@link Control}s allowing users to select the starting revision.
	 * 
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 * @return the {@link Text} control displaying the user input
	 */
	private Text createRevisionSelector(final Composite parent) {
		final Label label = new Label(parent, SWT.NONE);
		label.setText("Start revision:");
		
		final Text input = new Text(parent, SWT.BORDER);
		input.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		input.setMessage("FIRST");
		input.addModifyListener(new DelayedModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				getWizard().setRevision(((Text)e.widget).getText());
				validate();
			}
		}));
		
		final Button select = new Button(parent, SWT.NONE);
		select.setText("Select...");
		select.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final String url = getWizard().getBranch();
				
				try {
					final SVNUrl svnUrl = new SVNUrl(url);
					
					// check if the given URL is accessible
					SVNProviderPlugin.getPlugin().getSVNClient().getInfo(svnUrl);
					
					// when we get here, the selected URL points to a valid remote directory
					final ISVNRepositoryLocation svnRepo = SVNProviderPlugin.getPlugin().getRepository(url);
					final SVNRevision svnRev = SVNRevision.HEAD;
					final ISVNRemoteResource svnLoc = new RemoteFolder(svnRepo, svnUrl, svnRev);
					final HistoryDialog dialog = new HistoryDialog(e.display.getActiveShell(), svnLoc);
					
					if(dialog.open() == HistoryDialog.OK) {
						final ILogEntry[] entries = dialog.getSelectedLogEntries();
						if(entries != null && entries.length > 0) {
							final Number revision = entries[0].getRevision();
							input.setText(String.valueOf(revision.getNumber()));
						}
					}
				} catch (final Exception ex) {
					final Status status = new Status(IStatus.ERROR, SubcherryUI.getInstance().getBundle().getSymbolicName(), "Failed to access remote location.", ex);
					ErrorDialog.openError(e.display.getActiveShell(), "Subcherry Merge", "Revision information not available.", status);
				}
			}
		});
		
		return input;
	}
	
	/**
	 * Perform validation of the entire wizard page.
	 */
	private void validate() {
		String msg = null;
		
		// validate the branch first
		msg = validateBranch();
		
		// validate the revision (if it's still valid for the selected branch)
		if(msg == null) {
			msg = validateRevision();
		}
		
		// update the error message
		setErrorMessage(msg);
		setPageComplete(msg == null);
	}

	/**
	 * @return the error message generated during validation or {@code null} if the
	 *         selected branch is valid
	 */
	private String validateBranch() {
		final String url = _branch.getText();
		
		if(url.isEmpty()) {
			return "The source branch is mandatory.";
		} else {
			try {
				SVNProviderPlugin.getPlugin().getSVNClient().getInfo(new SVNUrl(url));
				return null;
			} catch (final Exception ex) {
				return "The given URL is not accessible.";
			}
		}
	}

	/**
	 * @return the error message generated during validation or {@code null} if the
	 *         selected revision is valid
	 */
	private String validateRevision() {
		final String text = _revision.getText();
		
		if(text.isEmpty()) {
			return null;
		} else {
			try {
				final long rev = Long.valueOf(text).longValue();
				final SVNUrl url = new SVNUrl(getWizard().getBranch());
				final ISVNInfo info = SVNProviderPlugin.getPlugin().getSVNClient().getInfo(url);
				final long startRev = 0;
				final long endRev = info.getRevision().getNumber();
				
				if(startRev <= rev && rev <= endRev) {
					return null;
				} else {
					return String.format("The given revision must be %d <= revision <= %d", startRev, endRev);
				}
			} catch(final Exception ex) {
				return "The given start revision is invalid.";
			}
		}
	}
}
