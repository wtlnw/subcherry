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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import de.haumacher.common.config.ObjectParser;
import de.haumacher.common.config.Value;
import de.haumacher.common.config.annotate.ValueParser;

/**
 * @version $Revision$ $Author$ $Date$
 */
public interface Configuration extends MergeConfig, CommitConfig, BranchConfig {
	
	long getStartRevision();

	void setStartRevision(long value);
	
	long getEndRevision();

	void setEndRevision(long value);

	/**
	 * Revision at which the source branch is found for listing relevant changes.
	 */
	long getPegRevision();

	void setPegRevision(long value);

	String[] getModules();

	void setModules(String[] value);

	@ValueParser(BranchParser.class)
	String getSourceBranch();

	void setSourceBranch(String value);

	@ValueParser(BranchParser.class)
	String getTargetBranch();

	void setTargetBranch(String value);

	boolean getNoCommit();

	void setNoCommit(boolean value);
	
	boolean getRebase();

	void setRebase(boolean value);

	/**
	 * Reorder commits to join "Follow-up" commits to their leading commit.
	 */
	boolean getReorderCommits();

	void setReorderCommits(boolean value);

	String getTracURL();

	void setTracURL(String value);
	
	File getPatchDir();

	void setPatchDir(File value);

	Long[] getIgnoreRevisions();

	void setIgnoreRevisions(Long[] value);

	String[] getIgnoreTickets();

	void setIgnoreTickets(String[] value);

	String getTargetMilestone();

	void setTargetMilestone(String value);

	String[] getMilestones();

	void setMilestones(String[] value);

	List<Long> getStopOnRevisions();

	void setStopOnRevisions(List<Long> value);

	String[] getAdditionalTickets();

	void setAdditionalTickets(String[] value);
	
	@ValueParser(TicketMappingFormat.class)
	Map<String, String> getTicketMapping();

	class TicketMappingFormat extends ObjectParser<Map<String, String>> {

		@Override
		public Map<String, String> init() {
			return new HashMap<String, String>();
		}

		@Override
		public Map<String, String> parse(String text) {
			HashMap<String, String> result = new HashMap<>();
			for (String pair : text.trim().split("\\s*,\\s*")) {
				String[] entry = pair.split("\\s*=\\s*");
				result.put(unwrap(entry[0]), unwrap(entry[1]));
			}
			return result;
		}

		private String unwrap(String ref) {
			if (ref.startsWith("#")) {
				return ref.substring(1);
			}
			return ref;
		}

		@Override
		public String unparse(Map<String, String> value) {
			StringBuilder result = new StringBuilder();
			for (Entry<String, String> entry : value.entrySet()) {
				if (result.length() > 0) {
					result.append(',');
				}
				result.append('#');
				result.append(entry.getKey());
				result.append('=');
				result.append('#');
				result.append(entry.getValue());
			}
			return result.toString();
		}

	}

	String getTicketQuery();

	void setTicketQuery(String value);

	boolean getAutoCommit();

	void setAutoCommit(boolean value);

	boolean getPreview();

	void setPreview(boolean value);
	
	boolean getReintegrate();

	void setReintegrate(boolean value);

	String getPortMessage();

	void setPortMessage(String value);

	boolean getDetectCommonModules();

	void setDetectCommonModules(boolean value);

	public class BranchParser extends ObjectParser<String> {

		@Override
		public String parse(String text) {
			if (text == null) {
				return "/";
			}
			if (text.startsWith("/")) {
				return text;
			}
			return "/" + text;
		}

		@Override
		public String unparse(String value) {
			return value;
		}

	}

	DependencyReport getDependencyReport();

	interface DependencyReport extends Value {

		@ValueParser(PatternParser.class)
		Pattern getExcludeTicketMilestone();

		void setExcludeTicketMilestone(Pattern value);

		@ValueParser(PatternParser.class)
		Pattern getExcludePath();

		void setExcludePath(Pattern value);

	}

	/**
	 * Whether processing does not stop on conflicts.
	 * 
	 * <p>
	 * Requires {@link #getNoCommit()} also be set to <code>true</code>.
	 * </p>
	 */
	boolean getAutoSkipConflicts();

	void setAutoSkipConflicts(boolean value);

	/**
	 * Whether ticket dependency analysis should be skipped.
	 */
	boolean getSkipDependencies();

	void setSkipDependencies(boolean value);

	/**
	 * Whether a merged change that has itself has been merged from somewhere else, is committed as
	 * if it was directly merged from its original source.
	 */
	boolean getSilentRebase();

	void setSilentRebase(boolean value);

	/**
	 * Whether all changes from the {@link #getSourceBranch()} should be merged ignoring ticket
	 * information.
	 */
	boolean getAllChanges();

	/**
	 * Whether SVN should not ensure the uniqueness of file time stamps in update operations.
	 */
	boolean getSkipWaitForTimestamp();

	/**
	 * Name of the repository binding provider.
	 */
	String getRepositoryProvider();

	/**
	 * A regular expression that excludes the revision if it matches the commit message.
	 */
	@ValueParser(PatternParser.class)
	Pattern getExcludeMessagePattern();

}
