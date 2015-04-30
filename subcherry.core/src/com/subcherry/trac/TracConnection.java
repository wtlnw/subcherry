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

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.lustin.trac.xmlprc.Ticket;
import org.lustin.trac.xmlprc.TrackerDynamicProxy;
import org.lustin.trac.xmlprc.Wiki;

/**
 * @version $Revision$ $Author$ $Date$
 */
public class TracConnection {

	private static String CONF_ENTRY_URL = "URL";

	private static String CONF_ENTRY_NAME = "user";

	private static String CONF_ENTRY_PASSWORD = "password";

	private static TracConnection INSTANCE;

	private String userName;

	private String password;

	private String url;

	private TrackerDynamicProxy proxy;

	/** 
	 * Creates a new {@link TracConnection}.
	 * 
	 * @param aUrl
	 * @param aUserName
	 * @param aPassword
	 * @throws MalformedURLException
	 */
	public TracConnection(String aUrl, String aUserName, String aPassword) throws MalformedURLException {
		super();

		assert aUrl != null;
		assert aUserName != null;
		assert aPassword != null;

		this.userName = aUserName;
		this.password = aPassword;
		this.url = aUrl;

		XmlRpcClientConfigImpl theClientConfig = new XmlRpcClientConfigImpl();
		theClientConfig.setServerURL(new URL(url));

		theClientConfig.setBasicUserName(aUserName);
		theClientConfig.setBasicPassword(aPassword);

		XmlRpcClient theClient = new XmlRpcClient();
		theClient.setConfig(theClientConfig);

		this.proxy = new TrackerDynamicProxy(theClient);

	}

	public Ticket getTicket() {
		return (Ticket) this.proxy.newInstance(Ticket.class);
	}

	public Wiki getWiki() {
		return (Wiki) this.proxy.newInstance(Wiki.class);
	}
	
}
