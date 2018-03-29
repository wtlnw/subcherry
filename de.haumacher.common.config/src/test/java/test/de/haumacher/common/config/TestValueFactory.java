package test.de.haumacher.common.config;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;
import test.de.haumacher.common.config.TestValueFactory.AllTypes.Nested;
import de.haumacher.common.config.DefaultValue;
import de.haumacher.common.config.IndexProperty;
import de.haumacher.common.config.PropertiesUtil;
import de.haumacher.common.config.Property;
import de.haumacher.common.config.Value;
import de.haumacher.common.config.ValueFactory;

public class TestValueFactory extends TestCase {

	public interface AllTypes extends Value {
		
		// Primitive types
		
		boolean getBooleanValue();
		void setBooleanValue(boolean value);
		
		byte getByteValue();
		void setByteValue(byte value);
		
		short getShortValue();
		void setShortValue(short value);
		
		int getIntValue();
		void setIntValue(int value);
		
		char getCharValue();
		void setCharValue(char value);
		
		float getFloatValue();
		void setFloatValue(float value);
		
		long getLongValue();
		void setLongValue(long value);
		
		double getDoubleValue();
		void setDoubleValue(double value);
		
		// Primitive array types

		boolean[] getBooleanArrayValue();

		void setBooleanArrayValue(boolean[] value);

		byte[] getByteArrayValue();

		void setByteArrayValue(byte[] value);

		short[] getShortArrayValue();

		void setShortArrayValue(short[] value);

		int[] getIntArrayValue();

		void setIntArrayValue(int[] value);

		char[] getCharArrayValue();

		void setCharArrayValue(char[] value);

		float[] getFloatArrayValue();

		void setFloatArrayValue(float[] value);

		long[] getLongArrayValue();

		void setLongArrayValue(long[] value);

		double[] getDoubleArrayValue();

		void setDoubleArrayValue(double[] value);

		// Primitive types with default values
		
		@DefaultValue(booleanValue=true)
		boolean getBooleanDefaultValue();
		void setBooleanDefaultValue(boolean value);
		
		@DefaultValue(byteValue=10)
		byte getByteDefaultValue();
		void setByteDefaultValue(byte value);
		
		@DefaultValue(shortValue=11)
		short getShortDefaultValue();
		void setShortDefaultValue(short value);
		
		@DefaultValue(intValue=12)
		int getIntDefaultValue();
		void setIntDefaultValue(int value);
		
		@DefaultValue(charValue='X')
		char getCharDefaultValue();
		void setCharDefaultValue(char value);
		
		@DefaultValue(floatValue=13.1F)
		float getFloatDefaultValue();
		void setFloatDefaultValue(float value);
		
		@DefaultValue(longValue=14L)
		long getLongDefaultValue();
		void setLongDefaultValue(long value);
		
		@DefaultValue(doubleValue=15.1)
		double getDoubleDefaultValue();
		void setDoubleDefaultValue(double value);
		
		// Primitive wrapper types
		
		Boolean getBooleanObject();
		void setBooleanObject(Boolean value);
		
		Byte getByteObject();
		void setByteObject(Byte value);
		
		Short getShortObject();
		void setShortObject(Short value);
		
		Integer getIntegerObject();
		void setIntegerObject(Integer value);
		
		Character getCharacterObject();
		void setCharacterObject(Character value);
		
		Float getFloatObject();
		void setFloatObject(Float value);
		
		Long getLongObject();
		void setLongObject(Long value);
		
		Double getDoubleObject();
		void setDoubleObject(Double value);

		// Primitive wrapper types

		Boolean[] getBooleanObjectArray();

		void setBooleanObjectArray(Boolean[] value);

		Byte[] getByteObjectArray();

		void setByteObjectArray(Byte[] value);

		Short[] getShortObjectArray();

		void setShortObjectArray(Short[] value);

		Integer[] getIntegerObjectArray();

		void setIntegerObjectArray(Integer[] value);

		Character[] getCharacterObjectArray();

		void setCharacterObjectArray(Character[] value);

