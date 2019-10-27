/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.util;

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class SystemPropertyUtilsTests {

	@Test1
	public void testReplaceFromSystemProperty() {
		System.setProperty("test.prop", "bar");
		try {
			String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop}");
			assertEquals("bar", resolved);
		}
		finally {
			System.getProperties().remove("test.prop");
		}
	}

	@Test1
	public void testReplaceFromSystemPropertyWithDefault() {
		System.setProperty("test.prop", "bar");
		try {
			String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:foo}");
			assertEquals("bar", resolved);
		}
		finally {
			System.getProperties().remove("test.prop");
		}
	}

	@Test1
	public void testReplaceFromSystemPropertyWithExpressionDefault() {
		System.setProperty("test.prop", "bar");
		try {
			String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:#{foo.bar}}");
			assertEquals("bar", resolved);
		}
		finally {
			System.getProperties().remove("test.prop");
		}
	}

	@Test1
	public void testReplaceFromSystemPropertyWithExpressionContainingDefault() {
		System.setProperty("test.prop", "bar");
		try {
			String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:Y#{foo.bar}X}");
			assertEquals("bar", resolved);
		}
		finally {
			System.getProperties().remove("test.prop");
		}
	}

	@Test1
	public void testReplaceWithDefault() {
		String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:foo}");
		assertEquals("foo", resolved);
	}

	@Test1
	public void testReplaceWithExpressionDefault() {
		String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:#{foo.bar}}");
		assertEquals("#{foo.bar}", resolved);
	}

	@Test1
	public void testReplaceWithExpressionContainingDefault() {
		String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:Y#{foo.bar}X}");
		assertEquals("Y#{foo.bar}X", resolved);
	}

	@Test1(expected=IllegalArgumentException.class)
	public void testReplaceWithNoDefault() {
		String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop}");
		assertEquals("", resolved);
	}

	@Test1
	public void testReplaceWithNoDefaultIgnored() {
		String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop}", true);
		assertEquals("${test.prop}", resolved);
	}

	@Test1
	public void testReplaceWithEmptyDefault() {
		String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:}");
		assertEquals("", resolved);
	}

	@Test1
	public void testRecursiveFromSystemProperty() {
		System.setProperty("test.prop", "foo=${bar}");
		System.setProperty("bar", "baz");
		try {
			String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop}");
			assertEquals("foo=baz", resolved);
		}
		finally {
			System.getProperties().remove("test.prop");
			System.getProperties().remove("bar");
		}
	}

	@Test1
	public void testReplaceFromEnv() {
		Map<String,String> env = System.getenv();
		if (env.containsKey("PATH")) {
			String text = "${PATH}";
			assertEquals(env.get("PATH"), SystemPropertyUtils.resolvePlaceholders(text));
		}
	}

}
