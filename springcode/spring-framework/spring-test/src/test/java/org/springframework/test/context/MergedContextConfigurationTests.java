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

package org.springframework.test.context;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.support.GenericXmlContextLoader;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link MergedContextConfiguration}.
 *
 * <p>These tests primarily exist to ensure that {@code MergedContextConfiguration}
 * can safely be used as the cache key for {@link ContextCache}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
public class MergedContextConfigurationTests {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];


	private final GenericXmlContextLoader loader = new GenericXmlContextLoader();


	@Test1
	public void hashCodeWithNulls() {
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(null, null, null, null, null);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(null, null, null, null, null);
		assertEquals(mergedConfig1.hashCode(), mergedConfig2.hashCode());
	}

	@Test1
	public void hashCodeWithNullArrays() {
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(), null, null, null, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), null, null, null, loader);
		assertEquals(mergedConfig1.hashCode(), mergedConfig2.hashCode());
	}

	@Test1
	public void hashCodeWithEmptyArrays() {
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		assertEquals(mergedConfig1.hashCode(), mergedConfig2.hashCode());
	}

	@Test1
	public void hashCodeWithEmptyArraysAndDifferentLoaders() {
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, new AnnotationConfigContextLoader());
		assertNotEquals(mergedConfig1.hashCode(), mergedConfig2.hashCode());
	}

	@Test1
	public void hashCodeWithSameLocations() {
		String[] locations = new String[] { "foo", "bar}" };
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(), locations,
				EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), locations,
				EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		assertEquals(mergedConfig1.hashCode(), mergedConfig2.hashCode());
	}

	@Test1
	public void hashCodeWithDifferentLocations() {
		String[] locations1 = new String[] { "foo", "bar}" };
		String[] locations2 = new String[] { "baz", "quux}" };
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(), locations1,
				EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), locations2,
				EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		assertNotEquals(mergedConfig1.hashCode(), mergedConfig2.hashCode());
	}

	@Test1
	public void hashCodeWithSameConfigClasses() {
		Class<?>[] classes = new Class<?>[] { String.class, Integer.class };
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, classes, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, classes, EMPTY_STRING_ARRAY, loader);
		assertEquals(mergedConfig1.hashCode(), mergedConfig2.hashCode());
	}

	@Test1
	public void hashCodeWithDifferentConfigClasses() {
		Class<?>[] classes1 = new Class<?>[] { String.class, Integer.class };
		Class<?>[] classes2 = new Class<?>[] { Boolean.class, Number.class };
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, classes1, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, classes2, EMPTY_STRING_ARRAY, loader);
		assertNotEquals(mergedConfig1.hashCode(), mergedConfig2.hashCode());
	}

	@Test1
	public void hashCodeWithSameProfiles() {
		String[] activeProfiles = new String[] { "catbert", "dogbert" };
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles, loader);
		assertEquals(mergedConfig1.hashCode(), mergedConfig2.hashCode());
	}

	@Test1
	public void hashCodeWithSameProfilesReversed() {
		String[] activeProfiles1 = new String[] { "catbert", "dogbert" };
		String[] activeProfiles2 = new String[] { "dogbert", "catbert" };
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles1, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles2, loader);
		assertEquals(mergedConfig1.hashCode(), mergedConfig2.hashCode());
	}

	@Test1
	public void hashCodeWithSameDuplicateProfiles() {
		String[] activeProfiles1 = new String[] { "catbert", "dogbert" };
		String[] activeProfiles2 = new String[] { "catbert", "dogbert", "catbert", "dogbert", "catbert" };
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles1, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles2, loader);
		assertEquals(mergedConfig1.hashCode(), mergedConfig2.hashCode());
	}

	@Test1
	public void hashCodeWithDifferentProfiles() {
		String[] activeProfiles1 = new String[] { "catbert", "dogbert" };
		String[] activeProfiles2 = new String[] { "X", "Y" };
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles1, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles2, loader);
		assertNotEquals(mergedConfig1.hashCode(), mergedConfig2.hashCode());
	}

	@Test1
	public void hashCodeWithSameInitializers() {
		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses1 =
				new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();
		initializerClasses1.add(FooInitializer.class);
		initializerClasses1.add(BarInitializer.class);

		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses2 =
				new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();
		initializerClasses2.add(BarInitializer.class);
		initializerClasses2.add(FooInitializer.class);

		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, initializerClasses1, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, initializerClasses2, EMPTY_STRING_ARRAY, loader);
		assertEquals(mergedConfig1.hashCode(), mergedConfig2.hashCode());
	}

	@Test1
	public void hashCodeWithDifferentInitializers() {
		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses1 =
				new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();
		initializerClasses1.add(FooInitializer.class);

		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses2 =
				new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();
		initializerClasses2.add(BarInitializer.class);

		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, initializerClasses1, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, initializerClasses2, EMPTY_STRING_ARRAY, loader);
		assertNotEquals(mergedConfig1.hashCode(), mergedConfig2.hashCode());
	}

	/**
	 * @since 3.2.2
	 */
	@Test1
	public void hashCodeWithSameParent() {
		MergedContextConfiguration parent = new MergedContextConfiguration(getClass(), new String[] { "foo", "bar}" },
				EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);

		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, loader, null, parent);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
				EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, loader, null, parent);
		assertEquals(mergedConfig1.hashCode(), mergedConfig2.hashCode());
	}

	/**
	 * @since 3.2.2
	 */
	@Test1
	public void hashCodeWithDifferentParents() {
		MergedContextConfiguration parent1 = new MergedContextConfiguration(getClass(), new String[] { "foo", "bar}" },
				EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration parent2 = new MergedContextConfiguration(getClass(), new String[] { "baz", "quux" },
				EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);

		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
				EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, loader, null, parent1);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
				EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, loader, null, parent2);
		assertNotEquals(mergedConfig1.hashCode(), mergedConfig2.hashCode());
	}

	@Test1
	public void equalsBasics() {
		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(null, null, null, null, null);
		assertEquals(mergedConfig, mergedConfig);
		assertNotEquals(mergedConfig, null);
		assertNotEquals(mergedConfig, 1);
	}

	@Test1
	public void equalsWithNulls() {
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(null, null, null, null, null);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(null, null, null, null, null);
		assertEquals(mergedConfig1, mergedConfig2);
	}

	@Test1
	public void equalsWithNullArrays() {
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(), null, null, null, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), null, null, null, loader);
		assertEquals(mergedConfig1, mergedConfig2);
	}

	@Test1
	public void equalsWithEmptyArrays() {
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		assertEquals(mergedConfig1, mergedConfig2);
	}

	@Test1
	public void equalsWithEmptyArraysAndDifferentLoaders() {
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, new AnnotationConfigContextLoader());
		assertNotEquals(mergedConfig1, mergedConfig2);
		assertNotEquals(mergedConfig2, mergedConfig1);
	}

	@Test1
	public void equalsWithSameLocations() {
		String[] locations = new String[] { "foo", "bar}" };
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				locations, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				locations, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		assertEquals(mergedConfig1, mergedConfig2);
	}

	@Test1
	public void equalsWithDifferentLocations() {
		String[] locations1 = new String[] { "foo", "bar}" };
		String[] locations2 = new String[] { "baz", "quux}" };
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				locations1, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				locations2, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		assertNotEquals(mergedConfig1, mergedConfig2);
		assertNotEquals(mergedConfig2, mergedConfig1);
	}

	@Test1
	public void equalsWithSameConfigClasses() {
		Class<?>[] classes = new Class<?>[] { String.class, Integer.class };
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, classes, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, classes, EMPTY_STRING_ARRAY, loader);
		assertEquals(mergedConfig1, mergedConfig2);
	}

	@Test1
	public void equalsWithDifferentConfigClasses() {
		Class<?>[] classes1 = new Class<?>[] { String.class, Integer.class };
		Class<?>[] classes2 = new Class<?>[] { Boolean.class, Number.class };
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, classes1, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, classes2, EMPTY_STRING_ARRAY, loader);
		assertNotEquals(mergedConfig1, mergedConfig2);
		assertNotEquals(mergedConfig2, mergedConfig1);
	}

	@Test1
	public void equalsWithSameProfiles() {
		String[] activeProfiles = new String[] { "catbert", "dogbert" };
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles, loader);
		assertEquals(mergedConfig1, mergedConfig2);
	}

	@Test1
	public void equalsWithSameProfilesReversed() {
		String[] activeProfiles1 = new String[] { "catbert", "dogbert" };
		String[] activeProfiles2 = new String[] { "dogbert", "catbert" };
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles1, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles2, loader);
		assertEquals(mergedConfig1, mergedConfig2);
	}

	@Test1
	public void equalsWithSameDuplicateProfiles() {
		String[] activeProfiles1 = new String[] { "catbert", "dogbert" };
		String[] activeProfiles2 = new String[] { "catbert", "dogbert", "catbert", "dogbert", "catbert" };
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles1, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles2, loader);
		assertEquals(mergedConfig1, mergedConfig2);
	}

	@Test1
	public void equalsWithDifferentProfiles() {
		String[] activeProfiles1 = new String[] { "catbert", "dogbert" };
		String[] activeProfiles2 = new String[] { "X", "Y" };
		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles1, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, activeProfiles2, loader);
		assertNotEquals(mergedConfig1, mergedConfig2);
		assertNotEquals(mergedConfig2, mergedConfig1);
	}

	@Test1
	public void equalsWithSameInitializers() {
		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses1 =
				new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();
		initializerClasses1.add(FooInitializer.class);
		initializerClasses1.add(BarInitializer.class);

		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses2 =
				new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();
		initializerClasses2.add(BarInitializer.class);
		initializerClasses2.add(FooInitializer.class);

		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, initializerClasses1, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, initializerClasses2, EMPTY_STRING_ARRAY, loader);
		assertEquals(mergedConfig1, mergedConfig2);
	}

	@Test1
	public void equalsWithDifferentInitializers() {
		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses1 =
				new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();
		initializerClasses1.add(FooInitializer.class);

		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses2 =
				new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();
		initializerClasses2.add(BarInitializer.class);

		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, initializerClasses1, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(),
				EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, initializerClasses2, EMPTY_STRING_ARRAY, loader);
		assertNotEquals(mergedConfig1, mergedConfig2);
		assertNotEquals(mergedConfig2, mergedConfig1);
	}

	/**
	 * @since 3.2.2
	 */
	@Test1
	public void equalsWithSameParent() {
		MergedContextConfiguration parent = new MergedContextConfiguration(getClass(), new String[] { "foo", "bar}" },
			EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);

		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
			EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, loader, null, parent);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
			EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, loader, null, parent);
		assertEquals(mergedConfig1, mergedConfig2);
		assertEquals(mergedConfig2, mergedConfig1);
	}

	/**
	 * @since 3.2.2
	 */
	@Test1
	public void equalsWithDifferentParents() {
		MergedContextConfiguration parent1 = new MergedContextConfiguration(getClass(), new String[] { "foo", "bar}" },
			EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
		MergedContextConfiguration parent2 = new MergedContextConfiguration(getClass(), new String[] { "baz", "quux" },
			EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);

		MergedContextConfiguration mergedConfig1 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
			EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, loader, null, parent1);
		MergedContextConfiguration mergedConfig2 = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
			EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, loader, null, parent2);
		assertNotEquals(mergedConfig1, mergedConfig2);
		assertNotEquals(mergedConfig2, mergedConfig1);
	}


	private static class FooInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext applicationContext) {
		}
	}


	private static class BarInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext applicationContext) {
		}
	}

}