		Float[] getFloatObjectArray();

		void setFloatObjectArray(Float[] value);

		Long[] getLongObjectArray();

		void setLongObjectArray(Long[] value);

		Double[] getDoubleObjectArray();

		void setDoubleObjectArray(Double[] value);

		// Primitive wrapper types with default values
		
		@DefaultValue(booleanValue=true)
		Boolean getBooleanDefaultObject();
		void setBooleanDefaultObject(Boolean value);
		
		@DefaultValue(byteValue=20)
		Byte getByteDefaultObject();
		void setByteDefaultObject(Byte value);
		
		@DefaultValue(shortValue=21)
		Short getShortDefaultObject();
		void setShortDefaultObject(Short value);
		
		@DefaultValue(intValue=22)
		Integer getIntDefaultObject();
		void setIntDefaultObject(Integer value);
		
		@DefaultValue(charValue='Y')
		Character getCharDefaultObject();
		void setCharDefaultObject(Character value);
		
		@DefaultValue(floatValue=23.1F)
		Float getFloatDefaultObject();
		void setFloatDefaultObject(Float value);
		
		@DefaultValue(longValue=24L)
		Long getLongDefaultObject();
		void setLongDefaultObject(Long value);
		
		@DefaultValue(doubleValue=25.1)
		Double getDoubleDefaultObject();
		void setDoubleDefaultObject(Double value);
				
		// Java built-in types
		
		String getStringValue();
		void setStringValue(String value);
		
		String[] getStringArrayValue();

		void setStringArrayValue(String[] value);

		@DefaultValue(stringValue="foobar")
		String getStringDefaultValue();
		void setStringDefaultValue(String value);
		
		File getFileValue();

		void setFileValue(File value);

		File[] getFileArrayValue();

		void setFileArrayValue(File[] value);

		// Nested values
		
		interface Nested extends Value {
			String getName();
			
			@DefaultValue(intValue=99)
			int getX();
			void setX(int value);
		}
		
		Nested getNestedValue();
		List<Nested> getNestedList();
		
		@IndexProperty("name")
		Map<String, Nested> getNestedMap();
		
	}
	
	public void testDefaultValues() {
		AllTypes a1 = newBasicTypesWithDefaultValues();
		checkDefaultValues(a1);
	}

