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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
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

	private static Pattern LINE_PATTERN =
		Pattern.compile(
			"((?:(?:[^\\\\=]+|(?:\\\\.))*(?:[^\\\\= ]+|(?:\\\\.)))?)\\s*=\\s*((?:[^ ](?:.*(?:[^ \\\\]|\\\\ ))?)?)\\s*");

	public static ResourceMapping loadMapping(File mappingFile) throws IOException {
		try (FileInputStream in = new FileInputStream(mappingFile)) {
			return loadMapping(in);
		}
	}

	public static ResourceMapping loadMapping(InputStream in) throws IOException, UnsupportedEncodingException {
		RegexpResourceMapping mapping = new RegexpResourceMapping();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, "ISO-8859-1"))) {
			String definition;
			while ((definition = nextDef(reader)) != null) {
				Matcher separator = LINE_PATTERN.matcher(definition);
				if (separator.matches()) {
					String key = separator.group(1);
					String value = separator.group(2);
					mapping.addReplacement(unescape(key), unescape(value));
				} else {
					throw new IllegalArgumentException("Cannot read mapping definition: " + definition);
				}
			}
		}
		return mapping;
	}

	private static Pattern ESC_PATTERN =
		Pattern.compile("(?:\\\\u([0-9a-fA-F]{4}))|(?:\\\\x([0-9a-fA-F]{2}))|(?:\\\\([^ux]))");

	private static String unescape(String s) {
		Matcher matcher = ESC_PATTERN.matcher(s);
		if (matcher.find()) {
			StringBuffer buffer = new StringBuffer();
			do {
				String replacement;
				if (matcher.group(1) != null) {
					replacement = "" + (char) (Integer.parseInt(matcher.group(1), 16));
				} else if (matcher.group(2) != null) {
					replacement = "" + (char) (Integer.parseInt(matcher.group(2), 16));
				} else {
					replacement = matcher.group(3);
				}

				matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
			} while (matcher.find());
			matcher.appendTail(buffer);
			return buffer.toString();
		} else {
			return s;
		}
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

			line = line.substring(0, line.length() - 1) + fragment;
		}

		return line;
	}

	private static String nextLine(BufferedReader reader) throws IOException {
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				return null;
			}

			if (line.trim().isEmpty()) {
				continue;
			}
			if (line.charAt(0) == '#') {
				continue;
			}

			return line;
		}
	}

}
