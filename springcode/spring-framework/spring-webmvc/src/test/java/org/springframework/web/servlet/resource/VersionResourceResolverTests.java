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
package org.springframework.web.servlet.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link VersionResourceResolver}
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
public class VersionResourceResolverTests {

	private List<Resource> locations;

	private VersionResourceResolver resolver;

	private ResourceResolverChain chain;

	private VersionStrategy versionStrategy;


	@Before
	public void setup() {
		this.locations = new ArrayList<>();
		this.locations.add(new ClassPathResource("test/", getClass()));
		this.locations.add(new ClassPathResource("testalternatepath/", getClass()));

		this.resolver = new VersionResourceResolver();
		this.chain = mock(ResourceResolverChain.class);
		this.versionStrategy = mock(VersionStrategy.class);
	}

	@Test1
	public void resolveResourceExisting() throws Exception {
		String file = "bar.css";
		Resource expected = new ClassPathResource("test/" + file, getClass());
		given(this.chain.resolveResource(null, file, this.locations)).willReturn(expected);

		this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
		Resource actual = this.resolver.resolveResourceInternal(null, file, this.locations, this.chain);
		assertEquals(expected, actual);
		verify(this.chain, times(1)).resolveResource(null, file, this.locations);
		verify(this.versionStrategy, never()).extractVersion(file);
	}

	@Test1
	public void resolveResourceNoVersionStrategy() throws Exception {
		String file = "missing.css";
		given(this.chain.resolveResource(null, file, this.locations)).willReturn(null);

		this.resolver.setStrategyMap(Collections.emptyMap());
		Resource actual = this.resolver.resolveResourceInternal(null, file, this.locations, this.chain);
		assertNull(actual);
		verify(this.chain, times(1)).resolveResource(null, file, this.locations);
	}

	@Test1
	public void resolveResourceNoVersionInPath() throws Exception {
		String file = "bar.css";
		given(this.chain.resolveResource(null, file, this.locations)).willReturn(null);
		given(this.versionStrategy.extractVersion(file)).willReturn("");

		this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
		Resource actual = this.resolver.resolveResourceInternal(null, file, this.locations, this.chain);
		assertNull(actual);
		verify(this.chain, times(1)).resolveResource(null, file, this.locations);
		verify(this.versionStrategy, times(1)).extractVersion(file);
	}

	@Test1
	public void resolveResourceNoResourceAfterVersionRemoved() throws Exception {
		String versionFile = "bar-version.css";
		String version = "version";
		String file = "bar.css";
		given(this.chain.resolveResource(null, versionFile, this.locations)).willReturn(null);
		given(this.chain.resolveResource(null, file, this.locations)).willReturn(null);
		given(this.versionStrategy.extractVersion(versionFile)).willReturn(version);
		given(this.versionStrategy.removeVersion(versionFile, version)).willReturn(file);

		this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
		Resource actual = this.resolver.resolveResourceInternal(null, versionFile, this.locations, this.chain);
		assertNull(actual);
		verify(this.versionStrategy, times(1)).removeVersion(versionFile, version);
	}

	@Test1
	public void resolveResourceVersionDoesNotMatch() throws Exception {
		String versionFile = "bar-version.css";
		String version = "version";
		String file = "bar.css";
		Resource expected = new ClassPathResource("test/" + file, getClass());
		given(this.chain.resolveResource(null, versionFile, this.locations)).willReturn(null);
		given(this.chain.resolveResource(null, file, this.locations)).willReturn(expected);
		given(this.versionStrategy.extractVersion(versionFile)).willReturn(version);
		given(this.versionStrategy.removeVersion(versionFile, version)).willReturn(file);
		given(this.versionStrategy.getResourceVersion(expected)).willReturn("newer-version");

		this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
		Resource actual = this.resolver.resolveResourceInternal(null, versionFile, this.locations, this.chain);
		assertNull(actual);
		verify(this.versionStrategy, times(1)).getResourceVersion(expected);
	}

	@Test1
	public void resolveResourceSuccess() throws Exception {
		String versionFile = "bar-version.css";
		String version = "version";
		String file = "bar.css";
		Resource expected = new ClassPathResource("test/" + file, getClass());
		given(this.chain.resolveResource(null, versionFile, this.locations)).willReturn(null);
		given(this.chain.resolveResource(null, file, this.locations)).willReturn(expected);
		given(this.versionStrategy.extractVersion(versionFile)).willReturn(version);
		given(this.versionStrategy.removeVersion(versionFile, version)).willReturn(file);
		given(this.versionStrategy.getResourceVersion(expected)).willReturn(version);

		this.resolver
				.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
		Resource actual = this.resolver.resolveResourceInternal(null, versionFile, this.locations, this.chain);
		assertEquals(expected, actual);
		verify(this.versionStrategy, times(1)).getResourceVersion(expected);
	}

	@Test1
	public void getStrategyForPath() throws Exception {
		Map<String, VersionStrategy> strategies = new HashMap<>();
		VersionStrategy jsStrategy = mock(VersionStrategy.class);
		VersionStrategy catchAllStrategy = mock(VersionStrategy.class);
		strategies.put("/**", catchAllStrategy);
		strategies.put("/**/*.js", jsStrategy);
		this.resolver.setStrategyMap(strategies);

		assertEquals(catchAllStrategy, this.resolver.getStrategyForPath("foo.css"));
		assertEquals(catchAllStrategy, this.resolver.getStrategyForPath("foo-js.css"));
		assertEquals(jsStrategy, this.resolver.getStrategyForPath("foo.js"));
		assertEquals(jsStrategy, this.resolver.getStrategyForPath("bar/foo.js"));
	}

}
