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

import org.apache.commons.codec.binary.Base64;

import de.haumacher.common.config.ObjectParser;
import de.haumacher.common.config.Value;
import de.haumacher.common.config.annotate.ValueParser;

/**
 * @version   $Revision$  $Author$  $Date$
 */
public interface LoginCredential extends Value {
	
	public static class PasswordDecoder extends ObjectParser<String> {

		@Override
		public String parse(String input) {
			return new String(Base64.decodeBase64(input));
		}
		
		@Override
		public String unparse(String value) {
			return Base64.encodeBase64String(value.getBytes());
		}

	}
	
	String getUser();
	
	@ValueParser(PasswordDecoder.class)
	String getPasswd();

}

