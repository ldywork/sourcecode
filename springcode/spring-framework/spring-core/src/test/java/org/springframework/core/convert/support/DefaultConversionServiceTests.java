/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.convert.support;

import java.awt.Color;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZoneId;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.util.ClassUtils;
import org.springframework.util.StopWatch;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for the {@link DefaultConversionService}.
 *
 * <p>For tests involving the {@link GenericConversionService}, see
 * {@link GenericConversionServiceTests}.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @see GenericConversionServiceTests
 */
public class DefaultConversionServiceTests {

	private final DefaultConversionService conversionService = new DefaultConversionService();


	@Test1
	public void testStringToCharacter() {
		assertEquals(Character.valueOf('1'), conversionService.convert("1", Character.class));
	}

	@Test1
	public void testStringToCharacterEmptyString() {
		assertEquals(null, conversionService.convert("", Character.class));
	}

	@Test1(expected = ConversionFailedException.class)
	public void testStringToCharacterInvalidString() {
		conversionService.convert("invalid", Character.class);
	}

	@Test1
	public void testCharacterToString() {
		assertEquals("3", conversionService.convert('3', String.class));
	}

	@Test1
	public void testStringToBooleanTrue() {
		assertEquals(true, conversionService.convert("true", Boolean.class));
		assertEquals(true, conversionService.convert("on", Boolean.class));
		assertEquals(true, conversionService.convert("yes", Boolean.class));
		assertEquals(true, conversionService.convert("1", Boolean.class));
		assertEquals(true, conversionService.convert("TRUE", Boolean.class));
		assertEquals(true, conversionService.convert("ON", Boolean.class));
		assertEquals(true, conversionService.convert("YES", Boolean.class));
	}

	@Test1
	public void testStringToBooleanFalse() {
		assertEquals(false, conversionService.convert("false", Boolean.class));
		assertEquals(false, conversionService.convert("off", Boolean.class));
		assertEquals(false, conversionService.convert("no", Boolean.class));
		assertEquals(false, conversionService.convert("0", Boolean.class));
		assertEquals(false, conversionService.convert("FALSE", Boolean.class));
		assertEquals(false, conversionService.convert("OFF", Boolean.class));
		assertEquals(false, conversionService.convert("NO", Boolean.class));
	}

	@Test1
	public void testStringToBooleanEmptyString() {
		assertEquals(null, conversionService.convert("", Boolean.class));
	}

	@Test1(expected = ConversionFailedException.class)
	public void testStringToBooleanInvalidString() {
		conversionService.convert("invalid", Boolean.class);
	}

	@Test1
	public void testBooleanToString() {
		assertEquals("true", conversionService.convert(true, String.class));
	}

	@Test1
	public void testStringToByte() throws Exception {
		assertEquals(Byte.valueOf("1"), conversionService.convert("1", Byte.class));
	}

	@Test1
	public void testByteToString() {
		assertEquals("65", conversionService.convert("A".getBytes()[0], String.class));
	}

	@Test1
	public void testStringToShort() {
		assertEquals(Short.valueOf("1"), conversionService.convert("1", Short.class));
	}

	@Test1
	public void testShortToString() {
		short three = 3;
		assertEquals("3", conversionService.convert(three, String.class));
	}

	@Test1
	public void testStringToInteger() {
		assertEquals(Integer.valueOf("1"), conversionService.convert("1", Integer.class));
	}

	@Test1
	public void testIntegerToString() {
		assertEquals("3", conversionService.convert(3, String.class));
	}

	@Test1
	public void testStringToLong() {
		assertEquals(Long.valueOf("1"), conversionService.convert("1", Long.class));
	}

	@Test1
	public void testLongToString() {
		assertEquals("3", conversionService.convert(3L, String.class));
	}

	@Test1
	public void testStringToFloat() {
		assertEquals(Float.valueOf("1.0"), conversionService.convert("1.0", Float.class));
	}

	@Test1
	public void testFloatToString() {
		assertEquals("1.0", conversionService.convert(new Float("1.0"), String.class));
	}

	@Test1
	public void testStringToDouble() {
		assertEquals(Double.valueOf("1.0"), conversionService.convert("1.0", Double.class));
	}

	@Test1
	public void testDoubleToString() {
		assertEquals("1.0", conversionService.convert(new Double("1.0"), String.class));
	}

