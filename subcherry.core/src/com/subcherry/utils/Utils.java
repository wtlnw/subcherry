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
package com.subcherry.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.subcherry.Configuration;
import com.subcherry.commit.MessageRewriter;

/**
 * @version   $Revision$  $Author$  $Date$
 */
public class Utils {

	/**
	 * {@link Exception} that is thrown when the commit message has illegal format.
	 */
	public static class IllegalMessageFormat extends Exception {

		/**
		 * Creates a new {@link IllegalMessageFormat}.
		 * 
		 * @see Exception#Exception(String, Throwable)
		 */
		public IllegalMessageFormat(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * Creates a new {@link IllegalMessageFormat}.
		 * 
		 * @see Exception#Exception(String)
		 */
		public IllegalMessageFormat(String message) {
			super(message);
		}

	}

	public static class TicketMessage {
		private static final MessageRewriter NO_REWRITE = null;

		public Matcher matcher;
		public String ticketNumber;
		public String apiChange;
		public String originalMessage;

		private final MessageRewriter _messageRewriter;

		private final long _originalRevision;

		private final String _commitMessage;

		private final long _leadRevision;

		private boolean _hasMatch;
		
		public TicketMessage(String commitMessage) throws IllegalMessageFormat {
			this(0, commitMessage, NO_REWRITE);
		}

		public TicketMessage(long originalRevision, String commitMessage, MessageRewriter messageRewriter) {
			_originalRevision = originalRevision;
			_commitMessage = commitMessage;
			_messageRewriter = messageRewriter;

			matcher = TICKET_PATTERN.matcher(commitMessage);
			_hasMatch = matcher.matches();
			if (!_hasMatch) {
				ticketNumber = "0";
				apiChange = null;
				originalMessage = commitMessage;
				_leadRevision = 0L;
			} else {
				ticketNumber = getTicketNumber(matcher);
				apiChange = getApiChange(matcher);
				originalMessage = getOriginalMessage(matcher);
				_leadRevision = extractLeadRevision(matcher);
			}
		}

		public String getSourceBranch() {
			if (!_hasMatch) {
				return null;
			}
			return matcher.group(PORTED_FROM_GROUP);
		}

		public String getDestinationBranch() {
			if (!_hasMatch) {
				return null;
			}
			String toBranch = matcher.group(PORTED_TO_GROUP);
			if (toBranch != null) {
				return toBranch;
			}
			String onBranch = matcher.group(ON_BRANCH_GROUP);
			if (onBranch != null) {
				return onBranch;
			}
			String previewBranch = matcher.group(PREVIEW_BRANCH_GROUP);
			if (previewBranch != null) {
				return previewBranch;
			}
			String hotfixBranch = matcher.group(HOTFIX_BRANCH_GROUP);
			if (hotfixBranch != null) {
				return hotfixBranch;
			}
			return null;
		}

		public String getMergedRevision() {
			if (!_hasMatch) {
				return null;
			}
			return matcher.group(MERGED_REVISION_GROUP);
		}
		
		public boolean isHotfix() {
			if (!_hasMatch) {
				return false;
			}
			return matcher.group(Utils.IS_HOTFIX_GROUP) != null;
		}
		
		public boolean isPreview() {
			if (!_hasMatch) {
				return false;
			}
			return matcher.group(Utils.PREVIEW_BRANCH_GROUP) != null;
		}
		
		public boolean isBranchChange() {
			if (!_hasMatch) {
				return false;
			}
			return matcher.group(Utils.ON_BRANCH_GROUP) != null;
		}

		public boolean isPort() {
			if (!_hasMatch) {
				return false;
			}
			return matcher.group(Utils.PORTED_FROM_GROUP) != null;
		}

		public String getLogEntryMessage() {
			return _commitMessage;
		}

		public String getMergeMessage() {
			return _messageRewriter.getMergeMessage(this);
		}

		public long getOriginalRevision() {
			return _originalRevision;
		}

		public long getLeadRevision() {
			return _leadRevision;
		}
	}

	private static int group = 1;
	
	private static int ORIG_MESSAGE_GROUP;
	private static int HOTFIX_BRANCH_GROUP;
	private static int IS_HOTFIX_GROUP;
	private static int API_CHANGE_GROUP;

	private static int FOLLOW_UP_GROUP;
	private static int FOLLOW_UP_GROUP_2;
	private static int MERGED_REVISION_GROUP;
	private static int TICKET_NUMBER_GROUP;
	private static int PREVIEW_BRANCH_GROUP;

	private static int ON_BRANCH_GROUP;
	private static int PORTED_FROM_GROUP;
	private static int PORTED_TO_GROUP;
	
	private static String group(int id, String regexp) {
		assert id != 0;
		group++;
		return "(" + regexp + ")";
	}
	private static String or(String ...regexps) {
		StringBuilder buffer = new StringBuilder();
		boolean first = true;
		for (String regexp : regexps) {
			if (first) {
				first = false;
			} else {
				buffer.append("|");
			}
			buffer.append(expr(regexp));
		}
		return expr(buffer.toString());
	}
	private static String expr(String regexp) {
		return "(?:" + regexp + ")";
	}
	public static Pattern TICKET_PATTERN = Pattern.compile(commitMessage(), Pattern.DOTALL);

	public static char SVN_SERVER_PATH_SEPARATOR = '/';
	
	public static  BufferedReader SYSTEM_IN = new BufferedReader(new InputStreamReader(System.in));

