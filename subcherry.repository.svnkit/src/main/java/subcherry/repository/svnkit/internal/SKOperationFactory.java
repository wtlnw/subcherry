/*
 * SubCherry - Cherry Picking with Trac and Subversion
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

import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

import com.subcherry.repository.command.Settings;
import com.subcherry.repository.command.merge.CommandExecutor;
import com.subcherry.repository.impl.DefaultOperationFactory;

public class SKOperationFactory extends DefaultOperationFactory {

	private final SKSettings _settings = new SKSettings();
	
	private final SKCommandExecutor _executor = new SKCommandExecutor();

	private SKClientManager _clientManager;

	public SKOperationFactory(SKClientManager clientManager) {
		_clientManager = clientManager;
	}
	
	public SKClientManager clientManager() {
		return _clientManager;
	}

	@Override
	public CommandExecutor getExecutor() {
		return _executor;
	}

	@Override
	public Settings settings() {
		return _settings;
	}

	public SvnOperationFactory impl() {
		return clientManager().impl().getOperationFactory();
	}

}
