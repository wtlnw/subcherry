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
package com.subcherry.repository.impl;

import com.subcherry.repository.command.OperationFactory;
import com.subcherry.repository.command.copy.Copy;
import com.subcherry.repository.command.merge.Merge;
import com.subcherry.repository.command.merge.ScheduledTreeConflict;
import com.subcherry.repository.command.wc.LocalDelete;
import com.subcherry.repository.command.wc.LocalMkDir;

public abstract class DefaultOperationFactory implements OperationFactory {

	@Override
	public Copy createCopy() {
		return new DefaultCopy(this);
	}

	@Override
	public Merge createMerge() {
		return new DefaultMerge(this);
	}

	@Override
	public LocalDelete createLocalFileDelete() {
		return new DefaultLocalDelete(this);
	}

	@Override
	public ScheduledTreeConflict newScheduledTreeConflict() {
		return new DefaultScheduledTreeConflict(this);
	}

	@Override
	public LocalMkDir newLocalMkDir() {
		return new DefaultLocalMkDir(this);
	}

}
