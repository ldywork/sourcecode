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

package org.springframework.test.web.servlet.samples.standalone.resultmatchers;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * Examples of expectations on the status and the status reason found in the response.
 *
 * @author Rossen Stoyanchev
 */
public class StatusAssertionTests {

	private MockMvc mockMvc;

	@Before
	public void setup() {
		this.mockMvc = standaloneSetup(new StatusController()).build();
	}

	@Test1
	public void testStatusInt() throws Exception {
		this.mockMvc.perform(get("/created")).andExpect(status().is(201));
		this.mockMvc.perform(get("/badRequest")).andExpect(status().is(400));
	}

	@Test1
	public void testHttpStatus() throws Exception {
		this.mockMvc.perform(get("/created")).andExpect(status().isCreated());
		this.mockMvc.perform(get("/badRequest")).andExpect(status().isBadRequest());
	}

	@Test1
	public void testMatcher() throws Exception {
		this.mockMvc.perform(get("/badRequest")).andExpect(status().is(equalTo(400)));
	}

	@Test1
	public void testReasonEqualTo() throws Exception {
		this.mockMvc.perform(get("/badRequest")).andExpect(status().reason("Expired token"));

		// Hamcrest matchers...
		this.mockMvc.perform(get("/badRequest")).andExpect(status().reason(equalTo("Expired token")));
	}

	@Test1
	public void testReasonMatcher() throws Exception {
		this.mockMvc.perform(get("/badRequest"))
			.andExpect(status().reason(endsWith("token")));
	}


	@Controller
	private static class StatusController {

		@RequestMapping("/created")
		@ResponseStatus(HttpStatus.CREATED)
		public @ResponseBody void created(){
		}

		@RequestMapping("/badRequest")
		@ResponseStatus(value=HttpStatus.BAD_REQUEST, reason="Expired token")
		public @ResponseBody void badRequest(){
		}

		@RequestMapping("/notImplemented")
		@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
		public @ResponseBody void notImplemented(){
		}
	}
}
