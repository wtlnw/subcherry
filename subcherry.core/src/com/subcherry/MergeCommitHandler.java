/*
 * SubCherry - Cherry Picking with Trac and Subversion
 * Copyright (C) 2014 Bernhard Haumacher and others
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
package com.subcherry;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.subcherry.commit.Commit;
import com.subcherry.commit.CommitContext;
import com.subcherry.commit.RevisionRewriter;
import com.subcherry.merge.MergeHandler;
import com.subcherry.repository.command.Client;
import com.subcherry.repository.command.ClientManager;
import com.subcherry.repository.command.Command;
import com.subcherry.repository.command.merge.CommandExecutor;
import com.subcherry.repository.command.merge.ConflictDescription;
import com.subcherry.repository.command.merge.MergeOperation;
import com.subcherry.repository.command.merge.TreeConflictDescription;
import com.subcherry.repository.core.CommitInfo;
import com.subcherry.repository.core.LogEntry;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.utils.Log;
import com.subcherry.utils.Utils;

import de.haumacher.common.config.Property;

/**
 * @version $Revision$ $Author$ $Date$
 */
public class MergeCommitHandler {

	private interface SystemTrayIcon {

		boolean isShown();

		void show();

		void hide();
	}

	public static abstract class AbstractSystemTrayIcon implements SystemTrayIcon {

		public static final SystemTrayIcon NO_TRAY_ICON = new SystemTrayIcon() {

			@Override
			public void show() {
				// nothing to show
			}

			@Override
			public void hide() {
				// nothing to hide
			}

			@Override
			public boolean isShown() {
				return false;
			}
		};

		private final SystemTray _tray;

		private final TrayIcon _trayIcon;

		private boolean _shown;

		AbstractSystemTrayIcon(SystemTray tray, Image image, String tooltip) {
			_tray = tray;
			_trayIcon = new TrayIcon(image, tooltip);
		}

		@Override
		public void show() {
			try {
				_tray.add(_trayIcon);
				_shown = true;
			} catch (AWTException ex) {
				// ignore
			}
		}

		@Override
		public void hide() {
			_tray.remove(_trayIcon);
			_shown = false;
		}

		@Override
		public boolean isShown() {
			return _shown;
		}

		public static SystemTrayIcon newIcon(Image image, String tooltip) {
			if (SystemTray.isSupported()) {
				return new AbstractSystemTrayIcon(SystemTray.getSystemTray(), image, tooltip) {
				};
			} else {
				return AbstractSystemTrayIcon.NO_TRAY_ICON;
			}
		}

	}

	private enum InputResult {
		SKIP,
		REAPPLY,
		CONTINUE,
		;

		public static AssertionError noSuchInputResult(InputResult result) {
			throw new AssertionError("No such " + InputResult.class.getName() + ": " + result);
		}
	}

	public static class UpdateableRevisionRewriter implements RevisionRewriter {

		private Map<Long, Long> _buffer = new HashMap<Long, Long>();

		@Override
		public long rewrite(long oldRevision) {
			Long newRevision = _buffer.get(oldRevision);
			if (newRevision == null) {
				return oldRevision;
			}
			return newRevision;
		}

		public void add(long originalRevision, long newRevision) {
			if (newRevision <= 0) {
				return;
			}
			_buffer.put(originalRevision, newRevision);
		}
	}

	private final MergeHandler _mergeHandler;
	private final CommitContext _commitContext;

	private final Client _client;

	private List<CommitSet> _commitSets;

	private final Set<Long> joinedRevisions = new HashSet<Long>();

	private int _totalRevs;

	private int _doneRevs;
	
	private final UpdateableRevisionRewriter _revisionRewrite = new UpdateableRevisionRewriter();

	private Configuration _config;

	private ClientManager _clientManager;

	private final SystemTrayIcon _mergeConflictIcon;

