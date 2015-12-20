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
			parsed = new URL(url.replace("#", "%23"));
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
		return new RepositoryURL(_protocol, _host, _port, append(path));
	}

	private String append(String path) {
		if (path.isEmpty()) {
			return _path;
		} else if (_path.charAt(_path.length() - 1) == '/') {
			if (path.charAt(0) == '/') {
				return _path + path.substring(1);
			} else {
				return _path + path;
			}
		} else if (path.charAt(0) == '/') {
			return _path + path;
		} else {
			return _path + '/' + path;
		}
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
		if ((!_path.isEmpty()) && (_path.charAt(0) != '/')) {
			result.append("/");
		}
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_host == null) ? 0 : _host.hashCode());
		result = prime * result + ((_path == null) ? 0 : _path.hashCode());
		result = prime * result + _port;
		result = prime * result + ((_protocol == null) ? 0 : _protocol.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RepositoryURL other = (RepositoryURL) obj;
		if (_host == null) {
			if (other._host != null)
				return false;
		} else if (!_host.equals(other._host))
			return false;
		if (_path == null) {
			if (other._path != null)
				return false;
		} else if (!_path.equals(other._path))
			return false;
		if (_port != other._port)
			return false;
		if (_protocol == null) {
			if (other._protocol != null)
				return false;
		} else if (!_protocol.equals(other._protocol))
			return false;
		return true;
	}

}

