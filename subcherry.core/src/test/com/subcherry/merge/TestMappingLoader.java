/*
 * SubCherry - Cherry Picking with Trac and Subversion
 * Copyright (C) 2016 Bernhard Haumacher and others
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
package test.com.subcherry.merge;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.subcherry.merge.MappingLoader;
import com.subcherry.merge.ResourceMapping;

import junit.framework.TestCase;

/**
 * Test case for {@link MappingLoader}.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
@SuppressWarnings("javadoc")
public class TestMappingLoader extends TestCase {

	public void testUnescapeHex() throws IOException {
		assertEquals("XY", mapping("B\\u0041\\x41B = X").map("BAABY"));
	}

	public void testUnescapeEquals() throws IOException {
		assertEquals("X==ZY", mapping("B\\=B = X=\\=Z").map("B=BY"));
	}

	public void testUnescapeSpaceKey() throws IOException {
		assertEquals("AX", mapping("\\ B\\  =  A ").map(" B X"));
	}

	public void testUnescapeSpaceValue() throws IOException {
		assertEquals(" A X", mapping("B  = \\ A\\  ").map("BX"));
	}

	public void testValueSingle() throws IOException {
		assertEquals("AX", mapping("B=A").map("BX"));
		assertEquals("AX", mapping("B= A ").map("BX"));
		assertEquals("AX", mapping("B=  A  ").map("BX"));
	}

	public void testValueTwoChar() throws IOException {
		assertEquals("AYX", mapping("B=AY").map("BX"));
		assertEquals("AYX", mapping("B= AY ").map("BX"));
		assertEquals("AYX", mapping("B=  AY  ").map("BX"));
		assertEquals("A YX", mapping("B=  A Y  ").map("BX"));
		assertEquals("A  YX", mapping("B=  A  Y  ").map("BX"));
		assertEquals("A  YX", mapping("B  =  A  Y  ").map("BX"));
		assertEquals(" A  Y X", mapping("B  = \\ A\\  Y\\  ").map("BX"));
	}

	public void testKeyTwoChar() throws IOException {
		assertEquals("AX", mapping("BY=A").map("BYX"));
		assertEquals("AX", mapping("B Y=A").map("B YX"));
		assertEquals("AX", mapping("B  Y =  A ").map("B  YX"));
		assertEquals("AX", mapping("\\ B  Y\\  =  A ").map(" B  Y X"));
	}

	public void testRegexp() throws IOException {
		assertEquals("foo/webapp",
			mapping("([-a-zA-Z0-9._\\\\*]+)/webapps/[-a-zA-Z0-9._\\\\*]+\\\\b = $1/webapp").map("foo/webapps/bar"));
	}

	public void testMultiLine() throws IOException {
		assertEquals("CDX", mapping("A\\\nB = \\\r\nC\\\rD").map("ABX"));
	}

	private ResourceMapping mapping(String definition) throws IOException, UnsupportedEncodingException {
		return MappingLoader.loadMapping(new ByteArrayInputStream(definition.getBytes("ISO-8859-1")));
	}

}
