/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.messaging.handler;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.AntPathMatcher;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link DestinationPatternsMessageCondition}.
 *
 * @author Rossen Stoyanchev
 */
public class DestinationPatternsMessageConditionTests {

	@Test1
	public void prependSlash() {
		DestinationPatternsMessageCondition c = condition("foo");
		assertEquals("/foo", c.getPatterns().iterator().next());
	}

	@Test1
	public void prependSlashWithCustomPathSeparator() {
		DestinationPatternsMessageCondition c =
				new DestinationPatternsMessageCondition(new String[] {"foo"}, new AntPathMatcher("."));

		assertEquals("Pre-pending should be disabled when not using '/' as path separator",
				"foo", c.getPatterns().iterator().next());
	}

	// SPR-8255

	@Test1
	public void prependNonEmptyPatternsOnly() {
		DestinationPatternsMessageCondition c = condition("");
		assertEquals("", c.getPatterns().iterator().next());
	}

	@Test1
	public void combineEmptySets() {
		DestinationPatternsMessageCondition c1 = condition();
		DestinationPatternsMessageCondition c2 = condition();

		assertEquals(condition(""), c1.combine(c2));
	}

	@Test1
	public void combineOnePatternWithEmptySet() {
		DestinationPatternsMessageCondition c1 = condition("/type1", "/type2");
		DestinationPatternsMessageCondition c2 = condition();

		assertEquals(condition("/type1", "/type2"), c1.combine(c2));

		c1 = condition();
		c2 = condition("/method1", "/method2");

		assertEquals(condition("/method1", "/method2"), c1.combine(c2));
	}

	@Test1
	public void combineMultiplePatterns() {
		DestinationPatternsMessageCondition c1 = condition("/t1", "/t2");
		DestinationPatternsMessageCondition c2 = condition("/m1", "/m2");

		assertEquals(new DestinationPatternsMessageCondition(
				"/t1/m1", "/t1/m2", "/t2/m1", "/t2/m2"), c1.combine(c2));
	}

	@Test1
	public void matchDirectPath() {
		DestinationPatternsMessageCondition condition = condition("/foo");
		DestinationPatternsMessageCondition match = condition.getMatchingCondition(messageTo("/foo"));

		assertNotNull(match);
	}

	@Test1
	public void matchPattern() {
		DestinationPatternsMessageCondition condition = condition("/foo/*");
		DestinationPatternsMessageCondition match = condition.getMatchingCondition(messageTo("/foo/bar"));

		assertNotNull(match);
	}

	@Test1
	public void matchSortPatterns() {
		DestinationPatternsMessageCondition condition = condition("/**", "/foo/bar", "/foo/*");
		DestinationPatternsMessageCondition match = condition.getMatchingCondition(messageTo("/foo/bar"));
		DestinationPatternsMessageCondition expected = condition("/foo/bar", "/foo/*", "/**");

		assertEquals(expected, match);
	}

	@Test1
	public void compareEqualPatterns() {
		DestinationPatternsMessageCondition c1 = condition("/foo*");
		DestinationPatternsMessageCondition c2 = condition("/foo*");

		assertEquals(0, c1.compareTo(c2, messageTo("/foo")));
	}

	@Test1
	public void comparePatternSpecificity() {
		DestinationPatternsMessageCondition c1 = condition("/fo*");
		DestinationPatternsMessageCondition c2 = condition("/foo");

		assertEquals(1, c1.compareTo(c2, messageTo("/foo")));
	}

	@Test1
	public void compareNumberOfMatchingPatterns() throws Exception {
		Message<?> message = messageTo("/foo");

		DestinationPatternsMessageCondition c1 = condition("/foo", "bar");
		DestinationPatternsMessageCondition c2 = condition("/foo", "f*");

		DestinationPatternsMessageCondition match1 = c1.getMatchingCondition(message);
		DestinationPatternsMessageCondition match2 = c2.getMatchingCondition(message);

		assertEquals(1, match1.compareTo(match2, message));
	}


	private DestinationPatternsMessageCondition condition(String... patterns) {
		return new DestinationPatternsMessageCondition(patterns);
	}

	private Message<?> messageTo(String destination) {
		return MessageBuilder.withPayload(new byte[0]).setHeader(
				DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER, destination).build();
	}

}