	private void checkDefaultValues(AllTypes a1) {
		assertEquals(false, a1.getBooleanValue());
		assertEquals((byte)0, a1.getByteValue());
		assertEquals((char)0, a1.getCharValue());
		assertEquals((short)0, a1.getShortValue());
		assertEquals(0, a1.getIntValue());
		assertEquals(0.0F, a1.getFloatValue());
		assertEquals(0L, a1.getLongValue());
		assertEquals(0.0, a1.getDoubleValue());
		
		assertEquals(boolean[].class, a1.getBooleanArrayValue().getClass());
		assertEquals(byte[].class, a1.getByteArrayValue().getClass());
		assertEquals(char[].class, a1.getCharArrayValue().getClass());
		assertEquals(short[].class, a1.getShortArrayValue().getClass());
		assertEquals(int[].class, a1.getIntArrayValue().getClass());
		assertEquals(float[].class, a1.getFloatArrayValue().getClass());
		assertEquals(long[].class, a1.getLongArrayValue().getClass());
		assertEquals(double[].class, a1.getDoubleArrayValue().getClass());

		assertEquals(Boolean[].class, a1.getBooleanObjectArray().getClass());
		assertEquals(Byte[].class, a1.getByteObjectArray().getClass());
		assertEquals(Character[].class, a1.getCharacterObjectArray().getClass());
		assertEquals(Short[].class, a1.getShortObjectArray().getClass());
		assertEquals(Integer[].class, a1.getIntegerObjectArray().getClass());
		assertEquals(Float[].class, a1.getFloatObjectArray().getClass());
		assertEquals(Long[].class, a1.getLongObjectArray().getClass());
		assertEquals(Double[].class, a1.getDoubleObjectArray().getClass());

		assertEquals(true, a1.getBooleanDefaultValue());
		assertEquals((byte)10, a1.getByteDefaultValue());
		assertEquals('X', a1.getCharDefaultValue());
		assertEquals((short)11, a1.getShortDefaultValue());
		assertEquals(12, a1.getIntDefaultValue());
		assertEquals(13.1F, a1.getFloatDefaultValue());
		assertEquals(14L, a1.getLongDefaultValue());
		assertEquals(15.1, a1.getDoubleDefaultValue());
		
		assertEquals(null, a1.getBooleanObject());
		assertEquals(null, a1.getByteObject());
		assertEquals(null, a1.getCharacterObject());
		assertEquals(null, a1.getShortObject());
		assertEquals(null, a1.getIntegerObject());
		assertEquals(null, a1.getFloatObject());
		assertEquals(null, a1.getLongObject());
		assertEquals(null, a1.getDoubleObject());
		
		assertEquals(Boolean.valueOf(true), a1.getBooleanDefaultObject());
		assertEquals(Byte.valueOf((byte)20), a1.getByteDefaultObject());
		assertEquals(Character.valueOf('Y'), a1.getCharDefaultObject());
		assertEquals(Short.valueOf((short)21), a1.getShortDefaultObject());
		assertEquals(Integer.valueOf(22), a1.getIntDefaultObject());
		assertEquals(Float.valueOf(23.1F), a1.getFloatDefaultObject());
		assertEquals(Long.valueOf(24L), a1.getLongDefaultObject());
		assertEquals(Double.valueOf(25.1), a1.getDoubleDefaultObject());
		
		assertEquals(null, a1.getStringValue());
		assertEquals("foobar", a1.getStringDefaultValue());
		
		assertEquals(Arrays.asList(), Arrays.asList(a1.getStringArrayValue()));

		assertEquals(null, a1.getFileValue());
		assertEquals(Arrays.asList(), Arrays.asList(a1.getFileArrayValue()));

		assertEquals(newNested(null), a1.getNestedValue());
		assertEquals(Collections.emptyList(), a1.getNestedList());
		assertEquals(Collections.emptyMap(), a1.getNestedMap());
	}
	
	public void testSetters() {
		AllTypes a1 = newBasicTypesWithCustomValues();
		
		assertEquals(true, a1.getBooleanValue());
		assertEquals((byte)3, a1.getByteValue());
		assertEquals((char)'A', a1.getCharValue());
		assertEquals((short) 5, a1.getShortValue());
		assertEquals(7, a1.getIntValue());
		assertEquals(13.3F, a1.getFloatValue());
		assertEquals(42L, a1.getLongValue());
		assertEquals(1.234, a1.getDoubleValue());
		
		assertEquals(Boolean.FALSE, a1.getBooleanObject());
		assertEquals(Byte.valueOf((byte) 4), a1.getByteObject());
		assertEquals(Character.valueOf('B'), a1.getCharacterObject());
		assertEquals(Short.valueOf((short) 6), a1.getShortObject());
		assertEquals(Integer.valueOf(8), a1.getIntegerObject());
		assertEquals(14.4F, a1.getFloatObject());
		assertEquals(Long.valueOf(43L), a1.getLongObject());
		assertEquals(2.345, a1.getDoubleObject());
		
		assertEquals("Hello world!", a1.getStringValue());
		assertEquals(Arrays.asList("foo", "bar"), Arrays.asList(a1.getStringArrayValue()));
		
		assertEquals(".", a1.getFileValue().getPath());
		assertEquals(Arrays.asList(new File("."), new File("..")), Arrays.asList(a1.getFileArrayValue()));
	}

