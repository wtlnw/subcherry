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
package com.subcherry.repository.command;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.subcherry.repository.core.Depth;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.repository.core.Revision;
import com.subcherry.repository.core.RevisionRange;
import com.subcherry.repository.core.Target;

public abstract class DefaultClient implements Client {

	enum State {
		HEADER, PROPERTIES, MERGEINFO, SKIPDIFF
	}

	@Override
	public Map<String, List<RevisionRange>> mergeInfoDiff(Target target, long rev) throws RepositoryException {
		ByteArrayOutputStream diffBuffer = new ByteArrayOutputStream();
		diff(target, Revision.create(rev - 1), Revision.create(rev), Depth.EMPTY, false, diffBuffer);

		try {
			InputStreamReader inStream = new InputStreamReader(new ByteArrayInputStream(diffBuffer.toByteArray()));
			try {
				return parseMergeInfoDiff(inStream);
			} finally {
				inStream.close();
			}
		} catch (IOException ex) {
			throw new IOError(ex);
		}
	}

	private static Map<String, List<RevisionRange>> parseMergeInfoDiff(InputStreamReader inStream) throws IOException {
		// TODO: API not as general as necessary: Negative merge info (reverse merge) is not
		// represented.
		Map<String, List<RevisionRange>> result = new HashMap<String, List<RevisionRange>>();
		BufferedReader in = new BufferedReader(inStream);
		String line;
		State state = State.HEADER;
		while ((line = in.readLine()) != null) {
			while (true) {
				switch (state) {
					case HEADER: {
						if (line.equals("___________________________________________________________________")) {
							state = State.PROPERTIES;
						}
						break;
					}

					case PROPERTIES: {
						if (line.endsWith(": svn:mergeinfo")) {
							state = State.MERGEINFO;
						} else {
							state = State.SKIPDIFF;
						}
						break;
					}

					case SKIPDIFF: {
						switch (line.charAt(0)) {
							case '#':
							case '+':
							case '-':
							case ' ':
							case '\\':
								// Ignore.
								break;
							default:
								state = State.PROPERTIES;
								continue;
						}
						break;
					}

					case MERGEINFO: {
						if (line.startsWith("   ")) {
							// TODO: Reverse merge is not detected (recorded as positive merge).
							int start = line.indexOf(" /");
							if (start >= 0) {
								int pathStart = start + 1;
								int revSepIndex = line.lastIndexOf(':');
								if (revSepIndex >= 0 && revSepIndex > pathStart) {
									String path = line.substring(pathStart, revSepIndex);
									List<RevisionRange> ranges = parseRanges(line, revSepIndex + 1);
									result.put(path, ranges);
								}
							}
						} else {
							state = State.PROPERTIES;
							continue;
						}
						break;
					}
				}

				break;
			}
		}

		return result;
	}

	private static List<RevisionRange> parseRanges(String line, int index) {
		ArrayList<RevisionRange> result = new ArrayList<>();
		int stop = line.length();
		while (index < stop) {
			if (line.charAt(index) == 'r') {
				index++;
				int end1 = endOfNumber(line, index);
				Revision r1 = Revision.create(Long.parseLong(line.substring(index, end1)));
				if (end1 < stop) {
					if (line.charAt(end1) == '*') {
						end1++;
					}
				}

				if (end1 < stop && line.charAt(end1) == '-') {
					int start2 = end1 + 1;
					int end2 = endOfNumber(line, start2);
					Revision r2 = Revision.create(Long.parseLong(line.substring(start2, end2)));
					if (end2 < stop && line.charAt(end2) == '*') {
						end2++;
					}

					result.add(RevisionRange.create(r1, r2));

					index = end2;
				} else {
					result.add(RevisionRange.create(r1, r1));
					index = end1;
				}

				if (index < stop && line.charAt(index) == ',') {
					index++;
				} else {
					break;
				}
			} else {
				break;
			}
		}
		return result;
	}

	private static int endOfNumber(String line, int index) {
		int length = line.length();
		while (index < length && Character.isDigit(line.charAt(index))) {
			index++;
		}
		return index;
	}

}
