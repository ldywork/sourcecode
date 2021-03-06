/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.format.datetime.standard;

import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.DataBinder;

import static org.junit.Assert.*;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
public class DateTimeFormattingTests {

	private FormattingConversionService conversionService;

	private DataBinder binder;

	@Before
	public void setUp() {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		setUp(registrar);
	}

	private void setUp(DateTimeFormatterRegistrar registrar) {
		conversionService = new FormattingConversionService();
		DefaultConversionService.addDefaultConverters(conversionService);

		registrar.registerFormatters(conversionService);

		DateTimeBean bean = new DateTimeBean();
		bean.getChildren().add(new DateTimeBean());
		binder = new DataBinder(bean);
		binder.setConversionService(conversionService);

		LocaleContextHolder.setLocale(Locale.US);
		DateTimeContext context = new DateTimeContext();
		context.setTimeZone(ZoneId.of("-05:00"));
		DateTimeContextHolder.setDateTimeContext(context);
	}

	@After
	public void tearDown() {
		LocaleContextHolder.setLocale(null);
		DateTimeContextHolder.setDateTimeContext(null);
	}

	@Test1
	public void testBindLocalDate() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", "10/31/09");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09", binder.getBindingResult().getFieldValue("localDate"));
	}

	@Test1
	public void testBindLocalDateWithSpecificStyle() throws Exception {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setDateStyle(FormatStyle.LONG);
		setUp(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", "October 31, 2009");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("October 31, 2009", binder.getBindingResult().getFieldValue("localDate"));
	}

	@Test1
	public void testBindLocalDateWithSpecificFormatter() throws Exception {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setDateFormatter(DateTimeFormatter.ofPattern("yyyyMMdd"));
		setUp(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", "20091031");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("20091031", binder.getBindingResult().getFieldValue("localDate"));
	}

	@Test1
	public void testBindLocalDateArray() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", new String[]{"10/31/09"});
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
	}

	@Test1
	public void testBindLocalDateAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateAnnotated", "Oct 31, 2009");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("Oct 31, 2009", binder.getBindingResult().getFieldValue("localDateAnnotated"));
	}

	@Test1
	public void testBindLocalDateAnnotatedWithError() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateAnnotated", "Oct -31, 2009");
		binder.bind(propertyValues);
		assertEquals(1, binder.getBindingResult().getFieldErrorCount("localDateAnnotated"));
		assertEquals("Oct -31, 2009", binder.getBindingResult().getFieldValue("localDateAnnotated"));
	}

	@Test1
	public void testBindNestedLocalDateAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("children[0].localDateAnnotated", "Oct 31, 2009");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("Oct 31, 2009", binder.getBindingResult().getFieldValue("children[0].localDateAnnotated"));
	}

	@Test1
	public void testBindLocalDateAnnotatedWithDirectFieldAccess() {
		binder.initDirectFieldAccess();
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateAnnotated", "Oct 31, 2009");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("Oct 31, 2009", binder.getBindingResult().getFieldValue("localDateAnnotated"));
	}

	@Test1
	public void testBindLocalDateAnnotatedWithDirectFieldAccessAndError() {
		binder.initDirectFieldAccess();
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateAnnotated", "Oct -31, 2009");
		binder.bind(propertyValues);
		assertEquals(1, binder.getBindingResult().getFieldErrorCount("localDateAnnotated"));
		assertEquals("Oct -31, 2009", binder.getBindingResult().getFieldValue("localDateAnnotated"));
	}

	@Test1
	public void testBindLocalDateFromJavaUtilCalendar() throws Exception {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", new GregorianCalendar(2009, 9, 31, 0, 0));
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09", binder.getBindingResult().getFieldValue("localDate"));
	}

	@Test1
	public void testBindLocalTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", "12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("12:00 PM", binder.getBindingResult().getFieldValue("localTime"));
	}

	@Test1
	public void testBindLocalTimeWithSpecificStyle() throws Exception {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setTimeStyle(FormatStyle.MEDIUM);
		setUp(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", "12:00:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("12:00:00 PM", binder.getBindingResult().getFieldValue("localTime"));
	}

	@Test1
	public void testBindLocalTimeWithSpecificFormatter() throws Exception {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setTimeFormatter(DateTimeFormatter.ofPattern("HHmmss"));
		setUp(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", "130000");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("130000", binder.getBindingResult().getFieldValue("localTime"));
	}

	@Test1
	public void testBindLocalTimeAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTimeAnnotated", "12:00:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("12:00:00 PM", binder.getBindingResult().getFieldValue("localTimeAnnotated"));
	}

	@Test1
	public void testBindLocalTimeFromJavaUtilCalendar() throws Exception {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", new GregorianCalendar(1970, 0, 0, 12, 0));
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("12:00 PM", binder.getBindingResult().getFieldValue("localTime"));
	}

	@Test1
	public void testBindLocalDateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTime", "10/31/09 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09 12:00 PM", binder.getBindingResult().getFieldValue("localDateTime"));
	}

	@Test1
	public void testBindLocalDateTimeAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTimeAnnotated", "Oct 31, 2009 12:00:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("Oct 31, 2009 12:00:00 PM", binder.getBindingResult().getFieldValue("localDateTimeAnnotated"));
	}

	@Test1
	public void testBindLocalDateTimeFromJavaUtilCalendar() throws Exception {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTime", new GregorianCalendar(2009, 9, 31, 12, 0));
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09 12:00 PM", binder.getBindingResult().getFieldValue("localDateTime"));
	}

	@Test1
	public void testBindDateTimeWithSpecificStyle() throws Exception {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setDateTimeStyle(FormatStyle.MEDIUM);
		setUp(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTime", "Oct 31, 2009 12:00:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("Oct 31, 2009 12:00:00 PM", binder.getBindingResult().getFieldValue("localDateTime"));
	}

	@Test1
	public void testBindDateTimeAnnotatedPattern() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("dateTimeAnnotatedPattern", "10/31/09 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09 12:00 PM", binder.getBindingResult().getFieldValue("dateTimeAnnotatedPattern"));
	}

	@Test1
	public void testBindISODate() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoDate", "2009-10-31");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("2009-10-31", binder.getBindingResult().getFieldValue("isoDate"));
	}

	@Test1
	public void testBindISOTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoTime", "12:00:00.000-05:00");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("12:00:00", binder.getBindingResult().getFieldValue("isoTime"));
	}

	@Test1
	public void testBindISODateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoDateTime", "2009-10-31T12:00:00.000Z");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("2009-10-31T12:00:00", binder.getBindingResult().getFieldValue("isoDateTime"));
	}

	@Test1
	public void testBindInstant() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("instant", "2009-10-31T12:00:00.000Z");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertTrue(binder.getBindingResult().getFieldValue("instant").toString().startsWith("2009-10-31T12:00"));
	}

	@Test1
	public void testBindInstantFromJavaUtilDate() throws Exception {
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.US);
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("instant", df.parse("10/31/09 12:00 PM"));
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertTrue(binder.getBindingResult().getFieldValue("instant").toString().startsWith("2009-10-31T12:00"));
	}


	public static class DateTimeBean {

		private LocalDate localDate;

		@DateTimeFormat(style="M-")
		private LocalDate localDateAnnotated;

		private LocalTime localTime;

		@DateTimeFormat(style="-M")
		private LocalTime localTimeAnnotated;

		private LocalDateTime localDateTime;

		@DateTimeFormat(style="MM")
		private LocalDateTime localDateTimeAnnotated;

		@DateTimeFormat(pattern="M/d/yy h:mm a")
		private LocalDateTime dateTimeAnnotatedPattern;

		@DateTimeFormat(iso=ISO.DATE)
		private LocalDate isoDate;

		@DateTimeFormat(iso=ISO.TIME)
		private LocalTime isoTime;

		@DateTimeFormat(iso=ISO.DATE_TIME)
		private LocalDateTime isoDateTime;

		private Instant instant;

		private final List<DateTimeBean> children = new ArrayList<DateTimeBean>();

		public LocalDate getLocalDate() {
			return localDate;
		}

		public void setLocalDate(LocalDate localDate) {
			this.localDate = localDate;
		}

		public LocalDate getLocalDateAnnotated() {
			return localDateAnnotated;
		}

		public void setLocalDateAnnotated(LocalDate localDateAnnotated) {
			this.localDateAnnotated = localDateAnnotated;
		}

		public LocalTime getLocalTime() {
			return localTime;
		}

		public void setLocalTime(LocalTime localTime) {
			this.localTime = localTime;
		}

		public LocalTime getLocalTimeAnnotated() {
			return localTimeAnnotated;
		}

		public void setLocalTimeAnnotated(LocalTime localTimeAnnotated) {
			this.localTimeAnnotated = localTimeAnnotated;
		}

		public LocalDateTime getLocalDateTime() {
			return localDateTime;
		}

		public void setLocalDateTime(LocalDateTime localDateTime) {
			this.localDateTime = localDateTime;
		}

		public LocalDateTime getLocalDateTimeAnnotated() {
			return localDateTimeAnnotated;
		}

		public void setLocalDateTimeAnnotated(LocalDateTime localDateTimeAnnotated) {
			this.localDateTimeAnnotated = localDateTimeAnnotated;
		}

		public LocalDateTime getDateTimeAnnotatedPattern() {
			return dateTimeAnnotatedPattern;
		}

		public void setDateTimeAnnotatedPattern(LocalDateTime dateTimeAnnotatedPattern) {
			this.dateTimeAnnotatedPattern = dateTimeAnnotatedPattern;
		}

		public LocalDate getIsoDate() {
			return isoDate;
		}

		public void setIsoDate(LocalDate isoDate) {
			this.isoDate = isoDate;
		}

		public LocalTime getIsoTime() {
			return isoTime;
		}

		public void setIsoTime(LocalTime isoTime) {
			this.isoTime = isoTime;
		}

		public LocalDateTime getIsoDateTime() {
			return isoDateTime;
		}

		public void setIsoDateTime(LocalDateTime isoDateTime) {
			this.isoDateTime = isoDateTime;
		}

		public Instant getInstant() {
			return instant;
		}

		public void setInstant(Instant instant) {
			this.instant = instant;
		}

		public List<DateTimeBean> getChildren() {
			return children;
		}
	}

}
