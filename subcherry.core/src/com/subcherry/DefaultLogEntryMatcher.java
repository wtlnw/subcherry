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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.subcherry.repository.core.LogEntry;
import com.subcherry.trac.Ticket;
import com.subcherry.trac.TicketStub;
import com.subcherry.trac.TracConnection;
import com.subcherry.utils.Log;

/**
 * @version   $Revision$  $Author$  $Date$
 */
public class DefaultLogEntryMatcher extends SVNLogEntryMatcher {

	private static final Logger LOG = Logger.getLogger(DefaultLogEntryMatcher.class.getName());
	
	private TracConnection _trac;
	
	private Set<Long> _ignoreRevisions;
	private Set<Long> _additionalRevisions;

	private final PortingTickets _portingTickets;

	private Set<String> _ignoreTickets;
	private Collection<String> _milestones;

	public DefaultLogEntryMatcher(TracConnection trac, Configuration config, PortingTickets portingTickets) throws MalformedURLException {
		_trac = trac;
		
		_ignoreRevisions = getIgnoreRevisions(config);
		_additionalRevisions = getAdditionalRevisions(config);
		_portingTickets = portingTickets;
		_ignoreTickets = getIgnoreTickets(config);
		_milestones = getMilestones(config);
	}
	
	public static ArrayList<String> toStringIds(Collection<Integer> ticketIds) {
		ArrayList<String> result = new ArrayList<String>();
		for (Integer id : ticketIds) {
			result.add(Integer.toString(id));
		}
		return result;
	}

	private Set<String> getIgnoreTickets(Configuration config) {
		return toSet(config.getIgnoreTickets());
	}

	private Set<Long> getAdditionalRevisions(Configuration config) {
		return config.getAdditionalRevisions().keySet();
	}

	private Collection<String> getMilestones(Configuration config) {
		Set<String> milestones = toSet(config.getMilestones());
		String targetMilestone = config.getTargetMilestone();
		if (!targetMilestone.isEmpty()) {
			milestones.add(targetMilestone);
		}
		return milestones;
	}

	private static Set<Long> getIgnoreRevisions(Configuration config) {
		return toSet(config.getIgnoreRevisions());
	}

	private static <T> Set<T> toSet(T[] array) {
		Set<T> set = new HashSet<T>();
		Collections.addAll(set, array);
		return set;
	}


	@Override
	public boolean matches(LogEntry logEntry) {
		long revision = logEntry.getRevision();
		if (_ignoreRevisions.contains(revision)) {
			Log.info("Ignore revision " + revision + " as it is configured to be ignored");
			return false;
		}
		
		if (_additionalRevisions.contains(revision)) {
			Log.info("Use " + revision + " as it is configured to be used explicitly.");
			return true;
		}

		String message = logEntry.getMessage();
		Ticket ticket = TicketStub.getTicket(_trac, message);

		boolean accept = portByTicket(ticket);
		if (accept) {
			Log.info("Using [" + revision + "] from #" + ticket.id());
		}
		return accept;
	}

	private boolean portByTicket(Ticket ticket) {
		if (_ignoreTickets.contains(ticket.id())) {
			return false;
		}
		if (_portingTickets.shouldPort(ticket)) {
			return true;
		}
		
		if (!_milestones.isEmpty()) {
			if ("closed".equals(ticket.status())) {
				String milestone = ticket.milestone();
				if (milestone != null && _milestones.contains(milestone)) {
					return true;
				}
			}
		}
		return false;
	}

}