	private static String commitMessage() {
		return "^" +
			ticketPrefix() +
			"(?:" + fromPrefix() + ")?" +
			"(?:" + apiChangePrefix() + ")?" +
			"(?:" + followUpRevisionPrefix() + ")?" +
			"(?:" + mergeRevisionPrefix() + ")?" +
			"(?:" + followUpRevisionPrefix2() + ")?" +
			group(ORIG_MESSAGE_GROUP = group, originalMessage()) + "$";
	}
	private static String ticketPrefix() {
		return "Ticket #" + group(TICKET_NUMBER_GROUP = group, "\\d+") + ":";
	}
	private static String fromPrefix() {
		return messagePart(or(hotfix(), preview(), ported(), onBranch()));
	}
	private static String hotfix() {
		return group(IS_HOTFIX_GROUP = group, "Hotfix for " + group(HOTFIX_BRANCH_GROUP = group, "[^:]*"));
	}
	private static String preview() {
		return "Preview on " + group(PREVIEW_BRANCH_GROUP = group, "[^:]*");
	}

	private static String onBranch() {
		return "On " + group(ON_BRANCH_GROUP = group, "[^:]*");
	}
	private static String ported() {
		return "Ported to " + group(PORTED_TO_GROUP = group, "[^ ]*") + " from " + group(PORTED_FROM_GROUP = group, "[^:]*");
	}
	private static String apiChangePrefix() {
		return group(API_CHANGE_GROUP = group, messagePart("API change"));
	}

	private static String followUpRevisionPrefix() {
		return messagePart("(?:Follow-up|Bugfix) (?:\\d+ )?for \\[" + group(FOLLOW_UP_GROUP = group, "\\d+") + "\\]");
	}

	private static String followUpRevisionPrefix2() {
		return messagePart("(?:Follow-up|Bugfix) (?:\\d+ )?for \\[" + group(FOLLOW_UP_GROUP_2 = group, "\\d+") + "\\]");
	}

	private static String mergeRevisionPrefix() {
		return messagePart("\\[" + group(MERGED_REVISION_GROUP = group, "\\d+") + "\\]");
	}

	private static String messagePart(String part) {
		return " " + part +  ":";
	}
	private static String originalMessage() {
		return ".*";
	}
	/** 
	 * @param matcher A {@link Matcher} matching {@link #TICKET_PATTERN}
	 */
	public static String getTicketNumber(Matcher matcher) {
		return matcher.group(TICKET_NUMBER_GROUP);
	}
	
	/** 
	 * @param matcher A {@link Matcher} matching {@link #TICKET_PATTERN}
	 */
	public static String getApiChange(Matcher matcher) {
		return matcher.group(API_CHANGE_GROUP);
	}
	
	/** 
	 * @param matcher A {@link Matcher} matching {@link #TICKET_PATTERN}
	 */
	public static String getHotfixBranch(Matcher matcher) {
		return matcher.group(HOTFIX_BRANCH_GROUP);
	}
	
	/** 
	 * @param matcher A {@link Matcher} matching {@link #TICKET_PATTERN}
	 */
	public static String getOriginalMessage(Matcher matcher) {
		return matcher.group(ORIG_MESSAGE_GROUP);
	}
	
	/**
	 * The revision to which the current one is a follow-up.
	 */
	public static long extractLeadRevision(Matcher matcher) {
		String leadRevision = matcher.group(FOLLOW_UP_GROUP);
		if (leadRevision == null) {
			leadRevision = matcher.group(FOLLOW_UP_GROUP_2);
			if (leadRevision == null) {
				return 0;
			}
		}
		return Long.parseLong(leadRevision);
	}

	public static <T> Collection<T> nonNull(Collection<T> list) {
		if (list == null) {
			return Collections.emptyList();
		}
		return list;
	}

	public static <T> List<T> nonNull(List<T> list) {
		if (list == null) {
			return Collections.emptyList();
		}
		return list;
	}

	public static <T> Set<T> nonNull(Set<T> list) {
		if (list == null) {
			return Collections.emptySet();
		}
		return list;
	}

	public static Pattern compileOptional(String pattern) {
		if (pattern == null || pattern.isEmpty()) {
			return null;
		}
		return Pattern.compile(pattern);
	}

	public static String[] toStringArray(Collection<String> list) {
		return list.toArray(new String[list.size()]);
	}

	public static boolean isNullOrEmpty(String s) {
		return s == null || s.isEmpty();
	}

	public static String getTicketId(String message) {
		Matcher matcher = TICKET_PATTERN.matcher(message);
		if (matcher.matches()) {
			return getTicketNumber(matcher);
		} else {
			return null;
		}
	}

	public static String getDetailMessage(String message) {
		Matcher matcher = TICKET_PATTERN.matcher(message);
		if (matcher.matches()) {
			return getOriginalMessage(matcher);
		} else {
			return null;
		}
	}

	public static String toResource(File workspaceRoot, String wcPath) {
		String root = workspaceRoot.getPath();
		if (wcPath.length() > root.length() && wcPath.startsWith(root)
			&& wcPath.charAt(root.length()) == File.separatorChar) {
			wcPath = wcPath.substring(root.length() + 1);
		}
		return wcPath.replace(File.separatorChar, '/');
	}

	/**
	 * Checks whether the merge is a merge to trunk. (By the name pattern given in the
	 * configuration.)
	 */
	public static boolean mergeToTrunk(Configuration config) {
		String targetBranch = config.getTargetBranch();
		if (targetBranch.charAt(targetBranch.length() - 1) != SVN_SERVER_PATH_SEPARATOR) {
			targetBranch += SVN_SERVER_PATH_SEPARATOR;
		}
		return targetBranch.matches(config.getTrunkPattern());
	}

}

