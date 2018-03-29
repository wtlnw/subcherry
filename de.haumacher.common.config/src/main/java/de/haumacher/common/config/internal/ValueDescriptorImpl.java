package de.haumacher.common.config.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.haumacher.common.config.Kind;
import de.haumacher.common.config.Property;
import de.haumacher.common.config.Value;
import de.haumacher.common.config.ValueDescriptor;

public class ValueDescriptorImpl<T> implements ValueDescriptor<T> {

	static final class ValueImpl implements InvocationHandler {
		final ValueDescriptorImpl<?> descriptor;
		final Object[] values;
		
		public ValueImpl(ValueDescriptorImpl<?> descriptor) {
			this.descriptor = descriptor;
			this.values = new Object[descriptor.getSize()];
			Collection<PropertyImpl> properties = descriptor.properties.values();
			for (PropertyImpl property : properties) {
				values[property.getIndex()] = property.getInitializer().init();
			}
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return descriptor.getHandler(method).handlePropertyAccess(proxy, this, args);
		}
	}

	private static final Method EQUALS_METHOD;
	private static final Method TO_STRING_METHOD;
	private static final Method HASH_CODE_METHOD;
	private static final Method DESCRIPTOR_METHOD;
	private static final Method VALUE_METHOD;
	private static final Method PUT_VALUE_METHOD;
	static {
		try {
			Method equalsMethod = Object.class.getMethod("equals", Object.class);
			Method hashCodeMethod = Object.class.getMethod("hashCode");
			Method toStringMethod = Object.class.getMethod("toString");
			Method descriptorMethod = Value.class.getMethod("descriptor");
			Method valueMethod = Value.class.getMethod("value", Property.class);
			Method putValueMethod = Value.class.getMethod("putValue", Property.class, Object.class);
			
			EQUALS_METHOD = equalsMethod;
			TO_STRING_METHOD = toStringMethod;
			HASH_CODE_METHOD = hashCodeMethod;
			DESCRIPTOR_METHOD = descriptorMethod;
			VALUE_METHOD = valueMethod;
			PUT_VALUE_METHOD = putValueMethod;
		} catch (SecurityException e) {
			throw (AssertionError) new AssertionError().initCause(e);
		} catch (NoSuchMethodException e) {
			throw (AssertionError) new AssertionError().initCause(e);
		}
	}
	
