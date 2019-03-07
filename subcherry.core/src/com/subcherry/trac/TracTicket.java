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

import java.util.Date;
import java.util.Map;

/**
 * Instances of this class represent trac tickets and provide access to the field values.
 */
public class TracTicket {

	/**
	 * @see #getSummary()
	 */
	public static final String TICKET_ATT_SUMMARY = "summary";

	/**
	 * @see #getComponent()
	 */
	public static final String TICKET_ATT_COMPONENT = "component";

	/**
	 * @see #getType()
	 */
	public static final String TICKET_ATT_TYPE = "type";

	/**
	 * @see #getCc()
	 */
	public static final String TICKET_ATT_CC = "cc";

	/**
	 * @see #getOwner()
	 */
	public static final String TICKET_ATT_OWNER = "owner";

	/**
	 * @see #getKeyword()
	 */
	public static final String TICKET_ATT_KEYWORD = "keywords";

	/**
	 * @see #getStatus()
	 */
	public static final String TICKET_ATT_STATUS = "status";

	/**
	 * @see #getResolution()
	 */
	public static final String TICKET_ATT_RESOLUTION = "resolution";

	/**
	 * @see #getVersion()
	 */
	public static final String TICKET_ATT_VERSION = "version";

	/**
	 * @see #getMilestone()
	 */
	public static final String TICKET_ATT_MILESTONE = "milestone";

	/**
	 * @see #getDescription()
	 */
	public static final String TICKET_ATT_DESCRIPTION = "description";

	/**
	 * @see #getReporter()
	 */
	public static final String TICKET_ATT_REPORTER = "reporter";

	/**
	 * @see #getPriority()
	 */
	public static final String TICKET_ATT_PRIORITY = "priority";

	/**
	 * @see #getImplementedIn()
	 */
	public static final String TICKET_ATT_IMPLEMENTED_IN = "implementedon";

	/**
	 * @see #getDependsOn()
	 */
	public static final String TICKET_ATT_DEPENDS_ON = "dependson";

	/**
	 * @see #getFollowUp()
	 */
	public static final String TICKET_ATT_FOLLOW_UP = "followup";

	/**
	 * @see #getNumber()
	 */
	public static final String TICKET_ATT__NUMBER = "_number";

	/**
	 * @see #getCreated()
	 */
	public static final String TICKET_ATT__DATE_CREATED = "_dateCreated";

	/**
	 * @see #getModified()
	 */
	public static final String TICKET_ATT__DATE_MODIFIED = "_dateModified";

	/**
	 * A {@link Map} of field values mapped by their respective field name.
	 */
	private final Map<?, ?> _attributes;

	/**
	 * @see #getCreated()
	 */
	private final Date _created;

	/**
	 * @see #getModified()
	 */
	private final Date _modified;

	/**
	 * @see #getNumber()
	 */
	private final Integer _number;

	/**
	 * Creates a {@link TracTicket}.
	 *
	 * @param number
	 *        see {@link #getNumber()}
	 * @param created
	 *        see {@link #getCreated()}
	 * @param modified
	 *        see {@link #getModified()}
	 * @param attributes
	 *        a {@link Map} of field values mapped by their respective field name
	 */
	public TracTicket(final Integer number, final Date created, final Date modified, final Map<?, ?> attributes) {
		_number = number;
		_created = created;
		_modified = modified;
		_attributes = attributes;
	}

	/**
	 * @return the ticket number
	 */
	public Integer getNumber() {
		return _number;
	}

	/**
	 * @return the {@link Date} of ticket creation
	 */
	public Date getCreated() {
		return _created;
	}

	/**
	 * @return the {@link Date} of last ticket modification
	 */
	public Date getModified() {
		return _modified;
	}

	/**
	 * @return the value of the {@value #TICKET_ATT_CC} field
	 */
	public String getCc() {
		return (String) _attributes.get(TICKET_ATT_CC);
	}

	/**
	 * @return the value of the {@value #TICKET_ATT_COMPONENT} field
	 */
	public String getComponent() {
		return (String) _attributes.get(TICKET_ATT_COMPONENT);
	}

	/**
	 * @return the value of the {@value #TICKET_ATT_DEPENDS_ON} field
	 */
	public String getDependsOn() {
		return (String) _attributes.get(TICKET_ATT_DEPENDS_ON);
	}

	/**
	 * @return the value of the {@value #TICKET_ATT_DESCRIPTION} field
	 */
	public String getDescription() {
		return (String) _attributes.get(TICKET_ATT_DESCRIPTION);
	}

	/**
	 * @return the value of the {@value #TICKET_ATT_FOLLOW_UP} field
	 */
	public String getFollowUp() {
		return (String) _attributes.get(TICKET_ATT_FOLLOW_UP);
	}

	/**
	 * @return the value of the {@value #TICKET_ATT_IMPLEMENTED_IN} field
	 */
	public String getImplementedIn() {
		return (String) _attributes.get(TICKET_ATT_IMPLEMENTED_IN);
	}

	/**
	 * @return the value of the {@value #TICKET_ATT_KEYWORD} field
	 */
	public String getKeyword() {
		return (String) _attributes.get(TICKET_ATT_KEYWORD);
	}

	/**
	 * @return the value of the {@value #TICKET_ATT_MILESTONE} field
	 */
	public String getMilestone() {
		return (String) _attributes.get(TICKET_ATT_MILESTONE);
	}

	/**
	 * @return the value of the {@value #TICKET_ATT_OWNER} field
	 */
	public String getOwner() {
		return (String) _attributes.get(TICKET_ATT_OWNER);
	}

	/**
	 * @return the value of the {@value #TICKET_ATT_PRIORITY} field
	 */
	public String getPriority() {
		return (String) _attributes.get(TICKET_ATT_PRIORITY);
	}

	/**
	 * @return the value of the {@value #TICKET_ATT_REPORTER} field
	 */
	public String getReporter() {
		return (String) _attributes.get(TICKET_ATT_REPORTER);
	}

	/**
	 * @return the value of the {@value #TICKET_ATT_RESOLUTION} field
	 */
	public String getResolution() {
		return (String) _attributes.get(TICKET_ATT_RESOLUTION);
	}

	/**
	 * @return the value of the {@value #TICKET_ATT_STATUS} field
	 */
	public String getStatus() {
		return (String) _attributes.get(TICKET_ATT_STATUS);
	}

	/**
	 * @return the value of the {@value #TICKET_ATT_SUMMARY} field
	 */
	public String getSummary() {
		return (String) _attributes.get(TICKET_ATT_SUMMARY);
	}

	/**
	 * @return the value of the {@value #TICKET_ATT_TYPE} field
	 */
	public String getType() {
		return (String) _attributes.get(TICKET_ATT_TYPE);
	}

	/**
	 * @return the value of the {@value #TICKET_ATT_VERSION} field
	 */
	public String getVersion() {
		return (String) _attributes.get(TICKET_ATT_VERSION);
	}
}
