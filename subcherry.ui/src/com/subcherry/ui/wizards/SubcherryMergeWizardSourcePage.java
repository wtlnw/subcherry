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

import java.net.MalformedURLException;
import java.util.ResourceBundle.Control;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNClientManager;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.repo.SVNRepositories;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.ui.dialogs.ChooseUrlDialog;
import org.tigris.subversion.subclipse.ui.dialogs.HistoryDialog;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.utils.SVNUrlUtils;

import com.subcherry.Configuration;
import com.subcherry.ui.SubcherryUI;
import com.subcherry.ui.help.IHelpContextIds;
import com.subcherry.ui.util.SubcherryUtils;
import com.subcherry.utils.Path;
import com.subcherry.utils.PathParser;

/**
 * An {@link WizardPage} implementation for {@link SubcherryMergeWizard} which
 * allows users to select the source branch and revision to merge the changes
 * from.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class SubcherryMergeWizardSourcePage extends WizardPage {
	
	/**
	 * @see #createBranchSelector(Composite)
	 */
	private Text _branchInput;
	
	/**
	 * @see #createRevisionSelector(Composite)
	 */
	private Text _revisionInput;
	
	/**
	 * @see #createRevisionButton(Composite)
	 */
	private Button _revisionButton;
	
	/**
	 * Create a {@link SubcherryMergeWizardSourcePage}.
	 */
	public SubcherryMergeWizardSourcePage() {
		super(L10N.SubcherryMergeWizardSourcePage_name);
		
		setTitle(L10N.SubcherryMergeWizardSourcePage_title);
		setMessage(L10N.SubcherryMergeWizardSourcePage_message);
	}
	
	@Override
	public SubcherryMergeWizard getWizard() {
		return (SubcherryMergeWizard) super.getWizard();
	}
	
	@Override
	public void createControl(final Composite parent) {
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(GridLayoutFactory.swtDefaults().numColumns(3).create());
	
		_branchInput = createBranchSelector(contents);
		createBranchButton(contents);
		_revisionInput = createRevisionSelector(contents);
		_revisionButton = createRevisionButton(contents);
		createMergeInfoToggle(contents);
		
		setControl(contents);
		setPageComplete(false);
		
		// register help context
		PlatformUI.getWorkbench().getHelpSystem().setHelp(contents, IHelpContextIds.id(IHelpContextIds.MERGE_WIZARD_SOURCE));
	}

	/**
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 * @return the {@link Text} control allowing users to enter the source branch
	 */
	private Text createBranchSelector(final Composite parent) {
		final Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		label.setText(L10N.SubcherryMergeWizardSourcePage_label_source);
		
		final Text input = new Text(parent, SWT.BORDER);
		input.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		input.setMessage(L10N.SubcherryMergeWizardSourcePage_hint_source);
		input.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(final FocusEvent e) {
				validate();
			}
		});
		
		return input;
	}

	/**
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 * @return the {@link Button} control opening the dialog for source branch
	 *         selection
	 */
	private Button createBranchButton(final Composite parent) {
		final Button button = new Button(parent, SWT.NONE);
		button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		button.setText(L10N.SubcherryMergeWizardSourcePage_label_source_select);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final ChooseUrlDialog dialog = new ChooseUrlDialog(e.display.getActiveShell(), null);
				
				// open the dialog and modify the input upon successful selection
				if(dialog.open() == ChooseUrlDialog.OK) {
					final String url = dialog.getUrl();
					
					if(url != null) {
						_branchInput.setText(url);
					}
					
					// update text selection for use convenience
					_branchInput.setSelection(0, _branchInput.getText().length());
					
					// set the focus to the text field for convenient changes
					_branchInput.setFocus();
					
					// force input validation
					validate();
				}
			}
		});
		
		return button;
	}
	
	/**
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 * @return the {@link Text} control allowing users to enter the start revision
	 */
	private Text createRevisionSelector(final Composite parent) {
		final Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		label.setText(L10N.SubcherryMergeWizardSourcePage_label_start);
		
		final Text input = new Text(parent, SWT.BORDER);
		input.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		input.setMessage(L10N.SubcherryMergeWizardSourcePage_hint_start);
		input.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(final VerifyEvent e) {
				final Text widget = (Text) e.widget;
				final String text = new StringBuilder(widget.getText()).replace(e.start, e.end, e.text).toString();
				
				if(!text.isEmpty()) {
					try {
						final long revision = Long.parseLong(text.toString());
						
						// disallow zero as starting revision since
						// it is used to indicate HEAD. However, HEAD
						// is mapped to empty input.
						if(revision < 1) {
							e.doit = false;
						}
						
					} catch (NumberFormatException ex) {
						e.doit = false;
					}
				} 
				else {
					// empty input is mapped to HEAD
				}
			}
		});
		input.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(final FocusEvent e) {
				validate();
			}
		});
		
		return input;
	}

	/**
	 * @param parent
	 *            the {@link Composite} to create the controls in
	 * @return the {@link Button} control opening the dialog for start revision
	 *         selection
	 */
	private Button createRevisionButton(final Composite parent) {
		final Button button = new Button(parent, SWT.NONE);
		button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		button.setEnabled(!_branchInput.getText().isEmpty());
		button.setText(L10N.SubcherryMergeWizardSourcePage_label_start_select);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final Configuration config = getWizard().getConfiguration();
				
				try {
					final SVNUrl svnUrl = getSourceUrl(config);
					
					// check if the given URL is accessible
					SVNProviderPlugin.getPlugin().getSVNClient().getInfo(svnUrl);
					
					// when we get here, the selected URL points to a valid remote directory
					final ISVNRepositoryLocation svnRepo = SVNProviderPlugin.getPlugin().getRepository(svnUrl.toString());
					final SVNRevision svnRev = SVNRevision.HEAD;
					final ISVNRemoteResource svnLoc = new RemoteFolder(svnRepo, svnUrl, svnRev);
					final HistoryDialog dialog = new HistoryDialog(e.display.getActiveShell(), svnLoc);
					
					if(dialog.open() == HistoryDialog.OK) {
						final ILogEntry[] entries = dialog.getSelectedLogEntries();
						
						if(entries != null && entries.length > 0) {
							final Number revision = entries[0].getRevision();
							_revisionInput.setText(String.valueOf(revision.getNumber()));
						}
						
						// update text selection for use convenience
						_revisionInput.setSelection(0, _revisionInput.getText().length());
						
						// set the focus to the text field for convenient changes
						_revisionInput.setFocus();
						
						// force input validation
						validate();
					}
				} catch (final Exception ex) {
					final Status status = new Status(IStatus.ERROR, SubcherryUI.id(), L10N.SubcherryMergeWizardSourcePage_error_status_svn, ex);
					ErrorDialog.openError(e.display.getActiveShell(), L10N.SubcherryMergeWizardSourcePage_error_title_svn, L10N.SubcherryMergeWizardSourcePage_error_message_svn, status);
				}
			}
		});
		
		return button;
	}
	
	/**
	 * Create {@link Control}s allowing users to configure whether merge information
	 * should be ignored or not.
	 * 
	 * @param contents the {@link Composite} to create the {@link Control}s in
	 */
	private void createMergeInfoToggle(final Composite contents) {
		final Button button = new Button(contents, SWT.CHECK);
		button.setText(L10N.SubcherryMergeWizardSourcePage_label_include_merged);
		button.setLayoutData(GridDataFactory
			.swtDefaults()
			.align(SWT.FILL, SWT.BEGINNING)
			.grab(true, false)
			.span(3, 1)
			.indent(0, 5)
			.create());
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				getWizard().getConfiguration().setIgnoreMergeInfo(((Button) e.widget).getSelection());
			}
		});
	}
	
	/**
	 * Perform validation of the entire wizard page.
	 */
	private void validate() {
		String msg = null;
		
		// validate the branch first
		msg = validateBranch();
		
		// update revision selector availability
		_revisionButton.setEnabled(msg == null);
		
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
		final String url = _branchInput.getText();
		
		if(url.isEmpty()) {
			return L10N.SubcherryMergeWizardSourcePage_error_message_no_branch;
		} else {
			
			try {
				final SVNProviderPlugin svn = SVNProviderPlugin.getPlugin();
				final SVNRepositories repos = svn.getRepositories();
				final SVNClientManager mgr = svn.getSVNClientManager();
				final ISVNClientAdapter client = mgr.getSVNClient();
				
				try {
					// 1. try url access
					final SVNUrl branchUrl = new SVNUrl(url);
					final ISVNInfo info = client.getInfo(branchUrl);
					
					// 2. check if repository is known
					final SVNUrl repoUrl = info.getRepository();
					final String repoUrlString = repoUrl.toString();
					if (!repos.isKnownRepository(repoUrlString, false)) {
						return L10N.SubcherryMergeWizardSourcePage_error_message_repository_invalid;
					}
					
					// 3. check if branch/trunk pattern match
					final String relativeUrl = SubcherryUtils.trailingSeparator(SVNUrlUtils.getRelativePath(repoUrl, branchUrl, true));
					final Configuration configuration = getWizard().getConfiguration();
					final PathParser parser = new PathParser(configuration);
					final Path path = parser.parsePath(relativeUrl);
					
					if (path.getBranch() == null || path.getBranch().isEmpty()) {
						return L10N.SubcherryMergeWizardSourcePage_error_message_branch_invalid;
					} else if(path.getModule() != null && !path.getModule().isEmpty()) {
						return L10N.SubcherryMergeWizardSourcePage_error_message_branch_invalid;
					} else {
						configuration.setSvnURL(SubcherryUtils.noTrailingSeparator(repoUrlString));
						configuration.setSourceBranch(SubcherryUtils.noTrailingSeparator(path.getBranch()));
						
						return null;
					}
				} finally {
					mgr.returnSVNClient(client);
				}
			} catch (final SVNException | SVNClientException ex) {
				return L10N.SubcherryMergeWizardSourcePage_error_message_repository_no_access;
			} catch (final MalformedURLException ex) {
				return L10N.SubcherryMergeWizardSourcePage_error_message_repository_malformed_url;
			}
		}
	}

	/**
	 * @return the error message generated during validation or {@code null} if the
	 *         selected revision is valid
	 */
	private String validateRevision() {
		final Configuration config = getWizard().getConfiguration();
		final String text = _revisionInput.getText();

		if (text.isEmpty()) {
			config.setStartRevision(0);

			return null;
		} else {
			try {
				final long rev = Long.valueOf(text).longValue();
				final SVNUrl url = getSourceUrl(config);
				final SVNProviderPlugin svn = SVNProviderPlugin.getPlugin();
				final SVNClientManager mgr = svn.getSVNClientManager();
				final ISVNClientAdapter client = svn.getSVNClient();
				final ISVNInfo info;
				
				try {
					info = client.getInfo(url);
				} finally {
					mgr.returnSVNClient(client);
				}
				
				final long startRev = 0;
				final long endRev = info.getRevision().getNumber();

				if (startRev <= rev && rev <= endRev) {
					config.setStartRevision(rev);

					return null;
				} else {
					return NLS.bind(L10N.SubcherryMergeWizardSourcePage_error_message_revision_range, startRev, endRev);
				}
			} catch (final Throwable ex) {
				return L10N.SubcherryMergeWizardSourcePage_error_message_revision_invalid;
			}
		}
	}
	
	@Override
	public void dispose() {
		_branchInput = null;
		_revisionInput = null;
		_revisionButton = null;
		
		// call super implementation
		super.dispose();
	}
	
	/**
	 * @param config
	 *            the {@link Configuration} to build the source URL from
	 * @return the {@link SVNUrl} defining the absolute path (including the
	 *         repository URL) to the configured source branch
	 * @throws MalformedURLException
	 *             if the configured path is invalid
	 */
	public static SVNUrl getSourceUrl(final Configuration config) throws MalformedURLException {
		return new SVNUrl(config.getSvnURL() + config.getSourceBranch());
	}
}
