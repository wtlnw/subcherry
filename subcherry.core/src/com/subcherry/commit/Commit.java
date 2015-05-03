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
package com.subcherry.commit;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.subcherry.CommitConfig;
import com.subcherry.MergeCommitHandler;
import com.subcherry.repository.command.Client;
import com.subcherry.repository.core.CommitInfo;
import com.subcherry.repository.core.Depth;
import com.subcherry.repository.core.LogEntry;
import com.subcherry.repository.core.NodeProperties;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.repository.core.Revision;
import com.subcherry.utils.ArrayUtil;
import com.subcherry.utils.PathParser;
import com.subcherry.utils.Utils;
import com.subcherry.utils.Utils.TicketMessage;

/**
 * @version $Revision$ $Author$ $Date$
 */
public class Commit {

	private static final NodeProperties NO_ADDITIONAL_PROPERTIES = null;

	private final CommitConfig _config;

	private final LogEntry _logEntry;

	private String commitMessage;

	private final TicketMessage _ticketMessage;

	private final Set<String> _touchedResources;

	public Commit(CommitConfig config, LogEntry logEntry, TicketMessage ticketMessage) {
		_config = config;
		_logEntry = logEntry;
		_ticketMessage = ticketMessage;
		_touchedResources = new HashSet<>();
	}

	public LogEntry getLogEntry() {
		return _logEntry;
	}

	public TicketMessage getTicketMessage() {
		return _ticketMessage;
	}

	public void join(Commit joinedCommit) {
		setCommitMessage(getCommitMessage() + "\\n" + joinedCommit.getCommitMessage());
		addTouchedResources(joinedCommit.getTouchedResources());
	}

	private File[] join(File[] paths1, File[] paths2) {
		HashSet<File> buffer = new HashSet<File>();
		buffer.addAll(Arrays.asList(paths1));
		buffer.addAll(Arrays.asList(paths2));
		return buffer.toArray(new File[buffer.size()]);
	}

	public CommitInfo run(CommitContext context) throws RepositoryException {
		CommitInfo commitInfo = doCommit(context.commitClient);
		updateToHEAD(context.client);
		return commitInfo;
	}

	CommitInfo doCommit(Client commitClient) throws RepositoryException {
		HashSet<File> commitPathes = new HashSet<File>();
		commitPathes.addAll(getAffectedPaths());
		commitPathes.addAll(getTouchedModules());
		boolean keepLocks = false;
		NodeProperties revisionProperties = NO_ADDITIONAL_PROPERTIES;
		String[] changelists = null;
		boolean keepChangelist = false;
		boolean force = false;
		Depth depth = Depth.EMPTY; // all pathes are given explicitly
		CommitInfo commitInfo =
			commitClient.commit(commitPathes.toArray(ArrayUtil.EMPTY_FILE_ARRAY), keepLocks, getCommitMessage(),
				revisionProperties, changelists, keepChangelist, force, depth);
		return commitInfo;
	}

	void updateToHEAD(Client client) throws RepositoryException {
		Revision revision = Revision.HEAD;
		Depth depth = Depth.INFINITY;
		boolean depthIsSticky = false;
		boolean allowUnversionedObstructions = false;
		File[] paths = getTouchedModules().toArray(ArrayUtil.EMPTY_FILE_ARRAY);
		client.update(paths, revision, depth, allowUnversionedObstructions, depthIsSticky);
	}

	private List<File> getTouchedModules() {
		List<File> files = new ArrayList<File>();
		for (String resource : _touchedResources) {
			String module = PathParser.getModule(resource);
			files.add(new File(getWorkspaceRoot(), module));
		}
		return files;
	}

	@Override
	public String toString() {
		StringBuilder toStringBuilder = new StringBuilder();
		toStringBuilder.append("Commit[");
		toStringBuilder.append("Msg:").append(getCommitMessage());
		toStringBuilder.append(',');
		toStringBuilder.append("Pathes:").append(getTouchedResourcesList());
		toStringBuilder.append("]");
		return toStringBuilder.toString();
	}

	public String getDescription() {
		return "[" + getLogEntry().getRevision() + "]: " + MergeCommitHandler.encode(getLogEntry().getMessage());
	}

	public long getRevision() {
		return getLogEntry().getRevision();
	}

	public long getFollowUpForRevison() {
		return _ticketMessage.getLeadRevision();
	}

	public String getCommitMessage() {
		if (commitMessage == null) {
			commitMessage = _ticketMessage.getMergeMessage();
		}
		return commitMessage;
	}

	public void setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
	}

	public Set<String> getTouchedResources() {
		return _touchedResources;
	}

	public List<String> getTouchedResourcesList() {
		ArrayList<String> result = new ArrayList<>(_touchedResources);
		Collections.sort(result);
		return result;
	}

	public void addTouchedResources(Set<String> touchedResources) {
		_touchedResources.addAll(touchedResources);
	}

	public List<File> getAffectedPaths() {
		ArrayList<File> result = new ArrayList<>(_touchedResources.size());
		for (String resource : _touchedResources) {
			result.add(new File(getWorkspaceRoot(), resource));
		}
		return result;
	}

	public boolean excludeResource(String path) {
		return _touchedResources.remove(toResource(path));
	}

	public boolean includeResource(String path) {
		return _touchedResources.add(toResource(path));
	}

	private String toResource(String path) {
		return Utils.toResource(getWorkspaceRoot(), path);
	}

	private File getWorkspaceRoot() {
		return _config.getWorkspaceRoot();
	}

}
