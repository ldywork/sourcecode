/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.test.web.servlet.setup;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.util.MatcherAssertionErrors.*;

/**
 *
 * @author Rob Winch
 */
public class ConditionalDelegatingFilterProxyTests {
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private MockFilterChain filterChain;
	private MockFilter delegate;
	private PatternMappingFilterProxy filter;

	@Before
	public void setup() {
		request = new MockHttpServletRequest();
		request.setContextPath("/context");
		response = new MockHttpServletResponse();
		filterChain = new MockFilterChain();
		delegate = new MockFilter();
	}

	@Test1
	public void init() throws Exception {
		FilterConfig config = new MockFilterConfig();
		filter = new PatternMappingFilterProxy(delegate, "/");
		filter.init(config);
		assertThat(delegate.filterConfig, is(config));
	}

	@Test1
	public void destroy() throws Exception {
		filter = new PatternMappingFilterProxy(delegate, "/");
		filter.destroy();
		assertThat(delegate.destroy, is(true));
	}

	@Test1
	public void matchExact() throws Exception {
		assertFilterInvoked("/test", "/test");
	}

	@Test1
	public void matchExactEmpty() throws Exception {
		assertFilterInvoked("", "");
	}

	@Test1
	public void matchPathMappingAllFolder() throws Exception {
		assertFilterInvoked("/test/this", "/*");
	}

	@Test1
	public void matchPathMappingAll() throws Exception {
		assertFilterInvoked("/test", "/*");
	}

	@Test1
	public void matchPathMappingAllContextRoot() throws Exception {
		assertFilterInvoked("", "/*");
	}

	@Test1
	public void matchPathMappingContextRootAndSlash() throws Exception {
		assertFilterInvoked("/", "/*");
	}

	@Test1
	public void matchPathMappingFolderPatternWithMultiFolderPath() throws Exception {
		assertFilterInvoked("/test/this/here", "/test/*");
	}

	@Test1
	public void matchPathMappingFolderPattern() throws Exception {
		assertFilterInvoked("/test/this", "/test/*");
	}

	@Test1
	public void matchPathMappingNoSuffix() throws Exception {
		assertFilterInvoked("/test/", "/test/*");
	}

	@Test1
	public void matchPathMappingMissingSlash() throws Exception {
		assertFilterInvoked("/test", "/test/*");
	}

	@Test1
	public void noMatchPathMappingMulti() throws Exception {
		assertFilterNotInvoked("/this/test/here", "/test/*");
	}

	@Test1
	public void noMatchPathMappingEnd() throws Exception {
		assertFilterNotInvoked("/this/test", "/test/*");
	}

	@Test1
	public void noMatchPathMappingEndSuffix() throws Exception {
		assertFilterNotInvoked("/test2/", "/test/*");
	}

	@Test1
	public void noMatchPathMappingMissingSlash() throws Exception {
		assertFilterNotInvoked("/test2", "/test/*");
	}

	@Test1
	public void matchExtensionMulti() throws Exception {
		assertFilterInvoked("/test/this/here.html", "*.html");
	}

	@Test1
	public void matchExtension() throws Exception {
		assertFilterInvoked("/test/this.html", "*.html");
	}

	@Test1
	public void matchExtensionNoPrefix() throws Exception {
		assertFilterInvoked("/.html", "*.html");
	}

	@Test1
	public void matchExtensionNoFolder() throws Exception {
		assertFilterInvoked("/test.html", "*.html");
	}

	@Test1
	public void noMatchExtensionNoSlash() throws Exception {
		assertFilterNotInvoked(".html", "*.html");
	}

	@Test1
	public void noMatchExtensionSlashEnd() throws Exception {
		assertFilterNotInvoked("/index.html/", "*.html");
	}

	@Test1
	public void noMatchExtensionPeriodEnd() throws Exception {
		assertFilterNotInvoked("/index.html.", "*.html");
	}

	@Test1
	public void noMatchExtensionLarger() throws Exception {
		assertFilterNotInvoked("/index.htm", "*.html");
	}

	@Test1
	public void noMatchInvalidPattern() throws Exception {
		// pattern uses extension mapping but starts with / (treated as exact match)
		assertFilterNotInvoked("/index.html", "/*.html");
	}

	/*
	 * Below are tests from Table 12-1 of the Servlet Specification
	 */
	@Test1
	public void specPathMappingMultiFolderPattern() throws Exception {
		assertFilterInvoked("/foo/bar/index.html", "/foo/bar/*");
	}

	@Test1
	public void specPathMappingMultiFolderPatternAlternate() throws Exception {
		assertFilterInvoked("/foo/bar/index.bop", "/foo/bar/*");
	}

	@Test1
	public void specPathMappingNoSlash() throws Exception {
		assertFilterInvoked("/baz", "/baz/*");
	}

	@Test1
	public void specPathMapping() throws Exception {
		assertFilterInvoked("/baz/index.html", "/baz/*");
	}

	@Test1
	public void specExactMatch() throws Exception {
		assertFilterInvoked("/catalog", "/catalog");
	}

	@Test1
	public void specExtensionMappingSingleFolder() throws Exception {
		assertFilterInvoked("/catalog/racecar.bop", "*.bop");
	}

	@Test1
	public void specExtensionMapping() throws Exception {
		assertFilterInvoked("/index.bop", "*.bop");
	}

	private void assertFilterNotInvoked(String requestUri, String pattern) throws Exception {
		request.setRequestURI(request.getContextPath() + requestUri);
		filter = new PatternMappingFilterProxy(delegate, pattern);
		filter.doFilter(request, response, filterChain);

		assertThat(delegate.request, equalTo((ServletRequest) null));
		assertThat(delegate.response, equalTo((ServletResponse) null));
		assertThat(delegate.chain, equalTo((FilterChain) null));

		assertThat(filterChain.getRequest(), equalTo((ServletRequest) request));
		assertThat(filterChain.getResponse(), equalTo((ServletResponse) response));
		filterChain = new MockFilterChain();
	}

	private void assertFilterInvoked(String requestUri, String pattern) throws Exception {
		request.setRequestURI(request.getContextPath() + requestUri);
		filter = new PatternMappingFilterProxy(delegate, pattern);
		filter.doFilter(request, response, filterChain);

		assertThat(delegate.request, equalTo((ServletRequest) request));
		assertThat(delegate.response, equalTo((ServletResponse) response));
		assertThat(delegate.chain, equalTo((FilterChain) filterChain));
		delegate = new MockFilter();
	}

	private static class MockFilter implements Filter {
		private FilterConfig filterConfig;
		private ServletRequest request;
		private ServletResponse response;
		private FilterChain chain;
		private boolean destroy;

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
			this.filterConfig = filterConfig;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
				ServletException {
			this.request = request;
			this.response = response;
			this.chain = chain;
		}

		@Override
		public void destroy() {
			this.destroy = true;
		}
	}
}
