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

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.subcherry.core.SubcherryCore;
import com.subcherry.ui.views.SubcherryMergeState;
import com.subcherry.ui.wizards.SubcherryMergeWizard;

/**
 * {@link AbstractUIPlugin} implementation for the {@code subcherry.ui} plug-in.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryUI extends AbstractUIPlugin {

	/**
	 * The locale specific path to bundle icons.
	 */
	public static final String ICONS_PATH = "$nl$/icons/full/"; //$NON-NLS-1$
	
	/**
	 * The identifier of the {@link Image} to be used for all
	 * {@link SubcherryMergeWizard} pages.
	 */
	public static final String IMG_WIZARD_DEFAULT = "com.subcherry.ui.wizards.SubcherryMergeWizard.banner"; //$NON-NLS-1$
	
	/**
	 * The symbolic name for the default {@link Font} with bold style.
	 */
	public static final String DEFAULT = "default"; //$NON-NLS-1$

	/**
	 * @see #getInstance()
	 */
	private static SubcherryUI _instance;

	/**
	 * @see #getFontRegistry()
	 */
	private FontRegistry _fontRegistry;
	
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		
		// _fontRegistry is initialized lazily
		_instance = this;
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		_fontRegistry = null;
		_instance = null;
		
		super.stop(context);
	}
	
	/**
	 * @return the {@link ISecurePreferences} for this {@link #getBundle()} or
	 *         {@code null} if {@link #getBundle()} has not been
	 *         {@link #start(BundleContext) started} yet or has already been
	 *         {@link #stop(BundleContext) stopped}
	 */
	public ISecurePreferences getSecurePreferences() {
		return SecurePreferencesFactory.getDefault().node(getBundle().getSymbolicName());
	}
	
	/**
	 * @return the {@link FontRegistry} defining common {@link Font}s for this
	 *         plugin {@code null} if {@link #getBundle()} has not been
	 *         {@link #start(BundleContext) started} yet or has already been
	 *         {@link #stop(BundleContext) stopped}
	 */
	public FontRegistry getFontRegistry() {
		if(_fontRegistry == null) {
			_fontRegistry = createFontRegistry();
			initFontRegistry(_fontRegistry);
		}
		
		return _fontRegistry;
	}

	/**
	 * @return a new {@link FontRegistry}
	 */
	protected FontRegistry createFontRegistry() {
		// the method was called from the UI thread -> use current Display
    	if(Display.getCurrent() != null) {
			return new FontRegistry(Display.getCurrent());
		}

    	// try to resolve the display of the running workbench
    	if(PlatformUI.isWorkbenchRunning()) {
			return new FontRegistry(PlatformUI.getWorkbench().getDisplay());
		}

    	// no display has been found -> throw an exception
    	throw new SWTError(SWT.ERROR_THREAD_INVALID_ACCESS);
	}
	
	/**
	 * Initialize the given registry with default {@link Font}s.
	 * 
	 * @param registry
	 *            the {@link FontRegistry} to initialize
	 */
	protected void initFontRegistry(final FontRegistry registry) {
		// nothing to initialize (yet)
	}
	
	@Override
	protected void initializeImageRegistry(final ImageRegistry registry) {
		super.initializeImageRegistry(registry);
		
		// load the wizard banner image
		final URL bannerUrl = FileLocator.find(getBundle(), new Path(ICONS_PATH + "wizban/mrg_wizban.png")); //$NON-NLS-1$
		if (bannerUrl != null) {
			final ImageDescriptor img = ImageDescriptor.createFromURL(bannerUrl);
			registry.put(IMG_WIZARD_DEFAULT, img);
		}
		
		// load icons for entry states
		final Bundle platform = Platform.getBundle(PlatformUI.PLUGIN_ID);
		for (final SubcherryMergeState state : SubcherryMergeState.values()) {
			final String path;
			
			switch (state) {
			case COMMITTED:
				path = ICONS_PATH + "elcl16/step_done.png"; //$NON-NLS-1$
				break;
			case CONFLICT:
				path = ICONS_PATH + "obj16/warn_tsk.png"; //$NON-NLS-1$
				break;
			case ERROR:
				path = ICONS_PATH + "obj16/error_tsk.png"; //$NON-NLS-1$
				break;
			case MERGED:
				path = ICONS_PATH + "obj16/change_obj.png"; //$NON-NLS-1$
				break;
			case NEW:
				path = ICONS_PATH + "obj16/generic_element.png"; //$NON-NLS-1$
				break;
			case NO_COMMIT:
				path = ICONS_PATH + "obj16/info_tsk.png"; //$NON-NLS-1$
				break;
			case SKIPPED:
				path = ICONS_PATH + "elcl16/trash.png"; //$NON-NLS-1$
				break;
			default:
				path = null;
				break;
			}
			
			if (path != null) {
				final URL url = FileLocator.find(platform, new Path(path));
				if (url != null) {
					final ImageDescriptor img = ImageDescriptor.createFromURL(url);
					registry.put(getStateImageKey(state), img);
				}
			}
		}
	}
	
	/**
	 * @return the shared {@link SubcherryCore} instance or {@code null} if it has
	 *         not been {@link #start(BundleContext) started} yet or has already
	 *         been {@link #stop(BundleContext) stopped}
	 */
	public static SubcherryUI getInstance() {
		return _instance;
	}
	
	/**
	 * @return the identifier of this plug-in being the {@link #getBundle()}'s
	 *         {@link Bundle#getSymbolicName()} or {@code null} if
	 *         {@link #getBundle()} has not been {@link #start(BundleContext)
	 *         started} yet or has already been {@link #stop(BundleContext) stopped}
	 */
	public static String id() {
		return getInstance().getBundle().getSymbolicName();
	}
	
	/**
	 * @return the {@link SWT#BOLD} styled default {@link Font}
	 */
	public static Font getBoldDefault() {
		return getInstance().getFontRegistry().getBold(SubcherryUI.DEFAULT);
	}
	
	/**
	 * @param state
	 *            the {@link SubcherryMergeState} to build the image key for
	 * @return the key {@link String} to be used for resolving the {@link Image} for
	 *         the given {@link SubcherryMergeState} from
	 *         {@link #getImageRegistry()}
	 */
	public static final String getStateImageKey(final SubcherryMergeState state) {
		return new StringBuilder(SubcherryMergeState.class.getName())
			.append('.')
			.append(state.toString())
			.toString();
	}
}
