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

package org.springframework.http.converter.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import org.junit.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.junit.Assert.*;

/**
 * Gson 2.x converter tests.
 *
 * @author Roy Clarkson
 */
public class GsonHttpMessageConverterTests {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private GsonHttpMessageConverter converter = new GsonHttpMessageConverter();


	@Test1
	public void canRead() {
		assertTrue(this.converter.canRead(MyBean.class, new MediaType("application", "json")));
		assertTrue(this.converter.canRead(Map.class, new MediaType("application", "json")));
	}

	@Test1
	public void canWrite() {
		assertTrue(this.converter.canWrite(MyBean.class, new MediaType("application", "json")));
		assertTrue(this.converter.canWrite(Map.class, new MediaType("application", "json")));
	}

	@Test1
	public void canReadAndWriteMicroformats() {
		assertTrue(this.converter.canRead(MyBean.class, new MediaType("application", "vnd.test-micro-type+json")));
		assertTrue(this.converter.canWrite(MyBean.class, new MediaType("application", "vnd.test-micro-type+json")));
	}

	@Test1
	public void readTyped() throws IOException {
		String body = "{\"bytes\":[1,2],\"array\":[\"Foo\",\"Bar\"]," +
				"\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
		MyBean result = (MyBean) this.converter.read(MyBean.class, inputMessage);

		assertEquals("Foo", result.getString());
		assertEquals(42, result.getNumber());
		assertEquals(42F, result.getFraction(), 0F);
		assertArrayEquals(new String[]{"Foo", "Bar"}, result.getArray());
		assertTrue(result.isBool());
		assertArrayEquals(new byte[]{0x1, 0x2}, result.getBytes());
	}

	@Test1
	@SuppressWarnings("unchecked")
	public void readUntyped() throws IOException {
		String body = "{\"bytes\":[1,2],\"array\":[\"Foo\",\"Bar\"]," +
				"\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
		HashMap<String, Object> result = (HashMap<String, Object>) this.converter.read(HashMap.class, inputMessage);
		assertEquals("Foo", result.get("string"));
		Number n = (Number) result.get("number");
		assertEquals(42, n.longValue());
		n = (Number) result.get("fraction");
		assertEquals(42D, n.doubleValue(), 0D);
		List<String> array = new ArrayList<String>();
		array.add("Foo");
		array.add("Bar");
		assertEquals(array, result.get("array"));
		assertEquals(Boolean.TRUE, result.get("bool"));
		byte[] bytes = new byte[2];
		List<Number> resultBytes = (ArrayList<Number>)result.get("bytes");
		for (int i = 0; i < 2; i++) {
			bytes[i] = resultBytes.get(i).byteValue();
		}
		assertArrayEquals(new byte[]{0x1, 0x2}, bytes);
	}

