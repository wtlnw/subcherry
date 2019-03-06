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
package com.subcherry;

import java.util.Map;
import java.util.regex.Pattern;

import de.haumacher.common.config.annotate.ValueParser;

public interface MergeConfig extends WorkspaceConfig, RepositoryConfig {

	@ValueParser(AdditionalRevision.Parser.class)
	Map<Long, AdditionalRevision> getAdditionalRevisions();

	void setAdditionalRevisions(Map<Long, AdditionalRevision> value);

	/**
	 * Whether intra-branch moves (and copies) should be merged semantically (as intra-branch move
	 * in the target branch an applying the changes that happened together with the move).
	 */
	boolean getSemanticMoves();

	void setSemanticMoves(boolean value);

	boolean getRevert();

	void setRevert(boolean value);

	@ValueParser(PatternParser.class)
	Pattern getIncludePaths();

	@ValueParser(PatternParser.class)
	Pattern getExcludePaths();

	/**
	 * Name of the property file containing resource mappings.
	 * 
	 * <p>
	 * The mapping is applied to resources in the source branch when deciding about a merge target
	 * resource. If a source resource is contained as key in the mapping, the merge is applied to
	 * the resource given as value.
	 * </p>
	 */
	String getResourceMapping();
	
	/**
	 * @see #getResourceMapping()
	 */
	void setResourceMapping(String value);

	/**
	 * Whether to skip revisions that are already on the target branch by inspecting SVN merge info.
	 */
	boolean getIgnoreMergeInfo();

	/**
	 * Setter for {@link #getIgnoreMergeInfo()}.
	 * 
	 * @param value
	 *        see {@link #getIgnoreMergeInfo()}
	 */
	void setIgnoreMergeInfo(boolean value);
}
