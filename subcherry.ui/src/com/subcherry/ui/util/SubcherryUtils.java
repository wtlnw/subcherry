/*
 * SubCherry - Cherry Picking with Trac and Subversion
 * Copyright (C) 2018 Bernhard Haumacher and others
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
package com.subcherry.ui.util;

/**
 * Static utility methods for Subcherry UI.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public interface SubcherryUtils {

	/**
	 * The path separator character as {@link String}.
	 */
	String PATH_SEPARATOR = "/"; //$NON-NLS-1$

	/**
	 * @param path
	 *            the path {@link String} to remove the trailing
	 *            {@value #PATH_SEPARATOR} from
	 * @return the given path without the trailing {@value #PATH_SEPARATOR} or
	 *         {@code path} if it did not end with {@value #PATH_SEPARATOR}
	 */
	static String noTrailingSeparator(final String path) {
		if (path.endsWith(PATH_SEPARATOR)) {
			return path.substring(0, path.length() - PATH_SEPARATOR.length());
		}

		return path;
	}

	/**
	 * @param path
	 *            the path {@link String} to append the {@value #PATH_SEPARATOR} to
	 * @return the given path with a trailing {@value #PATH_SEPARATOR} or
	 *         {@code path} if it already ended with {@value #PATH_SEPARATOR}
	 */
	static String trailingSeparator(final String path) {
		if (path.endsWith(PATH_SEPARATOR)) {
			return path;
		}

		return path + PATH_SEPARATOR;
	}
}
