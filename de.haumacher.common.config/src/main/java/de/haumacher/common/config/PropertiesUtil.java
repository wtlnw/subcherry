package de.haumacher.common.config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;


public class PropertiesUtil {

	private static final Map<Class<?>, Class<?>> wrapperTypes = new HashMap<Class<?>, Class<?>>();
	private static final int FIRST_INDEX = 1;
	static {
		wrapperTypes.put(boolean.class, Boolean.class);
		wrapperTypes.put(byte.class, Byte.class);
		wrapperTypes.put(short.class, Short.class);
		wrapperTypes.put(int.class, Integer.class);
		wrapperTypes.put(long.class, Long.class);
		wrapperTypes.put(float.class, Float.class);
		wrapperTypes.put(double.class, Double.class);
	}

	public static <T extends Value> T load(String fileName, Class<T> type) throws IOException {
		return load(loadProperties(fileName), type);
	}

	public static <T extends Value> T load(Properties properties, Class<T> type) {
		return load(properties, "", type);
	}
	
	public static <T extends Value> T load(String fileName, String prefix, Class<T> type) throws IOException {
		return load(loadProperties(fileName), prefix, type);
	}

	public static <T extends Value> T load(Properties properties, String prefix, Class<T> type) {
		ValueDescriptor<T> descriptor = ValueFactory.getDescriptor(type);
		T obj = descriptor.newInstance();
		
		load(properties, prefix, obj);
		
		return obj;
	}
	
	public static void load(String fileName, Value obj) throws IOException {
		load(loadProperties(fileName), obj);
	}

	public static void load(Properties properties, Value obj) {
		load(properties, "", obj);
	}
	
	public static void load(String fileName, String prefix, Value obj) throws IOException {
		load(loadProperties(fileName), prefix, obj);
	}

	public static void load(Properties properties, String prefix, Value obj) {
		StringBuilder keyBuffer = new StringBuilder();
		keyBuffer.append(prefix);
		loadValue(properties, keyBuffer, obj);
	}
	
	public static void save(String fileName, Value obj) throws IOException {
		storeProperties(save(new Properties(), obj), fileName);
	}

	public static Properties save(Properties properties, Value obj) {
		return save(properties, "", obj);
	}

	public static void save(String fileName, String prefix, Value obj) throws IOException {
		storeProperties(save(new Properties(), prefix, obj), fileName);
	}

	public static Properties save(Properties properties, String prefix, Value obj) {
		StringBuilder keyBuffer = new StringBuilder();
		keyBuffer.append(prefix);
		saveValue(properties, keyBuffer, obj);
		return properties;
	}

	private static void saveValue(Properties properties, StringBuilder keyBuffer, Value obj) {
		ValueDescriptor<?> descriptor = obj.descriptor();
		int len1 = keyBuffer.length();
		for (Property property : descriptor.getProperties().values()) {
			Object value = obj.value(property);
			String propertyName = property.getName();
			
			switch (property.getKind()) {
			case PRIMITIVE: {
				keyBuffer.append(propertyName);
				
				savePrimitive(properties, keyBuffer, property, value);
				break;
			}
			
			case VALUE: {
				keyBuffer.append(propertyName);
				keyBuffer.append('.');
				
				saveValue(properties, keyBuffer, (Value) value);
				break;
			}
			
			case LIST: {
				keyBuffer.append(propertyName);
				keyBuffer.append('.');
				
				List<?> list = (List<?>) value;
				int len2 = keyBuffer.length();
				int n = FIRST_INDEX;
				for (Object entry : list) {
					keyBuffer.append(n++);
					keyBuffer.append('.');
					
					saveValue(properties, keyBuffer, (Value) entry);
					keyBuffer.setLength(len2);
				}
				break;
			}
			
			case INDEX: {
				keyBuffer.append(propertyName);
				keyBuffer.append('.');

				Map<?, ?> map = (Map<?, ?>) value;
				int len2 = keyBuffer.length();
				int n = FIRST_INDEX;
				for (Entry<?, ?> entry : map.entrySet()) {
					keyBuffer.append(n++);
					keyBuffer.append('.');
					
					saveValue(properties, keyBuffer, (Value) entry.getValue());
					keyBuffer.setLength(len2);
				}
				break;
			}
			
			case REFERENCE: {
				break;
			}
			}
			
			keyBuffer.setLength(len1);
		}
	}