	public MergeCommitHandler(MergeHandler mergeHandler, ClientManager clientManager, Configuration config) {
		this._mergeHandler = mergeHandler;
		_clientManager = clientManager;
		_config = config;
		this._client = clientManager.getClient();
		_commitContext = new CommitContext(clientManager.getClient(), clientManager.getClient());
		SystemTrayIcon mergeConflictIcon;
		try {
			BufferedImage image = ImageIO.read(MergeCommitHandler.class.getResource("warning.png"));
			mergeConflictIcon = AbstractSystemTrayIcon.newIcon(image, "Merge has conflicts");
		} catch (IOException ex) {
			mergeConflictIcon = AbstractSystemTrayIcon.NO_TRAY_ICON;
		}
		_mergeConflictIcon = mergeConflictIcon;
	}

	public void run(List<CommitSet> commitSets) throws RepositoryException {
		_commitSets = commitSets;
		_totalRevs = getTotalRevs(commitSets);

		for (int n = 0, cnt = _commitSets.size(); n < cnt; n++) {
			CommitSet commitSet = _commitSets.get(n);
			
			for (Commit commit : commitSet.getCommits()) {
				long revision = commit.getRevision();
				if (joinedRevisions.contains(revision)) {
					continue;
				}

				try {
					Restart.setRevision(revision);
				} catch (IOException ex) {
					Log.info("Unable to store restart revision");
				}

				merge(commit, commit.getLogEntry());
			}
		}
	}

	private int getTotalRevs(List<CommitSet> commitSets) {
		int result = 0;
		for (CommitSet commitSet : commitSets) {
			result += commitSet.getCommits().size();
		}
		return result;
	}
	
	public void merge(Commit commit, LogEntry logEntry) throws RepositoryException {
		_doneRevs++;

		MergeOperation merge = _mergeHandler.parseMerge(logEntry);
		if (merge.isEmpty()) {
			Log.info("Skipping '" + merge.getRevision() + "' (no relevant modules touched).");
			return;
		}
		
		boolean commitAproval = _config.getAutoCommit() && !stopOn(merge.getRevision());
		
		System.out.println("Revision " + logEntry.getRevision() + " (" + _doneRevs + " of " + _totalRevs + "): "
			+ encode(logEntry.getMessage()));
		for (Command command : merge.getCommands()) {
			System.out.println("   " + command.toString());
		}

		merge:
		while (true) {
			CommandExecutor executor = _clientManager.getOperationsFactory().getExecutor();
			Map<File, List<ConflictDescription>> conflicts = executor.execute(merge.getCommands());
			commit.addTouchedResources(merge.getTouchedResources());

			if (!conflicts.isEmpty()) {
				log(conflicts);
				if (_config.getNoCommit() && _config.getAutoSkipConflicts()) {
					System.out.println("Automatically skipping conflicts in [" + logEntry.getRevision() + "].");
					return;
				}
				boolean mustDisplay = !_mergeConflictIcon.isShown();
				if (mustDisplay) {
					_mergeConflictIcon.show();
				}
				InputResult result;
				try {
					result = queryCommit(commit, "commit");
				} finally {
					if (mustDisplay) {
						_mergeConflictIcon.hide();
					}
				}
				switch (result) {
					case SKIP:
						return;
					case CONTINUE:
						break;
					case REAPPLY:
						continue merge;
					default:
						throw InputResult.noSuchInputResult(result);
				}
				commitAproval = true;
			}

			if (_config.getNoCommit()) {
				Log.info("Revision '" + logEntry.getRevision() + "' applied but not committed.");
			} else {
				if (!commitAproval) {
					InputResult result = queryCommit(commit, "commit");
					switch (result) {
						case SKIP:
							return;
						case CONTINUE:
							break;
						case REAPPLY:
							continue merge;
						default:
							throw InputResult.noSuchInputResult(result);
					}
				}
				while (true) {
					try {
						Log.info("Execute:" + commit);
						CommitInfo commitInfo = commit.run(_commitContext);
						Log.info("Revision '" + logEntry.getRevision() + "' merged and commited as '"
							+ commitInfo.getNewRevision() + "'.");

						_revisionRewrite.add(logEntry.getRevision(), commitInfo.getNewRevision());
						break;
					} catch (RepositoryException ex) {
						System.out.println("Commit failed: " + ex.getLocalizedMessage());

						InputResult result = queryCommit(commit, "retry");
						switch (result) {
							case SKIP:
								return;
							case CONTINUE:
								break;
							case REAPPLY:
								continue merge;
							default:
								throw InputResult.noSuchInputResult(result);
						}
					}
				}

			}
			break;
		}
		
	}

