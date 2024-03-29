/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.jms.support;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.SimpleMessageConverter;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for the {@link SimpleMessageConverter} class.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 18.09.2004
 */
public final class SimpleMessageConverterTests {

	@Test1
	public void testStringConversion() throws JMSException {
		Session session = mock(Session.class);
		TextMessage message = mock(TextMessage.class);

		String content = "test";

		given(session.createTextMessage(content)).willReturn(message);
		given(message.getText()).willReturn(content);

		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message msg = converter.toMessage(content, session);
		assertEquals(content, converter.fromMessage(msg));
	}

	@Test1
	public void testByteArrayConversion() throws JMSException {
		Session session = mock(Session.class);
		BytesMessage message = mock(BytesMessage.class);

		byte[] content = "test".getBytes();
		final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);

		given(session.createBytesMessage()).willReturn(message);
		given(message.getBodyLength()).willReturn((long) content.length);
		given(message.readBytes(any(byte[].class))).willAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				return byteArrayInputStream.read((byte[]) invocation.getArguments()[0]);
			}
		});

		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message msg = converter.toMessage(content, session);
		assertEquals(content.length, ((byte[]) converter.fromMessage(msg)).length);

		verify(message).writeBytes(content);
	}

	@Test1
	public void testMapConversion() throws JMSException {

		Session session = mock(Session.class);
		MapMessage message = mock(MapMessage.class);

		Map<String, String> content = new HashMap<String, String>(2);
		content.put("key1", "value1");
		content.put("key2", "value2");

		given(session.createMapMessage()).willReturn(message);
		given(message.getMapNames()).willReturn(Collections.enumeration(content.keySet()));
		given(message.getObject("key1")).willReturn("value1");
		given(message.getObject("key2")).willReturn("value2");

		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message msg = converter.toMessage(content, session);
		assertEquals(content, converter.fromMessage(msg));

		verify(message).setObject("key1", "value1");
		verify(message).setObject("key2", "value2");
	}

	@Test1
	public void testSerializableConversion() throws JMSException {
		Session session = mock(Session.class);
		ObjectMessage message = mock(ObjectMessage.class);

		Integer content = new Integer(5);

		given(session.createObjectMessage(content)).willReturn(message);
		given(message.getObject()).willReturn(content);

		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message msg = converter.toMessage(content, session);
		assertEquals(content, converter.fromMessage(msg));
	}

	@Test1(expected=MessageConversionException.class)
	public void testToMessageThrowsExceptionIfGivenNullObjectToConvert() throws Exception {
		new SimpleMessageConverter().toMessage(null, null);
	}

	@Test1(expected=MessageConversionException.class)
	public void testToMessageThrowsExceptionIfGivenIncompatibleObjectToConvert() throws Exception {
		new SimpleMessageConverter().toMessage(new Object(), null);
	}

	@Test1
	public void testToMessageSimplyReturnsMessageAsIsIfSuppliedWithMessage() throws JMSException {

		Session session = mock(Session.class);
		ObjectMessage message = mock(ObjectMessage.class);

		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message msg = converter.toMessage(message, session);
		assertSame(message, msg);
	}

	@Test1
	public void testFromMessageSimplyReturnsMessageAsIsIfSuppliedWithMessage() throws JMSException {

		Message message = mock(Message.class);

		SimpleMessageConverter converter = new SimpleMessageConverter();
		Object msg = converter.fromMessage(message);
		assertSame(message, msg);
	}

	@Test1
	public void testMapConversionWhereMapHasNonStringTypesForKeys() throws JMSException {

		MapMessage message = mock(MapMessage.class);
		final Session session = mock(Session.class);
		given(session.createMapMessage()).willReturn(message);

		final Map<Integer, String> content = new HashMap<Integer, String>(1);
		content.put(1, "value1");

		final SimpleMessageConverter converter = new SimpleMessageConverter();
		try {
			converter.toMessage(content, session);
			fail("expected MessageConversionException");
		} catch (MessageConversionException ex) { /* expected */ }
	}

	@Test1
	public void testMapConversionWhereMapHasNNullForKey() throws JMSException {

		MapMessage message = mock(MapMessage.class);
		final Session session = mock(Session.class);
		given(session.createMapMessage()).willReturn(message);

		final Map<Object, String> content = new HashMap<Object, String>(1);
		content.put(null, "value1");

		final SimpleMessageConverter converter = new SimpleMessageConverter();
		try {
			converter.toMessage(content, session);
			fail("expected MessageConversionException");
		} catch (MessageConversionException ex) { /* expected */ }
	}

}
