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
package test.com.subcherry.utils;

import junit.framework.TestCase;

import com.subcherry.utils.Utils;
import com.subcherry.utils.Utils.IllegalMessageFormat;
import com.subcherry.utils.Utils.TicketMessage;

/**
 * Test case for {@link Utils}.
 * 
 * @version   $Revision$  $Author$  $Date$
 */
public class TestUtils extends TestCase {

	public void testPortMessage() throws IllegalMessageFormat {
		TicketMessage message =
			new Utils.TicketMessage(
				"Ticket #9438: Ported to CWS_TL_5_7_3_Patch_11_2 from TL_trunk: Resolved merge conflict in Unimplementable: Added missing documentation.");
		assertEquals("9438", message.ticketNumber);
		assertNull(message.apiChange);
		assertEquals(" Resolved merge conflict in Unimplementable: Added missing documentation.",
			message.originalMessage);
	}
	
	public void testAPIChangePortMessage() throws IllegalMessageFormat {
		TicketMessage message = new Utils.TicketMessage("Ticket #9438: Ported to CWS_TL_5_7_3_Patch_11_2 from TL_trunk: API change: Added missing documentation.");
		assertEquals("9438", message.ticketNumber);
		assertNotNull(message.apiChange);
		assertEquals(" Added missing documentation.", message.originalMessage);
	}
	
	public void testMessage() throws IllegalMessageFormat {
		TicketMessage message = new Utils.TicketMessage("Ticket #9438: Added missing documentation.");
		assertEquals("9438", message.ticketNumber);
		assertNull(message.apiChange);
		assertEquals(" Added missing documentation.", message.originalMessage);
	}
	
	public void testAPIChangeMessage() throws IllegalMessageFormat {
		TicketMessage message = new Utils.TicketMessage("Ticket #9438: API change: Added missing documentation.");
		assertEquals("9438", message.ticketNumber);
		assertNotNull(message.apiChange);
		assertEquals(" Added missing documentation.", message.originalMessage);
	}
	
	public void testFollowUpMessage() throws IllegalMessageFormat {
		TicketMessage message =
			new Utils.TicketMessage("Ticket #9438: API change: Follow-up for [4711]: Added missing documentation.");
		assertTrue(message.apiChange != null);
		assertEquals(4711, message.getLeadRevision());
	}

}

