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
package com.subcherry.ui.wizards;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.subcherry.Configuration;

import de.haumacher.common.config.Property;
import de.haumacher.common.config.Value;
import de.haumacher.common.config.ValueDescriptor;
import de.haumacher.common.config.ValueFactory;

/**
 * A {@link WizardPage} for advanced users displaying additional
 * {@link Configuration} options.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class SubcherryMergeWizardAdvancedPage extends WizardPage {

	/**
	 * Create a {@link SubcherryMergeWizardAdvancedPage}. 
	 */
	public SubcherryMergeWizardAdvancedPage() {
		super("Advanced");
		
		setTitle("SVN Cherry Picking With Subcherry");
		setMessage("Advanced configuration options.");
	}
	
	@Override
	public void createControl(final Composite parent) {
		final ScrolledComposite composite = new ScrolledComposite(parent, SWT.V_SCROLL);
		composite.setExpandHorizontal(true);
		composite.setExpandVertical(true);

		final Composite contents = new Composite(composite, SWT.NONE);
		contents.setLayout(new GridLayout(2, false));
		
		final Value config = getWizard().getConfiguration();
		createControls(config.descriptor(), config, contents);
		
		composite.setContent(contents);
		composite.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle r = composite.getClientArea();
				composite.setMinSize(parent.computeSize(r.width, SWT.DEFAULT));
			}
		});
		
		setControl(composite);
		setPageComplete(true);
	}

	/**
	 * Create {@link Control}s for each property defined by the given
	 * {@code descriptor} and initialize them with the value provided by the given
	 * {@code config}.
	 * 
	 * @param descriptor the {@link ValueDescriptor} to defining the properties to
	 *                   create controls for
	 * @param config     the {@link Value} providing configuration for the
	 *                   properties of the given {@code descriptor} or {@code null}
	 *                   if no configuration was provided
	 * @param parent     the {@link Composite} to create the {@link Control}s in
	 */
	private void createControls(final ValueDescriptor<?> descriptor, final Value config, final Composite parent) {
		descriptor.getProperties().values()
			.stream()
			.sorted((p1, p2) -> p1.getName().compareTo(p2.getName()))
			.forEach(property -> {
				switch (property.getKind()) {
					case INDEX:
					case LIST:
					case PRIMITIVE:
					case REFERENCE: {
						final Label label = new Label(parent, SWT.NONE);
						label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
						label.setText(property.getName() + ":");
						
						final Control field;
						final Class<?> type = property.getType();
						if(boolean.class == type || Boolean.class == type) {
							field = newBooleanField(property, config, parent);
						} else if(Date.class.isAssignableFrom(type)) {
							field = newDateField(property, config, parent);
						} else {
							field = newTextField(property, config, parent);
						}
						field.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
						
						break;
					}
					case VALUE: {
						newValueField(property, config, parent);
						break;
					}
					default:
						break;
				}
			});
	}
	
	/**
	 * Create {@link Control}s for the given complex {@link Property}.
	 * 
	 * @param property the {@link Property} to create controls for
	 * @param config   the {@link Value} to be used to resolve the configuration for
	 *                 the given {@code property} or {@code null} if none
	 * @param parent   the {@link Composite} to create the {@link Control}s in
	 */
	private void newValueField(final Property property, final Value config, final Composite parent) {
		final Group group = new Group(parent, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		group.setLayout(new GridLayout(2, false));
		group.setText(property.getName());
		
		createControls(ValueFactory.getDescriptor(property.getType()), (Value) config.value(property), group);
	}

	/**
	 * @param property the boolean {@link Property} to create the {@link Control}
	 *                 for
	 * @param config   the {@link Value} to be used to resolve the configuration for
	 *                 the given {@code property} or {@code null} if none
	 * @param parent   {@link Composite} to create the {@link Control} in
	 * @return a new {@link Button} representing the given configuration
	 *         {@link Property}
	 */
	private Button newBooleanField(final Property property, final Value config, final Composite parent) {
		final Button field = new Button(parent, SWT.CHECK);
		
		if(config != null) {
			final Boolean value = (Boolean) config.value(property);
			if(value != null) {
				field.setSelection(value);
			}
		}
		
		// add modification listener
		field.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				config.putValue(property, field.getSelection());
			}
		});
		
		return field;
	}

	/**
	 * @param property the {@link Date} {@link Property} to create the
	 *                 {@link Control} for
	 * @param config   the {@link Value} to be used to resolve the configuration for
	 *                 the given {@code property} or {@code null} if none
	 * @param parent   {@link Composite} to create the {@link Control} in
	 * @return a new {@link DateTime} representing the given configuration
	 *         {@link Property}
	 */
	private DateTime newDateField(final Property property, final Value config, final Composite parent) {
		final DateTime field = new DateTime(parent, SWT.CALENDAR);
		
		if(config != null) {
			final Date date = (Date) config.value(property);
			if(date != null) {
				final Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				
				field.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
				field.setTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
			}
		}
		
		// add modification listener
		field.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final Calendar cal = Calendar.getInstance();
				
				cal.set(field.getYear(),
						field.getMonth(),
						field.getDay(),
						field.getHours(),
						field.getMinutes(),
						field.getSeconds());
				
				config.putValue(property, cal.getTime());
			}
		});
		
		return field;
	}

	/**
	 * @param property the {@link Property} to create the {@link Control} for
	 * @param config   the {@link Value} to be used to resolve the configuration for
	 *                 the given {@code property} or {@code null} if none
	 * @param parent   {@link Composite} to create the {@link Control} in
	 * @return a new {@link Text} representing the given configuration
	 *         {@link Property}
	 */
	private Text newTextField(final Property property, final Value config, final Composite parent) {
		final Text field = new Text(parent, SWT.BORDER | SWT.WRAP | SWT.MULTI);
		
		if(config != null) {
			final Object value = config.value(property);
			if(value != null) {
				field.setText(property.getParser().unparse(value));
			}
		}
		
		// add modification listener
		field.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				config.putValue(property, field.getText());
			}
		});
		
		return field;
	}

	@Override
	public SubcherryMergeWizard getWizard() {
		return (SubcherryMergeWizard) super.getWizard();
	}
}
