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
package com.subcherry.repository.javahl.internal;

import com.subcherry.repository.command.Settings;
import com.subcherry.repository.command.merge.CommandExecutor;
import com.subcherry.repository.impl.DefaultOperationFactory;

final class HLOperationFactory extends DefaultOperationFactory {

	private Settings _settings = new Settings() {

		@Override
		public void setSleepForTimestamp(boolean b) {
			// Ignore.
		}
	};

	private CommandExecutor _executor = new HLExecutor();

	private HLClientManager _clientManager;

	public HLOperationFactory(HLClientManager clientManager) {
		_clientManager = clientManager;
	}
	
	public HLClientManager getClientManager() {
		return _clientManager;
	}

	@Override
	public Settings settings() {
		return _settings;
	}

	@Override
	public CommandExecutor getExecutor() {
		return _executor;
	}
}