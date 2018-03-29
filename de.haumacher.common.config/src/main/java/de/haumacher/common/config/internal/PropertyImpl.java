package de.haumacher.common.config.internal;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.haumacher.common.config.DefaultValue;
import de.haumacher.common.config.IndexProperty;
import de.haumacher.common.config.Initializer;
import de.haumacher.common.config.Kind;
import de.haumacher.common.config.ObjectParser;
import de.haumacher.common.config.Parser;
import de.haumacher.common.config.Property;
import de.haumacher.common.config.Reference;
import de.haumacher.common.config.ValueDescriptor;
import de.haumacher.common.config.ValueFactory;
import de.haumacher.common.config.annotate.ValueParser;


class PropertyImpl implements Property {

	static abstract class PrimitiveParser<T> extends ObjectParser<T> {
		
		@Override
		public final T parse(String text) {
			if (text.isEmpty()) {
				return init();
			} else {
				return parseNonEmpty(text);
			}
		}
		
		protected abstract T parseNonEmpty(String text);

		@Override
		public String unparse(T value) {
			if (value == null) {
				return "";
			}
			return value.toString();
		}
	}

	static abstract class CollectionType {
		protected abstract int length(Object collection);
		
		protected abstract Object getValue(Object collection, int n);

		protected abstract void setValue(Object collection, int n, Object value);

		protected abstract Object createCollection(int length);
	}
	
	static abstract class CollectionParser implements Parser<Object> {
		
		private final CollectionType _type;
		
		public CollectionParser(CollectionType type) {
			_type = type;
		}

		@Override
		public Object init() {
			return createCollection(0);
		}
		
		@Override
		public Object parse(String text) {
			String trimmedText = text.trim();
			if (trimmedText.isEmpty()) {
				return init();
			}
			String[] tokens = trimmedText.split(getSeparatorRegexpr());
			Object result = createCollection(tokens.length);
			int n = 0;
			for (String token : tokens) {
				Object value = parseElement(token);
				setValue(result, n++, value);
			}
			return result;
		}

		@Override
		public String unparse(Object value) {
			StringBuilder buffer = new StringBuilder();
			for (int n = 0, cnt = length(value); n < cnt; n++) {
				if (n > 0) {
					buffer.append(getSeparator());
				}
				Object element = getValue(value, n);
				buffer.append(unparseElement(element));
			}

			return buffer.toString();
		}

		protected String getSeparatorRegexpr() {
			return "\\s*,\\s*";
		}

		protected String getSeparator() {
			return ", ";
		}

		protected abstract Object parseElement(String token);

		protected abstract String unparseElement(Object element);
		
		@Override
		public boolean equals(Object value1, Object value2) {
			if (value1 == value2) {
				return true;
			}
			if (value1 == null || value2 == null) {
				return false;
			}

			int length = length(value1);
			if (length(value2) != length) {
				return false;
			}

			for (int i = 0; i < length; i++) {
				Object o1 = getValue(value1, i);
				Object o2 = getValue(value2, i);
				if (!(o1 == null ? o2 == null : o1.equals(o2))) {
					return false;
				}
			}

			return true;
		}

		@Override
		public int hashCode(Object value) {
			if (value == null) {
				return 0;
			}
			
			int result = 1;
			for (int n = 0, cnt = length(value); n < cnt; n++) {
				Object element = getValue(value, n);
				result = 31 * result + (element == null ? 0 : element.hashCode());
			}
			
			return result;
		}

		protected final int length(Object value) {
			return _type.length(value);
		}
		
		protected final Object getValue(Object value, int n) {
			return _type.getValue(value, n);
		}

		protected final void setValue(Object result, int n, Object value) {
			_type.setValue(result, n, value);
		}

		protected final Object createCollection(int length) {
			return _type.createCollection(length);
		}

	}
	
	static abstract class ArrayType extends CollectionType {
		@Override
		protected void setValue(Object collection, int n, Object value) {
			Array.set(collection, n, value);
		}

		@Override
		protected Object getValue(Object collection, int n) {
			return Array.get(collection, n);
		}