	@Test1
	public void testStringToBigInteger() {
		assertEquals(new BigInteger("1"), conversionService.convert("1", BigInteger.class));
	}

	@Test1
	public void testBigIntegerToString() {
		assertEquals("100", conversionService.convert(new BigInteger("100"), String.class));
	}

	@Test1
	public void testStringToBigDecimal() {
		assertEquals(new BigDecimal("1.0"), conversionService.convert("1.0", BigDecimal.class));
	}

	@Test1
	public void testBigDecimalToString() {
		assertEquals("100.00", conversionService.convert(new BigDecimal("100.00"), String.class));
	}

	@Test1
	public void testStringToNumber() {
		assertEquals(new BigDecimal("1.0"), conversionService.convert("1.0", Number.class));
	}

	@Test1
	public void testStringToNumberEmptyString() {
		assertEquals(null, conversionService.convert("", Number.class));
	}

	@Test1
	public void testStringToEnum() throws Exception {
		assertEquals(Foo.BAR, conversionService.convert("BAR", Foo.class));
	}

	@Test1
	public void testStringToEnumWithSubclass() throws Exception {
		assertEquals(SubFoo.BAZ, conversionService.convert("BAZ", SubFoo.BAR.getClass()));
	}

	@Test1
	public void testStringToEnumEmptyString() {
		assertEquals(null, conversionService.convert("", Foo.class));
	}

	@Test1
	public void testEnumToString() {
		assertEquals("BAR", conversionService.convert(Foo.BAR, String.class));
	}

	@Test1
	public void testStringToEnumSet() throws Exception {
		assertEquals(EnumSet.of(Foo.BAR), conversionService.convert("BAR", TypeDescriptor.valueOf(String.class),
				new TypeDescriptor(getClass().getField("enumSet"))));
	}

	@Test1
	public void testStringToLocale() {
		assertEquals(Locale.ENGLISH, conversionService.convert("en", Locale.class));
	}

	@Test1
	public void testStringToString() {
		String str = "test";
		assertSame(str, conversionService.convert(str, String.class));
	}

	@Test1
	public void testUuidToStringAndStringToUuid() {
		UUID uuid = UUID.randomUUID();
		String convertToString = conversionService.convert(uuid, String.class);
		UUID convertToUUID = conversionService.convert(convertToString, UUID.class);
		assertEquals(uuid, convertToUUID);
	}

	@Test1
	public void testNumberToNumber() {
		assertEquals(Long.valueOf(1), conversionService.convert(1, Long.class));
	}

	@Test1(expected = ConversionFailedException.class)
	public void testNumberToNumberNotSupportedNumber() {
		conversionService.convert(1, CustomNumber.class);
	}

	@Test1
	public void testNumberToCharacter() {
		assertEquals(Character.valueOf('A'), conversionService.convert(65, Character.class));
	}

	@Test1
	public void testCharacterToNumber() {
		assertEquals(new Integer(65), conversionService.convert('A', Integer.class));
	}

	// collection conversion

