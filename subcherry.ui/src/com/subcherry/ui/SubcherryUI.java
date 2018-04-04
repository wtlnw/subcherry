/*
 * SubCherry - Cherry Picking with Trac and Subversion
 * Copyright (C) 2018 Bernhard Haumacher and others
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
package com.subcherry.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.subcherry.core.SubcherryCore;

/**
 * {@link AbstractUIPlugin} implementation for the {@code subcherry.ui} plug-in.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryUI extends AbstractUIPlugin {

	/**
	 * @see #getInstance()
	 */
	private static SubcherryUI _instance;

	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		
		_instance = this;
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		_instance = null;
		
		super.stop(context);
	}

	/**
	 * @return the shared {@link SubcherryCore} instance or {@code null} if it has
	 *         not been {@link #start(BundleContext) started} yet or has already
	 *         been {@link #stop(BundleContext) stopped}
	 */
	public static SubcherryUI getInstance() {
		return _instance;
	}
}