	private AllTypes newBasicTypesWithCustomValues() {
		AllTypes a1 = newBasicTypesWithDefaultValues();
		a1.setBooleanValue(true);
		a1.setByteValue((byte) 3);
		a1.setCharValue('A');
		a1.setShortValue((short) 5);
		a1.setIntValue(7);
		a1.setFloatValue(13.3f);
		a1.setLongValue(42L);
		a1.setDoubleValue(1.234);
		
		a1.setBooleanArrayValue(new boolean[] { true, false });
		a1.setByteArrayValue(new byte[] { 1, 2, 3 });
		a1.setCharArrayValue(new char[] { 'A', 'B', 'C' });
		a1.setShortArrayValue(new short[] { 4, 5, 6 });
		a1.setIntArrayValue(new int[] { 13, 42 });
		a1.setFloatArrayValue(new float[] { 13.3f, 42.2f });
		a1.setLongArrayValue(new long[] { 99L, 999L, 9999L });
		a1.setDoubleArrayValue(new double[] { 1.234, 12.34 });

		a1.setBooleanObjectArray(new Boolean[] { true, false });
		a1.setByteObjectArray(new Byte[] { 1, 2, 3 });
		a1.setCharacterObjectArray(new Character[] { 'A', 'B', 'C' });
		a1.setShortObjectArray(new Short[] { 4, 5, 6 });
		a1.setIntegerObjectArray(new Integer[] { 13, 42 });
		a1.setFloatObjectArray(new Float[] { 13.3f, 42.2f });
		a1.setLongObjectArray(new Long[] { 99L, 999L, 9999L });
		a1.setDoubleObjectArray(new Double[] { 1.234, 12.34 });

		a1.setBooleanObject(false);
		a1.setByteObject((byte) 4);
		a1.setCharacterObject('B');
		a1.setShortObject((short)6);
		a1.setIntegerObject(8);
		a1.setFloatObject(14.4f);
		a1.setLongObject(43L);
		a1.setDoubleObject(2.345);
		
		a1.setStringValue("Hello world!");
		a1.setFileValue(new File("."));
		
		a1.setStringArrayValue(new String[] { "foo", "bar" });
		a1.setFileArrayValue(new File[] { new File("."), new File("..") });

		a1.getNestedValue().setX(100);
		a1.getNestedList().add(newNested("v0"));
		a1.getNestedMap().put("v1", newNested("v1"));
		a1.getNestedMap().put("v2", newNested("v2"));
		
		return a1;
	}

	private Nested newNested(String name) {
		Nested result = ValueFactory.newInstance(Nested.class);
		result.putValue(result.descriptor().getProperties().get("name"), name);
		return result;
	}
	
	public void testEqualsWithDefaultValues() {
		AllTypes a1 = newBasicTypesWithDefaultValues();
		AllTypes a2 = newBasicTypesWithDefaultValues();
		checkEqualsAndHashCode(a1, a2);
		checkEqualsAndHashCode(a1, a1);
		checkNotEquals(a1, null);
		checkNotEquals(a1, "Any other value");
	}

	private AllTypes newBasicTypesWithDefaultValues() {
		AllTypes a1 = ValueFactory.newInstance(AllTypes.class);
		return a1;
	}
	
	public void testEqualsWithCustomValues() {
		AllTypes a1 = newBasicTypesWithCustomValues();
		AllTypes a2 = newBasicTypesWithCustomValues();
		checkEqualsAndHashCode(a1, a2);
	}
	
	public void testEqualsWithResetValues() {
		AllTypes a1 = reset(newBasicTypesWithCustomValues());
		AllTypes a2 = newBasicTypesWithDefaultValues();
		checkEqualsAndHashCode(a1, a2);
	}

	private <V extends Value> V reset(V value) {
		for (Property property : value.descriptor().getProperties().values()) {
			value.putValue(property, null);
		}
		return value;
	}

