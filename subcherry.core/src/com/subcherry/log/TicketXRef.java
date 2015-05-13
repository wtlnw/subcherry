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
package com.subcherry.log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.subcherry.repository.core.LogEntry;

public class TicketXRef {
	private Map<Integer, Set<String>> branchesByTicket = new HashMap<Integer, Set<String>>();
	private Map<Integer, List<LogEntry>> commitsByTicket = new HashMap<Integer, List<LogEntry>>();
	private Map<String, List<LogEntry>> invalidCommitsByReason = new HashMap<String, List<LogEntry>>();

	public TicketXRef(Pattern commitMessagePattern, Pattern commitMessageIgnorePattern, Pattern pathPattern, Pattern fileIgnorePattern, Iterable<LogEntry> svnLog) {
		Set<String> branchesBuffer = new HashSet<String>();
		
		for (Iterator<LogEntry> entries = svnLog.iterator(); entries.hasNext();) {
			LogEntry logEntry = entries.next();
			
			boolean isCommitRelevant = false;
			Set<String> changedPaths = logEntry.getChangedPaths().keySet();
			for (String path : changedPaths) {
				Matcher pathMatcher = pathPattern.matcher(path);
				if (pathMatcher.matches()) {
					String file = pathMatcher.group(2);
					if (! fileIgnorePattern.matcher(file).matches()) {
						isCommitRelevant = true;
						
						String branch = pathMatcher.group(1);
						branchesBuffer.add(branch);
					}
				}
			}
			
			if (isCommitRelevant) {
				String message = logEntry.getMessage();
				Matcher messageMatcher = commitMessagePattern.matcher(message);
	
				if (messageMatcher.matches()) {
					Integer ticketId = Integer.parseInt(messageMatcher.group(1));
	
					// Add branches to ticket.
					addAllSet(branchesByTicket, ticketId, branchesBuffer);
	
					// Add commit to ticket.
					addList(commitsByTicket, ticketId, logEntry);
				} else {
					if (! commitMessageIgnorePattern.matcher(message).matches()) {
						addList(invalidCommitsByReason, "No ticket ID in commit message", logEntry);
					}
				}
			}
			
			branchesBuffer.clear();
		}
	}

	public static <K, V> void addSet(Map<K, Set<V>> multiMap, K key, V values) {
		Set<V> branches = multiMap.get(key);
		if (branches == null) {
			branches = new HashSet<V>();
			multiMap.put(key, branches);
		}
		branches.add(values);
	}

	public static <K, V> void addAllSet(Map<K, Set<V>> multiMap, K key, Collection<V> values) {
		Set<V> branches = multiMap.get(key);
		if (branches == null) {
			branches = new HashSet<V>(values);
			multiMap.put(key, branches);
		} else {
			branches.addAll(values);
		}
	}
	
	public static <K, V> List<V> addList(Map<K, List<V>> multiMap, K key, V value) {
		List<V> commits = multiMap.get(key);
		if (commits == null) {
			commits = new ArrayList<V>();
			multiMap.put(key, commits);
		}
		commits.add(value);
		return commits;
	}

	public Set<String> getCommitBranches(Integer ticketId) {
		return branchesByTicket.get(ticketId);
	}

	public List<LogEntry> getCommits(Integer ticketId) {
		return commitsByTicket.get(ticketId);
	}

	public Set<Integer> getAllTicketIds() {
		return commitsByTicket.keySet();
	}
	
	public Map<String, List<LogEntry>> getInvalidCommitsByReason() {
		return invalidCommitsByReason;
	}
}