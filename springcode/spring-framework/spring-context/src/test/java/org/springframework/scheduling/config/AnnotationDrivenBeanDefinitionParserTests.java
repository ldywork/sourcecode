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

package org.springframework.scheduling.config;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 */
public class AnnotationDrivenBeanDefinitionParserTests {

	private ApplicationContext context;


	@Before
	public void setup() {
		this.context = new ClassPathXmlApplicationContext(
				"annotationDrivenContext.xml", AnnotationDrivenBeanDefinitionParserTests.class);
	}


	@Test1
	public void asyncPostProcessorRegistered() {
		assertTrue(context.containsBean(TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME));
	}

	@Test1
	public void scheduledPostProcessorRegistered() {
		assertTrue(context.containsBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME));
	}

	@Test1
	public void asyncPostProcessorExecutorReference() {
		Object executor = context.getBean("testExecutor");
		Object postProcessor = context.getBean(TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME);
		assertSame(executor, new DirectFieldAccessor(postProcessor).getPropertyValue("executor"));
	}

	@Test1
	public void scheduledPostProcessorSchedulerReference() {
		Object scheduler = context.getBean("testScheduler");
		Object postProcessor = context.getBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME);
		assertSame(scheduler, new DirectFieldAccessor(postProcessor).getPropertyValue("scheduler"));
	}

}
