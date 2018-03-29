package de.haumacher.common.config;

import java.util.Map;

public interface ValueDescriptor<T> {

	Map<String, Property> getProperties();

	T newInstance();

	Class<T> getValueInterface();

}