	private static void savePrimitive(Properties properties, StringBuilder keyBuffer, Property property, Object value) {
		if (value == null) {
			properties.remove(keyBuffer.toString());
		} else {
			properties.setProperty(keyBuffer.toString(), property.getParser().unparse(value));
		}
	}
	
	private static void loadValue(Properties properties, StringBuilder keyBuffer, Value obj) {
		ValueDescriptor<?> descriptor = obj.descriptor();
		int len1 = keyBuffer.length();
		for (Property property : descriptor.getProperties().values()) {
			String propertyName = property.getName();
			
			switch (property.getKind()) {
			case PRIMITIVE: {
				keyBuffer.append(propertyName);
				
				Object value = loadPrimitive(properties, keyBuffer, property);
				obj.putValue(property, value);
				break;
			}
			
			case VALUE: {
				keyBuffer.append(propertyName);
				keyBuffer.append('.');
				
				loadValue(properties, keyBuffer, (Value) obj.value(property));
				break;
			}
			
			case LIST: {
				keyBuffer.append(propertyName);
				keyBuffer.append('.');
				
				@SuppressWarnings("unchecked")
				List<Value> list = (List<Value>) obj.value(property);
				
				Value nullValue = (Value) ValueFactory.getDescriptor(property.getType()).newInstance();

				int len2 = keyBuffer.length();
				int n = FIRST_INDEX;
				while (true) {
					keyBuffer.append(n++);
					keyBuffer.append('.');
					
					Value entryValue = (Value) ValueFactory.getDescriptor(property.getType()).newInstance();
					loadValue(properties, keyBuffer, entryValue);
					
					if (entryValue.equals(nullValue)) {
						break;
					}
					
					list.add(entryValue);
					
					keyBuffer.setLength(len2);
				}
				break;
			}
			
			case INDEX: {
				keyBuffer.append(propertyName);
				keyBuffer.append('.');
				
				@SuppressWarnings("unchecked")
				Map<Object, Value> map = (Map<Object, Value>) obj.value(property);

				Value nullValue = (Value) ValueFactory.getDescriptor(property.getType()).newInstance();

				int len2 = keyBuffer.length();
				int n = FIRST_INDEX;
				while (true) {
					keyBuffer.append(n++);
					keyBuffer.append('.');
					
					Value entryValue = (Value) ValueFactory.getDescriptor(property.getType()).newInstance();
					loadValue(properties, keyBuffer, entryValue);
					
					if (nullValue.equals(entryValue)) {
						break;
					}
					
					Object key = entryValue.value(property.getIndexProperty());
					map.put(key, entryValue);
					
					keyBuffer.setLength(len2);
				}
				break;
			}
			
			case REFERENCE:
				continue;
			}
			
			keyBuffer.setLength(len1);
		}
	}
	
	private static Object loadPrimitive(Properties properties, StringBuilder keyBuffer, Property property) {
		String valueSource = properties.getProperty(keyBuffer.toString());
		
		Parser<Object> parser = property.getParser();
		if (valueSource == null) {
			return parser.init();
		} else {
			return parser.parse(valueSource);
		}
	}

	public static Properties loadProperties(String fileName) throws IOException {
		Properties result = new Properties();
		FileInputStream in = new FileInputStream(fileName);
		try {
			result.load(in);
		} finally {
			in.close();
		}
		return result;
	}

	public static void storeProperties(Properties properties, String fileName) throws IOException {
		storeProperties(properties, fileName, null);
	}

	public static void storeProperties(Properties properties, String fileName, String comment) throws IOException {
		FileOutputStream out = new FileOutputStream(fileName);
		try {
			properties.store(out, comment);
		} finally {
			out.close();
		}
	}

}
