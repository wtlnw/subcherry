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
package com.subcherry.repository.command.merge;

import com.subcherry.repository.command.TargetDepthCommand;
import com.subcherry.repository.command.diff.DiffOptions;
import com.subcherry.repository.core.RevisionRange;
import com.subcherry.repository.core.Target;

public interface Merge extends TargetDepthCommand {

	void setRecordOnly(boolean recordOnly);

	void setAllowMixedRevisions(boolean allowMixedRevisions);

	RevisionRange getRevisionRange();

	void addRevisionRange(RevisionRange range);

	void setIgnoreAncestry(boolean ignoreAnchestry);

	void setMergeOptions(DiffOptions mergeOptions);

	void setSource(Target mergeSource, boolean reintegrate);

	boolean getRecordOnly();

	boolean getAllowMixedRevisions();

	boolean getIgnoreAnchestry();

	DiffOptions getMergeOptions();

	Target getSource();

	boolean getReintegrate();

	void setReintegrate(boolean reintegrate);

	void setSource(Target source);

}
