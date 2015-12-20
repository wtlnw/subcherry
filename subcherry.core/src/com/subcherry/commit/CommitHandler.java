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
package com.subcherry.commit;

import com.subcherry.CommitConfig;
import com.subcherry.Configuration;
import com.subcherry.merge.Handler;
import com.subcherry.repository.core.LogEntry;
import com.subcherry.utils.Utils.TicketMessage;

/**
 * @version $Revision$ $Author$ $Date$
 */
public class CommitHandler extends Handler<CommitConfig> {
	
	static final String TRUNK = "trunk";
	static final String ORIGINAL = "{original}";
	static final String BACKPORT = "{Backport}";
	
	final MessageRewriter _messageRewriter;

	public CommitHandler(Configuration config, MessageRewriter messageRewriter) {
		super(config);
		
		_messageRewriter = messageRewriter;
	}

	public Commit parseCommit(LogEntry logEntry) {
		TicketMessage ticketMessage = new TicketMessage(logEntry.getRevision(), logEntry.getMessage(), _messageRewriter);
		return new Commit(_config, logEntry, ticketMessage);
	}

}