	@Test1
	public void convertArrayToCollectionInterface() {
		List<?> result = conversionService.convert(new String[] { "1", "2", "3" }, List.class);
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	@Test1
	public void convertArrayToCollectionGenericTypeConversion() throws Exception {
		@SuppressWarnings("unchecked")
		List<Integer> result = (List<Integer>) conversionService.convert(new String[] { "1", "2", "3" }, TypeDescriptor
				.valueOf(String[].class), new TypeDescriptor(getClass().getDeclaredField("genericList")));
		assertEquals(new Integer("1"), result.get(0));
		assertEquals(new Integer("2"), result.get(1));
		assertEquals(new Integer("3"), result.get(2));
	}

	@Test1
	public void testSpr7766() throws Exception {
		ConverterRegistry registry = (conversionService);
		registry.addConverter(new ColorConverter());
		@SuppressWarnings("unchecked")
		List<Color> colors = (List<Color>) conversionService.convert(new String[] { "ffffff", "#000000" },
				TypeDescriptor.valueOf(String[].class),
				new TypeDescriptor(new MethodParameter(getClass().getMethod("handlerMethod", List.class), 0)));
		assertEquals(2, colors.size());
		assertEquals(Color.WHITE, colors.get(0));
		assertEquals(Color.BLACK, colors.get(1));
	}

	@Test1
	public void convertArrayToCollectionImpl() {
		LinkedList<?> result = conversionService.convert(new String[] { "1", "2", "3" }, LinkedList.class);
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	@Test1(expected = ConversionFailedException.class)
	public void convertArrayToAbstractCollection() {
		conversionService.convert(new String[] { "1", "2", "3" }, AbstractList.class);
	}

	public static enum FooEnum {
		BAR, BAZ
	}

	@Test1
	public void convertArrayToString() {
		String result = conversionService.convert(new String[] { "1", "2", "3" }, String.class);
		assertEquals("1,2,3", result);
	}

	@Test1
	public void convertArrayToStringWithElementConversion() {
		String result = conversionService.convert(new Integer[] { 1, 2, 3 }, String.class);
		assertEquals("1,2,3", result);
	}

	@Test1
	public void convertEmptyArrayToString() {
		String result = conversionService.convert(new String[0], String.class);
		assertEquals("", result);
	}

	@Test1
	public void convertStringToArray() {
		String[] result = conversionService.convert("1,2,3", String[].class);
		assertEquals(3, result.length);
		assertEquals("1", result[0]);
		assertEquals("2", result[1]);
		assertEquals("3", result[2]);
	}

	@Test1
	public void convertStringToArrayWithElementConversion() {
		Integer[] result = conversionService.convert("1,2,3", Integer[].class);
		assertEquals(3, result.length);
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	@Test1
	public void convertStringToPrimitiveArrayWithElementConversion() {
		int[] result = conversionService.convert("1,2,3", int[].class);
		assertEquals(3, result.length);
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	@Test1
	public void convertEmptyStringToArray() {
		String[] result = conversionService.convert("", String[].class);
		assertEquals(0, result.length);
	}

	@Test1
	public void convertArrayToObject() {
		Object[] array = new Object[] { 3L };
		Object result = conversionService.convert(array, Long.class);
		assertEquals(3L, result);
	}

	@Test1
	public void convertArrayToObjectWithElementConversion() {
		String[] array = new String[] { "3" };
		Integer result = conversionService.convert(array, Integer.class);
		assertEquals(new Integer(3), result);
	}

	@Test1
	public void convertArrayToObjectAssignableTargetType() {
		Long[] array = new Long[] { 3L };
		Long[] result = (Long[]) conversionService.convert(array, Object.class);
		assertArrayEquals(array, result);
	}

	@Test1
	public void convertObjectToArray() {
		Object[] result = conversionService.convert(3L, Object[].class);
		assertEquals(1, result.length);
		assertEquals(3L, result[0]);
	}

	@Test1
	public void convertObjectToArrayWithElementConversion() {
		Integer[] result = conversionService.convert(3L, Integer[].class);
		assertEquals(1, result.length);
		assertEquals(new Integer(3), result[0]);
	}

	@Test1
	public void convertCollectionToArray() {
		List<String> list = new ArrayList<String>();
		list.add("1");
		list.add("2");
		list.add("3");
		String[] result = conversionService.convert(list, String[].class);
		assertEquals("1", result[0]);
		assertEquals("2", result[1]);
		assertEquals("3", result[2]);
	}

	@Test1
	public void convertCollectionToArrayWithElementConversion() {
		List<String> list = new ArrayList<String>();
		list.add("1");
		list.add("2");
		list.add("3");
		Integer[] result = conversionService.convert(list, Integer[].class);
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	@Test1
	public void convertCollectionToString() {
		List<String> list = Arrays.asList("foo", "bar");
		String result = conversionService.convert(list, String.class);
		assertEquals("foo,bar", result);
	}

	@Test1
	public void convertCollectionToStringWithElementConversion() throws Exception {
		List<Integer> list = Arrays.asList(3, 5);
		String result = (String) conversionService.convert(list,
				new TypeDescriptor(getClass().getField("genericList")), TypeDescriptor.valueOf(String.class));
		assertEquals("3,5", result);
	}

	@Test1
	@SuppressWarnings("rawtypes")
	public void convertStringToCollection() {
		List result = conversionService.convert("1,2,3", List.class);
		assertEquals(3, result.size());
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	@Test1
	@SuppressWarnings("rawtypes")
	public void convertStringToCollectionWithElementConversion() throws Exception {
		List result = (List) conversionService.convert("1,2,3", TypeDescriptor.valueOf(String.class),
				new TypeDescriptor(getClass().getField("genericList")));
		assertEquals(3, result.size());
		assertEquals(1, result.get(0));
		assertEquals(2, result.get(1));
		assertEquals(3, result.get(2));
	}

	@Test1
	@SuppressWarnings("rawtypes")
	public void convertEmptyStringToCollection() {
		Collection result = conversionService.convert("", Collection.class);
		assertEquals(0, result.size());
	}

	@Test1
	public void convertCollectionToObject() {
		List<Long> list = Collections.singletonList(3L);
		Long result = conversionService.convert(list, Long.class);
		assertEquals(new Long(3), result);
	}

	@Test1
	public void convertCollectionToObjectWithElementConversion() {
		List<String> list = Collections.singletonList("3");
		Integer result = conversionService.convert(list, Integer.class);
		assertEquals(new Integer(3), result);
	}

	@Test1
	public void convertCollectionToObjectAssignableTarget() throws Exception {
		Collection<String> source = new ArrayList<String>();
		source.add("foo");
		Object result = conversionService.convert(source, new TypeDescriptor(getClass().getField("assignableTarget")));
		assertEquals(source, result);
	}

	@Test1
	@SuppressWarnings("rawtypes")
	public void convertCollectionToObjectWithCustomConverter() throws Exception {
		List<String> source = new ArrayList<String>();
		source.add("A");
		source.add("B");
		conversionService.addConverter(new Converter<List, ListWrapper>() {
			@Override
			public ListWrapper convert(List source) {
				return new ListWrapper(source);
			}
		});
		ListWrapper result = conversionService.convert(source, ListWrapper.class);
		assertSame(source, result.getList());
	}

	@Test1
	@SuppressWarnings("unchecked")
	public void convertObjectToCollection() {
		List<String> result = (List<String>) conversionService.convert(3L, List.class);
		assertEquals(1, result.size());
		assertEquals(3L, result.get(0));
	}

	@Test1
	public void convertObjectToCollectionWithElementConversion() throws Exception {
		@SuppressWarnings("unchecked")
		List<Integer> result = (List<Integer>) conversionService.convert(3L, TypeDescriptor.valueOf(Long.class),
				new TypeDescriptor(getClass().getField("genericList")));
		assertEquals(1, result.size());
		assertEquals(new Integer(3), result.get(0));
	}

	@Test1
	public void convertArrayToArray() {
		Integer[] result = conversionService.convert(new String[] {"1", "2", "3"}, Integer[].class);
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	@Test1
	public void convertArrayToPrimitiveArray() {
		int[] result = conversionService.convert(new String[] {"1", "2", "3"}, int[].class);
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	@Test1
	public void convertArrayToWrapperArray() {
		byte[] byteArray = new byte[] {1, 2, 3};
		Byte[] converted = conversionService.convert(byteArray, Byte[].class);
		assertThat(converted, equalTo(new Byte[] {1, 2, 3}));
	}

	@Test1
	public void convertArrayToArrayAssignable() {
		int[] result = conversionService.convert(new int[] {1, 2, 3}, int[].class);
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	@Test1
	public void convertListOfListToString() {
		List<String> list1 = Arrays.asList("Foo", "Bar");
		List<String> list2 = Arrays.asList("Baz", "Boop");
		List<List<String>> list = Arrays.asList(list1, list2);
		String result = conversionService.convert(list, String.class);
		assertNotNull(result);
		assertEquals("Foo,Bar,Baz,Boop", result);
	}

	@Test1
	public void convertCollectionToCollection() throws Exception {
		Set<String> foo = new LinkedHashSet<String>();
		foo.add("1");
		foo.add("2");
		foo.add("3");
		@SuppressWarnings("unchecked")
		List<Integer> bar = (List<Integer>) conversionService.convert(foo, TypeDescriptor.forObject(foo),
				new TypeDescriptor(getClass().getField("genericList")));
		assertEquals(new Integer(1), bar.get(0));
		assertEquals(new Integer(2), bar.get(1));
		assertEquals(new Integer(3), bar.get(2));
	}

	@Test1
	public void convertCollectionToCollectionNull() throws Exception {
		@SuppressWarnings("unchecked")
		List<Integer> bar = (List<Integer>) conversionService.convert(null,
				TypeDescriptor.valueOf(LinkedHashSet.class), new TypeDescriptor(getClass().getField("genericList")));
		assertNull(bar);
	}

	@Test1
	@SuppressWarnings("rawtypes")
	public void convertCollectionToCollectionNotGeneric() throws Exception {
		Set<String> foo = new LinkedHashSet<String>();
		foo.add("1");
		foo.add("2");
		foo.add("3");
		List bar = (List) conversionService.convert(foo, TypeDescriptor.valueOf(LinkedHashSet.class), TypeDescriptor
				.valueOf(List.class));
		assertEquals("1", bar.get(0));
		assertEquals("2", bar.get(1));
		assertEquals("3", bar.get(2));
	}

	@Test1
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void convertCollectionToCollectionSpecialCaseSourceImpl() throws Exception {
		Map map = new LinkedHashMap();
		map.put("1", "1");
		map.put("2", "2");
		map.put("3", "3");
		Collection values = map.values();
		List<Integer> bar = (List<Integer>) conversionService.convert(values,
				TypeDescriptor.forObject(values), new TypeDescriptor(getClass().getField("genericList")));
		assertEquals(3, bar.size());
		assertEquals(new Integer(1), bar.get(0));
		assertEquals(new Integer(2), bar.get(1));
		assertEquals(new Integer(3), bar.get(2));
	}

	@Test1
	public void collection() {
		List<String> strings = new ArrayList<String>();
		strings.add("3");
		strings.add("9");
		@SuppressWarnings("unchecked")
		List<Integer> integers = (List<Integer>) conversionService.convert(strings,
				TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(Integer.class)));
		assertEquals(new Integer(3), integers.get(0));
		assertEquals(new Integer(9), integers.get(1));
	}

	@Test1
	public void convertMapToMap() throws Exception {
		Map<String, String> foo = new HashMap<String, String>();
		foo.put("1", "BAR");
		foo.put("2", "BAZ");
		@SuppressWarnings("unchecked")
		Map<Integer, FooEnum> map = (Map<Integer, FooEnum>) conversionService.convert(foo,
				TypeDescriptor.forObject(foo), new TypeDescriptor(getClass().getField("genericMap")));
		assertEquals(FooEnum.BAR, map.get(1));
		assertEquals(FooEnum.BAZ, map.get(2));
	}

	@Test1
	@SuppressWarnings("rawtypes")
	public void convertHashMapValuesToList() {
		Map<String, Integer> hashMap = new LinkedHashMap<String, Integer>();
		hashMap.put("1", 1);
		hashMap.put("2", 2);
		List converted = conversionService.convert(hashMap.values(), List.class);
		assertEquals(Arrays.asList(1, 2), converted);
	}

	@Test1
	public void map() {
		Map<String, String> strings = new HashMap<String, String>();
		strings.put("3", "9");
		strings.put("6", "31");
		@SuppressWarnings("unchecked")
		Map<Integer, Integer> integers = (Map<Integer, Integer>) conversionService.convert(strings,
				TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(Integer.class)));
		assertEquals(new Integer(9), integers.get(3));
		assertEquals(new Integer(31), integers.get(6));
	}

	@Test1
	public void convertPropertiesToString() {
		Properties foo = new Properties();
		foo.setProperty("1", "BAR");
		foo.setProperty("2", "BAZ");
		String result = conversionService.convert(foo, String.class);
		assertTrue(result.contains("1=BAR"));
		assertTrue(result.contains("2=BAZ"));
	}

	@Test1
	public void convertStringToProperties() {
		Properties result = conversionService.convert("a=b\nc=2\nd=", Properties.class);
		assertEquals(3, result.size());
		assertEquals("b", result.getProperty("a"));
		assertEquals("2", result.getProperty("c"));
		assertEquals("", result.getProperty("d"));
	}

	@Test1
	public void convertStringToPropertiesWithSpaces() {
		Properties result = conversionService.convert("   foo=bar\n   bar=baz\n    baz=boop", Properties.class);
		assertEquals("bar", result.get("foo"));
		assertEquals("baz", result.get("bar"));
		assertEquals("boop", result.get("baz"));
	}

	// generic object conversion

	@Test1
	public void convertObjectToStringWithValueOfMethodPresentUsingToString() {
		ISBN.reset();
		assertEquals("123456789", conversionService.convert(new ISBN("123456789"), String.class));

		assertEquals("constructor invocations", 1, ISBN.constructorCount);
		assertEquals("valueOf() invocations", 0, ISBN.valueOfCount);
		assertEquals("toString() invocations", 1, ISBN.toStringCount);
	}

	@Test1
	public void convertObjectToObjectUsingValueOfMethod() {
		ISBN.reset();
		assertEquals(new ISBN("123456789"), conversionService.convert("123456789", ISBN.class));

		assertEquals("valueOf() invocations", 1, ISBN.valueOfCount);
		// valueOf() invokes the constructor
		assertEquals("constructor invocations", 2, ISBN.constructorCount);
		assertEquals("toString() invocations", 0, ISBN.toStringCount);
	}

	@Test1
	public void convertObjectToStringUsingToString() {
		SSN.reset();
		assertEquals("123456789", conversionService.convert(new SSN("123456789"), String.class));

		assertEquals("constructor invocations", 1, SSN.constructorCount);
		assertEquals("toString() invocations", 1, SSN.toStringCount);
	}

	@Test1
	public void convertObjectToObjectUsingObjectConstructor() {
		SSN.reset();
		assertEquals(new SSN("123456789"), conversionService.convert("123456789", SSN.class));

		assertEquals("constructor invocations", 2, SSN.constructorCount);
		assertEquals("toString() invocations", 0, SSN.toStringCount);
	}

	@Test1
	public void convertObjectToStringWithJavaTimeOfMethodPresent() {
		assertTrue(conversionService.convert(ZoneId.of("GMT+1"), String.class).startsWith("GMT+"));
	}

	@Test1
	public void convertObjectToStringNotSupported() {
		assertFalse(conversionService.canConvert(TestEntity.class, String.class));
	}

	@Test1
	public void convertObjectToObjectWithJavaTimeOfMethod() {
		assertEquals(ZoneId.of("GMT+1"), conversionService.convert("GMT+1", ZoneId.class));
	}

	@Test1(expected = ConverterNotFoundException.class)
	public void convertObjectToObjectNoValueOfMethodOrConstructor() {
		conversionService.convert(new Long(3), SSN.class);
	}

	@Test1
	public void convertObjectToObjectFinderMethod() {
		TestEntity e = conversionService.convert(1L, TestEntity.class);
		assertEquals(new Long(1), e.getId());
	}

	@Test1
	public void convertObjectToObjectFinderMethodWithNull() {
		TestEntity entity = (TestEntity) conversionService.convert(null,
				TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(TestEntity.class));
		assertNull(entity);
	}

	@Test1
	public void convertObjectToObjectFinderMethodWithIdConversion() {
		TestEntity entity = conversionService.convert("1", TestEntity.class);
		assertEquals(new Long(1), entity.getId());
	}

	@Test1
	public void convertCharArrayToString() throws Exception {
		String converted = conversionService.convert(new char[] {'a', 'b', 'c'}, String.class);
		assertThat(converted, equalTo("a,b,c"));
	}

	@Test1
	public void convertStringToCharArray() throws Exception {
		char[] converted = conversionService.convert("a,b,c", char[].class);
		assertThat(converted, equalTo(new char[] {'a', 'b', 'c'}));
	}

	@Test1
	public void convertStringToCustomCharArray() throws Exception {
		conversionService.addConverter(new Converter<String, char[]>() {
			@Override
			public char[] convert(String source) {
				return source.toCharArray();
			}
		});
		char[] converted = conversionService.convert("abc", char[].class);
		assertThat(converted, equalTo(new char[] { 'a', 'b', 'c' }));
	}

	@Test1
	@SuppressWarnings("unchecked")
	public void multidimensionalArrayToListConversionShouldConvertEntriesCorrectly() {
		String[][] grid = new String[][] { new String[] { "1", "2", "3", "4" }, new String[] { "5", "6", "7", "8" },
				new String[] { "9", "10", "11", "12" } };
		List<String[]> converted = conversionService.convert(grid, List.class);
		String[][] convertedBack = conversionService.convert(converted, String[][].class);
		assertArrayEquals(grid, convertedBack);
	}

	@Test1
	public void convertCannotOptimizeArray() {
		conversionService.addConverter(new Converter<Byte, Byte>() {

			@Override
			public Byte convert(Byte source) {
				return (byte) (source + 1);
			}
		});
		byte[] byteArray = new byte[] { 1, 2, 3 };
		byte[] converted = conversionService.convert(byteArray, byte[].class);
		assertNotSame(byteArray, converted);
		assertTrue(Arrays.equals(new byte[] { 2, 3, 4 }, converted));
	}

	@Test1
	@SuppressWarnings("unchecked")
	public void convertObjectToOptional() {
		Method method = ClassUtils.getMethod(TestEntity.class, "handleOptionalValue", Optional.class);
		MethodParameter parameter = new MethodParameter(method, 0);
		TypeDescriptor descriptor = new TypeDescriptor(parameter);
		Object actual = conversionService.convert("1,2,3", TypeDescriptor.valueOf(String.class), descriptor);
		assertEquals(Optional.class, actual.getClass());
		assertEquals(Arrays.asList(1, 2, 3), ((Optional<List<Integer>>) actual).get());
	}

	@Test1
	public void convertObjectToOptionalNull() {
		assertSame(Optional.empty(), conversionService.convert(null, TypeDescriptor.valueOf(Object.class),
				TypeDescriptor.valueOf(Optional.class)));
		assertSame(Optional.empty(), conversionService.convert(null, Optional.class));
	}

	@Test1
	public void convertExistingOptional() {
		assertSame(Optional.empty(), conversionService.convert(Optional.empty(), TypeDescriptor.valueOf(Object.class),
				TypeDescriptor.valueOf(Optional.class)));
		assertSame(Optional.empty(), conversionService.convert(Optional.empty(), Optional.class));
	}

	@Test1
	public void testPerformance1() {
		Assume.group(TestGroup.PERFORMANCE);
		StopWatch watch = new StopWatch("integer->string conversionPerformance");
		watch.start("convert 4,000,000 with conversion service");
		for (int i = 0; i < 4000000; i++) {
			conversionService.convert(3, String.class);
		}
		watch.stop();
		watch.start("convert 4,000,000 manually");
		for (int i = 0; i < 4000000; i++) {
			new Integer(3).toString();
		}
		watch.stop();
		// System.out.println(watch.prettyPrint());
	}


	// test fields and helpers

	public List<Integer> genericList = new ArrayList<Integer>();

	public Map<Integer, FooEnum> genericMap = new HashMap<Integer, FooEnum>();

	public EnumSet<Foo> enumSet;

	public Object assignableTarget;


	public void handlerMethod(List<Color> color) {
	}


	public enum Foo {
		BAR, BAZ
	}


	public enum SubFoo {

		BAR {
			@Override
			String s() {
				return "x";
			}
		},
		BAZ {
			@Override
			String s() {
				return "y";
			}
		};

		abstract String s();
	}


	public class ColorConverter implements Converter<String, Color> {

		@Override
		public Color convert(String source) { if (!source.startsWith("#")) source = "#" + source; return Color.decode(source); }
	}


	@SuppressWarnings("serial")
	public static class CustomNumber extends Number {

		@Override
		public double doubleValue() {
			return 0;
		}

		@Override
		public float floatValue() {
			return 0;
		}

		@Override
		public int intValue() {
			return 0;
		}

		@Override
		public long longValue() {
			return 0;
		}
	}


	public static class TestEntity {

		private Long id;

		public TestEntity(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public static TestEntity findTestEntity(Long id) {
			return new TestEntity(id);
		}

		public void handleOptionalValue(Optional<List<Integer>> value) {
		}
	}


	private static class ListWrapper {

		private List<?> list;

		public ListWrapper(List<?> list) {
			this.list = list;
		}

		public List<?> getList() {
			return list;
		}
	}


	private static class SSN {

		static int constructorCount = 0;
		static int toStringCount = 0;


		static void reset() {
			constructorCount = 0;
			toStringCount = 0;
		}

		private final String value;

		public SSN(String value) {
			constructorCount++;
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof SSN)) {
				return false;
			}
			SSN ssn = (SSN) o;
			return this.value.equals(ssn.value);
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public String toString() {
			toStringCount++;
			return value;
		}
	}


	private static class ISBN {

		static int constructorCount = 0;
		static int toStringCount = 0;
		static int valueOfCount = 0;

		static void reset() {
			constructorCount = 0;
			toStringCount = 0;
			valueOfCount = 0;
		}

		private final String value;

		public ISBN(String value) {
			constructorCount++;
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof ISBN)) {
				return false;
			}
			ISBN isbn = (ISBN) o;
			return this.value.equals(isbn.value);
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public String toString() {
			toStringCount++;
			return value;
		}

		@SuppressWarnings("unused")
		public static ISBN valueOf(String value) {
			valueOfCount++;
			return new ISBN(value);
		}
	}

}
