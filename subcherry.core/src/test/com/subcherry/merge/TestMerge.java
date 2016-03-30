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
package test.com.subcherry.merge;

import static test.com.subcherry.scenario.Scenario.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.subcherry.BranchConfig;
import com.subcherry.CommitConfig;
import com.subcherry.MergeCommitHandler;
import com.subcherry.MergeConfig;
import com.subcherry.commit.Commit;
import com.subcherry.commit.CommitContext;
import com.subcherry.merge.MergeHandler;
import com.subcherry.repository.ClientManagerFactory;
import com.subcherry.repository.command.Client;
import com.subcherry.repository.command.ClientManager;
import com.subcherry.repository.command.log.LogEntryHandler;
import com.subcherry.repository.command.merge.CommandExecutor;
import com.subcherry.repository.command.merge.ConflictAction;
import com.subcherry.repository.command.merge.ConflictDescription;
import com.subcherry.repository.command.merge.MergeOperation;
import com.subcherry.repository.command.merge.TreeConflictDescription;
import com.subcherry.repository.core.ChangeType;
import com.subcherry.repository.core.CommitInfo;
import com.subcherry.repository.core.Depth;
import com.subcherry.repository.core.LogEntry;
import com.subcherry.repository.core.LogEntryPath;
import com.subcherry.repository.core.MergeInfo;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.repository.core.Resolution;
import com.subcherry.repository.core.Revision;
import com.subcherry.repository.core.RevisionRange;
import com.subcherry.repository.core.RevisionRanges;
import com.subcherry.repository.core.Target;
import com.subcherry.utils.PathParser;

import de.haumacher.common.config.ValueFactory;
import junit.extensions.TestSetup;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import test.com.subcherry.scenario.Scenario;
import test.com.subcherry.scenario.WC;

/**
 * Test case for {@link MergeHandler}.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 * @version $Revision$ $Author$ $Date$
 */
@SuppressWarnings("javadoc")
public class TestMerge extends TestCase {

	static class LogEntryCollector implements LogEntryHandler {

		private final List<LogEntry> _buffer = new ArrayList<>();

		@Override
		public void handleLogEntry(LogEntry logEntry) throws RepositoryException {
			_buffer.add(logEntry);
		}

		public List<LogEntry> getBuffer() {
			return _buffer;
		}

	}

	private static final List<String> MERGED_MODULES = Arrays.asList("module1", "module2");

	// Note: svn:ignore is expected to use Unix line ending style.
	private static final String NL = "\n";

