/*
 * SubCherry - Cherry Picking with Trac and Subversion
 * Copyright (C) 2018 Bernhard Haumacher and others
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
package com.subcherry.ui.model;

/**
 * Implementing classes provide functionality for checking/unchecking instances.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public interface SubcherryTreeNode {
	
	/**
	 * This enumeration defines all available states for {@link SubcherryTreeNode}s.
	 * 
	 * @author wta
	 */
	enum Check {
		CHECKED,
		GRAYED,
		UNCHECKED
	}
	
	/**
	 * @return the {@link Check} state of this node
	 */
	Check getState();

	/**
	 * Setter for {@link #getState()}.
	 * 
	 * @param state
	 *            see {@link #getState()}
	 */
	void setState(Check state);
}