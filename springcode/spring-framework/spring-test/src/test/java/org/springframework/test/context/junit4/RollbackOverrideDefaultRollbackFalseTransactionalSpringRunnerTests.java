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

package org.springframework.test.context.junit4;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.Assert.*;
import static org.springframework.test.transaction.TransactionTestUtils.*;

/**
 * Extension of {@link DefaultRollbackFalseTransactionalSpringRunnerTests} which
 * tests method-level <em>rollback override</em> behavior via the
 * {@link Rollback @Rollback} annotation.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see Rollback
 */
@ContextConfiguration
public class RollbackOverrideDefaultRollbackFalseTransactionalSpringRunnerTests extends
		DefaultRollbackFalseTransactionalSpringRunnerTests {

	protected static int originalNumRows;

	protected static JdbcTemplate jdbcTemplate;


	@AfterClass
	public static void verifyFinalTestData() {
		assertEquals("Verifying the final number of rows in the person table after all tests.", originalNumRows,
			countRowsInPersonTable(jdbcTemplate));
	}

	@Before
	@Override
	public void verifyInitialTestData() {
		originalNumRows = clearPersonTable(jdbcTemplate);
		assertEquals("Adding bob", 1, addPerson(jdbcTemplate, BOB));
		assertEquals("Verifying the initial number of rows in the person table.", 1,
			countRowsInPersonTable(jdbcTemplate));
	}

	@Test1
	@Rollback(true)
	@Override
	public void modifyTestDataWithinTransaction() {
		assertInTransaction(true);
		assertEquals("Deleting bob", 1, deletePerson(jdbcTemplate, BOB));
		assertEquals("Adding jane", 1, addPerson(jdbcTemplate, JANE));
		assertEquals("Adding sue", 1, addPerson(jdbcTemplate, SUE));
		assertEquals("Verifying the number of rows in the person table within a transaction.", 2,
			countRowsInPersonTable(jdbcTemplate));
	}


	public static class DatabaseSetup {

		@Resource
		public void setDataSource(DataSource dataSource) {
			jdbcTemplate = new JdbcTemplate(dataSource);
			createPersonTable(jdbcTemplate);
		}
	}

}
