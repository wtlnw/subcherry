/*
 * TimeCollect records time you spent on your development work.
 * Copyright (C) 2015 Bernhard Haumacher and others
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
package subcherry.repository.svnkit.internal;

import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.internal.wc.SVNDiffConflictChoiceStyle;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.wc.DefaultSVNRepositoryPool;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

import com.subcherry.repository.LoginCredential;
import com.subcherry.repository.command.ClientManager;

public class SKClientManager implements ClientManager {

	private SVNClientManager _impl;
	
	private SKClient _client = new SKClient(this);

	private SKOperationFactory _factory = new SKOperationFactory(this);

	public SKClientManager(LoginCredential credentials) {
		
		DefaultSVNOptions options = new DefaultSVNOptions() {
			@Override
			public org.tmatesoft.svn.core.wc.ISVNMerger createMerger(byte[] conflictStart, byte[] conflictSeparator, byte[] conflictEnd) {
				return new ContentSensitiveMerger(conflictStart, conflictSeparator, conflictEnd, getConflictResolver(),
					SVNDiffConflictChoiceStyle.CHOOSE_MODIFIED_LATEST);
			}
		};
		
		SVNWCContext svnwcContext = new SVNWCContext(options, null);
		SvnOperationFactory svnOperationFactory = new SvnOperationFactory(svnwcContext);
		
		if (credentials != null) {
			ISVNAuthenticationManager authManager = org.tmatesoft.svn.core.wc.SVNWCUtil.createDefaultAuthenticationManager(
					credentials.getUser(), credentials.getPasswd());
			svnOperationFactory.setRepositoryPool(new DefaultSVNRepositoryPool(authManager, options));
		}
		
		svnOperationFactory.setAutoDisposeRepositoryPool(true);
		
		_impl = SVNClientManager.newInstance(svnOperationFactory);
	}
	
	@Override
	public SKOperationFactory getOperationsFactory() {
		return _factory;
	}
	
	@Override
	public SKClient getClient() {
		return _client;
	}

	@Override
	public void close() {
		_impl.dispose();
	}

	protected final SVNClientManager impl() {
		return _impl;
	}

}
