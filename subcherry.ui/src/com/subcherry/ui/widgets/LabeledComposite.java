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
package com.subcherry.ui.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * A {@link Composite} specialization providing a functionality which is similar
 * to {@link Group}s but does not enforce borders to the entire composite.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class LabeledComposite extends Composite {

	/**
	 * @see #getLabelText()
	 */
	private String _labelText;
	
	/**
	 * @see #getLabelFont()
	 */
	private Font _labelFont;
	
	/**
	 * Create a {@link LabeledComposite}.
	 * 
	 * @param parent
	 *            see {@link #getParent()}
	 * @param style
	 *            see {@link #getStyle()}
	 */
	public LabeledComposite(final Composite parent, final int style) {
		super(parent, style);
		
		addPaintListener(this::paintLabel);
	}
	
	/**
	 * @return the {@link Font} to be used for {@link #getLabelText()}. If
	 *         {@link #setLabelFont(Font)} was not called, {@link #getFont()} is
	 *         returned
	 */
	public Font getLabelFont() {
		checkWidget();
		
		if (_labelFont != null) {
			return _labelFont;
		}
		
		return getFont();
	}
	
	/**
	 * Setter for {@link #getLabelFont()}.
	 * 
	 * @param font
	 *            the {@link Font} to be used for {@link #getLabelText()} or
	 *            {@code null} to use {@link #getFont()}
	 */
	public void setLabelFont(final Font font) {
		checkWidget();
		
		_labelFont = font;
		
		redraw();
	}
	
	/**
	 * @return the label text or {@code null} if none was set
	 */
	public String getLabelText() {
		checkWidget();
		
		return _labelText;
	}
	
	/**
	 * Setter for {@link #getLabelText()}.
	 * 
	 * @param label
	 *            see {@link #getLabelText()}
	 */
	public void setLabelText(final String label) {
		checkWidget();
		
		_labelText = label;
		
		redraw();
	}
	
	@Override
	public Rectangle computeTrim(final int x, final int y, final int width, final int height) {
		final Rectangle trim = super.computeTrim(x, y, width, height);

		if(getLabelText() != null) {
			final GC gc = new GC(this);
			try {
				gc.setFont(getLabelFont());
				final Point labelSize = textExtent(getLabelText(), gc);
				
				trim.y -= labelSize.y;
				trim.height += labelSize.y;
			} finally {
				gc.dispose();
			}
		}
		
		return trim;
	}
	
	@Override
	public Rectangle getClientArea() {
		final Rectangle area = super.getClientArea();
		
		if(getLabelText() != null) {
			final GC gc = new GC(this);
			try {
				gc.setFont(getLabelFont());
				final Point labelSize = textExtent(getLabelText(), gc);
				
				area.y += labelSize.y;
				area.height -= labelSize.y;
			} finally {
				gc.dispose();
			}
		}
		
		return area;
	}
	
	/**
	 * Paint the composite's label.
	 * 
	 * @param event
	 *            the {@link PaintEvent} which triggered composite redraw
	 */
	private void paintLabel(final PaintEvent event) {
		final GC gc = event.gc;
		final Device device = gc.getDevice();
		final String text = getLabelText();
		
		// make sure to set the font before computing text extent
		gc.setFont(getLabelFont());
		final Point textSize = textExtent(text, gc);
		
		// draw the separator line
		final int textHeightHalf = textSize.y >> 1;
		gc.setForeground(device.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		gc.drawLine(0, textHeightHalf, getBounds().width, textHeightHalf);
		
		// draw the label text
		gc.setForeground(device.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
		gc.drawString(text, 10, 0);
	}
	
	/**
	 * @param text
	 *            the {@link String} to compute the bounding rectangle size for
	 * @param gc
	 *            the {@link GC} to be used for computing
	 * @return {@link GC#textExtent(String, int)} using {@link SWT#NONE} as flags
	 */
	static Point textExtent(final String text, final GC gc) {
		return gc.textExtent(text, SWT.NONE);
	}
}
