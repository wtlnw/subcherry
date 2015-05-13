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
package com.subcherry.repository.javahl.internal;

import org.apache.subversion.javahl.callback.UserPasswordCallback;

import com.subcherry.repository.LoginCredential;

public class ProvidePassword implements UserPasswordCallback {

	private LoginCredential _credentials;

	public ProvidePassword(LoginCredential credentials) {
		_credentials = credentials;
	}

	@Override
	public int askTrustSSLServer(String info, boolean allowPermanently) {
		return AcceptTemporary;
	}

	@Override
	public boolean prompt(String realm, String username) {
		return true;
	}

	@Override
	public boolean askYesNo(String realm, String question,
			boolean yesIsDefault) {
		return true;
	}

	@Override
	public String askQuestion(String realm, String question,
			boolean showAnswer) {
		return null;
	}

	@Override
	public String getUsername() {
		return _credentials.getUser();
	}

	@Override
	public String getPassword() {
		return _credentials.getPasswd();
	}

	@Override
	public boolean prompt(String realm, String username, boolean maySave) {
		return true;
	}

	@Override
	public String askQuestion(String realm, String question,
			boolean showAnswer, boolean maySave) {
		return null;
	}

	@Override
	public boolean userAllowedSave() {
		return false;
	}

}