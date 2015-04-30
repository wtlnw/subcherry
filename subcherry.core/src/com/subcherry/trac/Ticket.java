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

/**
 * @version   $Revision$  $Author$  $Date$
 */
public abstract class Ticket {

	public abstract String id();

	public abstract String title();

	public abstract String component();

	public abstract String type();

	public abstract String status();

	public abstract String resolution();

	public abstract String milestone();

	public abstract String implementedIn();

	@Override
	public String toString() {
		return " Ticket " + id() + "[milestone:" + milestone() + ",status:" + status() + ",resolution:" + resolution() + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || obj.getClass() != Ticket.class) {
			return false;
		}
		return id() == null ? ((Ticket) obj).id() == null : id().equals(((Ticket) obj).id());
	}

	@Override
	public int hashCode() {
		if (id() == null) {
			return 4711;
		}
		return id().hashCode() + 17;
	}

}
