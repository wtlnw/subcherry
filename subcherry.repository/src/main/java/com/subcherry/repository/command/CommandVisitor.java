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

import com.subcherry.repository.command.copy.Copy;
import com.subcherry.repository.command.merge.Merge;
import com.subcherry.repository.command.merge.ScheduledTreeConflict;
import com.subcherry.repository.command.wc.LocalDelete;
import com.subcherry.repository.command.wc.LocalMkDir;


public interface CommandVisitor<R, A> {

	R visitCopy(Copy command, A arg);

	R visitMerge(Merge command, A arg);

	R visitLocalDelete(LocalDelete command, A arg);

	R visitLocalMkDir(LocalMkDir command, A arg);

	R visitScheduledTreeConflict(ScheduledTreeConflict command, A arg);
}
