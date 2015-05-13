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
import com.subcherry.repository.command.TargetDepthCommand;
import com.subcherry.repository.core.Depth;

public abstract class DefaultTargetDepthCommand extends DefaultTargetCommand implements TargetDepthCommand {

	private Depth _depth = Depth.INFINITY;

	public DefaultTargetDepthCommand(OperationFactory factory) {
		super(factory);
	}

	@Override
	public Depth getDepth() {
		return _depth;
	}

	@Override
	public void setDepth(Depth depth) {
		_depth = depth;
	}

}
