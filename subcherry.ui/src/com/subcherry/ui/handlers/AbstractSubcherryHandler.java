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
package com.subcherry.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import com.subcherry.ui.views.SubcherryMergeContext;
import com.subcherry.ui.views.SubcherryMergeView;

/**
 * A base {@link AbstractHandler} implementation for all commands in the
 * {@link SubcherryMergeView}.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public abstract class AbstractSubcherryHandler extends AbstractHandler {
	
	/**
	 * @param event
	 *            the {@link ExecutionEvent} for this
	 *            {@link AbstractSubcherryHandler}
	 * @return the {@link SubcherryMergeView} resolved from the given event
	 * @throws ExecutionException
	 *             if an error occurred when resolving the context
	 */
	protected SubcherryMergeView getView(final ExecutionEvent event) throws ExecutionException {
		return (SubcherryMergeView) HandlerUtil.getActivePartChecked(event);
	}

	/**
	 * @param event
	 *            the {@link ExecutionEvent} for this
	 *            {@link AbstractSubcherryHandler}
	 * @return the {@link SubcherryMergeContext} resolved from the given event
	 * @throws ExecutionException
	 *             if an error occurred when resolving the context
	 */
	protected SubcherryMergeContext getContext(final ExecutionEvent event) throws ExecutionException {
		final SubcherryMergeView view = getView(event);
		final SubcherryMergeContext context = (SubcherryMergeContext) view.getViewer().getInput();
		
		return context;
	}
}
