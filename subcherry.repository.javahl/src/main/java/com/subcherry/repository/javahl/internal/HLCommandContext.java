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
package com.subcherry.repository.javahl.internal;

import static com.subcherry.repository.javahl.internal.Conversions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.subversion.javahl.ConflictDescriptor;

import com.subcherry.repository.command.merge.CommandContext;
import com.subcherry.repository.command.merge.ConflictDescription;

class HLCommandContext implements CommandContext, HLConflictListener {

	private Map<File, List<ConflictDescription>> _conflicts = new HashMap<File, List<ConflictDescription>>();

	@Override
	public Map<File, List<ConflictDescription>> getConflicts() {
		return _conflicts;
	}

	@Override
	public void conflictDetected(ConflictDescriptor descriptor) {
		addConflict(wrapFile(descriptor.getPath()), wrap(descriptor));
	}

	private void addConflict(File file, ConflictDescription description) {
		List<ConflictDescription> descriptions = _conflicts.get(file);
		if (descriptions == null) {
			descriptions = new ArrayList<ConflictDescription>();
			_conflicts.put(file, descriptions);
		}
		descriptions.add(description);
	}
	
}