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

package org.springframework.format.support;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.Formatter;
import org.springframework.format.Printer;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.datetime.joda.DateTimeParser;
import org.springframework.format.datetime.joda.JodaDateTimeFormatAnnotationFormatterFactory;
import org.springframework.format.datetime.joda.ReadablePartialPrinter;
import org.springframework.format.number.NumberFormatter;

import static org.junit.Assert.*;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Kazuki Shimizu
 * @author Sam Brannen
 */
public class FormattingConversionServiceTests {

	private FormattingConversionService formattingService;

	@Before
	public void setUp() {
		formattingService = new FormattingConversionService();
		DefaultConversionService.addDefaultConverters(formattingService);
		LocaleContextHolder.setLocale(Locale.US);
	}

	@After
	public void tearDown() {
		LocaleContextHolder.setLocale(null);
	}

	@Test1
	public void testFormatFieldForTypeWithFormatter() throws ParseException {
		formattingService.addFormatterForFieldType(Number.class, new NumberFormatter());
		String formatted = formattingService.convert(3, String.class);
		assertEquals("3", formatted);
		Integer i = formattingService.convert("3", Integer.class);
		assertEquals(new Integer(3), i);
	}

	@Test1
	public void testFormatFieldForTypeWithPrinterParserWithCoercion() throws ParseException {
		formattingService.addConverter(new Converter<DateTime, LocalDate>() {
			@Override
			public LocalDate convert(DateTime source) {
				return source.toLocalDate();
			}
		});
		formattingService.addFormatterForFieldType(LocalDate.class, new ReadablePartialPrinter(DateTimeFormat
				.shortDate()), new DateTimeParser(DateTimeFormat.shortDate()));
		String formatted = formattingService.convert(new LocalDate(2009, 10, 31), String.class);
		assertEquals("10/31/09", formatted);
		LocalDate date = formattingService.convert("10/31/09", LocalDate.class);
		assertEquals(new LocalDate(2009, 10, 31), date);
	}

	@Test1
	@SuppressWarnings("resource")
	public void testFormatFieldForValueInjection() {
		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();
		ac.registerBeanDefinition("valueBean", new RootBeanDefinition(ValueBean.class));
		ac.registerBeanDefinition("conversionService", new RootBeanDefinition(FormattingConversionServiceFactoryBean.class));
		ac.refresh();
		ValueBean valueBean = ac.getBean(ValueBean.class);
		assertEquals(new LocalDate(2009, 10, 31), new LocalDate(valueBean.date));
	}

	@Test1
	@SuppressWarnings("resource")
	public void testFormatFieldForValueInjectionUsingMetaAnnotations() {
		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();
		RootBeanDefinition bd = new RootBeanDefinition(MetaValueBean.class);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		ac.registerBeanDefinition("valueBean", bd);
		ac.registerBeanDefinition("conversionService", new RootBeanDefinition(FormattingConversionServiceFactoryBean.class));
		ac.registerBeanDefinition("ppc", new RootBeanDefinition(PropertyPlaceholderConfigurer.class));
		ac.refresh();
		System.setProperty("myDate", "10-31-09");
		System.setProperty("myNumber", "99.99%");
		try {
			MetaValueBean valueBean = ac.getBean(MetaValueBean.class);
			assertEquals(new LocalDate(2009, 10, 31), new LocalDate(valueBean.date));
			assertEquals(Double.valueOf(0.9999), valueBean.number);
		}
		finally {
			System.clearProperty("myDate");
			System.clearProperty("myNumber");
		}
	}

	@Test1
	public void testFormatFieldForAnnotation() throws Exception {
		formattingService.addFormatterForFieldAnnotation(new JodaDateTimeFormatAnnotationFormatterFactory());
		doTestFormatFieldForAnnotation(Model.class, false);
	}

	@Test1
	public void testFormatFieldForAnnotationWithDirectFieldAccess() throws Exception {
		formattingService.addFormatterForFieldAnnotation(new JodaDateTimeFormatAnnotationFormatterFactory());
		doTestFormatFieldForAnnotation(Model.class, true);
	}

	@Test1
	@SuppressWarnings("resource")
	public void testFormatFieldForAnnotationWithPlaceholders() throws Exception {
		GenericApplicationContext context = new GenericApplicationContext();
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		Properties props = new Properties();
		props.setProperty("dateStyle", "S-");
		props.setProperty("datePattern", "M-d-yy");
		ppc.setProperties(props);
		context.getBeanFactory().registerSingleton("ppc", ppc);
		context.refresh();
		context.getBeanFactory().initializeBean(formattingService, "formattingService");
		formattingService.addFormatterForFieldAnnotation(new JodaDateTimeFormatAnnotationFormatterFactory());
		doTestFormatFieldForAnnotation(ModelWithPlaceholders.class, false);
	}

