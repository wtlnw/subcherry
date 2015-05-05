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


public class LogFilter {

	private static final char SLASH = '/';
	private static final String ROOT = Character.toString(SLASH);
	
	private String _url;
	private String _prefix;
	private String[] _suffixes;

	public LogFilter(String url, String[] paths) {
		_url = removeTailingSlash(url);
		
		String prefix = startingWithSlash(paths[0]);
		for (int n = 1, cnt = paths.length; n < cnt; n++) {
			String path = startingWithSlash(paths[n]);
			prefix = commonPrefix(prefix, path);
		}
		_prefix = prefix;
		
		_suffixes = new String[paths.length];
		int prefixLength = prefix.length();
		for (int n = 0, cnt = paths.length; n < cnt; n++) {
			String path = startingWithSlash(paths[n]);
			_suffixes[n] = path.substring(prefixLength);
		}
	}
	
	private static String removeTailingSlash(String url) {
		if (url.charAt(url.length() - 1) == SLASH) {
			return url.substring(0, url.length() - 1);
		} else {
			return url;
		}
	}

	private static String startingWithSlash(String path) {
		if (path.isEmpty()) {
			return ROOT;
		} else if (path.charAt(0) == SLASH) {
			return path;
		} else {
			return SLASH + path;
		}
	}

	public String getPrefixUrl() {
		return _url + _prefix;
	}

	private String commonPrefix(String p1, String p2) {
		String shorter;
		String longer;
		if (p1.length() < p2.length()) {
			shorter = p1;
			longer = p2;
		} else {
			shorter = p2;
			longer = p1;
		}
		int cnt = shorter.length();
		for (int n = 0; n < cnt; n++) {
			if (shorter.charAt(n) != longer.charAt(n)) {
				return path(shorter, n);
			}
		}
		if (cnt < longer.length()) {
			if (longer.charAt(cnt) == SLASH) {
				// The shorter is the parent directory of the longer without a
				// trailing slash.
				return shorter;
			} else {
				return path(longer, cnt);
			}
		} else {
			// Paths were equal.
			return p1;
		}
	}

	private String path(String path, int size) {
		for (int n = size - 1; n >= 0; n--) {
			if (path.charAt(n) == SLASH) {
				return path.substring(0, n + 1);
			}
		}
		return ROOT;
	}

	public boolean accept(String path) {
		for (String suffix : _suffixes) {
			if (path.startsWith(suffix, _prefix.length())) {
				return true;
			}
		}
		return false;
	}

}