	private static final MethodHandler EQUALS_IMPL = new MethodHandler() {
		@Override
		public Object handlePropertyAccess(Object self, ValueImpl impl, Object... args) {
			Object other = args[0];
			if (other == self) {
				return true;
			}
			
			ValueDescriptorImpl<?> descriptor = impl.descriptor;
			if (! descriptor.valueInterface.isInstance(other)) {
				return false;
			}
			
			try {
				for (PropertyImpl property : descriptor.internalGetProperties()) {
					Object selfValue = property.getGetHandler().handlePropertyAccess(self, impl);
					Object otherValue = property.getGetter().invoke(other);
					if (!property.getParser().equals(selfValue, otherValue)) {
						return false;
					}
				}
				return true;
			} catch (IllegalAccessException e) {
				throw (AssertionError) new AssertionError().initCause(e);
			} catch (InvocationTargetException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RuntimeException) {
					throw ((RuntimeException) cause);
				}
				if (cause instanceof Error) {
					throw ((Error) cause);
				}
				// Getters must not declare exceptions.
				throw (AssertionError) new AssertionError().initCause(e);
			}
		}
	};
	
	private static final MethodHandler HASH_CODE_IMPL = new MethodHandler() {
		@Override
		public Object handlePropertyAccess(Object self, ValueImpl impl, Object... args) {
			ValueDescriptorImpl<?> descriptor = impl.descriptor;
			
			int result = 0;
			for (PropertyImpl property : descriptor.internalGetProperties()) {
				Object value = property.getGetHandler().handlePropertyAccess(self, impl);
				result += (property.getIndex() + 1) * property.getParser().hashCode(value);
			}
			return result;
		}
	};
	
	private static final MethodHandler TO_STRING_IMPL = new MethodHandler() {
		@Override
		public Object handlePropertyAccess(Object self, ValueImpl impl, Object... args) {
			ValueDescriptorImpl<?> descriptor = impl.descriptor;
			
			boolean first = true;
			StringBuilder result = new StringBuilder(descriptor.valueInterface.getName());
			result.append('{');
			for (PropertyImpl property : descriptor.internalGetProperties()) {
				Object selfValue = property.getGetHandler().handlePropertyAccess(self, impl);
				
				if (first) {
					first = false;
				} else {
					result.append("; ");
				}
				result.append(property.getName());
				result.append(": ");
				if (property.getKind() == Kind.PRIMITIVE) {
					result.append(property.getParser().unparse(selfValue));
				} else {
					result.append(selfValue);
				}
			}
			result.append('}');
			
			return result.toString();
		}
	};
	
	private static final MethodHandler DESCRIPTOR_IMPL = new MethodHandler() {
		@Override
		public Object handlePropertyAccess(Object self, ValueImpl impl, Object... args) {
			return impl.descriptor;
		}
	};
	
	private static final MethodHandler VALUE_IMPL = new MethodHandler() {
		@Override
		public Object handlePropertyAccess(Object self, ValueImpl impl, Object... args) {
			PropertyImpl property = (PropertyImpl) args[0];
			return property.getGetHandler().handlePropertyAccess(self, impl);
		}
	};
	
	private static final MethodHandler PUT_VALUE_IMPL = new MethodHandler() {
		@Override
		public Object handlePropertyAccess(Object self, ValueImpl impl, Object... args) {
			PropertyImpl property = (PropertyImpl) args[0];
			Object value = args[1];
			if (value == null) {
				value = property.getInitializer().init();
			}
			return property.getSetHandler().handlePropertyAccess(self, impl, value);
		}
	};
	
	
	private final Class<T> valueInterface;
	private final Class<?>[] implInterfaces;
	
	private Map<String, PropertyImpl> properties = new HashMap<String, PropertyImpl>();
	private Map<String, Property> propertiesView = Collections.<String, Property>unmodifiableMap(properties);
	
	private Map<Method, MethodHandler> handlerByMethod = new HashMap<Method, MethodHandler>();
	
	public ValueDescriptorImpl(Class<T> valueInterface) {
		this.valueInterface = valueInterface;
		this.implInterfaces = new Class<?>[] {valueInterface, Value.class};
	}
	
	@Override
	public Class<T> getValueInterface() {
		return valueInterface;
	}

	public void init() {
		Map<Method, PropertyImpl> propertyByMethod = new HashMap<Method, PropertyImpl>();
		for (Method method : valueInterface.getMethods()) {
			if (method.getDeclaringClass().isAssignableFrom(Value.class)) {
				// Ignore Value and Object type methods.
				continue;
			}
			
			int modifiers = method.getModifiers();
			if (! Modifier.isPublic(modifiers)) {
				throw new AssertionError("Expected public modifier on '" + method + "'");
			}
			
			if (Modifier.isStatic(modifiers)) {
				throw new AssertionError("No static methods allowed on '" + method + "'");
			}

			if (method.getExceptionTypes().length > 0) {
				throw new AssertionError("Method must not declare exceptions: " + method);
			}
			
			boolean isGetter;
			String prefix;
			String methodName = method.getName();
			if (methodName.startsWith("get")) {
				prefix = "get";
				isGetter = true;
			} else if (methodName.startsWith("set")) {
				prefix = "set";
				isGetter = false;
			} else if (methodName.startsWith("is")) {
				prefix = "is";
				isGetter = true;
			} else if (methodName.startsWith("has")) {
				prefix = "has";
				isGetter = true;
			} else if (methodName.startsWith("can")) {
				prefix = "can";
				isGetter = true;
			} else if (methodName.startsWith("must")) {
				prefix = "must";
				isGetter = true;
			} else {
				throw new AssertionError("Invalid method prefix: " + method);
			}
			
			Class<?> type;
			if (isGetter) {
				Class<?>[] types = method.getParameterTypes();
				if (types.length != 0) {
					throw new AssertionError("Getter must not have parameters: " + method);
				}

				type = method.getReturnType();
				if (type == Void.class || type == void.class) {
					throw new AssertionError("Getter must not have void return type: " + method);
				}
				
				boolean isBoolean = type == boolean.class || type == Boolean.class;
				if (! prefix.equals("get") && ! isBoolean) {
					throw new AssertionError("Non boolean getters must have '" + "get" + "' prefix: " + method);
				}
			} else {
				Class<?>[] types = method.getParameterTypes();
				if (types.length != 1) {
					throw new AssertionError("Setter must have exactly one argument: " + method);
				}

				Class<?> returnType = method.getReturnType();
				if (returnType != void.class) {
					throw new AssertionError("Setter must have void return type: " + method);
				}
				
				type = types[0];
			}
			
			String postfix = methodName.substring(prefix.length());
			
			if (Character.isLowerCase(postfix.charAt(0))) {
				throw new AssertionError("Expected upper case letter after method prefix: " + method);
			}
			
			String propertyName = Character.toLowerCase(postfix.charAt(0)) + postfix.substring(1);
			
			PropertyImpl property = properties.get(propertyName);
			if (property == null) {
				int index = properties.size();
				property = new PropertyImpl(this, propertyName, index);
				properties.put(propertyName, property);
			}
			
			if (isGetter) {
				property.initGetter(method);
			}

			propertyByMethod.put(method, property);
		}
		
		for (Entry<Method, PropertyImpl> entry : propertyByMethod.entrySet()) {
			Method method = entry.getKey();
			boolean isGetter = ! method.getName().startsWith("set");
			PropertyImpl property = entry.getValue();
			handlerByMethod.put(method, isGetter ? property.getGetHandler() : property.getSetHandler());
		}
		
		// Add object methods.
		handlerByMethod.put(EQUALS_METHOD, EQUALS_IMPL);
		handlerByMethod.put(HASH_CODE_METHOD, HASH_CODE_IMPL);
		handlerByMethod.put(TO_STRING_METHOD, TO_STRING_IMPL);
		
		// Add value interface methods.
		handlerByMethod.put(DESCRIPTOR_METHOD, DESCRIPTOR_IMPL);
		handlerByMethod.put(VALUE_METHOD, VALUE_IMPL);
		handlerByMethod.put(PUT_VALUE_METHOD, PUT_VALUE_IMPL);
	}

	Collection<PropertyImpl> internalGetProperties() {
		return properties.values();
	}

	@Override
	public Map<String, Property> getProperties() {
		return propertiesView;
	}
	
	MethodHandler getHandler(Method method) {
		return handlerByMethod.get(method);
	}

	int getSize() {
		return properties.size();
	}

	@Override
	public T newInstance() {
		@SuppressWarnings("unchecked")
		T result = (T) Proxy.newProxyInstance(valueInterface.getClassLoader(), implInterfaces, new ValueImpl(this));
		
		return result;
	}
	
}
