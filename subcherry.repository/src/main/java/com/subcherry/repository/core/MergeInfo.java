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
package com.subcherry.repository.core;

import java.util.List;
import java.util.Set;

public interface MergeInfo {

	public Set<RepositoryURL> getPaths();

	/**
	 * Get the revision ranges for the specified merge source URL.
	 * 
	 * @param path
	 *        The merge source URL, or <code>null</code>.
	 * @return List of RevisionRange objects, or <code>null</code>.
	 */
	public List<RevisionRange> getRevisions(RepositoryURL path);

}