	@Test1
	@SuppressWarnings("resource")
	public void testFormatFieldForAnnotationWithPlaceholdersAndFactoryBean() throws Exception {
		GenericApplicationContext context = new GenericApplicationContext();
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		Properties props = new Properties();
		props.setProperty("dateStyle", "S-");
		props.setProperty("datePattern", "M-d-yy");
		ppc.setProperties(props);
		context.registerBeanDefinition("formattingService", new RootBeanDefinition(FormattingConversionServiceFactoryBean.class));
		context.getBeanFactory().registerSingleton("ppc", ppc);
		context.refresh();
		formattingService = context.getBean("formattingService", FormattingConversionService.class);
		doTestFormatFieldForAnnotation(ModelWithPlaceholders.class, false);
	}

	@SuppressWarnings("unchecked")
	private void doTestFormatFieldForAnnotation(Class<?> modelClass, boolean directFieldAccess) throws Exception {
		formattingService.addConverter(new Converter<Date, Long>() {
			@Override
			public Long convert(Date source) {
				return source.getTime();
			}
		});
		formattingService.addConverter(new Converter<DateTime, Date>() {
			@Override
			public Date convert(DateTime source) {
				return source.toDate();
			}
		});

		String formatted = (String) formattingService.convert(new LocalDate(2009, 10, 31).toDateTimeAtCurrentTime()
				.toDate(), new TypeDescriptor(modelClass.getField("date")), TypeDescriptor.valueOf(String.class));
		assertEquals("10/31/09", formatted);
		LocalDate date = new LocalDate(formattingService.convert("10/31/09", TypeDescriptor.valueOf(String.class),
				new TypeDescriptor(modelClass.getField("date"))));
		assertEquals(new LocalDate(2009, 10, 31), date);

		List<Date> dates = new ArrayList<Date>();
		dates.add(new LocalDate(2009, 10, 31).toDateTimeAtCurrentTime().toDate());
		dates.add(new LocalDate(2009, 11, 1).toDateTimeAtCurrentTime().toDate());
		dates.add(new LocalDate(2009, 11, 2).toDateTimeAtCurrentTime().toDate());
		formatted = (String) formattingService.convert(dates,
				new TypeDescriptor(modelClass.getField("dates")), TypeDescriptor.valueOf(String.class));
		assertEquals("10-31-09,11-1-09,11-2-09", formatted);
		dates = (List<Date>) formattingService.convert("10-31-09,11-1-09,11-2-09",
				TypeDescriptor.valueOf(String.class), new TypeDescriptor(modelClass.getField("dates")));
		assertEquals(new LocalDate(2009, 10, 31), new LocalDate(dates.get(0)));
		assertEquals(new LocalDate(2009, 11, 1), new LocalDate(dates.get(1)));
		assertEquals(new LocalDate(2009, 11, 2), new LocalDate(dates.get(2)));

		Object model = BeanUtils.instantiate(modelClass);
		ConfigurablePropertyAccessor accessor = directFieldAccess ? PropertyAccessorFactory.forDirectFieldAccess(model) :
				PropertyAccessorFactory.forBeanPropertyAccess(model);
		accessor.setConversionService(formattingService);
		accessor.setPropertyValue("dates", "10-31-09,11-1-09,11-2-09");
		dates = (List<Date>) accessor.getPropertyValue("dates");
		assertEquals(new LocalDate(2009, 10, 31), new LocalDate(dates.get(0)));
		assertEquals(new LocalDate(2009, 11, 1), new LocalDate(dates.get(1)));
		assertEquals(new LocalDate(2009, 11, 2), new LocalDate(dates.get(2)));
		if (!directFieldAccess) {
			accessor.setPropertyValue("dates[0]", "10-30-09");
			accessor.setPropertyValue("dates[1]", "10-1-09");
			accessor.setPropertyValue("dates[2]", "10-2-09");
			dates = (List<Date>) accessor.getPropertyValue("dates");
			assertEquals(new LocalDate(2009, 10, 30), new LocalDate(dates.get(0)));
			assertEquals(new LocalDate(2009, 10, 1), new LocalDate(dates.get(1)));
			assertEquals(new LocalDate(2009, 10, 2), new LocalDate(dates.get(2)));
		}
	}

	@Test1
	public void testPrintNull() throws ParseException {
		formattingService.addFormatterForFieldType(Number.class, new NumberFormatter());
		assertEquals("", formattingService.convert(null, TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(String.class)));
	}

