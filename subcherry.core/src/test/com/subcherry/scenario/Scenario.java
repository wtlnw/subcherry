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
package test.com.subcherry.scenario;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import com.subcherry.merge.LastLogEntry;
import com.subcherry.repository.command.Client;
import com.subcherry.repository.command.ClientManager;
import com.subcherry.repository.command.copy.CopySource;
import com.subcherry.repository.core.CommitInfo;
import com.subcherry.repository.core.Depth;
import com.subcherry.repository.core.LogEntry;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.repository.core.RepositoryURL;
import com.subcherry.repository.core.Revision;

public class Scenario extends FileSystem {

	private ClientManager _clientManager;
	private RepositoryURL _repositoryUrl;

	private int _id = 1;

	private int _fileId = 1;

	private static int _scenarioId = 1;

	public static Scenario scenario(ClientManager clientManager) throws IOException, RepositoryException {
		File repositoryRoot = File.createTempFile("repos", "");
		repositoryRoot.delete();
		repositoryRoot.mkdir();

		RepositoryURL repositoryUrl =
			clientManager.getClient().createRepository(repositoryRoot, "uuid-" + (_scenarioId++), true, false);

		return new Scenario(clientManager, repositoryUrl);
	}

	private Scenario(ClientManager clientManager, RepositoryURL repositoryUrl) {
		_clientManager = clientManager;
		_repositoryUrl = repositoryUrl;
	}

	public RepositoryURL getRepositoryUrl() {
		return _repositoryUrl;
	}

	public ClientManager clientManager() {
		return _clientManager;
	}

	public WC wc(String path) throws IOException, RepositoryException {
		return new WC(this, path);
	}

	public LogEntry log(long revision) throws RepositoryException {
		String[] paths = { "/" };
		Revision startRevision = Revision.create(revision);
		Revision endRevision = Revision.create(revision);
		Revision pegRevision = startRevision;
		LastLogEntry handler = new LastLogEntry();
		clientManager().getClient().log(getRepositoryUrl(), paths, pegRevision, startRevision, endRevision, false,
			true, false, 0, null, handler);
		return handler.getLogEntry();
	}

	@Override
	public long mkdir(String path) throws RepositoryException {
		RepositoryURL url = getRepositoryUrl().appendPath(path);
		CommitInfo result = commitClient().mkDir(new RepositoryURL[] { url }, createMessage(), null, true);
		return result.getNewRevision();
	}

	@Override
	public long file(String path) throws IOException, RepositoryException {
		File dir = File.createTempFile("upload", "");
		dir.delete();
		dir.mkdir();
		File file = new File(dir, fileName(path));
		fillFileContent(file);
		RepositoryURL dstURL = getRepositoryUrl().appendPath(path);
		CommitInfo result = commitClient().importResource(file, dstURL, createMessage(), null, false, true, Depth.FILES);
		return result.getNewRevision();
	}

	static String fileName(String path) {
		int separator = path.lastIndexOf('/');
		if (separator < 0) {
			return path;
		} else {
			return path.substring(separator + 1);
		}
	}

	@Override
	public long copy(String toPath, String fromPath) throws RepositoryException {
		return internalCopy(toPath, fromPath, Revision.HEAD);
	}

	@Override
	public long copy(String toPath, String fromPath, long revision) throws RepositoryException {
		return internalCopy(toPath, fromPath, Revision.create(revision));
	}

	private long internalCopy(String toPath, String fromPath, Revision revision) throws RepositoryException {
		RepositoryURL srcUrl = getRepositoryUrl().appendPath(fromPath);
		CopySource source = CopySource.create(Revision.HEAD, revision, srcUrl);
		CopySource[] sources = { source };
		RepositoryURL dst = getRepositoryUrl().appendPath(toPath);
		CommitInfo result = copyClient().copy(sources, dst, false, false, true, createMessage(), null);
		return result.getNewRevision();
	}

	private Client copyClient() {
		return clientManager().getClient();
	}

	public String createMessage() {
		return "Commit " + createCommitId() + ".";
	}

	int createCommitId() {
		return _id++;
	}

	void fillFileContent(File file) throws FileNotFoundException, IOException {
		FileOutputStream out = new FileOutputStream(file);
		try {
			Writer writer = new OutputStreamWriter(out, Charset.forName("utf-8"));
			writer.write(createFileContent());
			writer.close();
		} finally {
			out.close();
		}
	}

	String createFileContent() {
		return "File " + createFileId();
	}

	int createFileId() {
		return _fileId++;
	}

	private Client commitClient() {
		return _clientManager.getClient();
	}

}
