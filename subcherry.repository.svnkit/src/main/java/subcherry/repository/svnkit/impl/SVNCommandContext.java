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
package subcherry.repository.svnkit.impl;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.subcherry.repository.command.merge.CommandContext;
import com.subcherry.repository.command.merge.ConflictDescription;

public class SVNCommandContext implements CommandContext {

	private Map<File, List<ConflictDescription>> _conflicts = new HashMap<File, List<ConflictDescription>>();

	@Override
	public Map<File, List<ConflictDescription>> getConflicts() {
		return _conflicts;
	}

}
