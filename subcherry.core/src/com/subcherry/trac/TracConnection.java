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
package com.subcherry.trac;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.lustin.trac.xmlprc.Ticket;
import org.lustin.trac.xmlprc.TrackerDynamicProxy;

import com.subcherry.utils.Utils;

/**
 * Instances of this class provide access to the trac server.
 * 
 * <p>
 * Tickets requested using the {@link TracConnection#getTicket(Integer)} or
 * {@link TracConnection#getTicket(String)} API are cached to speed up consequent resolving attempts
 * for the same ticket.
 * </p>
 */
public class TracConnection {

	/**
	 * The maximum number of consequent retries upon RPC failure.
	 */
	private static final int FETCH_TICKET_RETRY = 20;

	/**
	 * The {@link TrackerDynamicProxy} instance to be used for providing access instances to the
	 * trac server.
	 */
	private final TrackerDynamicProxy _proxy;

	/**
	 * A {@link Map} of {@link TracTicket}s by their {@link TracTicket#getNumber()}.
	 */
	private final Map<Integer, TracTicket> _tickets = new HashMap<Integer, TracTicket>();

	/**
	 * @see #getTicketAccessor()
	 */
	private Ticket _ticketAccessor;

	/**
	 * Creates a new {@link TracConnection}.
	 * 
	 * @param url
	 *        the URL to connect to
	 * @param login
	 *        the user's login
	 * @param password
	 *        the user's password
	 * @throws MalformedURLException
	 *         if the given {@code url} is invalid
	 */
	public TracConnection(final String url, final String login, final String password) throws MalformedURLException {
		assert url != null;
		assert login != null;
		assert password != null;

		final XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(new URL(url));
		config.setBasicUserName(login);
		config.setBasicPassword(password);

		final XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);

		_proxy = new TrackerDynamicProxy(client);
	}

	/**
	 * @param message
	 *        the commit message to resolve the referenced {@link TracTicket} from
	 * @return the {@link TracTicket} referenced by the given commit message or {@code null} if none
	 *         could be resolved
	 */
	public TracTicket getTicket(final String message) {
		final String number = Utils.getTicketId(message);
		if (number != null) {
			return getTicket(Integer.parseInt(number));
		} else {
			return null;
		}
	}

	/**
	 * @param number
	 *        the number of the {@link TracTicket} to resolve
	 * @return the {@link TracTicket} with the given {@link TracTicket#getNumber()} or {@code null}
	 *         if none could be resolved
	 */
	public TracTicket getTicket(final Integer number) {
		return _tickets.computeIfAbsent(number, this::fetchTicket);
	}

	/**
	 * @param number
	 *        the number of the {@link TracTicket} to fetch from the server
	 * @return the {@link TracTicket} with the given {@link TracTicket#getNumber()} or {@code null}
	 *         if it does not exist
	 */
	private TracTicket fetchTicket(final Integer number) {
		final Ticket ticket = getTicketAccessor();
		int i = 0;
		while (i < FETCH_TICKET_RETRY) {
			try {
				final Vector<?> data = ticket.get(number);
				if (data == null) {
					return null;
				}
				return new TracTicket(
					(Integer) data.get(0),
					(Date) data.get(1),
					(Date) data.get(2),
					(Map<?, ?>) data.get(3));
			} catch (RuntimeException ex) {
				// Under unclear circumstances, sometimes contacting XmlRPC fails. Just try again.
				i++;
				if (i < FETCH_TICKET_RETRY) {
					System.err.println("Unable to fetch Ticket for number " + number + ". Retry " + (FETCH_TICKET_RETRY - i) + " times.");
					try {
						Thread.sleep(2000);
					} catch (InterruptedException ex1) {
						// ignore
					}
				} else {
					throw ex;
				}
			}
		}

		throw new RuntimeException("Unable to fetch Ticket " + number);
	}

	/**
	 * @return the {@link Ticket} instance to be used for accessing the trac server
	 */
	public Ticket getTicketAccessor() {
		if (_ticketAccessor == null) {
			_ticketAccessor = (Ticket) _proxy.newInstance(Ticket.class);
		}

		return _ticketAccessor;
	}
}
