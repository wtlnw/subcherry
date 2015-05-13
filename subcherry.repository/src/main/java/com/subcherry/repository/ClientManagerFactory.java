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
package com.subcherry.repository;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import com.subcherry.repository.command.ClientManager;

public abstract class ClientManagerFactory {

	public static final String DEFAULT_NAME = "DEFAULT";

	private static Map<String, ClientManagerFactory> _factories;

	public static ClientManagerFactory getInstance(String providerName) {
		if (_factories == null) {
			_factories = new HashMap<String, ClientManagerFactory>();

			ServiceLoader<ClientManagerFactory> providerLoader = ServiceLoader.load(ClientManagerFactory.class);
			Iterator<ClientManagerFactory> providers = providerLoader.iterator();
			boolean first = true;
			while (providers.hasNext()) {
				ClientManagerFactory factory = providers.next();
				if (first) {
					first = false;
					_factories.put(DEFAULT_NAME, factory);
				}
				_factories.put(factory.getProviderName(), factory);
			}
		}

		return _factories.get(applyDefault(providerName));
	}

	private static Object applyDefault(String providerName) {
		return providerName == null ? DEFAULT_NAME : (providerName.isEmpty() ? DEFAULT_NAME : providerName);
	}

	public final ClientManager createClientManager() {
		return createClientManager(null);
	}

	public abstract String getProviderName();

	public abstract ClientManager createClientManager(LoginCredential svnCredentials);

}