	private boolean stopOn(long revision) {
		return _config.getStopOnRevisions().contains(revision);
	}

	private InputResult queryCommit(Commit commit, String continueCommand) throws RepositoryException {
		String skipCommand = "skip";
		String stopCommand = "stop";
		String apiCommand = "api";
		String reapplyCommand = "re-apply";
		String excludeCommand = "exclude: ";
		String includeCommand = "include: ";
		String commitCommand = "commit: ";
		String joinCommand = "join: ";
		String reloadCommand = "reload";
		String setCommand = "set";
		try {
			while (true) {
				System.out.println(commitCommand + encode(commit.getCommitMessage()));
				System.out.println("Enter " + 
				"'" + continueCommand  + "' to commit, " + 
				"'" + commitCommand + "<message>' to commit with another message, " + 
				"'" + apiCommand + "', to add \"API change\" to the message, " + 
				"'" + reapplyCommand + "', to re-apply current change set, " +  
				"'" + excludeCommand + "<path to exclude>', to exclude a certain path from commit, " + 
				"'" + includeCommand + "<path to include>', to include a certain path from commit, " + 
				"'" + skipCommand + "' to skip this revision or " + 
				"'" + joinCommand + "<revision>' to join a following revision with the current commit, " + 
				"'" + setCommand + ": property=value' updates the given configuration property to the given value, " + 
				"'" + reloadCommand + "' to reload the current settings or " + 
				"'" + stopCommand + "' to stop the tool!");
				String input = Utils.SYSTEM_IN.readLine();
				if (input.startsWith(commitCommand)) {
					commit.setCommitMessage(decode(input.substring(commitCommand.length())));
					return InputResult.CONTINUE;
				}
				if (input.startsWith(excludeCommand)) {
					String excludedPath = input.substring(excludeCommand.length());
					
					boolean removed = commit.excludeResource(excludedPath);
					if (!removed) {
						System.err.println("Not in pathes being committed '" + excludedPath + ":");
						for (String path : commit.getTouchedResourcesList()) {
							System.err.println("   " + path);
						}
					}
					continue;
				}
				if (input.startsWith(includeCommand)) {
					String includePath = input.substring(includeCommand.length());
					
					boolean added = commit.includeResource(includePath);
					if (!added) {
						System.err.println("Path already contained: " + includePath);
					}
					continue;
				}
				if (apiCommand.equals(input)) {
					String portedTo = "Ported to [^ ]+(?: from [^:]+)?: ";
					String previewOn = "Preview on [^ ]+: ";
					String mergeReason = "(?:" + portedTo + '|' + previewOn + ')';
					Pattern apiChangeInsertPattern =
						Pattern.compile("^(Ticket #\\d+: " + mergeReason + ")(.*)$", Pattern.DOTALL);
					Matcher matcher = apiChangeInsertPattern.matcher(commit.getCommitMessage());
					if (! matcher.matches()) {
						System.err.println("Message could not be parsed to insert API change flag.");
						continue;
					}
					commit.setCommitMessage(matcher.group(1) + "API change: " + matcher.group(2));
					return InputResult.CONTINUE;
				}
				if (continueCommand.equals(input)) {
					return InputResult.CONTINUE;
				}
				if (skipCommand.equals(input)) {
					return InputResult.SKIP;
				}
				if (reapplyCommand.equals(input)) {
					return InputResult.REAPPLY;
				}
				if (reloadCommand.equals(input)) {
					Globals.reloadConfig();
					continue;
				}
				if (input.startsWith(setCommand)) {
					Pattern pattern = Pattern.compile(setCommand + "\\s+([a-zA-Z0-9_]+)\\s*=\\s*(.*)");
					Matcher matcher = pattern.matcher(input);
					if (matcher.matches()) {
						String var = matcher.group(1);
						String valueString = matcher.group(2);
						Property property = _config.descriptor().getProperties().get(var);
						if (property == null) {
							System.err.println("No such configuration option: " + var);
							continue;
						}
						Object value;
						try {
							value = property.getParser().parse(valueString);
						} catch (RuntimeException ex) {
							System.err.println("Parsing value failed: " + ex.getMessage());
							continue;
						}

						try {
							_config.putValue(property, value);
						} catch (RuntimeException ex) {
							System.err.println("Invalid value: " + ex.getMessage());
							continue;
						}
						continue;
					}
				}
				if (input.startsWith(joinCommand)) {
					String revisionText = input.substring(joinCommand.length());
					long joinedRevision;
					try {
						joinedRevision = Long.parseLong(revisionText);
					} catch (NumberFormatException ex) {
						System.err.println("Not a revision number: " + revisionText);
						continue;
					}
					Commit joinedCommit = getEntry(joinedRevision);
					if (joinedCommit == null) {
						System.err.println("Revision [" + joinedRevision + "] is not part of this merge.");
						continue;
					}
					joinedRevisions.add(joinedRevision);
					
					commit.join(joinedCommit);
					
					// Do not directly commit, but ask again.
					_config.getStopOnRevisions().add(joinedCommit.getRevision());

					merge(commit, joinedCommit.getLogEntry());
					return InputResult.SKIP;
				}
				if (stopCommand.equals(input)) {
					System.out.println("Stopping tool");
					System.exit(0);
				}
				System.err.println("Illegal input.");
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private Commit getEntry(long joinedRevision) {
		for (CommitSet commitSet : _commitSets) {
			Commit commit = commitSet.getCommit(joinedRevision);
			if (commit != null) {
				return commit;
			}
		}
		return null;
	}

	public static String encode(String s) {
		return s.replace("\n", "\\n").replace("\r", "\\r");
	}

	public static String decode(String s) {
		return s.replace("\\n", "\n").replace("\\r", "\r");
	}
	
	public void log(Map<File, List<ConflictDescription>> conflicts) {
		System.out.println(toStringConflicts(_config.getWorkspaceRoot(), conflicts));
	}

	public static String toStringConflicts(File workspaceRoot, Map<File, List<ConflictDescription>> conflicts) {
		StringBuilder message = new StringBuilder("Merge has conflicts in files:");
		for (Entry<File, List<ConflictDescription>> entry : conflicts.entrySet()) {
			String absolutePath = entry.getKey().getAbsolutePath();
			message.append('\n');
			message.append(Utils.toResource(workspaceRoot, absolutePath));
			message.append(':');
			message.append(' ');
			boolean first = true;
			for (ConflictDescription conflict : entry.getValue()) {
				if (first) {
					first = false;
				} else {
					message.append(',');
				}
				if (conflict.isPropertyConflict()) {
					message.append("property");
				}
				if (conflict.isTextConflict()) {
					message.append("text");
				}
				if (conflict.isTreeConflict()) {
					TreeConflictDescription treeConflict = (TreeConflictDescription) conflict;
					message.append("tree (");
					message.append(treeConflict.getConflictAction());
					message.append(" but locally ");
					message.append(treeConflict.getConflictReason());
					message.append(")");
				}
			}

		}
		return message.toString();
	}

	public RevisionRewriter getRevisionRewriter() {
		return _revisionRewrite;
	}

}
