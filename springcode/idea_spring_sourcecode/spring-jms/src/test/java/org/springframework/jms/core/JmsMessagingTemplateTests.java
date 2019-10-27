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

package org.springframework.jms.core;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotWriteableException;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.jms.InvalidDestinationException;
import org.springframework.jms.MessageNotReadableException;
import org.springframework.jms.StubTextMessage;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolutionException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Stephane Nicoll
 */
public class JmsMessagingTemplateTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Captor
	private ArgumentCaptor<MessageCreator> messageCreator;

	@Mock
	private JmsTemplate jmsTemplate;

	private JmsMessagingTemplate messagingTemplate;


	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		messagingTemplate = new JmsMessagingTemplate(jmsTemplate);
	}

	@Test1
	public void validateJmsTemplate() {
		assertSame(this.jmsTemplate, messagingTemplate.getJmsTemplate());
	}

	@Test1
	public void send() {
		Destination destination = new Destination() {};
		Message<String> message = createTextMessage();

		messagingTemplate.send(destination, message);
		verify(jmsTemplate).send(eq(destination), messageCreator.capture());
		assertTextMessage(messageCreator.getValue());
	}

	@Test1
	public void sendName() {
		Message<String> message = createTextMessage();

		messagingTemplate.send("myQueue", message);
		verify(jmsTemplate).send(eq("myQueue"), messageCreator.capture());
		assertTextMessage(messageCreator.getValue());
	}

	@Test1
	public void sendDefaultDestination() {
		Destination destination = new Destination() {};
		messagingTemplate.setDefaultDestination(destination);
		Message<String> message = createTextMessage();

		messagingTemplate.send(message);
		verify(jmsTemplate).send(eq(destination), messageCreator.capture());
		assertTextMessage(messageCreator.getValue());
	}

	@Test1
	public void sendDefaultDestinationName() {
		messagingTemplate.setDefaultDestinationName("myQueue");
		Message<String> message = createTextMessage();

		messagingTemplate.send(message);
		verify(jmsTemplate).send(eq("myQueue"), messageCreator.capture());
		assertTextMessage(messageCreator.getValue());
	}

	@Test1
	public void sendNoDefaultSet() {
		Message<String> message = createTextMessage();

		thrown.expect(IllegalStateException.class);
		messagingTemplate.send(message);
	}

	@Test1
	public void sendPropertyInjection() {
		JmsMessagingTemplate t = new JmsMessagingTemplate();
		t.setJmsTemplate(jmsTemplate);
		t.setDefaultDestinationName("myQueue");
		t.afterPropertiesSet();
		Message<String> message = createTextMessage();

		t.send(message);
		verify(jmsTemplate).send(eq("myQueue"), messageCreator.capture());
		assertTextMessage(messageCreator.getValue());
	}

	@Test1
	public void convertAndSendPayload() throws JMSException {
		Destination destination = new Destination() {};

		messagingTemplate.convertAndSend(destination, "my Payload");
		verify(jmsTemplate).send(eq(destination), messageCreator.capture());
		TextMessage textMessage = createTextMessage(messageCreator.getValue());
		assertEquals("my Payload", textMessage.getText());
	}

	@Test1
	public void convertAndSendPayloadName() throws JMSException {
		messagingTemplate.convertAndSend("myQueue", "my Payload");
		verify(jmsTemplate).send(eq("myQueue"), messageCreator.capture());
		TextMessage textMessage = createTextMessage(messageCreator.getValue());
		assertEquals("my Payload", textMessage.getText());
	}

	@Test1
	public void convertAndSendDefaultDestination() throws JMSException {
		Destination destination = new Destination() {};
		messagingTemplate.setDefaultDestination(destination);

		messagingTemplate.convertAndSend("my Payload");
		verify(jmsTemplate).send(eq(destination), messageCreator.capture());
		TextMessage textMessage = createTextMessage(messageCreator.getValue());
		assertEquals("my Payload", textMessage.getText());
	}

	@Test1
	public void convertAndSendDefaultDestinationName() throws JMSException {
		messagingTemplate.setDefaultDestinationName("myQueue");

		messagingTemplate.convertAndSend("my Payload");
		verify(jmsTemplate).send(eq("myQueue"), messageCreator.capture());
		TextMessage textMessage = createTextMessage(messageCreator.getValue());
		assertEquals("my Payload", textMessage.getText());
	}

	@Test1
	public void convertAndSendNoDefaultSet() throws JMSException {
		thrown.expect(IllegalStateException.class);
		messagingTemplate.convertAndSend("my Payload");
	}

	@Test1
	public void convertAndSendCustomJmsMessageConverter() throws JMSException {
		messagingTemplate.setJmsMessageConverter(new SimpleMessageConverter() {
			@Override
			public javax.jms.Message toMessage(Object object, Session session)
					throws JMSException, org.springframework.jms.support.converter.MessageConversionException {
				throw new org.springframework.jms.support.converter.MessageConversionException("Test exception");
			}
		});

		messagingTemplate.convertAndSend("myQueue", "msg to convert");
		verify(jmsTemplate).send(eq("myQueue"), messageCreator.capture());

		thrown.expect(org.springframework.messaging.converter.MessageConversionException.class);
		thrown.expectMessage(new StringContains("Test exception"));
		messageCreator.getValue().createMessage(mock(Session.class));
	}

	@Test1
	public void convertAndSendPayloadAndHeaders() throws JMSException {
		Destination destination = new Destination() {};
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "bar");

		messagingTemplate.convertAndSend(destination, "Hello", headers);
		verify(jmsTemplate).send(eq(destination), messageCreator.capture());
		assertTextMessage(messageCreator.getValue()); // see createTextMessage
	}

	@Test1
	public void convertAndSendPayloadAndHeadersName() throws JMSException {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "bar");

		messagingTemplate.convertAndSend("myQueue", "Hello", headers);
		verify(jmsTemplate).send(eq("myQueue"), messageCreator.capture());
		assertTextMessage(messageCreator.getValue()); // see createTextMessage
	}

	@Test1
	public void receive() {
		Destination destination = new Destination() {};
		javax.jms.Message jmsMessage = createJmsTextMessage();
		given(jmsTemplate.receive(destination)).willReturn(jmsMessage);

		Message<?> message = messagingTemplate.receive(destination);
		verify(jmsTemplate).receive(destination);
		assertTextMessage(message);
	}

	@Test1
	public void receiveName() {
		javax.jms.Message jmsMessage = createJmsTextMessage();
		given(jmsTemplate.receive("myQueue")).willReturn(jmsMessage);

		Message<?> message = messagingTemplate.receive("myQueue");
		verify(jmsTemplate).receive("myQueue");
		assertTextMessage(message);
	}

	@Test1
	public void receiveDefaultDestination() {
		Destination destination = new Destination() {};
		messagingTemplate.setDefaultDestination(destination);
		javax.jms.Message jmsMessage = createJmsTextMessage();
		given(jmsTemplate.receive(destination)).willReturn(jmsMessage);

		Message<?> message = messagingTemplate.receive();
		verify(jmsTemplate).receive(destination);
		assertTextMessage(message);
	}

	@Test1
	public void receiveDefaultDestinationName() {
		messagingTemplate.setDefaultDestinationName("myQueue");
		javax.jms.Message jmsMessage = createJmsTextMessage();
		given(jmsTemplate.receive("myQueue")).willReturn(jmsMessage);

		Message<?> message = messagingTemplate.receive();
		verify(jmsTemplate).receive("myQueue");
		assertTextMessage(message);
	}

	@Test1
	public void receiveNoDefaultSet() {
		thrown.expect(IllegalStateException.class);
		messagingTemplate.receive();
	}

	@Test1
	public void receiveAndConvert() {
		Destination destination = new Destination() {};
		javax.jms.Message jmsMessage = createJmsTextMessage("my Payload");
		given(jmsTemplate.receive(destination)).willReturn(jmsMessage);

		String payload = messagingTemplate.receiveAndConvert(destination, String.class);
		assertEquals("my Payload", payload);
		verify(jmsTemplate).receive(destination);
	}

	@Test1
	public void receiveAndConvertName() {
		javax.jms.Message jmsMessage = createJmsTextMessage("my Payload");
		given(jmsTemplate.receive("myQueue")).willReturn(jmsMessage);

		String payload = messagingTemplate.receiveAndConvert("myQueue", String.class);
		assertEquals("my Payload", payload);
		verify(jmsTemplate).receive("myQueue");
	}

	@Test1
	public void receiveAndConvertDefaultDestination() {
		Destination destination = new Destination() {};
		messagingTemplate.setDefaultDestination(destination);
		javax.jms.Message jmsMessage = createJmsTextMessage("my Payload");
		given(jmsTemplate.receive(destination)).willReturn(jmsMessage);

		String payload = messagingTemplate.receiveAndConvert(String.class);
		assertEquals("my Payload", payload);
		verify(jmsTemplate).receive(destination);
	}

	@Test1
	public void receiveAndConvertDefaultDestinationName() {
		messagingTemplate.setDefaultDestinationName("myQueue");
		javax.jms.Message jmsMessage = createJmsTextMessage("my Payload");
		given(jmsTemplate.receive("myQueue")).willReturn(jmsMessage);

		String payload = messagingTemplate.receiveAndConvert(String.class);
		assertEquals("my Payload", payload);
		verify(jmsTemplate).receive("myQueue");
	}

	@Test1
	public void receiveAndConvertWithConversion() {
		javax.jms.Message jmsMessage = createJmsTextMessage("123");
		given(jmsTemplate.receive("myQueue")).willReturn(jmsMessage);

		messagingTemplate.setMessageConverter(new GenericMessageConverter());

		Integer payload = messagingTemplate.receiveAndConvert("myQueue", Integer.class);
		assertEquals(Integer.valueOf(123), payload);
		verify(jmsTemplate).receive("myQueue");
	}

	@Test1
	public void receiveAndConvertNoConverter() {
		javax.jms.Message jmsMessage = createJmsTextMessage("Hello");
		given(jmsTemplate.receive("myQueue")).willReturn(jmsMessage);

		thrown.expect(org.springframework.messaging.converter.MessageConversionException.class);
		messagingTemplate.receiveAndConvert("myQueue", Writer.class);
	}

	@Test1
	public void receiveAndConvertNoInput() {
		given(jmsTemplate.receive("myQueue")).willReturn(null);

		assertNull(messagingTemplate.receiveAndConvert("myQueue", String.class));
	}

	@Test1
	public void sendAndReceive() {
		Destination destination = new Destination() {};
		Message<String> request = createTextMessage();
		javax.jms.Message replyJmsMessage = createJmsTextMessage();
		given(jmsTemplate.sendAndReceive(eq(destination), anyObject())).willReturn(replyJmsMessage);

		Message<?> actual = messagingTemplate.sendAndReceive(destination, request);
		verify(jmsTemplate, times(1)).sendAndReceive(eq(destination), anyObject());
		assertTextMessage(actual);
	}

	@Test1
	public void sendAndReceiveName() {
		Message<String> request = createTextMessage();
		javax.jms.Message replyJmsMessage = createJmsTextMessage();
		given(jmsTemplate.sendAndReceive(eq("myQueue"), anyObject())).willReturn(replyJmsMessage);

		Message<?> actual = messagingTemplate.sendAndReceive("myQueue", request);
		verify(jmsTemplate, times(1)).sendAndReceive(eq("myQueue"), anyObject());
		assertTextMessage(actual);
	}

	@Test1
	public void sendAndReceiveDefaultDestination() {
		Destination destination = new Destination() {};
		messagingTemplate.setDefaultDestination(destination);
		Message<String> request = createTextMessage();
		javax.jms.Message replyJmsMessage = createJmsTextMessage();
		given(jmsTemplate.sendAndReceive(eq(destination), anyObject())).willReturn(replyJmsMessage);

		Message<?> actual = messagingTemplate.sendAndReceive(request);
		verify(jmsTemplate, times(1)).sendAndReceive(eq(destination), anyObject());
		assertTextMessage(actual);
	}

	@Test1
	public void sendAndReceiveDefaultDestinationName() {
		messagingTemplate.setDefaultDestinationName("myQueue");
		Message<String> request = createTextMessage();
		javax.jms.Message replyJmsMessage = createJmsTextMessage();
		given(jmsTemplate.sendAndReceive(eq("myQueue"), anyObject())).willReturn(replyJmsMessage);

		Message<?> actual = messagingTemplate.sendAndReceive(request);
		verify(jmsTemplate, times(1)).sendAndReceive(eq("myQueue"), anyObject());
		assertTextMessage(actual);
	}

	@Test1
	public void sendAndReceiveNoDefaultSet() {
		Message<String> message = createTextMessage();

		thrown.expect(IllegalStateException.class);
		messagingTemplate.sendAndReceive(message);
	}

	@Test1
	public void convertSendAndReceivePayload() throws JMSException {
		Destination destination = new Destination() {};
		javax.jms.Message replyJmsMessage = createJmsTextMessage("My reply");
		given(jmsTemplate.sendAndReceive(eq(destination), anyObject())).willReturn(replyJmsMessage);

		String reply = messagingTemplate.convertSendAndReceive(destination, "my Payload", String.class);
		verify(jmsTemplate, times(1)).sendAndReceive(eq(destination), anyObject());
		assertEquals("My reply", reply);
	}

	@Test1
	public void convertSendAndReceivePayloadName() throws JMSException {
		javax.jms.Message replyJmsMessage = createJmsTextMessage("My reply");
		given(jmsTemplate.sendAndReceive(eq("myQueue"), anyObject())).willReturn(replyJmsMessage);

		String reply = messagingTemplate.convertSendAndReceive("myQueue", "my Payload", String.class);
		verify(jmsTemplate, times(1)).sendAndReceive(eq("myQueue"), anyObject());
		assertEquals("My reply", reply);
	}

	@Test1
	public void convertSendAndReceiveDefaultDestination() throws JMSException {
		Destination destination = new Destination() {};
		messagingTemplate.setDefaultDestination(destination);
		javax.jms.Message replyJmsMessage = createJmsTextMessage("My reply");
		given(jmsTemplate.sendAndReceive(eq(destination), anyObject())).willReturn(replyJmsMessage);

		String reply = messagingTemplate.convertSendAndReceive("my Payload", String.class);
		verify(jmsTemplate, times(1)).sendAndReceive(eq(destination), anyObject());
		assertEquals("My reply", reply);
	}

	@Test1
	public void convertSendAndReceiveDefaultDestinationName() throws JMSException {
		messagingTemplate.setDefaultDestinationName("myQueue");
		javax.jms.Message replyJmsMessage = createJmsTextMessage("My reply");
		given(jmsTemplate.sendAndReceive(eq("myQueue"), anyObject())).willReturn(replyJmsMessage);

		String reply = messagingTemplate.convertSendAndReceive("my Payload", String.class);
		verify(jmsTemplate, times(1)).sendAndReceive(eq("myQueue"), anyObject());
		assertEquals("My reply", reply);
	}

	@Test1
	public void convertSendAndReceiveNoDefaultSet() throws JMSException {
		thrown.expect(IllegalStateException.class);
		messagingTemplate.convertSendAndReceive("my Payload", String.class);
	}

	@Test1
	public void convertMessageConversionExceptionOnSend() throws JMSException {
		Message<String> message = createTextMessage();
		MessageConverter messageConverter = mock(MessageConverter.class);
		willThrow(org.springframework.jms.support.converter.MessageConversionException.class)
				.given(messageConverter).toMessage(eq(message), anyObject());
		messagingTemplate.setJmsMessageConverter(messageConverter);
		invokeMessageCreator("myQueue");

		thrown.expect(org.springframework.messaging.converter.MessageConversionException.class);
		messagingTemplate.send("myQueue", message);
	}

	@Test1
	public void convertMessageConversionExceptionOnReceive() throws JMSException {
		javax.jms.Message message = createJmsTextMessage();
		MessageConverter messageConverter = mock(MessageConverter.class);
		willThrow(org.springframework.jms.support.converter.MessageConversionException.class)
				.given(messageConverter).fromMessage(message);
		messagingTemplate.setJmsMessageConverter(messageConverter);
		given(jmsTemplate.receive("myQueue")).willReturn(message);

		thrown.expect(org.springframework.messaging.converter.MessageConversionException.class);
		messagingTemplate.receive("myQueue");
	}

	@Test1
	public void convertMessageNotReadableException() throws JMSException {
		willThrow(MessageNotReadableException.class).given(jmsTemplate).receive("myQueue");

		thrown.expect(MessagingException.class);
		messagingTemplate.receive("myQueue");
	}

	@Test1
	public void convertDestinationResolutionExceptionOnSend() {
		Destination destination = new Destination() {};
		willThrow(DestinationResolutionException.class).given(jmsTemplate).send(eq(destination), anyObject());

		thrown.expect(org.springframework.messaging.core.DestinationResolutionException.class);
		messagingTemplate.send(destination, createTextMessage());
	}

	@Test1
	public void convertDestinationResolutionExceptionOnReceive() {
		Destination destination = new Destination() {};
		willThrow(DestinationResolutionException.class).given(jmsTemplate).receive(destination);

		thrown.expect(org.springframework.messaging.core.DestinationResolutionException.class);
		messagingTemplate.receive(destination);
	}

	@Test1
	public void convertMessageFormatException() throws JMSException {
		Message<String> message = createTextMessage();
		MessageConverter messageConverter = mock(MessageConverter.class);
		willThrow(MessageFormatException.class).given(messageConverter).toMessage(eq(message), anyObject());
		messagingTemplate.setJmsMessageConverter(messageConverter);
		invokeMessageCreator("myQueue");

		thrown.expect(org.springframework.messaging.converter.MessageConversionException.class);
		messagingTemplate.send("myQueue", message);
	}

	@Test1
	public void convertMessageNotWritableException() throws JMSException {
		Message<String> message = createTextMessage();
		MessageConverter messageConverter = mock(MessageConverter.class);
		willThrow(MessageNotWriteableException.class).given(messageConverter).toMessage(eq(message), anyObject());
		messagingTemplate.setJmsMessageConverter(messageConverter);
		invokeMessageCreator("myQueue");

		thrown.expect(org.springframework.messaging.converter.MessageConversionException.class);
		messagingTemplate.send("myQueue", message);
	}

	@Test1
	public void convertInvalidDestinationExceptionOnSendAndReceiveWithName() {
		willThrow(InvalidDestinationException.class).given(jmsTemplate).sendAndReceive(eq("unknownQueue"), anyObject());

		thrown.expect(org.springframework.messaging.core.DestinationResolutionException.class);
		messagingTemplate.sendAndReceive("unknownQueue", createTextMessage());
	}

	@Test1
	public void convertInvalidDestinationExceptionOnSendAndReceive() {
		Destination destination = new Destination() {};
		willThrow(InvalidDestinationException.class).given(jmsTemplate).sendAndReceive(eq(destination), anyObject());

		thrown.expect(org.springframework.messaging.core.DestinationResolutionException.class);
		messagingTemplate.sendAndReceive(destination, createTextMessage());
	}

	private void invokeMessageCreator(String destinationName) {
		willAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				MessageCreator messageCreator = (MessageCreator) invocation.getArguments()[1];
				messageCreator.createMessage(null);
				return null;
			}
		}).given(jmsTemplate).send(eq("myQueue"), anyObject());
	}


	private Message<String> createTextMessage(String payload) {
		return MessageBuilder
				.withPayload(payload).setHeader("foo", "bar").build();
	}

	private Message<String> createTextMessage() {
		return createTextMessage("Hello");
	}

	private javax.jms.Message createJmsTextMessage(String payload) {
		try {
			StubTextMessage jmsMessage = new StubTextMessage(payload);
			jmsMessage.setStringProperty("foo", "bar");
			return jmsMessage;
		}
		catch (JMSException e) {
			throw new IllegalStateException("Should not happen", e);
		}
	}

	private javax.jms.Message createJmsTextMessage() {
		return createJmsTextMessage("Hello");
	}


	private void assertTextMessage(MessageCreator messageCreator) {
		try {
			TextMessage jmsMessage = createTextMessage(messageCreator);
			assertEquals("Wrong body message", "Hello", jmsMessage.getText());
			assertEquals("Invalid foo property", "bar", jmsMessage.getStringProperty("foo"));
		}
		catch (JMSException e) {
			throw new IllegalStateException("Wrong text message", e);
		}
	}

	private void assertTextMessage(Message<?> message) {
		assertNotNull("message should not be null", message);
		assertEquals("Wrong payload", "Hello", message.getPayload());
		assertEquals("Invalid foo property", "bar", message.getHeaders().get("foo"));
	}


	protected TextMessage createTextMessage(MessageCreator creator) throws JMSException {
		Session mock = mock(Session.class);
		given(mock.createTextMessage(BDDMockito.<String> any())).willAnswer(
				new Answer<TextMessage>() {
			@Override
			public TextMessage answer(InvocationOnMock invocation) throws Throwable {
				return new StubTextMessage((String) invocation.getArguments()[0]);
			}
		});
		javax.jms.Message message = creator.createMessage(mock);
		verify(mock).createTextMessage(BDDMockito.<String> any());
		return TextMessage.class.cast(message);
	}

}
