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
package com.subcherry.repository.command.merge;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.subcherry.repository.command.Command;
import com.subcherry.repository.core.RepositoryException;

public abstract class CommandExecutor {

	public abstract CommandContext createContext();

	public abstract void execute(CommandContext context, Command command) throws RepositoryException;

	public Map<File, List<ConflictDescription>> execute(Collection<Command> commands) throws RepositoryException {
		CommandContext context = createContext();
		for (Command command : commands) {
			execute(context, command);
		}
		return context.getConflicts();
	}

}
