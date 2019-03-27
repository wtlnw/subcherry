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
package com.subcherry.ui.views;

/**
 * This enumeration defines all possible states for a {@link SubcherryMergeEntry}.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public enum SubcherryMergeState {

	NEW(true),
	MERGED(true),
	CONFLICT(true),
	ERROR(true),
	COMMITTED(false),
	SKIPPED(false),
	NO_COMMIT(false);
	
	/**
	 * @see #isPending()
	 */
	private final boolean _pending;

	/**
	 * Create a {@link SubcherryMergeState}.
	 * 
	 * @param pending see {@link #isPending()}
	 */
	private SubcherryMergeState(final boolean pending) {
		_pending = pending;
	}
	
	/**
	 * @return {@code true} if a {@link SubcherryMergeEntry} needs further
	 *         processing, {@code false} indicates that it has been successfully
	 *         merged and committed, skipped by the user or ignored since it has
	 *         already been merged.
	 */
	public boolean isPending() {
		return _pending;
	}
	
	/**
	 * @return {@code true} if a {@link SubcherryMergeEntry} is currently being
	 *         merged, {@code false} is returned for new, completely processed or
	 *         skipped entries.
	 */
	public boolean isWorking() {
		return this != NEW && isPending();
	}
}
