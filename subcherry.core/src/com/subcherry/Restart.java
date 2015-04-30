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
package com.subcherry;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import de.haumacher.common.config.PropertiesUtil;

/**
 * @version   $Revision$  $Author$  $Date$
 */
public class Restart {

	public static final long NO_REVISION_FOUND = -1;
	
	private static final String REVISION_PROPERTY = "revision";
	private static final String FILE_NAME = "conf/restart.properties";

	public static void setRevision(long revision) throws IOException {
		Properties p = new Properties();
		p.setProperty(REVISION_PROPERTY, Long.toString(revision));
		PropertiesUtil.storeProperties(p, FILE_NAME, 
				"\n\tProperties are used by merge Tool to restart merge at stored revision.\n"
						+ "\tThe file is deleted after completing merge.\n"
						+ "\tDo not modify.\n\n\tDelete when configured revision should be used.\n");
	}
	
	public static long getRevision() {
		try {
			Properties properties = PropertiesUtil.loadProperties(FILE_NAME);
			String nextRevision = properties.getProperty(REVISION_PROPERTY);
			return Long.parseLong(nextRevision);
		} catch (IOException ex) {
			return NO_REVISION_FOUND;
		}
	}
	
	public static void clear() {
		File f = new File(FILE_NAME);
		f.delete();
	}
	
	

}

