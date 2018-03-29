package de.haumacher.common.config;

import java.util.HashMap;
import java.util.Map;

import de.haumacher.common.config.internal.ValueDescriptorImpl;

public class ValueFactory {

	private static final Map<Class<?>, ValueDescriptorImpl<?>> DESCRIPTORS_BY_CLASS = new HashMap<Class<?>, ValueDescriptorImpl<?>>();

	public static synchronized <T> Factory<T> newFactory(final Class<T> valueInterface) {
		return new Factory<T>() {
			@Override
			public T newInstance() {
				return ValueFactory.newInstance(valueInterface);
			}
		};
	}
	
	public static synchronized <T> T newInstance(Class<T> valueInterface) {
		return getDescriptor(valueInterface).newInstance();
	}
	
	public static synchronized <T> ValueDescriptor<T> getDescriptor(Class<T> valueInterface) {
		@SuppressWarnings("unchecked")
		ValueDescriptorImpl<T> result = (ValueDescriptorImpl<T>) DESCRIPTORS_BY_CLASS.get(valueInterface);
		if (result == null) {
			result = new ValueDescriptorImpl<T>(valueInterface);
			DESCRIPTORS_BY_CLASS.put(valueInterface, result);
			
			// Prevent stack overflow in mutable recursive structures.
			result.init();
		}
		return result;
	}

}
