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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.subcherry.repository.command.Client;
import com.subcherry.repository.command.ClientManager;
import com.subcherry.repository.command.copy.CopySource;
import com.subcherry.repository.command.status.Status;
import com.subcherry.repository.command.status.StatusHandler;
import com.subcherry.repository.core.CommitInfo;
import com.subcherry.repository.core.Depth;
import com.subcherry.repository.core.PropertyData;
import com.subcherry.repository.core.PropertyValue;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.repository.core.RepositoryURL;
import com.subcherry.repository.core.Resolution;
import com.subcherry.repository.core.Revision;
import com.subcherry.repository.core.RevisionRange;

public class WC extends FileSystem {

	private Scenario _scenario;

	private String _path;

	private RepositoryURL _baseUrl;

	private File _wcPath;

	WC(Scenario scenario, String path) throws IOException, RepositoryException {
		_scenario = scenario;
		_path = path;

		_wcPath = File.createTempFile("workspace", "");
		_wcPath.delete();
		_wcPath.mkdir();

		_baseUrl = scenario().getRepositoryUrl().appendPath(path);
		scenario.clientManager().getClient().checkout(_baseUrl, _wcPath, Revision.HEAD, Revision.HEAD,
			Depth.INFINITY, false);
	}

	public long commit() throws RepositoryException {
		File[] paths = { _wcPath };
		CommitInfo commitInfo = clientManager().getClient()
			.commit(paths, false, scenario().createMessage(), null, null, false, false, Depth.INFINITY);
		return commitInfo.getNewRevision();
	}

	public File getDirectory() {
		return _wcPath;
	}

	@Override
	public long mkdir(String path) throws RepositoryException {
		File dir = toFile(path);
		dir.mkdirs();
		add(dir);
		return -1;
	}

	@Override
	public long file(String path) throws RepositoryException, IOException {
		File file = toFile(path);
		scenario().fillFileContent(file);
		add(file);
		return -1;
	}

	public void setProperty(String path, String name, String value) throws RepositoryException {
		File file = toFile(path);
		clientManager().getClient().setProperty(file, name, PropertyValue.create(value), false, Depth.EMPTY,
			null, null);
	}

	@Override
	public long copy(String toPath, String fromPath) throws RepositoryException {
		return internalCopy(toPath, fromPath, Revision.BASE);
	}

	@Override
	public long copy(String toPath, String fromPath, long revision) throws RepositoryException {
		return internalCopy(toPath, fromPath, Revision.create(revision));
	}

	private long internalCopy(String toPath, String fromPath, Revision rev) throws RepositoryException {
		CopySource[] sources = { CopySource.create(Revision.BASE, rev, toFile(fromPath)) };
		File dst = toFile(toPath);
		clientManager().getClient().copy(sources, dst, false, false, true);
		return -1;
	}

	public void copyFromRemote(String toPath, String remoteFromPath, long revision) throws RepositoryException {
		Revision svnRevision = Revision.create(revision);
		CopySource[] sources =
			{ CopySource.create(svnRevision, svnRevision, scenario().getRepositoryUrl()
				.appendPath(remoteFromPath)) };
		File dst = toFile(toPath);
		clientManager().getClient().copy(sources, dst, false, false, true);
	}

	public File toFile(String path) {
		File dir = new File(getDirectory(), path);
		return dir;
	}

	private void add(File file) throws RepositoryException {
		wcClient().add(file, false, false, true, Depth.EMPTY, false, true);
	}

	private ClientManager clientManager() {
		return scenario().clientManager();
	}

	private Scenario scenario() {
		return _scenario;
	}

	public void delete(String path) throws RepositoryException {
		wcClient().delete(toFile(path), false, true, false);
	}

	private Client wcClient() {
		return clientManager().getClient();
	}

	public void update(String path) throws IOException {
		scenario().fillFileContent(toFile(path));
	}

	public String load(String path) throws IOException {
		FileInputStream in = new FileInputStream(toFile(path));
		try {
			CharBuffer target = CharBuffer.allocate(500);
			Reader r = new InputStreamReader(in);
			try {
				while (r.read(target) >= 0) {
					// Read until the end of file is reached.
				}
			} finally {
				r.close();
			}
			target.flip();
			return target.toString();
		} finally {
			in.close();
		}
	}

	public String getProperty(String path, String property) throws RepositoryException {
		Client wcClient = scenario().clientManager().getClient();
		PropertyData data =
			wcClient.getProperty(new File(_wcPath, path), property, Revision.WORKING, Revision.WORKING);
		if (data == null) {
			return null;
		}
		return data.getValue().asString();
	}

	public List<File> getModified() throws RepositoryException {
		final List<File> modified = new ArrayList<>();
		StatusHandler handler = new StatusHandler() {
			@Override
			public void handleStatus(Status status) throws RepositoryException {
				File file = status.getFile();
				modified.add(file);
			}
		};
		clientManager().getClient().
			status(getDirectory(), Revision.HEAD, Depth.INFINITY, false, false, false, handler, null);
		return modified;
	}

	public long merge(String branch, long revision, List<String> mergedModules) throws RepositoryException {
		RepositoryURL sourceBranch = scenario().getRepositoryUrl().appendPath(branch);
		Revision baseRevision = Revision.create(revision - 1);
		Revision svnRevision = Revision.create(revision);
		Revision pegRevision = svnRevision;
		Collection<RevisionRange> rangesToMerge = Arrays.asList(new RevisionRange(baseRevision, svnRevision));
		for (String module : mergedModules) {
			RepositoryURL sourceModule = sourceBranch.appendPath(module);
			File targetModule = new File(getDirectory(), module);
			clientManager().getClient().merge(sourceModule, pegRevision, rangesToMerge, targetModule,
				Depth.INFINITY, true, false, false, false);
		}
		return commit();
	}

	public void resolve(String path, Depth depth, Resolution resolution) throws RepositoryException {
		Client client = clientManager().getClient();
		client.resolve(toFile(path), depth, resolution);
	}

}