	private void checkEqualsAndHashCode(Object a1, Object a2) {
		assertEquals(a1, a2);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	public void testEqualsDetectsDifference() {
		AllTypes a1 = newBasicTypesWithCustomValues();
		AllTypes a2 = newBasicTypesWithCustomValues();

		a1.setBooleanValue(false);
		checkNotEquals(a1, a2);
		
		a2.setBooleanValue(false);
		checkEqualsAndHashCode(a1, a2);
		
		a1.setIntValue(1);
		checkNotEquals(a1, a2);
		
		a2.setIntValue(1);
		checkEqualsAndHashCode(a1, a2);
		
		a1.setBooleanObject(true);
		checkNotEquals(a1, a2);
		
		a2.setBooleanObject(true);
		checkEqualsAndHashCode(a1, a2);
		
		a1.setIntegerObject(1);
		checkNotEquals(a1, a2);
		
		a2.setIntegerObject(1);
		checkEqualsAndHashCode(a1, a2);
		
		a1.setStringValue("Foobar");
		checkNotEquals(a1, a2);
		
		a2.setStringValue("Foobar");
		checkEqualsAndHashCode(a1, a2);
		
		a1.setFileValue(new File(".."));
		checkNotEquals(a1, a2);

		a2.setFileValue(new File(".."));
		checkEqualsAndHashCode(a1, a2);

		a1.getNestedValue().setX(200);
		checkNotEquals(a1, a2);
		
		a2.getNestedValue().setX(200);
		checkEqualsAndHashCode(a1, a2);
		
		a1.getNestedList().add(newNested("new1"));
		checkNotEquals(a1, a2);
		
		a2.getNestedList().add(newNested("new1"));
		checkEqualsAndHashCode(a1, a2);
		
		a1.getNestedMap().get("v1").setX(300);
		checkNotEquals(a1, a2);
		
		a2.getNestedMap().get("v1").setX(300);
		checkEqualsAndHashCode(a1, a2);
		
	}
	
	private void checkNotEquals(Object a1, Object a2) {
		assertFalse(a1.equals(a2));
	}
	
	public void testPropertyBindingWithDefaultValues() {
		AllTypes a1 = newBasicTypesWithDefaultValues();
		checkSaveLoad(a1);
	}
	
	public void testPropertyBindingWithCustomValues() {
		AllTypes a1 = newBasicTypesWithCustomValues();
		checkSaveLoad(a1);
	}

	private void checkSaveLoad(AllTypes a1) {
		Properties buffer = new Properties();
		PropertiesUtil.save(buffer, a1);
		AllTypes a2 = PropertiesUtil.load(buffer, AllTypes.class);
		checkEqualsAndHashCode(a1, a2);
	}
	
	public interface A extends Value {
		
		int getX();
		int getY();
		
	}
	
	public interface B extends A {
		
		int getZ();
		
	}
	
	public void testInheritance() {
		B b = newBWithCustomValues();
		
		assertEquals(13, b.getX());
		assertEquals(42, b.getY());
		assertEquals(255, b.getZ());
		
		assertEquals(13, b.value(b.descriptor().getProperties().get("x")));
		assertEquals(42, b.value(b.descriptor().getProperties().get("y")));
		assertEquals(255, b.value(b.descriptor().getProperties().get("z")));
		
		A a = b;
		assertEquals(13, a.getX());
		assertEquals(42, a.getY());

		assertEquals(13, a.value(a.descriptor().getProperties().get("x")));
		assertEquals(42, a.value(a.descriptor().getProperties().get("y")));
	}

	private B newBWithCustomValues() {
		B b = ValueFactory.newInstance(B.class);
		
		b.putValue(b.descriptor().getProperties().get("x"), 13);
		b.putValue(b.descriptor().getProperties().get("y"), 42);
		b.putValue(b.descriptor().getProperties().get("z"), 255);
		return b;
	}
	
	public void testToString() {
		B b = newBWithCustomValues();
		assertEquals("test.de.haumacher.common.config.TestValueFactory$B{z: 255; y: 42; x: 13}", b.toString());
	}
	
	public interface BooleanGetters {
		boolean getBoolean();
		boolean isValueSet();
		boolean canSetValue();
		boolean hasValue();
		boolean mustValueBeSet();
	}
	
	public void testBooleanGetterNames() {
		BooleanGetters b = ValueFactory.newInstance(BooleanGetters.class);
		assertFalse(b.getBoolean());
		assertFalse(b.isValueSet());
		assertFalse(b.canSetValue());
		assertFalse(b.hasValue());
		assertFalse(b.mustValueBeSet());
	}
	
}