	@Test1
	public void testParseNull() throws ParseException {
		formattingService.addFormatterForFieldType(Number.class, new NumberFormatter());
		assertNull(formattingService
				.convert(null, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class)));
	}

	@Test1
	public void testParseEmptyString() throws ParseException {
		formattingService.addFormatterForFieldType(Number.class, new NumberFormatter());
		assertNull(formattingService.convert("", TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class)));
	}

	@Test1
	public void testParseBlankString() throws ParseException {
		formattingService.addFormatterForFieldType(Number.class, new NumberFormatter());
		assertNull(formattingService.convert("     ", TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class)));
	}

	@Test1(expected=ConversionFailedException.class)
	public void testParseParserReturnsNull() throws ParseException {
		formattingService.addFormatterForFieldType(Integer.class, new NullReturningFormatter());
		assertNull(formattingService.convert("1", TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class)));
	}

	@Test1(expected=ConversionFailedException.class)
	public void testParseNullPrimitiveProperty() throws ParseException {
		formattingService.addFormatterForFieldType(Integer.class, new NumberFormatter());
		assertNull(formattingService.convert(null, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(int.class)));
	}

	@Test1
	public void testPrintNullDefault() throws ParseException {
		assertEquals(null, formattingService
				.convert(null, TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(String.class)));
	}

	@Test1
	public void testParseNullDefault() throws ParseException {
		assertNull(formattingService
				.convert(null, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class)));
	}

	@Test1
	public void testParseEmptyStringDefault() throws ParseException {
		assertNull(formattingService.convert("", TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class)));
	}

	@Test1
	public void testFormatFieldForAnnotationWithSubclassAsFieldType() throws Exception {
		formattingService.addFormatterForFieldAnnotation(new JodaDateTimeFormatAnnotationFormatterFactory() {
			@Override
			public Printer<?> getPrinter(org.springframework.format.annotation.DateTimeFormat annotation, Class<?> fieldType) {
				assertEquals(MyDate.class, fieldType);
				return super.getPrinter(annotation, fieldType);
			}
		});
		formattingService.addConverter(new Converter<MyDate, Long>() {
			@Override
			public Long convert(MyDate source) {
				return source.getTime();
			}
		});
		formattingService.addConverter(new Converter<MyDate, Date>() {
			@Override
			public Date convert(MyDate source) {
				return source;
			}
		});

		formattingService.convert(new MyDate(), new TypeDescriptor(ModelWithSubclassField.class.getField("date")),
				TypeDescriptor.valueOf(String.class));
	}

	@Test1
	public void testRegisterDefaultValueViaFormatter() {
		registerDefaultValue(Date.class, new Date());
	}

	private <T> void registerDefaultValue(Class<T> clazz, final T defaultValue) {
		formattingService.addFormatterForFieldType(clazz, new Formatter<T>() {
			@Override
			public T parse(String text, Locale locale) throws ParseException {
				return defaultValue;
			}
			@Override
			public String print(T t, Locale locale) {
				return defaultValue.toString();
			}
			@Override
			public String toString() {
				return defaultValue.toString();
			}
		});
	}


	public static class ValueBean {

		@Value("10-31-09")
		@org.springframework.format.annotation.DateTimeFormat(pattern="MM-d-yy")
		public Date date;
	}

	public static class MetaValueBean {

		@MyDateAnn
		public Date date;

		@MyNumberAnn
		public Double number;
	}

	@Value("${myDate}")
	@org.springframework.format.annotation.DateTimeFormat(pattern="MM-d-yy")
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface MyDateAnn {
	}

	@Value("${myNumber}")
	@NumberFormat(style = NumberFormat.Style.PERCENT)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface MyNumberAnn {
	}

	public static class Model {

		@org.springframework.format.annotation.DateTimeFormat(style="S-")
		public Date date;

		@org.springframework.format.annotation.DateTimeFormat(pattern="M-d-yy")
		public List<Date> dates;

		public List<Date> getDates() {
			return dates;
		}

		public void setDates(List<Date> dates) {
			this.dates = dates;
		}
	}

	public static class ModelWithPlaceholders {

		@org.springframework.format.annotation.DateTimeFormat(style="${dateStyle}")
		public Date date;

		@MyDatePattern
		public List<Date> dates;

		public List<Date> getDates() {
			return dates;
		}

		public void setDates(List<Date> dates) {
			this.dates = dates;
		}
	}

	@org.springframework.format.annotation.DateTimeFormat(pattern="${datePattern}")
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface MyDatePattern {
	}

	public static class NullReturningFormatter implements Formatter<Integer> {

		@Override
		public String print(Integer object, Locale locale) {
			return null;
		}

		@Override
		public Integer parse(String text, Locale locale) throws ParseException {
			return null;
		}

	}

	@SuppressWarnings("serial")
	public static class MyDate extends Date {
	}

	private static class ModelWithSubclassField {

		@org.springframework.format.annotation.DateTimeFormat(style = "S-")
		public MyDate date;
	}

}
