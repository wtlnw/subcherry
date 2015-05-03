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
package com.subcherry.repository.core;

import java.net.MalformedURLException;
import java.net.URL;

public class RepositoryURL {

	private final String _protocol;

	private final String _host;

	private final int _port;

	private final String _path;

	public RepositoryURL(String protocol, String host, int port, String path) {
		_protocol = protocol;
		_host = host;
		_port = port;
		_path = path;
	}

	public static RepositoryURL parse(String url) {
		URL parsed;
		try {
			parsed = new URL(url);
		} catch (MalformedURLException ex) {
			throw new IllegalArgumentException(ex);
		}
		String path = parsed.getPath();
		int start = 0;
		while (start < path.length() && path.charAt(start) == '/') {
			start++;
		}
		if (start > 0) {
			path = path.substring(start);
		}
		return new RepositoryURL(parsed.getProtocol(), parsed.getHost(), parsed.getPort(), path);
	}

	public RepositoryURL appendPath(String path) {
		return new RepositoryURL(_protocol, _host, _port, _path + '/' + path);
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(_protocol);
		result.append("://");
		result.append(_host);
		if (_port > 0) {
			result.append(":");
			result.append(_port);
		}
		result.append("/");
		result.append(_path);
		return result.toString();
	}

	public String getProtocol() {
		return _protocol;
	}

	public String getHost() {
		return _host;
	}

	public int getPort() {
		return _port;
	}

	public String getPath() {
		return _path;
	}

}

