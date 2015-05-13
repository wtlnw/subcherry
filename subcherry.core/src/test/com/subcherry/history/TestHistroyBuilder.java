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
package test.com.subcherry.history;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.subcherry.history.Change;
import com.subcherry.history.HistroyBuilder;
import com.subcherry.history.Node;
import com.subcherry.history.Node.Kind;
import com.subcherry.repository.core.ChangeType;
import com.subcherry.repository.core.LogEntry;
import com.subcherry.repository.core.LogEntryPath;
import com.subcherry.repository.core.NodeKind;
import com.subcherry.repository.core.RepositoryException;

/**
 * Test case for {@link HistroyBuilder}.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 * @version $Revision$ $Author$ $Date$
 */
@SuppressWarnings("javadoc")
public class TestHistroyBuilder extends TestCase {
	private static final Date DATE = new Date();

	private HistroyBuilder _builder;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		_builder = new HistroyBuilder(10);
	}

	@Override
	protected void tearDown() throws Exception {
		_builder = null;

		super.tearDown();
	}

	public void testDeleteParent() throws RepositoryException {
		create(50, "/branches/unstable/module/file-1");
		modify(60, "/branches/unstable/module/file-1");

		assertChanges(revisions(50, 60), "/branches/unstable/module/file-1");

		delete(70, "/branches/unstable");

		assertNoChanges("/branches/unstable");
		assertNoChanges("/branches/unstable/module/file-1");
	}

	public void testBranchRename() throws RepositoryException {
		create(40, "/branches/unstable");

		create(50, "/branches/unstable/module/file-1");
		modify(60, "/branches/unstable/module/file-1");

		assertChanges(revisions(50, 60), "/branches/unstable/module/file-1");

		move(70, "/branches/unstable", "/branches/stable");

		assertNoChanges("/branches/unstable");
		assertNoChanges("/branches/unstable/module/file-1");

		modify(80, "/branches/stable/module/file-1");

		assertChanges(revisions(50, 60, 80), "/branches/stable/module/file-1");

		move(90, "/branches/stable", "/trunk");
		modify(100, "/trunk/module/file-1");

		assertChanges(revisions(50, 60, 80, 100), "/trunk/module/file-1");
		assertChanges(revisions(40, 70, 90), "/trunk");

		assertNoChanges("/branches/stable");
		assertNoChanges("/branches/stable/module/file-1");
		assertNoChanges("/branches/unstable");
		assertNoChanges("/branches/unstable/module/file-1");
	}

	public void testLookupDeletedBranchedDir() throws RepositoryException {
		create(50, "/branch");
		create(60, "/branch/module");
		create(70, "/branch/module/file");
		modify(80, "/branch/module/file");

		copy(90, "/branch", "/trunk");
		delete(100, "/trunk/module");

		assertNoChanges("/trunk/module");
	}

	public void testImplicitBranchCreation() throws RepositoryException {
		// The creation of "/branches/unstable" is not recorded.
	
		create(50, "/branches/unstable/module/file-1");
		modify(60, "/branches/unstable/module/file-1");
	
		assertChanges(revisions(50, 60), "/branches/unstable/module/file-1");
	
		move(70, "/branches/unstable", "/branches/stable");
		modify(80, "/branches/stable/module/file-1");
	
		assertChanges(revisions(50, 60, 80), "/branches/stable/module/file-1");
	}

	public void testMultipleCopiesFromImplicitBranch() throws RepositoryException {
		// The creation of "/branches/unstable" is not recorded.

		create(50, "/branches/unstable/module/file-1");
		modify(60, "/branches/unstable/module/file-1");

		assertChanges(revisions(50, 60), "/branches/unstable/module/file-1");

		copy(70, "/branches/unstable", "/branches/rc1");
		modify(80, "/branches/rc1/module/file-1");

		copy(90, "/branches/unstable", "/branches/rc2");
		modify(100, "/branches/rc2/module/file-1");

		assertChanges(revisions(50, 60, 100), "/branches/rc2/module/file-1");
	}

	public void testCopyFromBeforeImplicitCreation() throws RepositoryException {
		// The creation of "/branches/unstable" is not recorded.

		modify(60, "/branches/unstable/file");
		copy(70, "/branches/unstable", 50, "/branches/stable");

		modify(80, "/branches/stable/file");

		assertChanges(revisions(60), "/branches/unstable/file");
		assertChanges(revisions(80), "/branches/stable/file");
	}

	public void testReplace() throws RepositoryException {
		create(60, "/b1/file");
		create(70, "/b2/file");

		modify(80, "/b2/file");
		modify(90, "/b1/file");

		replace(100, "/b2", "/b1");

		assertChanges(revisions(70, 80), "/b1/file");
		assertNoChanges("/b2/file");
	}

	public void testSwapBranches() throws RepositoryException {
		create(70, "/b1");
		create(80, "/b2");

		create(90, "/b1/file");
		create(100, "/b2/file");

		move(110, "/b1", "/tmp");
		move(120, "/b2", "/b1");
		move(130, "/tmp", "/b2");

		modify(140, "/b2/file");
		modify(150, "/b1/file");

		move(160, "/b2", "/tmp");
		move(170, "/b1", "/b2");
		move(180, "/tmp", "/b1");

		modify(190, "/b1/file");
		modify(200, "/b2/file");

		assertChanges(revisions(90, 140, 190), "/b1/file");
		assertChanges(revisions(100, 150, 200), "/b2/file");
	}

	public void testCurrentViewOfCopiedChanges() throws RepositoryException {
		create(50, "/branches/unstable");
		create(60, "/branches/unstable/file-1");
		modify(70, "/branches/unstable/file-1");

		copy(80, "/branches/unstable", "/branches/stable");
		create(90, "/branches/stable/file-2");
		modify(100, "/branches/stable/file-2");

		expandContents("/branches/stable");
		assertNodes(
			"/branches/stable",
			"/branches/stable/file-1",
			"/branches/stable/file-2");
	}

	public void testCurrentViewOfCopiedChangesBeforeDeletion() throws RepositoryException {
		create(50, "/branches/unstable");
		create(60, "/branches/unstable/file-1");
		modify(70, "/branches/unstable/file-1");
		create(80, "/branches/unstable/file-2");
		modify(90, "/branches/unstable/file-3");
		delete(100, "/branches/unstable");

		copy(110, "/branches/unstable", 70, "/branches/stable");
		create(120, "/branches/stable/file-3");
		modify(130, "/branches/stable/file-3");

		expandContents("/branches/stable");
		assertNodes(
			"/branches/stable",
			"/branches/stable/file-1",
			"/branches/stable/file-3");
	}

	private void assertNodes(String... expectedPaths) {
		List<Node> nodes = _builder.getHistory().getNodes(expectedPaths[0]);
		List<String> paths = new ArrayList<>();
		for (Node node : nodes) {
			paths.add(node.getPath());
		}
		assertEquals(Arrays.asList(expectedPaths), paths);
	}

	private void expandContents(String path) {
		_builder.getHistory().expandContents(path);
	}

	private void create(long revision, String path) throws RepositoryException {
		Map<String, LogEntryPath> paths = paths(added(path));
		apply(revision, paths);
	}

	private void modify(long revision, String path) throws RepositoryException {
		Map<String, LogEntryPath> paths = paths(modified(path));
		apply(revision, paths);
	}

	private void delete(long revision, String path) throws RepositoryException {
		Map<String, LogEntryPath> paths = paths(deleted(path));
		apply(revision, paths);
	}

	private void copy(long revision, String fromPath, String toPath) throws RepositoryException {
		copy(revision, fromPath, revision - 1, toPath);
	}

	private void copy(long revision, String fromPath, long fromRevision, String toPath) throws RepositoryException {
		Map<String, LogEntryPath> paths = paths(copied(fromPath, toPath, fromRevision));
		apply(revision, paths);
	}

	private void move(long revision, String fromPath, String toPath) throws RepositoryException {
		Map<String, LogEntryPath> paths = paths(copied(fromPath, toPath, revision - 1), deleted(fromPath));
		apply(revision, paths);
	}

	private void replace(long revision, String fromPath, String toPath) throws RepositoryException {
		Map<String, LogEntryPath> paths = paths(deleted(fromPath), replaced(fromPath, toPath, revision - 1));
		apply(revision, paths);
	}

	private void apply(long revision, Map<String, LogEntryPath> paths) throws RepositoryException {
		LogEntry logEntry = new LogEntry(paths, revision, "", DATE, "message-" + revision, false);
		_builder.handleLogEntry(logEntry);
	}

	private void assertChanges(List<Long> revisions, String path) {
		assertChanges(revisions, getNodeNotNull(path));
	}

	private Node getNodeNotNull(String path) {
		Node node = getNode(path);
		assertNotNull("Path not found: " + path, node);
		return node;
	}

	private Node getNode(String path) {
		Node node = _builder.getHistory().getCurrentNode(Kind.UNKNOWN, path);
		return node;
	}

	private void assertChanges(List<Long> revisions, Node node) {
		assertEquals(revisions, revisions(node.getChanges()));
	}

	private void assertNoChanges(String path) {
		assertNull(getNode(path));
	}

	private static LogEntryPath added(String path) {
		return new LogEntryPath(NodeKind.UNKNOWN, path, ChangeType.ADDED, null, 0);
	}

	private static LogEntryPath copied(String fromPath, String toPath, long copyRevision) {
		return new LogEntryPath(NodeKind.UNKNOWN, toPath, ChangeType.ADDED, fromPath, copyRevision);
	}

	private static LogEntryPath replaced(String fromPath, String toPath, long copyRevision) {
		return new LogEntryPath(NodeKind.UNKNOWN, toPath, ChangeType.REPLACED, fromPath, copyRevision);
	}

	private static LogEntryPath modified(String path) {
		return new LogEntryPath(NodeKind.UNKNOWN, path, ChangeType.MODIFIED, null, 0);
	}

	private static LogEntryPath deleted(String fromPath) {
		return new LogEntryPath(NodeKind.UNKNOWN, fromPath, ChangeType.DELETED, null, 0);
	}

	private static List<Long> revisions(long... revisions) {
		ArrayList<Long> result = new ArrayList<>();
		for (long revision : revisions) {
			result.add(revision);
		}
		return result;
	}

	private static List<Long> revisions(List<Change> changes) {
		ArrayList<Long> result = new ArrayList<>(changes.size());
		for (Change change : changes) {
			result.add(change.getRevision());
		}
		return result;
	}

	private static Map<String, LogEntryPath> paths(LogEntryPath pathEntry) {
		return Collections.singletonMap(pathEntry.getPath(), pathEntry);
	}

	private static Map<String, LogEntryPath> paths(LogEntryPath... pathEntries) {
		HashMap<String, LogEntryPath> result = new LinkedHashMap<>();
		for (LogEntryPath pathEntry : pathEntries) {
			LogEntryPath clash = result.put(pathEntry.getPath(), pathEntry);
			assertNull(clash);
		}
		return result;
	}
}