	private ClientManager _clientManager;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		String providerName = Setup.getProviderName();
		_clientManager = ClientManagerFactory.getInstance(providerName).createClientManager();
		_clientManager.getOperationsFactory().settings().setSleepForTimestamp(false);
	}

	@Override
	protected void tearDown() throws Exception {
		_clientManager.close();
		super.tearDown();
	}

	public void testConflictDetection() throws IOException, RepositoryException {
		Scenario s = moduleScenario();
		s.file("/branches/branch1/module1/foo");
		s.file("/branches/branch1/module1/bar");

		s.copy("/branches/branch2", "/branches/branch1");

		WC wc1 = s.wc("branches/branch1");
		wc1.update("module1/foo");
		wc1.update("module1/bar");
		long r1 = wc1.commit();

		WC wc2 = s.wc("branches/branch2");
		wc2.update("module1/foo");
		wc2.commit();

		MergeOperation merge = createMerge(s, wc2, s.log(r1));
		Map<File, List<ConflictDescription>> conflicts = tryMerge(s, merge);
		List<ConflictDescription> conflictsFoo = conflicts.get(wc2.toFile("module1/foo"));
		assertNotNull(conflictsFoo);
		assertEquals(1, conflictsFoo.size());

		List<ConflictDescription> conflictsBar = conflicts.get(wc2.toFile("module1/bar"));
		assertNull(conflictsBar);
	}

	public void testMergeInfo() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create branch2.
		s.copy("/branches/branch2", "/branches/branch1");

		// Create change in original branch1.
		WC wc1 = s.wc("/branches/branch1");
		wc1.file("/module1/foo");
		wc1.setProperty("/module1", "myproperty", "myvalue");
		long r1 = wc1.commit();

		// Merge changes into branch2.
		LogEntry merge = doMerge(s, r1);
		long r2 = merge.getRevision();

		MergeInfo mergeInfo = s.mergeInfo("branches/branch2/module1");
		assertEquals(set(s.url("branches/branch1/module1")), mergeInfo.getPaths());
		List<RevisionRange> mergedFromBranch1 = mergeInfo.getRevisions(s.url("branches/branch1/module1"));
		assertTrue(RevisionRanges.containsAll(mergedFromBranch1, ranges(range(r1))));
		assertFalse(RevisionRanges.containsAll(mergedFromBranch1, ranges(range(r1 + 1))));
		assertFalse(RevisionRanges.containsAll(mergedFromBranch1, ranges(range(r1 - 1))));
		
		Client client = s.clientManager().getClient();
		LogEntryCollector collector = new LogEntryCollector();
		client.getMergeInfoLog(
			Target.fromURL(s.url("branches/branch2/module1")),
			Target.fromURL(s.url("branches/branch1/module1")),
			Revision.create(1),
			Revision.HEAD, collector);
		List<LogEntry> entries = collector.getBuffer();
		assertEquals(1, entries.size());
		assertEquals(r1, entries.get(0).getRevision());

		Map<String, List<RevisionRange>> mergeInfoDiff = s.mergeInfoDiff("branches/branch2/module1", r2);
		assertEquals(
			Collections.singletonMap("/branches/branch1/module1", ranges(range(r1))),
			mergeInfoDiff);


		WC merged = s.wc("/branches/branch2");
		merged.setProperty("/module1", "svn:mergeinfo", "");
		long r3 = merged.commit();

		// TODO: Reverse merge info is not supported.
		System.out.println(s.mergeInfoDiff("branches/branch2/module1", r3));
	}

	private <T> Set<T> set(T... values) {
		return new HashSet<>(Arrays.asList(values));
	}

	public void testChangeInDirectoryThatDoesNotExistOnTargetBranch() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.file("module1/file1");
		wc1.commit();

		s.copy("/branches/branch2", "/branches/branch1");

		wc1.mkdir("module1/some");
		wc1.mkdir("module1/some/path");
		wc1.file("module1/some/path/file2");
		wc1.file("module1/some/path/file3");
		wc1.commit();

		// Trigger semantic move.
		wc1.copy("module1/fileRenamed", "module1/file1");
		// Touch file in directory that does not exist in target branch.
		wc1.update("module1/some/path/file2");
		// Delete file in directory that does not exist in target branch.
		wc1.delete("module1/some/path/file3");
		// Create file in directory that does not exist in target branch.
		wc1.file("module1/some/path/file4");
		long revision = wc1.commit();

		WC wc2 = s.wc("/branches/branch2");
		LogEntry entry = s.log(revision);
		MergeOperation merge = createMerge(s, wc2, entry);
		Map<File, List<ConflictDescription>> result = tryMerge(s, merge);

		List<ConflictDescription> conflictsFile2 = result.get(wc2.toFile("module1/some/path/file2"));
		assertNotNull(result.toString(), conflictsFile2);
		assertEquals(1, conflictsFile2.size());
		ConflictDescription conflictFile2 = conflictsFile2.get(0);
		assertTrue(conflictFile2.isTreeConflict());
		assertEquals(ConflictAction.EDITED, ((TreeConflictDescription) conflictFile2).getConflictAction());

		List<ConflictDescription> conflictsFile3 = result.get(wc2.toFile("module1/some/path/file3"));
		assertNotNull(result.toString(), conflictsFile3);
		assertEquals(1, conflictsFile3.size());
		ConflictDescription conflictFile3 = conflictsFile3.get(0);
		assertTrue(conflictFile3.isTreeConflict());
		assertEquals(ConflictAction.DELETED, ((TreeConflictDescription) conflictFile3).getConflictAction());
		
		// Parent directories of added resources are automatically created.
		assertFalse(result.toString(), result.containsKey(wc2.toFile("module1/some/path/file4")));
		
		wc2.resolve("module1/some/path/file2", Depth.EMPTY, Resolution.CHOOSE_MERGED);
		wc2.resolve("module1/some/path/file3", Depth.EMPTY, Resolution.CHOOSE_MERGED);

		long merged = wc2.commit();

		LogEntry mergedEntry = s.log(merged);
		LogEntryPath addedPath = mergedEntry.getChangedPaths().get("/branches/branch2/module1/some/path");
		assertNotNull(addedPath);
		assertEquals(ChangeType.ADDED, addedPath.getType());
	}

	public void testCrossBranchCopyWithExistingResourceWithSameName() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create target branch.
		s.copy("/branches/branch2", "/branches/branch1");
		s.copy("/branches/branch3", "/branches/branch1");

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.mkdir("module1/foo");
		wc1.file("module1/foo/file1");
		long r1 = wc1.commit();

		// Create a revision <b>not</b> to revert in merge
		wc1.file("module1/foo/file2");
		wc1.commit();

		// An folder with name of the merge source exists by accident
		s.mkdir("/branches/branch2/module1/foo");

		long revision = s.copy("/branches/branch3/module1/bar", "/branches/branch1/module1/foo", r1);

		LogEntry mergedEntry = doMerge(s, revision);
		assertCopyFrom(mergedEntry, "/branches/branch1/module1/foo", "/branches/branch2/module1/bar");
	}

	public void testMergeFolderInReverseCommitOrder() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.mkdir("module1/foo");
		wc1.commit();

		// Create target branch.
		s.copy("/branches/branch2", "/branches/branch1");
		s.copy("/branches/branch3", "/branches/branch1");

		wc1.mkdir("module1/foo/bar");
		wc1.file("module1/foo/bar/file1");
		wc1.commit();

		// Second revision to merge
		long revision = s.copy("/branches/branch3/module1/foo/bar", "/branches/branch1/module1/foo/bar");

		// First revision to merge
		wc1.file("module1/foo/bar/file2");
		wc1.commit();
		long revision2 = s.copy("/branches/branch3/module1/foo/bar/file2", "/branches/branch1/module1/foo/bar/file2");

		WC wc2 = s.wc("/branches/branch2");
		LogEntry entry = s.log(revision2);
		MergeOperation merge = createMerge(s, wc2, entry);
		doMerge(s, wc2, merge);
		Set<String> commitResources = new HashSet<String>(merge.getTouchedResources());
		// currently a bug that the parent of the created file is not contained in the touched
		// resources
		boolean added = commitResources.add("module1/foo/bar");
		assertTrue("Remove temporary code to workaround a bug", added);
		Commit commit = createCommit(s, wc2, entry, commitResources);
		doCommit(s, wc2, commit);

		LogEntry mergedEntry;
		try {
			mergedEntry = doMerge(s, revision);
		} catch (AssertionFailedError err) {
			AssertionFailedError assertionFailedError =
				new AssertionFailedError(
					"Conflict has to be resolved automatically: if the folder already exists, the contents of the source folder must be copied.");
			assertionFailedError.initCause(err);
			throw assertionFailedError;
		}
		Map<String, LogEntryPath> changedPaths = mergedEntry.getChangedPaths();
	}

	public void testRegularMerge() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.file("module1/foo");
		wc1.file("module1/bar");
		wc1.commit();

		// Create target branch.
		s.copy("/branches/branch2", "/branches/branch1");

		// Create revision to merge.
		wc1.update("module1/foo");
		wc1.delete("module1/bar");
		wc1.file("module2/baz");
		long origRevision = wc1.commit();

		LogEntry mergedEntry = doMerge(s, origRevision);

		Map<String, LogEntryPath> changedPaths = mergedEntry.getChangedPaths();
		// Merge info has been recorded.
		assertTrue(changedPaths.get("/branches/branch2/module1") != null);
		assertTrue(changedPaths.get("/branches/branch2/module2") != null);

		// Changes have been applied.
		assertType(mergedEntry, ChangeType.MODIFIED, "/branches/branch2/module1/foo");
		assertType(mergedEntry, ChangeType.DELETED, "/branches/branch2/module1/bar");
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module2/baz");

		assertNull(changedPaths.get("/branches/branch2/module1/foo").getCopyPath());
		assertNull(changedPaths.get("/branches/branch2/module1/bar").getCopyPath());
		assertCopyFrom(mergedEntry, "/branches/branch1/module2/baz", "/branches/branch2/module2/baz");
	}

	public void testMoveMerge() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.file("module1/foo");
		wc1.file("module2/bar");
		wc1.commit();

		// Create target branch.
		s.copy("/branches/branch2", "/branches/branch1");

		// Create revision to merge, move foo to module2 and bar to module1.
		wc1.copy("module2/foo", "module1/foo");
		wc1.delete("module1/foo");
		wc1.copy("module1/bar", "module2/bar");
		wc1.delete("module2/bar");
		long origRevision = wc1.commit();

		LogEntry mergedEntry = doMerge(s, origRevision);

		Map<String, LogEntryPath> changedPaths = mergedEntry.getChangedPaths();
		// Merge info has been recorded.
		assertTrue(changedPaths.get("/branches/branch2/module1") != null);
		assertTrue(changedPaths.get("/branches/branch2/module2") != null);

		// Changes have been applied.
		assertType(mergedEntry, ChangeType.DELETED, "/branches/branch2/module1/foo");
		assertType(mergedEntry, ChangeType.DELETED, "/branches/branch2/module2/bar");
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module2/foo");
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/bar");

		// Intra-branch copy has been created.
		assertCopyFrom(mergedEntry, "/branches/branch2/module1/foo", "/branches/branch2/module2/foo");
		assertCopyFrom(mergedEntry, "/branches/branch2/module2/bar", "/branches/branch2/module1/bar");
	}

	public void testModifyResourceCopiedFromModifiedSource() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.file("module1/A");
		wc1.commit();

		// Create target branch.
		s.copy("/branches/branch2", "/branches/branch1");

		// Create copy and modify both, source and target. The problem here is that the copy
		// operation must be applied before applying any of the modifications. If operations are
		// simply replayed in alphabetical order of the resources, the resulting merge fails.
		wc1.copy("module1/B", "module1/A", Revision.WORKING);
		wc1.update("module1/A");
		wc1.update("module1/B");

		long origRevision = wc1.commit();

		LogEntry mergedEntry = doMerge(s, origRevision);

		assertCopyFrom(mergedEntry, "/branches/branch2/module1/A",
			"/branches/branch2/module1/B");
	}

	public void testCyclicRename() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.mkdir("module1/folder");
		wc1.file("module1/folder/foo");
		wc1.file("module1/folder/bar");
		wc1.file("module1/folder/zzz");
		wc1.commit();

		// Create target branch.
		s.copy("/branches/branch2", "/branches/branch1");

		// Create cyclic rename.
		wc1.copy("module1/folder/tmp", "module1/folder/foo", Revision.WORKING);
		wc1.delete("module1/folder/foo");
		wc1.copy("module1/folder/foo", "module1/folder/bar", Revision.WORKING);
		wc1.delete("module1/folder/bar");
		wc1.copy("module1/folder/bar", "module1/folder/zzz", Revision.WORKING);
		wc1.delete("module1/folder/zzz");
		wc1.copy("module1/folder/zzz", "module1/folder/tmp", Revision.WORKING);
		wc1.delete("module1/folder/tmp");
		long origRevision = wc1.commit();

		LogEntry mergedEntry = doMerge(s, origRevision);

		assertType(mergedEntry, ChangeType.REPLACED, "/branches/branch2/module1/folder/foo");
		assertType(mergedEntry, ChangeType.REPLACED, "/branches/branch2/module1/folder/bar");
		assertType(mergedEntry, ChangeType.REPLACED, "/branches/branch2/module1/folder/zzz");

		assertCopyFrom(mergedEntry, "/branches/branch2/module1/folder/bar",
			"/branches/branch2/module1/folder/foo");
		assertCopyFrom(mergedEntry, "/branches/branch2/module1/folder/zzz",
			"/branches/branch2/module1/folder/bar");
		assertCopyFrom(mergedEntry, "/branches/branch2/module1/folder/foo",
			"/branches/branch2/module1/folder/zzz");
	}

	public void testMoveInRenamedFolder() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.mkdir("module1/oldfolder");
		wc1.file("module1/oldfolder/foo");
		wc1.file("module1/oldfolder/bar");
		wc1.commit();

		// Create target branch.
		s.copy("/branches/branch2", "/branches/branch1");

		// Move folder.
		wc1.copy("module2/newfolder", "module1/oldfolder");
		wc1.delete("module1/oldfolder");
		wc1.commit();

		// Create revision to merge, rename foo to aaa and bar to zzz.
		wc1.copy("module2/newfolder/aaa", "module2/newfolder/foo");
		wc1.delete("module2/newfolder/foo");
		wc1.copy("module2/newfolder/zzz", "module2/newfolder/bar");
		wc1.delete("module2/newfolder/bar");

		wc1.update("module2/newfolder/zzz");
		wc1.update("module2/newfolder/aaa");
		String origZzz = wc1.load("module2/newfolder/zzz");
		String origAaa = wc1.load("module2/newfolder/aaa");
		long origRevision = wc1.commit();

		LogEntry mergedEntry =
			doMerge(s, origRevision, Collections.singletonMap("module2/newfolder/", "module1/oldfolder/"));

		Map<String, LogEntryPath> changedPaths = mergedEntry.getChangedPaths();
		// Merge info has been recorded to original module.
		assertTrue(changedPaths.get("/branches/branch2/module2") != null);
		// No changes happened to module 1 in merged change set.
		assertTrue(changedPaths.get("/branches/branch2/module1") == null);

		// Changes have been applied.
		assertType(mergedEntry, ChangeType.DELETED, "/branches/branch2/module1/oldfolder/foo");
		assertType(mergedEntry, ChangeType.DELETED, "/branches/branch2/module1/oldfolder/bar");
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/oldfolder/aaa");
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/oldfolder/zzz");

		// Intra-branch copy has been created.
		assertCopyFrom(mergedEntry, "/branches/branch2/module1/oldfolder/foo", "/branches/branch2/module1/oldfolder/aaa");
		assertCopyFrom(mergedEntry, "/branches/branch2/module1/oldfolder/bar", "/branches/branch2/module1/oldfolder/zzz");

		WC wc2 = s.wc("/branches/branch2");
		String mergedZzz = wc2.load("module1/oldfolder/zzz");
		String mergedAaa = wc2.load("module1/oldfolder/aaa");

		assertEquals(origAaa, mergedAaa);
		assertEquals(origZzz, mergedZzz);
	}
	
	public void testCreateInRenamedFolder() throws IOException, RepositoryException {
		Scenario s = moduleScenario();
		
		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.mkdir("module1/oldfolder");
		wc1.file("module1/oldfolder/foo");
		wc1.commit();
		
		// Create target branch.
		s.copy("/branches/branch2", "/branches/branch1");
		
		// Move folder.
		wc1.copy("module2/newfolder", "module1/oldfolder");
		wc1.delete("module1/oldfolder");
		wc1.commit();
		
		// Create revision to merge.
		wc1.file("module2/newfolder/bar");
		long origRevision = wc1.commit();
		
		LogEntry mergedEntry =
			doMerge(s, origRevision, Collections.singletonMap("module2/newfolder/", "/module1/oldfolder/"));
		
		Map<String, LogEntryPath> changedPaths = mergedEntry.getChangedPaths();
		// Merge info has been recorded to original module.
		assertTrue(changedPaths.get("/branches/branch2/module2") != null);
		// No changes happened to module 1 in merged change set.
		assertTrue(changedPaths.get("/branches/branch2/module1") == null);
		
		// Changes have been applied.
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/oldfolder/bar");
		
		// Cross-branch copy has been created.
		assertCopyFrom(mergedEntry, "/branches/branch1/module2/newfolder/bar", "/branches/branch2/module1/oldfolder/bar");
	}

	public void testMoveWithinMovedFolder() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.mkdir("module1/folder");
		wc1.file("module1/folder/foo");
		wc1.commit();

		// Create target branch.
		s.copy("/branches/branch2", "/branches/branch1");

		// Create revision to merge, move foo to module2 and bar to module1.
		wc1.copy("module1/newfolder", "module1/folder");
		wc1.delete("module1/folder");
		wc1.copy("module1/newfolder/bar", "module1/folder/foo");
		wc1.delete("module1/newfolder/foo");
		long origRevision = wc1.commit();

		LogEntry mergedEntry = doMerge(s, origRevision);

		// Changes have been applied.
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/newfolder");
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/newfolder/bar");
		assertType(mergedEntry, ChangeType.DELETED, "/branches/branch2/module1/folder");
		assertType(mergedEntry, ChangeType.DELETED, "/branches/branch2/module1/newfolder/foo");

		// Intra-branch copy has been created.
		assertCopyFrom(mergedEntry, "/branches/branch2/module1/folder", "/branches/branch2/module1/newfolder");
		assertCopyFrom(mergedEntry, "/branches/branch2/module1/folder/foo", "/branches/branch2/module1/newfolder/bar");
	}

	/**
	 * The source of a move is replaced in the same revision.
	 */
	public void testParallelMove() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.file("module1/foo");
		wc1.file("module1/bar");
		wc1.commit();

		// Create target branch.
		s.copy("/branches/branch2", "/branches/branch1");

		// Create revision to merge, move foo to module2 and bar to module1.
		wc1.copy("module1/newfoo", "module1/foo");
		wc1.delete("module1/foo");
		wc1.copy("module1/foo", "module1/bar");
		wc1.delete("module1/bar");
		long origRevision = wc1.commit();

		LogEntry mergedEntry = doMerge(s, origRevision);

		// Changes have been applied.
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/newfoo");
		assertType(mergedEntry, ChangeType.REPLACED, "/branches/branch2/module1/foo");
		assertType(mergedEntry, ChangeType.DELETED, "/branches/branch2/module1/bar");

		// Intra-branch copy has been created.
		assertCopyFrom(mergedEntry, "/branches/branch2/module1/foo", "/branches/branch2/module1/newfoo");
		assertCopyFrom(mergedEntry, "/branches/branch2/module1/bar", "/branches/branch2/module1/foo");
	}

	public void testMoveOutOfDeletedFolder() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.mkdir("module1/folder");
		wc1.file("module1/folder/foo");
		wc1.commit();

		// Create target branch.
		s.copy("/branches/branch-intermediate", "/branches/branch1");
		s.copy("/branches/branch2", "/branches/branch1");

		// Create revision to merge, move foo to module2 and bar to module1.
		wc1.copy("module1/foo", "module1/folder/foo");
		wc1.delete("module1/folder");
		long origRevision = wc1.commit();

		long rebasedRevision = rebaseSvn(s, origRevision);

		LogEntry mergedEntry = doMerge(s, rebasedRevision);

		// Changes have been applied.
		assertType(mergedEntry, ChangeType.DELETED, "/branches/branch2/module1/folder");
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/foo");

		// Intra-branch copy has been created.
		assertCopyFrom(mergedEntry, "/branches/branch2/module1/folder/foo", "/branches/branch2/module1/foo");
	}

	public void testRegularlyRebasedContentChangeInMovedFolder() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.mkdir("module1/folder");
		wc1.file("module1/folder/foo");
		wc1.commit();

		// Create target branch.
		s.copy("/branches/branch-intermediate", "/branches/branch1");
		s.copy("/branches/branch2", "/branches/branch1");

		// Create revision to merge, move foo to module2 and bar to module1.
		wc1.copy("module1/newfolder", "module1/folder");
		wc1.delete("module1/folder");
		wc1.update("module1/newfolder/foo");
		long origRevision = wc1.commit();

		long rebasedRevision = rebaseSvn(s, origRevision);

		LogEntry mergedEntry = doMerge(s, rebasedRevision);

		// Changes have been applied.
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/newfolder");
		assertType(mergedEntry, ChangeType.DELETED, "/branches/branch2/module1/folder");
		assertType(mergedEntry, ChangeType.MODIFIED, "/branches/branch2/module1/newfolder/foo");

		// Intra-branch copy has been created.
		assertCopyFrom(mergedEntry, "/branches/branch2/module1/folder", "/branches/branch2/module1/newfolder");
	}

	public void testRegularRebasedFileAddToFolderWithPropertyModification() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.mkdir("module1/folder");
		wc1.commit();

		// Create target branch.
		s.copy("/branches/branch-intermediate", "/branches/branch1");
		s.copy("/branches/branch2", "/branches/branch1");

		// Create revision to merge.
		wc1.setProperty("module1/folder", "svn:ignore", "tmp");
		wc1.file("module1/folder/foo");
		long origRevision = wc1.commit();

		long rebasedRevision = rebaseSvn(s, origRevision);

		LogEntry mergedEntry = doMerge(s, rebasedRevision);

		// Changes have been applied.
		assertType(mergedEntry, ChangeType.MODIFIED, "/branches/branch2/module1/folder");
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/folder/foo");

		// Direct source branch copy has been created.
		assertCopyFrom(mergedEntry, "/branches/branch1/module1/folder/foo", "/branches/branch2/module1/folder/foo");
	}

	public void testFixRegularRebasedMove() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.file("module1/foo");
		wc1.commit();

		// Create intermediate branch and target branch.
		s.copy("/branches/branch-intermediate", "/branches/branch1");
		s.copy("/branches/branch2", "/branches/branch1");

		// Create original move.
		wc1.copy("module1/bar", "module1/foo");
		wc1.delete("module1/foo");
		long origRevision = wc1.commit();

		long rebasedRevision = rebaseSvn(s, origRevision);

		// Fix the intermediate rebase by applying a semantic move merge.
		LogEntry mergedEntry = doMerge(s, rebasedRevision);

		// Changes have been applied.
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/bar");
		assertType(mergedEntry, ChangeType.DELETED, "/branches/branch2/module1/foo");

		// Intra-branch copy has been created.
		assertCopyFrom(mergedEntry, "/branches/branch2/module1/foo", "/branches/branch2/module1/bar");
	}

	public void testShortCircuitRegularRebasedCreate() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create intermediate branch and target branch.
		s.copy("/branches/branch-intermediate", "/branches/branch1");
		s.copy("/branches/branch2", "/branches/branch1");

		// Create original create.
		WC wc1 = s.wc("/branches/branch1");
		wc1.file("module1/foo");
		long origRevision = wc1.commit();

		long rebasedRevision = rebaseSvn(s, origRevision);

		// Fix the intermediate rebase by applying a semantic move merge.
		LogEntry mergedEntry = doMerge(s, rebasedRevision);

		Map<String, LogEntryPath> changedPaths = mergedEntry.getChangedPaths();

		// Changes have been applied.
		assertTrue(changedPaths.get("/branches/branch2/module1/foo") != null);

		// Direct copy from original branch has been created.
		assertCopyFrom(mergedEntry, "/branches/branch1/module1/foo", "/branches/branch2/module1/foo");
	}

	public void testFixCopyFromOldRevisionByRevertingChanges() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.file("module1/foo");
		String originalContents = wc1.load("module1/foo");
		assertTrue(!originalContents.isEmpty());
		long oldRevision = wc1.commit();

		wc1.update("module1/foo");
		String newContents = wc1.load("module1/foo");
		assertTrue(!newContents.equals(originalContents));
		wc1.commit();

		// Create intermediate branch and target branch.
		s.copy("/branches/branch-intermediate", "/branches/branch1");
		s.copy("/branches/branch2", "/branches/branch1");

		// Create original change
		wc1.delete("module1/foo");
		wc1.copyFromRemote("module1/foo", "/branches/branch1/module1/foo", oldRevision);
		String copiedContents = wc1.load("module1/foo");
		assertEquals(originalContents, copiedContents);
		wc1.update("module1/foo");
		String changedRevertedContents = wc1.load("module1/foo");
		long origRevision = wc1.commit();

		long mergedRevision = origRevision;

		// Fix the intermediate rebase by applying a semantic move merge.
		LogEntry mergedEntry = doMerge(s, mergedRevision);

		Map<String, LogEntryPath> changedPaths = mergedEntry.getChangedPaths();

		// Changes have been applied.
		assertTrue(changedPaths.get("/branches/branch2/module1/foo") != null);

		WC wc2 = s.wc("/branches/branch2");
		assertEquals(changedRevertedContents, wc2.load("module1/foo"));

		// No copy has been created, but a simple content change.
		assertCopyFrom("A cross-branch copy has been created, where it is not expceted.",
			mergedEntry, null, "/branches/branch2/module1/foo");
	}

	public void testFixNoOpCopy() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.file("module1/foo");
		wc1.file("module1/bar");
		String originalContents = wc1.load("module1/foo");
		assertTrue(!originalContents.isEmpty());
		long lastRevision = wc1.commit();

		// Create intermediate branch and target branch.
		s.copy("/branches/branch-intermediate", "/branches/branch1");
		s.copy("/branches/branch2", "/branches/branch1");

		// Create original change
		wc1.delete("module1/foo");
		wc1.copyFromRemote("module1/foo", "/branches/branch1/module1/foo", lastRevision);
		// Additional change to even force creating a commit.
		wc1.update("module1/bar");
		long origRevision = wc1.commit();

		long mergedRevision = origRevision;

		// Fix the intermediate rebase by applying a semantic move merge.
		LogEntry mergedEntry = doMerge(s, mergedRevision);

		Map<String, LogEntryPath> changedPaths = mergedEntry.getChangedPaths();

		// No changes have been applied.
		assertNull(changedPaths.get("/branches/branch2/module1/foo"));
	}

	public void testFixNoOpCopyWithModification() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.file("module1/foo");
		String originalContents = wc1.load("module1/foo");
		assertTrue(!originalContents.isEmpty());
		long lastRevision = wc1.commit();

		// Create intermediate branch and target branch.
		s.copy("/branches/branch-intermediate", "/branches/branch1");
		s.copy("/branches/branch2", "/branches/branch1");

		// Create original change
		wc1.delete("module1/foo");
		wc1.copyFromRemote("module1/foo", "/branches/branch1/module1/foo", lastRevision);
		wc1.update("module1/foo");
		long origRevision = wc1.commit();

		long mergedRevision = origRevision;

		// Fix the intermediate rebase by applying a semantic move merge.
		LogEntry mergedEntry = doMerge(s, mergedRevision);

		Map<String, LogEntryPath> changedPaths = mergedEntry.getChangedPaths();

		// No changes have been applied.
		assertNotNull(changedPaths.get("/branches/branch2/module1/foo"));

		// No copy has been created, but a simple content change.
		assertCopyFrom(mergedEntry, null, "/branches/branch2/module1/foo");
	}

	public void testMoveIntoNewFolder() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.file("module1/foo");
		wc1.file("module1/bar");
		wc1.commit();

		// Create intermediate branch and target branch.
		s.copy("/branches/branch-intermediate", "/branches/branch1");
		s.copy("/branches/branch2", "/branches/branch1");

		// Create original change
		wc1.mkdir("module1/folder");
		wc1.setProperty("module1/folder", "svn:ignore", "tmp" + NL);
		wc1.copy("module1/folder/foo", "module1/foo");
		wc1.copy("module1/folder/bar", "module1/bar");
		wc1.delete("module1/foo");
		wc1.file("module1/folder/zzz");
		wc1.mkdir("module1/folder/sub");
		wc1.file("module1/folder/sub/zzz");
		long origRevision = wc1.commit();

		assertEquals(Arrays.asList("tmp"), splitSvnIgnore(wc1.getProperty("module1/folder", "svn:ignore")));

		long mergedRevision = origRevision;

		// Fix the intermediate rebase by applying a semantic move merge.
		LogEntry mergedEntry = doMerge(s, mergedRevision);

		// No changes have been applied.
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/folder");
		assertType(mergedEntry, ChangeType.DELETED, "/branches/branch2/module1/foo");
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/folder/foo");
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/folder/bar");
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/folder/zzz");
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/folder/sub");
		assertMissing(mergedEntry, "/branches/branch2/module1/folder/sub/zzz");

		// Folders that are targets of branch local copies must not be cross-branch copied
		// themselves.
		assertCopyFrom(mergedEntry, null, "/branches/branch2/module1/folder");
		assertCopyFrom(mergedEntry, "/branches/branch2/module1/foo", "/branches/branch2/module1/folder/foo");
		assertCopyFrom(mergedEntry, "/branches/branch2/module1/bar", "/branches/branch2/module1/folder/bar");
		assertCopyFrom(mergedEntry, "/branches/branch1/module1/folder/zzz", "/branches/branch2/module1/folder/zzz");
		assertCopyFrom(mergedEntry, "/branches/branch1/module1/folder/sub", "/branches/branch2/module1/folder/sub");
		
		WC wc2 = s.wc("/branches/branch2");
		assertEquals("Property of new folder has not been copied.",
			Arrays.asList("tmp"), splitSvnIgnore(wc2.getProperty("module1/folder", "svn:ignore")));
	}

	private List<String> splitSvnIgnore(String svnIgnore) {
		if (svnIgnore == null) {
			return null;
		}
		return Arrays.asList(svnIgnore.split("\r?\n"));
	}

	public void testMoveIntoRevertedFolder() throws IOException, RepositoryException {
		Scenario s = moduleScenario();

		// Create scenario base.
		WC wc1 = s.wc("/branches/branch1");
		wc1.mkdir("module1/oldfolder");
		wc1.file("module1/oldfolder/foo");
		String originalFooContents = wc1.load("module1/oldfolder/foo");
		wc1.file("module1/bar");
		long revertRevision = wc1.commit();

		wc1.update("module1/oldfolder/foo");
		wc1.commit();

		// Create intermediate branch and target branch.
		s.copy("/branches/branch-intermediate", "/branches/branch1");
		s.copy("/branches/branch2", "/branches/branch1");

		// Create original change
		wc1.copyFromRemote("module1/newfolder", "/branches/branch1/module1/oldfolder", revertRevision);
		wc1.copy("module1/newfolder/bar", "module1/bar");
		wc1.delete("module1/bar");
		long origRevision = wc1.commit();

		long mergedRevision = origRevision;

		// Fix the intermediate rebase by applying a semantic move merge.
		LogEntry mergedEntry = doMerge(s, mergedRevision);

		// No changes have been applied.
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/newfolder");
		assertType(mergedEntry, ChangeType.ADDED, "/branches/branch2/module1/newfolder/bar");
		assertType(mergedEntry, ChangeType.DELETED, "/branches/branch2/module1/bar");

		// Folders that are targets of branch local copies must not be cross-branch copied
		// themselves.
		assertCopyFrom(mergedEntry, "/branches/branch2/module1/oldfolder", "/branches/branch2/module1/newfolder");
		assertCopyFrom(mergedEntry, "/branches/branch2/module1/bar", "/branches/branch2/module1/newfolder/bar");

		WC wc2 = s.wc("/branches/branch2");
		String mergedFooContents = wc2.load("module1/newfolder/foo");
		assertEquals("The contents revert was not performed in the merge result.",
			originalFooContents, mergedFooContents);
	}

	private long rebaseSvn(Scenario s, long origRevision) throws IOException, RepositoryException {
		// Create rebase of the move with a regular SVN merge creating a cross branch copy.
		WC wc2 = s.wc("/branches/branch-intermediate");
		long rebasedRevision = wc2.merge("branches/branch1", origRevision, MERGED_MODULES);
		return rebasedRevision;
	}

	private LogEntry doMerge(Scenario s, long revision) throws IOException, RepositoryException {
		return doMerge(s, revision, Collections.<String, String> emptyMap());
	}

	private LogEntry doMerge(Scenario s, long revision, Map<String, String> resourceMapping)
			throws IOException, RepositoryException {
		WC wc2 = s.wc("/branches/branch2");
		LogEntry entry = s.log(revision);

		MergeOperation merge = createMerge(s, wc2, entry, resourceMapping);
		doMerge(s, wc2, merge);

		Commit commit = createCommit(s, wc2, entry, merge.getTouchedResources());
		CommitInfo info = doCommit(s, wc2, commit);

		return s.log(info.getNewRevision());
	}

	private CommitInfo doCommit(Scenario s, WC wc, Commit commit) throws RepositoryException {
		CommitInfo info =
			commit.run(new CommitContext(s.clientManager().getClient(), s.clientManager().getClient()));

		assertEquals("Not all merged resources were committed.", Collections.emptyList(), wc.getModified());
		return info;
	}

	private Commit createCommit(Scenario s, WC wc, LogEntry entry, Set<String> resources) {
		CommitConfig commitConfig = ValueFactory.newInstance(CommitConfig.class);
		commitConfig.setWorkspaceRoot(wc.getDirectory());
		Commit commit = new Commit(commitConfig, entry, null);
		commit.setCommitMessage(s.createMessage());
		commit.addTouchedResources(resources);
		return commit;
	}

	private void doMerge(Scenario s, WC wc, MergeOperation merge) throws RepositoryException {
		Map<File, List<ConflictDescription>> conflicts = tryMerge(s, merge);
		assertTrue(MergeCommitHandler.toStringConflicts(wc.getDirectory(), conflicts), conflicts.isEmpty());
	}

	protected Map<File, List<ConflictDescription>> tryMerge(Scenario s, MergeOperation merge) throws RepositoryException {
		CommandExecutor executor = s.clientManager().getOperationsFactory().getExecutor();
		Map<File, List<ConflictDescription>> conflicts = executor.execute(merge.getCommands());
		return conflicts;
	}

	private MergeOperation createMerge(Scenario s, WC wc, LogEntry entry) throws RepositoryException {
		return createMerge(s, wc, entry, Collections.<String, String> emptyMap());
	}

	private MergeOperation createMerge(Scenario s, WC wc, LogEntry entry, Map<String, String> resourceMapping)
			throws RepositoryException {
		MergeConfig mergeConfig = ValueFactory.newInstance(MergeConfig.class);
		mergeConfig.setSvnURL(s.getRepositoryUrl().toString());
		mergeConfig.setSemanticMoves(true);
		mergeConfig.setWorkspaceRoot(wc.getDirectory());
		mergeConfig.getResourceMapping().putAll(resourceMapping);
		BranchConfig branchConfig = ValueFactory.newInstance(BranchConfig.class);
		branchConfig.setBranchPattern("/branches/[^/]+/");
		MergeHandler handler =
			new MergeHandler(s.clientManager(), mergeConfig, new PathParser(branchConfig), new HashSet<>(
				MERGED_MODULES));

		MergeOperation merge = handler.parseMerge(entry);
		return merge;
	}

	private Scenario moduleScenario() throws IOException, RepositoryException {
		Scenario s = scenario(_clientManager);
		s.mkdir("/branches");
		s.mkdir("/branches/branch1");
		s.mkdir("/branches/branch1/module1");
		s.mkdir("/branches/branch1/module2");
		s.mkdir("/branches/branch1/module3");
		return s;
	}

	private static List<RevisionRange> ranges(RevisionRange... ranges) {
		return Arrays.asList(ranges);
	}

	private static RevisionRange range(long r1) {
		return revisionRange(r1 - 1, r1);
	}

	private static RevisionRange revisionRange(long r1, long r2) {
		return RevisionRange.create(revision(r1), revision(r2));
	}

	private static Revision revision(long r1) {
		return Revision.create(r1);
	}

	private static void assertType(LogEntry changedPaths, ChangeType expectedType, String path) {
		LogEntryPath entry = changedPaths.getChangedPaths().get(path);
		assertPathExistsInChangeSet(entry, path);
		assertEquals(expectedType, entry.getType());
	}

	private static void assertCopyFrom(LogEntry mergedEntry, String expectedCopyPath, String path) {
		assertCopyFrom(null, mergedEntry, expectedCopyPath, path);
	}

	private static void assertCopyFrom(String message, LogEntry mergedEntry, String expectedCopyPath, String path) {
		LogEntryPath entry = mergedEntry.getChangedPaths().get(path);
		assertPathExistsInChangeSet(entry, path);
		assertEquals(message, expectedCopyPath, entry.getCopyPath());
	}

	private static void assertPathExistsInChangeSet(LogEntryPath entry, String path) {
		assertNotNull("Path '" + path + "' does not exist in change set.", entry);
	}

	private static void assertMissing(LogEntry mergedEntry, String path) {
		assertNull(mergedEntry.getChangedPaths().get(path));
	}

	static class Setup extends TestSetup {

		private final String _providerName;

		private static String _activeProviderName;

		public Setup(Class<TestMerge> test, String providerName) {
			super(new TestSuite(test, providerName));
			_providerName = providerName;
		}

		@Override
		public String toString() {
			return super.toString() + "(" + _providerName + ")";
		}

		@Override
		protected void setUp() throws Exception {
			super.setUp();

			_activeProviderName = _providerName;
		}

		public static String getProviderName() {
			return _activeProviderName;
		}

	}

	/**
	 * @return a cumulative {@link Test} for all Tests in {@link TestMerge}.
	 */
	@SuppressWarnings("unused")
	public static Test suite() {
		if (!true) {
			TestCase t = new TestMerge();
			t.setName("testMergeFolderConflict4");
			return t;
		}
		TestSuite suite = new TestSuite();
		suite.addTest(new Setup(TestMerge.class, "javahl"));
		suite.addTest(new Setup(TestMerge.class, "svnkit"));
		return suite;
	}
}
