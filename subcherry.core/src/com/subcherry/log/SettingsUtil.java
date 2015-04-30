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
package com.subcherry.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class SettingsUtil {

	public static Properties loadConfig(File settingsFile) throws IOException {
		Properties loadedProperties = new Properties();
		if (settingsFile.exists()) {
			FileInputStream settingsIn = new FileInputStream(settingsFile);
			loadedProperties.load(settingsIn);
			settingsIn.close();
		}
		return loadedProperties;
	}

	public static void saveConfig(File settingsFile, Properties properties) throws IOException {
		FileOutputStream settingsOut = new FileOutputStream(settingsFile);
		properties.store(settingsOut, "Ticket check settings");
		settingsOut.close();
	}

}