	@Test1
	public void write() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MyBean body = new MyBean();
		body.setString("Foo");
		body.setNumber(42);
		body.setFraction(42F);
		body.setArray(new String[]{"Foo", "Bar"});
		body.setBool(true);
		body.setBytes(new byte[]{0x1, 0x2});
		this.converter.write(body, null, outputMessage);
		Charset utf8 = Charset.forName("UTF-8");
		String result = outputMessage.getBodyAsString(utf8);
		assertTrue(result.contains("\"string\":\"Foo\""));
		assertTrue(result.contains("\"number\":42"));
		assertTrue(result.contains("fraction\":42.0"));
		assertTrue(result.contains("\"array\":[\"Foo\",\"Bar\"]"));
		assertTrue(result.contains("\"bool\":true"));
		assertTrue(result.contains("\"bytes\":[1,2]"));
		assertEquals("Invalid content-type", new MediaType("application", "json", utf8),
				outputMessage.getHeaders().getContentType());
	}

	@Test1
	public void writeUTF16() throws IOException {
		Charset utf16 = Charset.forName("UTF-16BE");
		MediaType contentType = new MediaType("application", "json", utf16);
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		String body = "H\u00e9llo W\u00f6rld";
		this.converter.write(body, contentType, outputMessage);
		assertEquals("Invalid result", "\"" + body + "\"", outputMessage.getBodyAsString(utf16));
		assertEquals("Invalid content-type", contentType, outputMessage.getHeaders().getContentType());
	}

	@Test1(expected = HttpMessageNotReadableException.class)
	public void readInvalidJson() throws IOException {
		String body = "FooBar";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
		this.converter.read(MyBean.class, inputMessage);
	}

	@Test1
	@SuppressWarnings("unchecked")
	public void readGenerics() throws IOException {
		GsonHttpMessageConverter converter = new GsonHttpMessageConverter() {
			@Override
			protected TypeToken<?> getTypeToken(Type type) {
				if (type instanceof Class && List.class.isAssignableFrom((Class<?>) type)) {
					return new TypeToken<ArrayList<MyBean>>() {
					};
				}
				else {
					return super.getTypeToken(type);
				}
			}
		};
		String body = "[{\"bytes\":[1,2],\"array\":[\"Foo\",\"Bar\"]," +
				"\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}]";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(
				body.getBytes(UTF8));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));

		List<MyBean> results = (List<MyBean>) converter.read(List.class, inputMessage);
		assertEquals(1, results.size());
		MyBean result = results.get(0);
		assertEquals("Foo", result.getString());
		assertEquals(42, result.getNumber());
		assertEquals(42F, result.getFraction(), 0F);
		assertArrayEquals(new String[] { "Foo", "Bar" }, result.getArray());
		assertTrue(result.isBool());
		assertArrayEquals(new byte[] { 0x1, 0x2 }, result.getBytes());
	}

	@Test1
	@SuppressWarnings("unchecked")
	public void readParameterizedType() throws IOException {
		ParameterizedTypeReference<List<MyBean>> beansList = new ParameterizedTypeReference<List<MyBean>>() {
		};

		String body = "[{\"bytes\":[1,2],\"array\":[\"Foo\",\"Bar\"]," +
				"\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}]";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(
				body.getBytes(UTF8));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));

		GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
		List<MyBean> results = (List<MyBean>) converter.read(beansList.getType(), null, inputMessage);
		assertEquals(1, results.size());
		MyBean result = results.get(0);
		assertEquals("Foo", result.getString());
		assertEquals(42, result.getNumber());
		assertEquals(42F, result.getFraction(), 0F);
		assertArrayEquals(new String[] { "Foo", "Bar" }, result.getArray());
		assertTrue(result.isBool());
		assertArrayEquals(new byte[] { 0x1, 0x2 }, result.getBytes());
	}

	@Test1
	public void prefixJson() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.setPrefixJson(true);
		this.converter.writeInternal("foo", outputMessage);
		assertEquals("{} && \"foo\"", outputMessage.getBodyAsString(UTF8));
	}

	@Test1
	public void prefixJsonCustom() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.setJsonPrefix(")]}',");
		this.converter.writeInternal("foo", outputMessage);
		assertEquals(")]}',\"foo\"", outputMessage.getBodyAsString(UTF8));
	}


	public static class MyBean {

		private String string;

		private int number;

		private float fraction;

		private String[] array;

		private boolean bool;

		private byte[] bytes;

		public byte[] getBytes() {
			return bytes;
		}

		public void setBytes(byte[] bytes) {
			this.bytes = bytes;
		}

		public boolean isBool() {
			return bool;
		}

		public void setBool(boolean bool) {
			this.bool = bool;
		}

		public String getString() {
			return string;
		}

		public void setString(String string) {
			this.string = string;
		}

		public int getNumber() {
			return number;
		}

		public void setNumber(int number) {
			this.number = number;
		}

		public float getFraction() {
			return fraction;
		}

		public void setFraction(float fraction) {
			this.fraction = fraction;
		}

		public String[] getArray() {
			return array;
		}

		public void setArray(String[] array) {
			this.array = array;
		}
	}

}
