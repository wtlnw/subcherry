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
package com.subcherry.ticketcheck;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lustin.trac.xmlprc.Ticket;

import com.subcherry.Configuration;
import com.subcherry.LoginCredentialsValue;
import com.subcherry.repository.LoginCredential;
import com.subcherry.trac.TracConnection;
import com.subcherry.trac.TracTicket;
import com.subcherry.utils.Utils;

import de.haumacher.common.config.PropertiesUtil;
import de.haumacher.common.config.Value;

/**
 * Tool to check a set of tickets to include all their dependencies (dependson
 * and followup).
 * 
 * @version $Revision$ $Author$ $Date$
 */
public class TicketConsistency {

	private static final Logger LOG = Logger.getLogger(TicketConsistency.class.getName());
	
	public interface Options extends Value {

		String getTicketQuery();
		String getIgnoredQuery();
		String getIgnoredMilestonePattern();
		
	}
	private static TracConnection tracConnection;
	private static Options options;
	
	public static void main(String[] args) throws IOException {
		LoginCredential tracCredentials = PropertiesUtil.load("conf/loginCredentials.properties", "trac.",
			LoginCredentialsValue.class);
		Configuration config = PropertiesUtil.load("conf/configuration.properties", Configuration.class);
		tracConnection = 
				new TracConnection(config.getTracURL(), tracCredentials.getUser(), tracCredentials.getPasswd());
		
		options = PropertiesUtil.load("conf/ticketConsistency.properties", Options.class);

		Ticket ticketQuery = tracConnection.getTicketAccessor();
		Vector<Integer> tickets = ticketQuery.query(options.getTicketQuery());
		Collections.sort(tickets);
		
		Set<Integer> ignored;
		if (Utils.isNullOrEmpty(options.getIgnoredQuery())) {
			ignored = Collections.emptySet();
		} else {
			ignored = new HashSet<Integer>(ticketQuery.query(options.getIgnoredQuery()));
		}
		
		Set<Integer> dependsOn = new HashSet<Integer>();
		Set<Integer> followup = new HashSet<Integer>();
		
		Set<Integer> allTickets = new HashSet<Integer>(tickets);
		for (Integer id : tickets) {
			TracTicket ticket = tracConnection.getTicket(id);
			Set<Integer> localDependson = parseIds(ticket.getDependsOn());
			Set<Integer> localFollowup = parseIds(ticket.getFollowUp());

			localDependson.removeAll(allTickets);
			localDependson.removeAll(ignored);
			localFollowup.removeAll(allTickets);
			localFollowup.removeAll(ignored);

			dropIgnored(localDependson);
			dropIgnored(localFollowup);

			if (!localDependson.isEmpty() || !localFollowup.isEmpty()) {
				System.out.println(" * #" + id + ": " + ticket.getSummary());
				
				printDependencies(id, ticket, localDependson, "depends on");
				printDependencies(id, ticket, localFollowup, "causes");
			}
			
			dependsOn.addAll(localDependson);
			followup.addAll(localFollowup);
		}
	}

	private static void printDependencies(Integer id, TracTicket ticket, Set<Integer> dependencies, String dependencyType) {
		if (!dependencies.isEmpty()) {
			System.out.println("    * " + dependencyType);
			for (Integer dependencyId : dependencies) {
				TracTicket dependency = tracConnection.getTicket(dependencyId);
				System.out.println("       * #"+ dependencyId + ": " + dependency.getSummary() + " (" + dependency.getStatus() + " " + dependency.getType() + ")");
			}
		}
	}

	private static String toIDs(Collection<Integer> ids) {
		StringBuilder buffer = new StringBuilder();
		for (Integer id : ids) {
			if (buffer.length() > 0) {
				buffer.append(", ");
			}
			buffer.append("#");
			buffer.append(id);
		}
		return buffer.toString();
	}

	private static void dropIgnored(Set<Integer> ids) {
		for (Iterator<Integer> it = ids.iterator(); it.hasNext(); ) {
			int id = it.next();
			
			TracTicket ticket = tracConnection.getTicket(id);
			String milestone = ticket.getMilestone();
			
			if (Pattern.matches(options.getIgnoredMilestonePattern(), milestone)) {
				it.remove();
			}
		}
	}

	private static final Pattern TICKET_IDS_PATTERN = Pattern.compile("\\s*#(\\d+)(?:,\\s*)?");
	private static Set<Integer> parseIds(Object attributeValue) {
		HashSet<Integer> result = new HashSet<Integer>();
		
		String ticketString = (String) attributeValue;
		
		if (ticketString == null || ticketString.trim().length() == 0) {
			return result;
		}
		
		int length = ticketString.length();
		Matcher matcher = TICKET_IDS_PATTERN.matcher(ticketString);
		while (matcher.lookingAt()) {
			result.add(Integer.parseInt(matcher.group(1)));
			
			matcher.region(matcher.end(), length);
		}
		
		if (matcher.regionEnd() < length) {
			LOG.warning("Could not parse ticket IDs: " + ticketString);
		}
		
		return result;
	}
	
}

