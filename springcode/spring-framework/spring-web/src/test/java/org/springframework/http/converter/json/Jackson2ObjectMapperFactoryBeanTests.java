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

package org.springframework.http.converter.json;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.deser.BasicDeserializerFactory;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.DateDeserializer;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.BasicSerializerFactory;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.ClassSerializer;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.FatalBeanException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Test cases for {@link Jackson2ObjectMapperFactoryBean} class.
 *
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 * @author Brian Clozel
 * @author Sebastien Deleuze
 */
public class Jackson2ObjectMapperFactoryBeanTests {

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private Jackson2ObjectMapperFactoryBean factory;


	@Before
	public void setUp() {
		factory = new Jackson2ObjectMapperFactoryBean();
	}


	@Test1
	public void settersWithNullValues() {
		// Should not crash:
		factory.setSerializers((JsonSerializer<?>[]) null);
		factory.setSerializersByType(null);
		factory.setDeserializersByType(null);
		factory.setFeaturesToEnable((Object[]) null);
		factory.setFeaturesToDisable((Object[]) null);
	}

	@Test1(expected = FatalBeanException.class)
	public void unknownFeature() {
		this.factory.setFeaturesToEnable(Boolean.TRUE);
		this.factory.afterPropertiesSet();
	}

	@Test1
	public void booleanSetters() {
		this.factory.setAutoDetectFields(false);
		this.factory.setAutoDetectGettersSetters(false);
		this.factory.setDefaultViewInclusion(false);
		this.factory.setFailOnEmptyBeans(false);
		this.factory.setIndentOutput(true);
		this.factory.afterPropertiesSet();

		ObjectMapper objectMapper = this.factory.getObject();

		assertFalse(objectMapper.getSerializationConfig().isEnabled(MapperFeature.AUTO_DETECT_FIELDS));
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(MapperFeature.AUTO_DETECT_FIELDS));
		assertFalse(objectMapper.getSerializationConfig().isEnabled(MapperFeature.AUTO_DETECT_GETTERS));
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(MapperFeature.AUTO_DETECT_SETTERS));
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION));
		assertFalse(objectMapper.getSerializationConfig().isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
		assertTrue(objectMapper.getSerializationConfig().isEnabled(SerializationFeature.INDENT_OUTPUT));
		assertTrue(objectMapper.getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.ALWAYS);
	}

	@Test1
	public void setNotNullSerializationInclusion() {
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.ALWAYS);

		factory.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.NON_NULL);
	}

	@Test1
	public void setNotDefaultSerializationInclusion() {
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.ALWAYS);

		factory.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.NON_DEFAULT);
	}

	@Test1
	public void setNotEmptySerializationInclusion() {
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.ALWAYS);

		factory.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.NON_EMPTY);
	}

	@Test1
	public void dateTimeFormatSetter() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

		this.factory.setDateFormat(dateFormat);
		this.factory.afterPropertiesSet();

		assertEquals(dateFormat, this.factory.getObject().getSerializationConfig().getDateFormat());
		assertEquals(dateFormat, this.factory.getObject().getDeserializationConfig().getDateFormat());
	}

	@Test1
	public void simpleDateFormatStringSetter() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

		this.factory.setSimpleDateFormat(DATE_FORMAT);
		this.factory.afterPropertiesSet();

		assertEquals(dateFormat, this.factory.getObject().getSerializationConfig().getDateFormat());
		assertEquals(dateFormat, this.factory.getObject().getDeserializationConfig().getDateFormat());
	}

	@Test1
	public void localeSetter() {
		this.factory.setLocale(Locale.FRENCH);
		this.factory.afterPropertiesSet();

		assertEquals(Locale.FRENCH, this.factory.getObject().getSerializationConfig().getLocale());
		assertEquals(Locale.FRENCH, this.factory.getObject().getDeserializationConfig().getLocale());
	}

	@Test1
	public void timeZoneSetter() {
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");

		this.factory.setTimeZone(timeZone);
		this.factory.afterPropertiesSet();

		assertEquals(timeZone, this.factory.getObject().getSerializationConfig().getTimeZone());
		assertEquals(timeZone, this.factory.getObject().getDeserializationConfig().getTimeZone());
	}

	@Test1
	public void timeZoneStringSetter() {
		String zoneId = "Europe/Paris";

		this.factory.setTimeZone(TimeZone.getTimeZone(zoneId));
		this.factory.afterPropertiesSet();

		TimeZone timeZone = TimeZone.getTimeZone(zoneId);
		assertEquals(timeZone, this.factory.getObject().getSerializationConfig().getTimeZone());
		assertEquals(timeZone, this.factory.getObject().getDeserializationConfig().getTimeZone());
	}

	@Test1
	public void wrongTimeZoneStringSetter() {
		String zoneId = "foo";

		this.factory.setTimeZone(TimeZone.getTimeZone(zoneId));
		this.factory.afterPropertiesSet();

		TimeZone timeZone = TimeZone.getTimeZone("GMT");
		assertEquals(timeZone, this.factory.getObject().getSerializationConfig().getTimeZone());
		assertEquals(timeZone, this.factory.getObject().getDeserializationConfig().getTimeZone());
	}

	@Test1
	public void setModules() {
		NumberSerializer serializer1 = new NumberSerializer();
		SimpleModule module = new SimpleModule();
		module.addSerializer(Integer.class, serializer1);

		this.factory.setModules(Arrays.asList(new Module[]{module}));
		this.factory.afterPropertiesSet();
		ObjectMapper objectMapper = this.factory.getObject();

		Serializers serializers = getSerializerFactoryConfig(objectMapper).serializers().iterator().next();
		assertTrue(serializers.findSerializer(null, SimpleType.construct(Integer.class), null) == serializer1);
	}

	@Test1
	public void defaultModules() throws JsonProcessingException, UnsupportedEncodingException {
		this.factory.afterPropertiesSet();
		ObjectMapper objectMapper = this.factory.getObject();

		Long timestamp = 1322903730000L;
		DateTime dateTime = new DateTime(timestamp, DateTimeZone.UTC);
		assertEquals(timestamp.toString(), new String(objectMapper.writeValueAsBytes(dateTime), "UTF-8"));
	}

	@Test1 // SPR-12634
	public void customizeDefaultModulesWithModuleClass() throws JsonProcessingException, UnsupportedEncodingException {
		this.factory.setModulesToInstall(CustomIntegerModule.class);
		this.factory.afterPropertiesSet();
		ObjectMapper objectMapper = this.factory.getObject();

		DateTime dateTime = new DateTime(1322903730000L, DateTimeZone.UTC);
		assertEquals("1322903730000", new String(objectMapper.writeValueAsBytes(dateTime), "UTF-8"));
		assertThat(new String(objectMapper.writeValueAsBytes(new Integer(4)), "UTF-8"), containsString("customid"));
	}

	@Test1 // SPR-12634
	public void customizeDefaultModulesWithSerializer() throws JsonProcessingException, UnsupportedEncodingException {
		Map<Class<?>, JsonSerializer<?>> serializers = new HashMap<>();
		serializers.put(Integer.class, new CustomIntegerSerializer());

		this.factory.setSerializersByType(serializers);
		this.factory.afterPropertiesSet();
		ObjectMapper objectMapper = this.factory.getObject();

		DateTime dateTime = new DateTime(1322903730000L, DateTimeZone.UTC);
		assertEquals("1322903730000", new String(objectMapper.writeValueAsBytes(dateTime), "UTF-8"));
		assertThat(new String(objectMapper.writeValueAsBytes(new Integer(4)), "UTF-8"), containsString("customid"));
	}

	@Test1
	public void simpleSetup() {
		this.factory.afterPropertiesSet();

		assertNotNull(this.factory.getObject());
		assertTrue(this.factory.isSingleton());
		assertEquals(ObjectMapper.class, this.factory.getObjectType());
	}

	@Test1
	public void undefinedObjectType() {
		assertEquals(null, this.factory.getObjectType());
	}

	private static SerializerFactoryConfig getSerializerFactoryConfig(ObjectMapper objectMapper) {
		return ((BasicSerializerFactory) objectMapper.getSerializerFactory()).getFactoryConfig();
	}

	private static DeserializerFactoryConfig getDeserializerFactoryConfig(ObjectMapper objectMapper) {
		return ((BasicDeserializerFactory) objectMapper.getDeserializationContext().getFactory()).getFactoryConfig();
	}

	@Test1
	public void propertyNamingStrategy() {
		PropertyNamingStrategy strategy = new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy();
		this.factory.setPropertyNamingStrategy(strategy);
		this.factory.afterPropertiesSet();

		assertSame(strategy, this.factory.getObject().getSerializationConfig().getPropertyNamingStrategy());
		assertSame(strategy, this.factory.getObject().getDeserializationConfig().getPropertyNamingStrategy());
	}

	@Test1
	public void setMixIns() {
		Class<?> target = String.class;
		Class<?> mixinSource = Object.class;
		Map<Class<?>, Class<?>> mixIns = new HashMap<Class<?>, Class<?>>();
		mixIns.put(target, mixinSource);

		this.factory.setMixIns(mixIns);
		this.factory.afterPropertiesSet();
		ObjectMapper objectMapper = this.factory.getObject();

		assertEquals(1, objectMapper.mixInCount());
		assertSame(mixinSource, objectMapper.findMixInClassFor(target));
	}

	@Test1
	public void completeSetup() {
		NopAnnotationIntrospector annotationIntrospector = NopAnnotationIntrospector.instance;
		ObjectMapper objectMapper = new ObjectMapper();

		factory.setObjectMapper(objectMapper);
		assertTrue(this.factory.isSingleton());
		assertEquals(ObjectMapper.class, this.factory.getObjectType());

		Map<Class<?>, JsonDeserializer<?>> deserializers = new HashMap<Class<?>, JsonDeserializer<?>>();
		deserializers.put(Date.class, new DateDeserializer());

		JsonSerializer<Class<?>> serializer1 = new ClassSerializer();
		JsonSerializer<Number> serializer2 = new NumberSerializer();

		factory.setModules(new ArrayList<>()); // Disable well-known modules detection
		factory.setSerializers(serializer1);
		factory.setSerializersByType(Collections.<Class<?>, JsonSerializer<?>> singletonMap(Boolean.class, serializer2));
		factory.setDeserializersByType(deserializers);
		factory.setAnnotationIntrospector(annotationIntrospector);

		this.factory.setFeaturesToEnable(SerializationFeature.FAIL_ON_EMPTY_BEANS,
				DeserializationFeature.UNWRAP_ROOT_VALUE,
				JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER,
				JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS);

		this.factory.setFeaturesToDisable(MapperFeature.AUTO_DETECT_GETTERS,
				MapperFeature.AUTO_DETECT_FIELDS,
				JsonParser.Feature.AUTO_CLOSE_SOURCE,
				JsonGenerator.Feature.QUOTE_FIELD_NAMES);

		assertFalse(getSerializerFactoryConfig(objectMapper).hasSerializers());
		assertFalse(getDeserializerFactoryConfig(objectMapper).hasDeserializers());

		this.factory.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		this.factory.afterPropertiesSet();

		assertTrue(objectMapper == this.factory.getObject());
		assertTrue(getSerializerFactoryConfig(objectMapper).hasSerializers());
		assertTrue(getDeserializerFactoryConfig(objectMapper).hasDeserializers());

		Serializers serializers = getSerializerFactoryConfig(objectMapper).serializers().iterator().next();
		assertTrue(serializers.findSerializer(null, SimpleType.construct(Class.class), null) == serializer1);
		assertTrue(serializers.findSerializer(null, SimpleType.construct(Boolean.class), null) == serializer2);
		assertNull(serializers.findSerializer(null, SimpleType.construct(Number.class), null));

		assertTrue(annotationIntrospector == objectMapper.getSerializationConfig().getAnnotationIntrospector());
		assertTrue(annotationIntrospector == objectMapper.getDeserializationConfig().getAnnotationIntrospector());

		assertTrue(objectMapper.getSerializationConfig().isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
		assertTrue(objectMapper.getDeserializationConfig().isEnabled(DeserializationFeature.UNWRAP_ROOT_VALUE));
		assertTrue(objectMapper.getFactory().isEnabled(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER));
		assertTrue(objectMapper.getFactory().isEnabled(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS));

		assertFalse(objectMapper.getSerializationConfig().isEnabled(MapperFeature.AUTO_DETECT_GETTERS));
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION));
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(MapperFeature.AUTO_DETECT_FIELDS));
		assertFalse(objectMapper.getFactory().isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
		assertFalse(objectMapper.getFactory().isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
		assertTrue(objectMapper.getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.NON_NULL);
	}

	@Test1
	public void xmlMapper() {
		this.factory.setObjectMapper(new XmlMapper());
		this.factory.afterPropertiesSet();

		assertNotNull(this.factory.getObject());
		assertTrue(this.factory.isSingleton());
		assertEquals(XmlMapper.class, this.factory.getObjectType());
	}

	@Test1
	public void createXmlMapper() {
		this.factory.setCreateXmlMapper(true);
		this.factory.afterPropertiesSet();

		assertNotNull(this.factory.getObject());
		assertTrue(this.factory.isSingleton());
		assertEquals(XmlMapper.class, this.factory.getObjectType());
	}


	public static class CustomIntegerModule extends Module {

		@Override
		public String getModuleName() {
			return this.getClass().getSimpleName();
		}

		@Override
		public Version version() {
			return Version.unknownVersion();
		}

		@Override
		public void setupModule(SetupContext context) {
			SimpleSerializers serializers = new SimpleSerializers();
			serializers.addSerializer(Integer.class, new CustomIntegerSerializer());
			context.addSerializers(serializers);
		}
	}


	public static class CustomIntegerSerializer extends JsonSerializer<Integer> {

		@Override
		public void serialize(Integer value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeStartObject();
			gen.writeNumberField("customid", value);
			gen.writeEndObject();
		}
	}

}
