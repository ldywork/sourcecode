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

package org.springframework.jms.annotation;

import javax.jms.JMSException;
import javax.jms.Session;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.context.ApplicationContext;
import org.springframework.jms.StubTextMessage;
import org.springframework.jms.config.JmsListenerContainerTestFactory;
import org.springframework.jms.config.JmsListenerEndpoint;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.config.MethodJmsListenerEndpoint;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessagingMessageListenerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Stephane Nicoll
 */
public abstract class AbstractJmsAnnotationDrivenTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test1
	public abstract void sampleConfiguration();

	@Test1
	public abstract void fullConfiguration();

	@Test1
	public abstract void fullConfigurableConfiguration();

	@Test1
	public abstract void customConfiguration();

	@Test1
	public abstract void explicitContainerFactory();

	@Test1
	public abstract void defaultContainerFactory();

	@Test1
	public abstract void jmsHandlerMethodFactoryConfiguration() throws JMSException;

	/**
	 * Test for {@link SampleBean} discovery. If a factory with the default name
	 * is set, an endpoint will use it automatically
	 */
	public void testSampleConfiguration(ApplicationContext context) {
		JmsListenerContainerTestFactory defaultFactory =
				context.getBean("jmsListenerContainerFactory", JmsListenerContainerTestFactory.class);
		JmsListenerContainerTestFactory simpleFactory =
				context.getBean("simpleFactory", JmsListenerContainerTestFactory.class);
		assertEquals(1, defaultFactory.getListenerContainers().size());
		assertEquals(1, simpleFactory.getListenerContainers().size());
	}

	@Component
	static class SampleBean {

		@JmsListener(destination = "myQueue")
		public void defaultHandle(String msg) {
		}

		@JmsListener(containerFactory = "simpleFactory", destination = "myQueue")
		public void simpleHandle(String msg) {
		}
	}

	/**
	 * Test for {@link FullBean} discovery. In this case, no default is set because
	 * all endpoints provide a default registry. This shows that the default factory
	 * is only retrieved if it needs to be.
	 */
	public void testFullConfiguration(ApplicationContext context) {
		JmsListenerContainerTestFactory simpleFactory =
				context.getBean("simpleFactory", JmsListenerContainerTestFactory.class);
		assertEquals(1, simpleFactory.getListenerContainers().size());
		MethodJmsListenerEndpoint endpoint = (MethodJmsListenerEndpoint)
				simpleFactory.getListenerContainers().get(0).getEndpoint();
		assertEquals("listener1", endpoint.getId());
		assertEquals("queueIn", endpoint.getDestination());
		assertEquals("mySelector", endpoint.getSelector());
		assertEquals("mySubscription", endpoint.getSubscription());
		assertEquals("1-10", endpoint.getConcurrency());
	}

	@Component
	static class FullBean {

		@JmsListener(id = "listener1", containerFactory = "simpleFactory", destination = "queueIn",
				selector = "mySelector", subscription = "mySubscription", concurrency = "1-10")
		public String fullHandle(String msg) {
			return "reply";
		}
	}

	@Component
	static class FullConfigurableBean {

		@JmsListener(id = "${jms.listener.id}", containerFactory = "${jms.listener.containerFactory}",
				destination = "${jms.listener.destination}", selector = "${jms.listener.selector}",
				subscription = "${jms.listener.subscription}", concurrency = "${jms.listener.concurrency}")
		public String fullHandle(String msg) {
			return "reply";
		}
	}

	/**
	 * Test for {@link CustomBean} and an manually endpoint registered
	 * with "myCustomEndpointId". The custom endpoint does not provide
	 * any factory so it's registered with the default one
	 */
	public void testCustomConfiguration(ApplicationContext context) {
		JmsListenerContainerTestFactory defaultFactory =
				context.getBean("jmsListenerContainerFactory", JmsListenerContainerTestFactory.class);
		JmsListenerContainerTestFactory customFactory =
				context.getBean("customFactory", JmsListenerContainerTestFactory.class);
		assertEquals(1, defaultFactory.getListenerContainers().size());
		assertEquals(1, customFactory.getListenerContainers().size());
		JmsListenerEndpoint endpoint = defaultFactory.getListenerContainers().get(0).getEndpoint();
		assertEquals("Wrong endpoint type", SimpleJmsListenerEndpoint.class, endpoint.getClass());
		assertEquals("Wrong listener set in custom endpoint", context.getBean("simpleMessageListener"),
				((SimpleJmsListenerEndpoint) endpoint).getMessageListener());

		JmsListenerEndpointRegistry customRegistry =
				context.getBean("customRegistry", JmsListenerEndpointRegistry.class);
		assertEquals("Wrong number of containers in the registry", 2,
				customRegistry.getListenerContainers().size());
		assertNotNull("Container with custom id on the annotation should be found",
				customRegistry.getListenerContainer("listenerId"));
		assertNotNull("Container created with custom id should be found",
				customRegistry.getListenerContainer("myCustomEndpointId"));
	}

	@Component
	static class CustomBean {

		@JmsListener(id = "listenerId", containerFactory = "customFactory", destination = "myQueue")
		public void customHandle(String msg) {
		}
	}

	/**
	 * Test for {@link DefaultBean} that does not define the container
	 * factory to use as a default is registered with an explicit
	 * default.
	 */
	public void testExplicitContainerFactoryConfiguration(ApplicationContext context) {
		JmsListenerContainerTestFactory defaultFactory =
				context.getBean("simpleFactory", JmsListenerContainerTestFactory.class);
		assertEquals(1, defaultFactory.getListenerContainers().size());
	}

	/**
	 * Test for {@link DefaultBean} that does not define the container
	 * factory to use as a default is registered with the default name.
	 */
	public void testDefaultContainerFactoryConfiguration(ApplicationContext context) {
		JmsListenerContainerTestFactory defaultFactory =
				context.getBean("jmsListenerContainerFactory", JmsListenerContainerTestFactory.class);
		assertEquals(1, defaultFactory.getListenerContainers().size());
	}

	static class DefaultBean {

		@JmsListener(destination = "myQueue")
		public void handleIt(String msg) {
		}
	}

	/**
	 * Test for {@link ValidationBean} with a validator ({@link TestValidator}) specified
	 * in a custom {@link org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory}.
	 *
	 * The test should throw a {@link org.springframework.jms.listener.adapter.ListenerExecutionFailedException}
	 */
	public void testJmsHandlerMethodFactoryConfiguration(ApplicationContext context) throws JMSException {
		JmsListenerContainerTestFactory simpleFactory =
				context.getBean("defaultFactory", JmsListenerContainerTestFactory.class);
		assertEquals(1, simpleFactory.getListenerContainers().size());
		MethodJmsListenerEndpoint endpoint = (MethodJmsListenerEndpoint)
				simpleFactory.getListenerContainers().get(0).getEndpoint();

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		endpoint.setupListenerContainer(container);
		MessagingMessageListenerAdapter listener = (MessagingMessageListenerAdapter) container.getMessageListener();
		listener.onMessage(new StubTextMessage("failValidation"), mock(Session.class));
	}

	@Component
	static class ValidationBean {

		@JmsListener(containerFactory = "defaultFactory", destination = "myQueue")
		public void defaultHandle(@Validated String msg) {
		}
	}

	static class TestValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return String.class.isAssignableFrom(clazz);
		}

		@Override
		public void validate(Object target, Errors errors) {
			String value = (String) target;
			if ("failValidation".equals(value)) {
				errors.reject("TEST: expected invalid value");
			}
		}
	}
}
