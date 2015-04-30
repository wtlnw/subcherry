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
package com.subcherry.utils;

import java.util.logging.Logger;

/**
 * @version   $Revision$  $Author$  $Date$
 */
public class Log {

	public static void info(String info) {
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info(info);
	}

	public static void warning(String message) {
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).warning(message);
	}
	
}

