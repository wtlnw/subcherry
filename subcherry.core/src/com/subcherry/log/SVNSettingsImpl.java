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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Properties;

public class SVNSettingsImpl implements SVNSettings {

	public static final String SVN_PASSWORD_PROPERTY = "svn.user.password";
	public static final String SVN_URL_PROPERTY = "svn.url";
	public static final String SVN_USER_PROPERTY = "svn.user.name";
	
	private String url;
	private String user;
	private String password;
	
	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public String getUser() {
		return user;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public void setUser(String user) {
		this.user = user;
	}

	@Override
	public void setPassword(String password) {
		this.password = password;
	}

	public static SVNSettings loadSettings(BufferedReader in, Properties globalProperties) throws IOException {
		SVNSettingsImpl result = new SVNSettingsImpl();
		
		result.setUrl(TicketsWithCommits.inputOnce(in, "SVN URL", globalProperties.getProperty(SVNSettingsImpl.SVN_URL_PROPERTY)));
		result.setUser(TicketsWithCommits.inputOnce(in, "SVN user", globalProperties.getProperty(SVNSettingsImpl.SVN_USER_PROPERTY)));
		result.setPassword(TicketsWithCommits.inputOnce(in, "SVN password (will be echoed)", globalProperties.getProperty(SVNSettingsImpl.SVN_PASSWORD_PROPERTY)));
		
		return result;
	}
	
	public static void saveSettings(SVNSettings svn, Properties updatedProperties) {
		updatedProperties.setProperty(SVN_USER_PROPERTY, svn.getUser());
		updatedProperties.setProperty(SVN_PASSWORD_PROPERTY, svn.getPassword());
		updatedProperties.setProperty(SVN_URL_PROPERTY, svn.getUrl());
	}
	
}
