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
package com.subcherry.repository.merge.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import com.subcherry.repository.command.diff.DiffOptions;
import com.subcherry.repository.command.status.StatusType;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.repository.merge.FileMerge;
import com.subcherry.repository.merge.properties.PropertiesEditor.Property;

/**
 * {@link FileMerge} for {@link Properties} files.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 * @version $Revision$ $Author$ $Date$
 */
public class PropertiesMerge implements FileMerge {

	@Override
	public StatusType merge(File baseFile, File localFile, File latestFile, DiffOptions options, File resultFile)
			throws RepositoryException {
		StatusType status = StatusType.MERGED;

		try {
			PropertiesEditor baseProperties = loadProperties(baseFile);
			PropertiesEditor latestProperties = loadProperties(latestFile);
			PropertiesEditor localProperties = loadProperties(localFile);

			PropertiesEditor resultProperties = localProperties.copy();

			Map<String, String> localValues = localProperties.asMap();
			Map<String, String> latestValues = latestProperties.asMap();
			Map<String, String> baseValues = baseProperties.asMap();

			Map<String, String> added = substract(latestValues, baseValues);
			Map<String, String> deleted = substract(baseValues, latestValues);
			Map<String, String> unmodified = intersectWithEqualValue(latestValues, baseValues);
			Map<String, String> modified =
				substract(substract(substract(latestValues, added), deleted), unmodified);

			Map<String, String> addedExisting = intersect(added, localValues);
			Map<String, String> addedWithEqualValue = intersectWithEqualValue(addedExisting, localValues);
			Map<String, String> addConflicts = substract(addedExisting, addedWithEqualValue);
			Map<String, String> toAdd = substract(added, addedExisting);

			if (!addConflicts.isEmpty()) {
				status = StatusType.CONFLICTED;
			}

			Map<String, String> identicalModifications = intersectWithEqualValue(modified, localValues);
			Map<String, String> changes = substract(modified, identicalModifications);
			Map<String, String> locallyUnmodified = intersectWithEqualValue(baseValues, localValues);
			Map<String, String> changesConflicting = substract(changes, locallyUnmodified);
			Map<String, String> toChange = substract(changes, changesConflicting);

			if (!changesConflicting.isEmpty()) {
				status = StatusType.CONFLICTED;
			}

			// Execute deletes.
			for (String key : deleted.keySet()) {
				Property property = resultProperties.getProperty(key);
				if (property != null) {
					resultProperties.removeProperty(property);
				}
			}

			// Execute adds.
			copyInto(resultProperties, latestProperties, toAdd.keySet());
			copyInto(resultProperties, latestProperties, addConflicts.keySet());

			// Execute modifications.
			copyValues(resultProperties, latestProperties, toChange.keySet());
			copyInto(resultProperties, latestProperties, changesConflicting.keySet());

			storeProperties(resultProperties, resultFile);
		} catch (IOException ex) {
			throw new IOError(ex);
		}

		return status;
	}

	private void copyValues(PropertiesEditor result, PropertiesEditor from, Set<String> keys) {
		for (String key : keys) {
			Property property = result.getProperty(key);
			property.setValue(from.getProperty(key).getValue());
		}
	}

	private void copyInto(PropertiesEditor result, PropertiesEditor from, Set<String> keys) {
		for (String key : keys) {
			Property property = from.getProperty(key);

			// Note: In case of a conflict (if the same property is already given in the result) the
			// new property must be added below that property to actually become visible in the
			// result. Therefore, one must search the property after which to insert the new one and
			// start the search with the newly inserted key (not the predecessor).
			Property belowInResult = null;
			boolean atTop = true;
			for (Property insertedBelow = property; insertedBelow != null; insertedBelow = insertedBelow.getPrev()) {
				String afterKey = insertedBelow.getKey();
				if (afterKey == null) {
					continue;
				}

				if (insertedBelow != property) {
					atTop = false;
				}

				belowInResult = result.getProperty(afterKey);
				if (belowInResult != null) {
					break;
				}
			}

			boolean clash;
			Property before;
			if (belowInResult == null) {
				clash = false;
				if (atTop) {
					before = result.getFirst();
				} else {
					before = null;
				}
			}
			else {
				clash = belowInResult.getKey().equals(key);
				before = belowInResult.getNext();
			}
			Property copy = clash ? property.copySource() : property.copy();
			result.addProperty(copy, before);
		}
	}

	private <K, V> Map<K, V> intersectWithEqualValue(Map<K, V> base, Map<K, V> remove) {
		Map<K, V> result = new HashMap<K, V>();
		for (Iterator<Entry<K, V>> it = base.entrySet().iterator(); it.hasNext();) {
			Entry<K, V> entry = it.next();
			K key = entry.getKey();

			V baseValue = entry.getValue();
			V removeValue = remove.get(key);
			if (baseValue.equals(removeValue)) {
				// Not a conflict at all.
				result.put(key, baseValue);
			}
		}
		return result;
	}

	private <K, V> Map<K, V> intersect(Map<K, V> values1,
			Map<K, V> values2) {
		HashMap<K, V> result = new HashMap<K, V>(values1);
		result.keySet().retainAll(values2.keySet());
		return result;
	}

	private <K, V> Map<K, V> substract(Map<K, V> base, Map<K, V> substract) {
		HashMap<K, V> result = new LinkedHashMap<K, V>(base);
		result.keySet().removeAll(substract.keySet());
		return result;
	}

	private <K, V> PropertiesEditor loadProperties(File baseFile) throws IOException, RepositoryException {
		PropertiesEditor result = new PropertiesEditor();
		InputStream in = new FileInputStream(baseFile);
		try {
			result.load(in);
		} finally {
			in.close();
		}
		return result;
	}

	private void storeProperties(PropertiesEditor properties, File file) throws RepositoryException, IOException {
		OutputStream out = new FileOutputStream(file);
		try {
			properties.store(out);
		} finally {
			out.close();
		}
	}

}
