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
package org.springframework.test.web.client.samples.matchers;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.Person;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Examples of defining expectations on JSON request content with
 * <a href="http://goessner.net/articles/JsonPath/">JSONPath</a> expressions.
 *
 * @author Rossen Stoyanchev
 */
public class JsonPathRequestMatcherTests {

	private MockRestServiceServer mockServer;

	private RestTemplate restTemplate;

	private MultiValueMap<String, Person> people;


	@Before
	public void setup() {
		this.people = new LinkedMultiValueMap<String, Person>();
		this.people.add("composers", new Person("Johann Sebastian Bach"));
		this.people.add("composers", new Person("Johannes Brahms"));
		this.people.add("composers", new Person("Edvard Grieg"));
		this.people.add("composers", new Person("Robert Schumann"));
		this.people.add("performers", new Person("Vladimir Ashkenazy"));
		this.people.add("performers", new Person("Yehudi Menuhin"));

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new MappingJackson2HttpMessageConverter());

		this.restTemplate = new RestTemplate();
		this.restTemplate.setMessageConverters(converters);

		this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
	}

	@Test1
	public void testExists() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json;charset=UTF-8"))
			.andExpect(jsonPath("$.composers[0]").exists())
			.andExpect(jsonPath("$.composers[1]").exists())
			.andExpect(jsonPath("$.composers[2]").exists())
			.andExpect(jsonPath("$.composers[3]").exists())
			.andRespond(withSuccess());

		this.restTemplate.put(new URI("/composers"), this.people);
		this.mockServer.verify();
	}

	@Test1
	public void testDoesNotExist() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json;charset=UTF-8"))
			.andExpect(jsonPath("$.composers[?(@.name == 'Edvard Grieeeeeeg')]").doesNotExist())
			.andExpect(jsonPath("$.composers[?(@.name == 'Robert Schuuuuuuman')]").doesNotExist())
			.andExpect(jsonPath("$.composers[-1]").doesNotExist())
			.andExpect(jsonPath("$.composers[4]").doesNotExist())
			.andRespond(withSuccess());

		this.restTemplate.put(new URI("/composers"), this.people);
		this.mockServer.verify();
	}

	@Test1
	public void testEqualTo() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json;charset=UTF-8"))
			.andExpect(jsonPath("$.composers[0].name").value("Johann Sebastian Bach"))
			.andExpect(jsonPath("$.performers[1].name").value("Yehudi Menuhin"))
			.andExpect(jsonPath("$.composers[0].name").value(equalTo("Johann Sebastian Bach"))) // Hamcrest
			.andExpect(jsonPath("$.performers[1].name").value(equalTo("Yehudi Menuhin"))) // Hamcrest
			.andRespond(withSuccess());

		this.restTemplate.put(new URI("/composers"), this.people);
		this.mockServer.verify();
	}

	@Test1
	public void testHamcrestMatcher() throws Exception {
		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json;charset=UTF-8"))
			.andExpect(jsonPath("$.composers[0].name", startsWith("Johann")))
			.andExpect(jsonPath("$.performers[0].name", endsWith("Ashkenazy")))
			.andExpect(jsonPath("$.performers[1].name", containsString("di Me")))
			.andExpect(jsonPath("$.composers[1].name", isIn(Arrays.asList("Johann Sebastian Bach", "Johannes Brahms"))))
			.andExpect(jsonPath("$.composers[:3].name", hasItem("Johannes Brahms")))
			.andRespond(withSuccess());

		this.restTemplate.put(new URI("/composers"), this.people);
		this.mockServer.verify();
	}

	@Test1
	public void testHamcrestMatcherWithParameterizedJsonPath() throws Exception {
		String composerName = "$.composers[%s].name";
		String performerName = "$.performers[%s].name";

		this.mockServer.expect(requestTo("/composers"))
			.andExpect(content().contentType("application/json;charset=UTF-8"))
			.andExpect(jsonPath(composerName, 0).value(startsWith("Johann")))
			.andExpect(jsonPath(performerName, 0).value(endsWith("Ashkenazy")))
			.andExpect(jsonPath(performerName, 1).value(containsString("di Me")))
			.andExpect(jsonPath(composerName, 1).value(isIn(Arrays.asList("Johann Sebastian Bach", "Johannes Brahms"))))
			.andRespond(withSuccess());

		this.restTemplate.put(new URI("/composers"), this.people);
		this.mockServer.verify();
	}

}