		@Override
		protected int length(Object collection) {
			return Array.getLength(collection);
		}
	}
	
	static abstract class PrimitiveArrayParser extends CollectionParser {
		
		public PrimitiveArrayParser(CollectionType type) {
			super(type);
		}

		@Override
		protected String unparseElement(Object element) {
			return "" + element;
		}
	}

	static final class BooleanParser extends BooleanWrapperParser {
		@Override
		public Boolean init() {
			return false;
		}
	}

	static final class BooleanArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new boolean[length];
		}
	}
	
	static class BooleanArrayParser extends PrimitiveArrayParser {

		public BooleanArrayParser(CollectionType type) {
			super(type);
		}

		public BooleanArrayParser() {
			this(new BooleanArrayType());
		}
		
		@Override
		protected Object parseElement(String token) {
			return Boolean.valueOf(token);
		}

	}

	static final class BooleanWrapperArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new Boolean[length];
		}
	}
	
	static final class BooleanWrapperArrayParser extends BooleanArrayParser {
		public BooleanWrapperArrayParser() {
			super(new BooleanWrapperArrayType());
		}
	}

	static final class ByteParser extends ByteWrapperParser {
		@Override
		public Byte init() {
			return (byte) 0;
		}
	}

	static final class ByteArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new byte[length];
		}
	}
	
	static class ByteArrayParser extends PrimitiveArrayParser {
		public ByteArrayParser(CollectionType type) {
			super(type);
		}
		
		public ByteArrayParser() {
			this(new BooleanArrayType());
		}

		@Override
		protected Object parseElement(String token) {
			return Byte.valueOf(token);
		}
	}

	static final class ByteWrapperArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new Byte[length];
		}
	}
	
	static final class ByteWrapperArrayParser extends ByteArrayParser {
		public ByteWrapperArrayParser() {
			this(new ByteWrapperArrayType());
		}

		public ByteWrapperArrayParser(CollectionType type) {
			super(type);
		}
	}

	static final class CharParser extends CharWrapperParser {
		@Override
		public Character init() {
			return '\0';
		}
	}

	static final class CharArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new char[length];
		}
	}
	
	static class CharArrayParser extends PrimitiveArrayParser {
		public CharArrayParser(CollectionType type) {
			super(type);
		}
		
		public CharArrayParser() {
			this(new CharArrayType());
		}

		@Override
		protected Object parseElement(String token) {
			return token.charAt(0);
		}
	}

	static final class CharWrapperArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new Character[length];
		}
	}

	static final class CharWrapperArrayParser extends CharArrayParser {
		public CharWrapperArrayParser() {
			this(new CharWrapperArrayType());
		}

		public CharWrapperArrayParser(CollectionType type) {
			super(type);
		}
	}

	static final class ShortParser extends ShortWrapperParser {
		@Override
		public Short init() {
			return (short)0;
		}
	}

	static final class ShortArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new short[length];
		}
	}

	static class ShortArrayParser extends PrimitiveArrayParser {
		public ShortArrayParser(CollectionType type) {
			super(type);
		}
		
		public ShortArrayParser() {
			this(new ShortArrayType());
		}

		@Override
		protected Object parseElement(String token) {
			return Short.valueOf(token);
		}
	}

	static final class ShortWrapperArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new Short[length];
		}
	}
	
	static final class ShortWrapperArrayParser extends ShortArrayParser {

		public ShortWrapperArrayParser() {
			this(new ShortWrapperArrayType());
		}

		public ShortWrapperArrayParser(CollectionType type) {
			super(type);
		}
		
	}

	static final class IntegerParser extends IntegerWrapperParser {
		@Override
		public Integer init() {
			return 0;
		}
	}

	static final class IntegerArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new int[length];
		}
	}
	
	static class IntegerArrayParser extends PrimitiveArrayParser {
		public IntegerArrayParser(CollectionType type) {
			super(type);
		}
		
		public IntegerArrayParser() {
			this(new IntegerArrayType());
		}

		@Override
		protected Object parseElement(String token) {
			return Integer.valueOf(token);
		}
	}

	static final class IntegerWrapperArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new Integer[length];
		}
	}

	static final class IntegerWrapperArrayParser extends IntegerArrayParser {
		public IntegerWrapperArrayParser() {
			this(new IntegerWrapperArrayType());
		}

		public IntegerWrapperArrayParser(CollectionType type) {
			super(type);
		}
	}

	static final class LongParser extends LongWrapperParser {
		@Override
		public Long init() {
			return 0L;
		}
	}
	
	static final class LongArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new long[length];
		}
	}
	
	static class LongArrayParser extends PrimitiveArrayParser {
		public LongArrayParser(CollectionType type) {
			super(type);
		}
		
		public LongArrayParser() {
			this(new LongArrayType());
		}

		@Override
		protected Object parseElement(String token) {
			return Long.valueOf(token);
		}
	}

	static final class LongWrapperArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new Long[length];
		}
	}

	static final class LongWrapperArrayParser extends LongArrayParser {
		public LongWrapperArrayParser() {
			this(new LongWrapperArrayType());
		}

		public LongWrapperArrayParser(CollectionType type) {
			super(type);
		}
	}

	static final class FloatParser extends FloatWrapperParser {
		@Override
		public Float init() {
			return 0F;
		}
	}

	static final class FloatArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new float[length];
		}
	}
	
	static class FloatArrayParser extends PrimitiveArrayParser {
		public FloatArrayParser(CollectionType type) {
			super(type);
		}
		
		public FloatArrayParser() {
			this(new FloatArrayType());
		}

		@Override
		protected Object parseElement(String token) {
			return Float.valueOf(token);
		}

	}

	static final class FloatWrapperArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new Float[length];
		}
	}
	
	static final class FloatWrapperArrayParser extends FloatArrayParser {
		public FloatWrapperArrayParser() {
			this(new FloatWrapperArrayType());
		}

		public FloatWrapperArrayParser(CollectionType type) {
			super(type);
		}
	}

	static final class DoubleParser extends DoubleWrapperParser {
		@Override
		public Double init() {
			return 0D;
		}
	}

	static final class DoubleArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new double[length];
		}
	}

	static class DoubleArrayParser extends PrimitiveArrayParser {
		public DoubleArrayParser(CollectionType type) {
			super(type);
		}
		
		public DoubleArrayParser() {
			this(new DoubleArrayType());
		}

		@Override
		protected Object parseElement(String token) {
			return Double.valueOf(token);
		}

	}

	static final class DoubleWrapperArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new Double[length];
		}
	}
	
	static final class DoubleWrapperArrayParser extends DoubleArrayParser {
		public DoubleWrapperArrayParser() {
			this(new DoubleWrapperArrayType());
		}

		public DoubleWrapperArrayParser(CollectionType type) {
			super(type);
		}
	}

	static class BooleanWrapperParser extends PrimitiveParser<Boolean> {
		@Override
		protected Boolean parseNonEmpty(String text) {
			return Boolean.valueOf(text);
		}
	}

	static class ByteWrapperParser extends PrimitiveParser<Byte> {
		@Override
		protected Byte parseNonEmpty(String text) {
			return Byte.valueOf(text);
		}
	}

	static class CharWrapperParser extends PrimitiveParser<Character> {
		@Override
		protected Character parseNonEmpty(String text) {
			return text.charAt(0);
		}
	}

	static class ShortWrapperParser extends PrimitiveParser<Short> {
		@Override
		protected Short parseNonEmpty(String text) {
			return Short.valueOf(text);
		}
	}

	static class IntegerWrapperParser extends PrimitiveParser<Integer> {
		@Override
		protected Integer parseNonEmpty(String text) {
			return Integer.valueOf(text);
		}
	}

	static class LongWrapperParser extends PrimitiveParser<Long> {
		@Override
		protected Long parseNonEmpty(String text) {
			return Long.valueOf(text);
		}
	}
	
	static class FloatWrapperParser extends PrimitiveParser<Float> {
		@Override
		protected Float parseNonEmpty(String text) {
			return Float.valueOf(text);
		}
	}

	static class DoubleWrapperParser extends PrimitiveParser<Double> {
		@Override
		protected Double parseNonEmpty(String text) {
			return Double.valueOf(text);
		}
	}

	static final class StringParser extends PrimitiveParser<String> {
		@Override
		public String init() {
			return "";
		}
		
		@Override
		protected String parseNonEmpty(String text) {
			return text;
		}
	}

	static final class StringArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new String[length];
		}
	}
	
	static final class StringArrayParser extends PrimitiveArrayParser {
		public StringArrayParser() {
			super(new StringArrayType());
		}
		
		@Override
		protected Object parseElement(String token) {
			return token;
		}
	}

	static final class FileParser extends ObjectParser<File> {
		@Override
		public File parse(String text) {
			if (text.isEmpty()) {
				return init();
			}
			return new File(text);
		}

		@Override
		public String unparse(File value) {
			if (value == null) {
				return "";
			}
			return value.getPath();
		}
	}

	static final class FileArrayType extends ArrayType {
		@Override
		protected Object createCollection(int length) {
			return new File[length];
		}
	}
	
	static final class FileArrayParser extends CollectionParser {
		public FileArrayParser() {
			super(new FileArrayType());
		}
		
		@Override
		protected String getSeparatorRegexpr() {
			return File.pathSeparator;
		}
		
		@Override
		protected String getSeparator() {
			return File.pathSeparator;
		}

		@Override
		protected Object parseElement(String token) {
			return new File(token);
		}

		@Override
		protected String unparseElement(Object element) {
			return ((File) element).getPath();
		}
	}

	static final class DateParser extends ObjectParser<Date> {
		@Override
		public Date parse(String text) {
			if (text.isEmpty()) {
				return init();
			}
			try {
				return getFormat().parse(text);
			} catch (ParseException ex) {
				throw new IllegalArgumentException("Invalid date format: " + text, ex);
			}
		}

		@Override
		public String unparse(Date value) {
			if (value == null) {
				return "";
			}
			return getFormat().format(value);
		}
		
		private SimpleDateFormat getFormat() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		}

	}

	static final class GenericArrayType<T> extends ArrayType {
		private final Class<T> elementType;
		
		public GenericArrayType(Class<T> elementType) {
			this.elementType = elementType;
		}
		
		@Override
		protected Object createCollection(int length) {
			return Array.newInstance(elementType, length);
		}
	}
	
	static final class ListType extends CollectionType {
		
		/**
		 * Singleton {@link ListType} instance.
		 */
		public static final ListType INSTANCE = new ListType();
		
		private ListType() {
			// Singleton constructor.
		}

		@Override
		protected Object createCollection(int length) {
			return new ArrayList<Object>(length);
		}

		@Override
		protected int length(Object collection) {
			return list(collection).size();
		}

		@Override
		protected Object getValue(Object collection, int n) {
			return list(collection).get(n);
		}

		@Override
		protected void setValue(Object collection, int n, Object value) {
			List<Object> list = list(collection);
			if (n >= list.size()) {
				while (n > list.size()) {
					list.add(null);
				}
				list.add(value);
			} else {
				list.set(n, value);
			}
		}
		
		@SuppressWarnings("unchecked")
		private List<Object> list(Object value) {
			return (List<Object>) value;
		}
	}
	
	static final class GenericCollectionParser<T> extends CollectionParser {
		private final Parser<T> elementParser;

		public GenericCollectionParser(CollectionType type, Parser<T> elementParser) {
			super(type);
			this.elementParser = elementParser;
		}

		@Override
		protected Object parseElement(String token) {
			return elementParser.parse(token);
		}

		@Override
		protected String unparseElement(Object element) {
			return elementParser.unparse((T) element);
		}
	}

	class ValueInitializer implements Initializer<Object> {

		private final ValueDescriptor<?> descriptor;

		public ValueInitializer(ValueDescriptor<?> descriptor) {
			this.descriptor = descriptor;
		}

		@Override
		public Object init() {
			return descriptor.newInstance();
		}

	}

	private static final class ConstantInitializer implements Initializer<Object> {
		private final Object initialValue;

		ConstantInitializer(Object initialValue) {
			this.initialValue = initialValue;
		}

		@Override
		public Object init() {
			return initialValue;
		}
	}

	private static final Map<Class<?>, Object> nullValue = new HashMap<Class<?>, Object>();
	static {
		nullValue.put(boolean.class, false);
		nullValue.put(byte.class, (byte) 0);
		nullValue.put(char.class, (char) 0);
		nullValue.put(short.class, (short) 0);
		nullValue.put(int.class, 0);
		nullValue.put(long.class, 0l);
		nullValue.put(float.class, 0f);
		nullValue.put(double.class, 0d);

		nullValue.put(boolean[].class, new boolean[0]);
		nullValue.put(byte[].class, new byte[0]);
		nullValue.put(char[].class, new char[0]);
		nullValue.put(short[].class, new short[0]);
		nullValue.put(int[].class, new int[0]);
		nullValue.put(long[].class, new long[0]);
		nullValue.put(float[].class, new float[0]);
		nullValue.put(double[].class, new double[0]);

		nullValue.put(Boolean[].class, new Boolean[0]);
		nullValue.put(Byte[].class, new Byte[0]);
		nullValue.put(Character[].class, new Character[0]);
		nullValue.put(Short[].class, new Short[0]);
		nullValue.put(Integer[].class, new Integer[0]);
		nullValue.put(Long[].class, new Long[0]);
		nullValue.put(Float[].class, new Float[0]);
		nullValue.put(Double[].class, new Double[0]);

		nullValue.put(String[].class, new String[0]);
		nullValue.put(File[].class, new File[0]);
	}

	private static final Map<Class<?>, Parser<?>> parsers = new HashMap<Class<?>, Parser<?>>();
	static {
		parsers.put(boolean.class, new BooleanParser());
		parsers.put(byte.class, new ByteParser());
		parsers.put(char.class, new CharParser());
		parsers.put(short.class, new ShortParser());
		parsers.put(int.class, new IntegerParser());
		parsers.put(long.class, new LongParser());
		parsers.put(float.class, new FloatParser());
		parsers.put(double.class, new DoubleParser());

		parsers.put(boolean[].class, new BooleanArrayParser());
		parsers.put(byte[].class, new ByteArrayParser());
		parsers.put(char[].class, new CharArrayParser());
		parsers.put(short[].class, new ShortArrayParser());
		parsers.put(int[].class, new IntegerArrayParser());
		parsers.put(long[].class, new LongArrayParser());
		parsers.put(float[].class, new FloatArrayParser());
		parsers.put(double[].class, new DoubleArrayParser());

		parsers.put(Boolean[].class, new BooleanWrapperArrayParser());
		parsers.put(Byte[].class, new ByteWrapperArrayParser());
		parsers.put(Character[].class, new CharWrapperArrayParser());
		parsers.put(Short[].class, new ShortWrapperArrayParser());
		parsers.put(Integer[].class, new IntegerWrapperArrayParser());
		parsers.put(Long[].class, new LongWrapperArrayParser());
		parsers.put(Float[].class, new FloatWrapperArrayParser());
		parsers.put(Double[].class, new DoubleWrapperArrayParser());

		parsers.put(Boolean.class, new BooleanWrapperParser());
		parsers.put(Byte.class, new ByteWrapperParser());
		parsers.put(Character.class, new CharWrapperParser());
		parsers.put(Short.class, new ShortWrapperParser());
		parsers.put(Integer.class, new IntegerWrapperParser());
		parsers.put(Long.class, new LongWrapperParser());
		parsers.put(Float.class, new FloatWrapperParser());
		parsers.put(Double.class, new DoubleWrapperParser());
		
		parsers.put(String.class, new StringParser());
		parsers.put(String[].class, new StringArrayParser());
		parsers.put(File.class, new FileParser());
		parsers.put(File[].class, new FileArrayParser());
		parsers.put(Date.class, new DateParser());
		parsers.put(Date[].class, new GenericCollectionParser<Date>(new GenericArrayType<Date>(Date.class), new DateParser()));
	}
	
	private static final Initializer<Object> NULL = new Initializer<Object>() {
		@Override
		public Object init() {
			return null;
		}
	};
	
	private static final Initializer<Object> NEW_LIST = new Initializer<Object>() {
		@Override
		public Object init() {
			return new ArrayList<Object>();
		}
	};
	
	private static final Initializer<Object> NEW_INDEX = new Initializer<Object>() {
		@Override
		public Object init() {
			return new HashMap<Object, Object>();
		}
	};

	private static final Parser<Object> NO_PARSER = new ObjectParser<Object>() {
		@Override
		public Object parse(String text) {
			throw new AssertionError("Only primitive types can be parsed.");
		}

		@Override
		public String unparse(Object value) {
			throw new AssertionError("Only primitive types can be serialized.");
		}
	};
	
	private final String name;
	private final int index;
	
	private MethodHandler getHandler;
	private MethodHandler setHandler;
	
	private Method getter;
	private Kind kind;
	private Class<?> type;
	private Property indexProperty;
	private Initializer<?> initializer;
	private Parser<Object> parser;

	private final ValueDescriptorImpl<?> descriptor;

	public PropertyImpl(ValueDescriptorImpl<?> descriptor, String propertyName, int index) {
		this.descriptor = descriptor;
		this.name = propertyName;
		this.index = index;
	}

	@Override
	public ValueDescriptorImpl<?> getDescriptor() {
		return descriptor;
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public Kind getKind() {
		return kind;
	}
	
	@Override
	public Class<?> getType() {
		return type;
	}

	@Override
	public Property getIndexProperty() {
		return indexProperty;
	}
	
	@Override
	public Parser<Object> getParser() {
		return parser;
	}
	
	int getIndex() {
		return index;
	}

	Method getGetter() {
		return getter;
	}
	
	void initGetter(Method proposedGetter) {
		if (this.getter != null) {
			if (this.getter.getDeclaringClass().isAssignableFrom(proposedGetter.getDeclaringClass())) {
				// Find a getter declared on a most general type.
				return;
			}
		}
		
		Class<?> accessType = proposedGetter.getReturnType();
		Type genericType = proposedGetter.getGenericReturnType();
		
		DefaultValue defaultAnnotation = proposedGetter.getAnnotation(DefaultValue.class);
		if (defaultAnnotation != null) {
			if (defaultAnnotation.initializer() != Initializer.class) {
				try {
					initializer = defaultAnnotation.initializer().newInstance();
				} catch (InstantiationException ex) {
					throw (AssertionError) new AssertionError("Cannot instantiate initializer.").initCause(ex);
				} catch (IllegalAccessException ex) {
					throw (AssertionError) new AssertionError("Cannot instantiate initializer.").initCause(ex);
				}
			}
			else if (accessType == boolean.class || accessType == Boolean.class) {
				initializer = new ConstantInitializer(defaultAnnotation.booleanValue());
			}
			else if (accessType == byte.class || accessType == Byte.class) {
				initializer = new ConstantInitializer(defaultAnnotation.byteValue());
			}
			else if (accessType == char.class || accessType == Character.class) {
				initializer = new ConstantInitializer(defaultAnnotation.charValue());
			}
			else if (accessType == short.class || accessType == Short.class) {
				initializer = new ConstantInitializer(defaultAnnotation.shortValue());
			}
			else if (accessType == int.class || accessType == Integer.class) {
				initializer = new ConstantInitializer(defaultAnnotation.intValue());
			}
			else if (accessType == long.class || accessType == Long.class) {
				initializer = new ConstantInitializer(defaultAnnotation.longValue());
			}
			else if (accessType == float.class || accessType == Float.class) {
				initializer = new ConstantInitializer(defaultAnnotation.floatValue());
			}
			else if (accessType == double.class || accessType == Double.class) {
				initializer = new ConstantInitializer(defaultAnnotation.doubleValue());
			}
			else if (accessType == String.class) {
				initializer = new ConstantInitializer(defaultAnnotation.stringValue());
			}
			else {
				throw new AssertionError("Invalid default value specification for property '" + getName() + "'.");
			}
		}
		
		ValueParser parserAnnotation = proposedGetter.getAnnotation(ValueParser.class);
		if (parserAnnotation != null) {
			this.kind = Kind.PRIMITIVE;
			
			this.type = accessType;
			this.getHandler = new GetHandler(index);
			this.setHandler = new SetHandler(index);
			
			try {
				this.parser = parserAnnotation.value().newInstance();
			} catch (InstantiationException ex) {
				throw (AssertionError)new AssertionError("Parser cannot be instantiated.").initCause(ex);
			} catch (IllegalAccessException ex) {
				throw (AssertionError)new AssertionError("Parser cannot be instantiated.").initCause(ex);
			}
			if (initializer == null) {
				this.initializer = parser;
			}
		}
		else if (proposedGetter.getAnnotation(Reference.class) != null) {
			this.kind = Kind.REFERENCE;
			this.type = accessType;
			
			if (type.isPrimitive()) {
				throw new AssertionError("A primitive value must not be marked as reference.");
			}
			
			this.getHandler = new GetHandler(index);
			this.setHandler = new SetHandler(index);
			if (initializer == null) {
				this.initializer = NULL;
			}
			
			this.parser = NO_PARSER;
		}
		else if (accessType == List.class) {
			Class<?> elementType = (Class<?>) ((ParameterizedType) genericType ).getActualTypeArguments()[0];
			Parser<?> elementParser = parsers.get(elementType);
			if (elementParser != null) {
				// List of primitives.
				this.kind = Kind.PRIMITIVE;
				this.type = accessType;
				this.getHandler = new GetHandler(index);
				if (initializer == null) {
					this.initializer = NEW_LIST;
				}
				this.setHandler = new NonNullSetHandler(index, initializer);
				this.parser = new GenericCollectionParser(ListType.INSTANCE, elementParser);
			} else {
				this.kind = Kind.LIST;
				this.type = elementType;
				if (initializer == null) {
					this.initializer = NEW_LIST;
				}
				
				this.getHandler = new GetHandler(index);
				this.setHandler = new NonNullSetHandler(index, initializer);
				
				this.parser = NO_PARSER;
			}
		}
		else if (accessType == Map.class) {
			this.kind = Kind.INDEX;
			this.type = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[1];
			if (initializer == null) {
				this.initializer = NEW_INDEX;
			}
			
			this.getHandler = new GetHandler(index);
			this.setHandler = new NonNullSetHandler(index, initializer);
			
			IndexProperty indexAnnotation = proposedGetter.getAnnotation(IndexProperty.class);
			if (indexAnnotation == null) {
				throw new AssertionError("Indexed property requires an '" + IndexProperty.class.getName() + "' annotation.");
			}
			
			this.indexProperty = ValueFactory.getDescriptor(type).getProperties().get(indexAnnotation.value());

			this.parser = NO_PARSER;
		}
		else if (parsers.get(accessType) != null) {
			this.kind = Kind.PRIMITIVE;
			this.type = accessType;
			this.getHandler = new GetHandler(index);
			if (initializer == null) {
				this.initializer = new ConstantInitializer(nullValue.get(accessType));
			}
			this.setHandler = new NonNullSetHandler(index, initializer);
			this.parser = (Parser) parsers.get(accessType);
		}
		else {
			this.kind = Kind.VALUE;
			this.type = accessType;
			this.getHandler = new GetHandler(index);
			if (initializer == null) {
				this.initializer = new ValueInitializer(ValueFactory.getDescriptor(type));
			}
			this.setHandler = new NonNullSetHandler(index, initializer);
			this.parser = NO_PARSER;
		}
		
		this.getter = proposedGetter;
	}
	
	Initializer<?> getInitializer() {
		return initializer;
	}

	MethodHandler getGetHandler() {
		return getHandler;
	}
	
	MethodHandler getSetHandler() {
		return setHandler;
	}
	
}
