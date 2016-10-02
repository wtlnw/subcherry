/*
 * SubCherry - Cherry Picking with Trac and Subversion
 * Copyright (C) 2016 Bernhard Haumacher and others
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
package com.subcherry.merge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.subcherry.merge.ResourceMapping.RegexpResourceMapping;

/**
 * Parser for a properties-like text file containing definitions of the form:
 * 
 * <pre>
 * # Comment
 * regexp = replacement
 * regexp = replacement
 * </pre>
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class MappingLoader {

	private static Pattern SEPARATOR_PATTERN = Pattern.compile("\\s*=\\s*");

	public static ResourceMapping loadMapping(File mappingFile) throws IOException {
		RegexpResourceMapping mapping = new RegexpResourceMapping();
		try (FileInputStream in = new FileInputStream(mappingFile)) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, "ISO-8859-1"))) {
				String definition;
				while ((definition = nextDef(reader)) != null) {
					Matcher separator = SEPARATOR_PATTERN.matcher(definition);
					if (separator.find()) {
						String key = definition.substring(0, separator.start());
						String value = definition.substring(separator.end());
						mapping.addReplacement(key, value);
					}
				}
			}
		}
		return mapping;
	}

	private static String nextDef(BufferedReader reader) throws IOException {
		String line = nextLine(reader);
		if (line == null) {
			return null;
		}

		while (line.endsWith("\\")) {
			String fragment = nextLine(reader);
			if (fragment == null) {
				break;
			}

			line += fragment;
		}

		return line;
	}

	private static String nextLine(BufferedReader reader) throws IOException {
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				return null;
			}

			line = line.trim();
			if (line.isEmpty()) {
				continue;
			}
			if (line.charAt(0) == '#') {
				continue;
			}

			return line;
		}
	}

}
