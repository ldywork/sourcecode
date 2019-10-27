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

package org.springframework.expression.spel;

import org.junit.Test;

import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import static org.junit.Assert.*;

/**
 * Parse some expressions and check we get the AST we expect. Rather than inspecting each node in the AST, we ask it to
 * write itself to a string form and check that is as expected.
 *
 * @author Andy Clement
 */
public class ParsingTests {

	private SpelExpressionParser parser = new SpelExpressionParser();

	// literals
	@Test1
	public void testLiteralBoolean01() {
		parseCheck("false");
	}

	@Test1
	public void testLiteralLong01() {
		parseCheck("37L", "37");
	}

	@Test1
	public void testLiteralBoolean02() {
		parseCheck("true");
	}

	@Test1
	public void testLiteralBoolean03() {
		parseCheck("!true");
	}

	@Test1
	public void testLiteralInteger01() {
		parseCheck("1");
	}

	@Test1
	public void testLiteralInteger02() {
		parseCheck("1415");
	}

	@Test1
	public void testLiteralString01() {
		parseCheck("'hello'");
	}

	@Test1
	public void testLiteralString02() {
		parseCheck("'joe bloggs'");
	}

	@Test1
	public void testLiteralString03() {
		parseCheck("'Tony''s Pizza'", "'Tony's Pizza'");
	}

	@Test1
	public void testLiteralReal01() {
		parseCheck("6.0221415E+23", "6.0221415E23");
	}

	@Test1
	public void testLiteralHex01() {
		parseCheck("0x7FFFFFFF", "2147483647");
	}

	@Test1
	public void testLiteralDate01() {
		parseCheck("date('1974/08/24')");
	}

	@Test1
	public void testLiteralDate02() {
		parseCheck("date('19740824T131030','yyyyMMddTHHmmss')");
	}

	@Test1
	public void testLiteralNull01() {
		parseCheck("null");
	}

	// boolean operators
	@Test1
	public void testBooleanOperatorsOr01() {
		parseCheck("false or false", "(false or false)");
	}

	@Test1
	public void testBooleanOperatorsOr02() {
		parseCheck("false or true", "(false or true)");
	}

	@Test1
	public void testBooleanOperatorsOr03() {
		parseCheck("true or false", "(true or false)");
	}

	@Test1
	public void testBooleanOperatorsOr04() {
		parseCheck("true or false", "(true or false)");
	}

	@Test1
	public void testBooleanOperatorsMix01() {
		parseCheck("false or true and false", "(false or (true and false))");
	}

	// relational operators
	@Test1
	public void testRelOperatorsGT01() {
		parseCheck("3>6", "(3 > 6)");
	}

	@Test1
	public void testRelOperatorsLT01() {
		parseCheck("3<6", "(3 < 6)");
	}

	@Test1
	public void testRelOperatorsLE01() {
		parseCheck("3<=6", "(3 <= 6)");
	}

	@Test1
	public void testRelOperatorsGE01() {
		parseCheck("3>=6", "(3 >= 6)");
	}

	@Test1
	public void testRelOperatorsGE02() {
		parseCheck("3>=3", "(3 >= 3)");
	}

	@Test1
	public void testElvis() {
		parseCheck("3?:1", "3 ?: 1");
	}

	// public void testRelOperatorsIn01() {
	// parseCheck("3 in {1,2,3,4,5}", "(3 in {1,2,3,4,5})");
	// }
	//
	// public void testRelOperatorsBetween01() {
	// parseCheck("1 between {1, 5}", "(1 between {1,5})");
	// }

	// public void testRelOperatorsBetween02() {
	// parseCheck("'efg' between {'abc', 'xyz'}", "('efg' between {'abc','xyz'})");
	// }// true

	@Test1
	public void testRelOperatorsIs01() {
		parseCheck("'xyz' instanceof int", "('xyz' instanceof int)");
	}// false

	// public void testRelOperatorsIs02() {
	// parseCheck("{1, 2, 3, 4, 5} instanceof List", "({1,2,3,4,5} instanceof List)");
	// }// true

	@Test1
	public void testRelOperatorsMatches01() {
		parseCheck("'5.0067' matches '^-?\\d+(\\.\\d{2})?$'", "('5.0067' matches '^-?\\d+(\\.\\d{2})?$')");
	}// false

	@Test1
	public void testRelOperatorsMatches02() {
		parseCheck("'5.00' matches '^-?\\d+(\\.\\d{2})?$'", "('5.00' matches '^-?\\d+(\\.\\d{2})?$')");
	}// true

	// mathematical operators
	@Test1
	public void testMathOperatorsAdd01() {
		parseCheck("2+4", "(2 + 4)");
	}

	@Test1
	public void testMathOperatorsAdd02() {
		parseCheck("'a'+'b'", "('a' + 'b')");
	}

	@Test1
	public void testMathOperatorsAdd03() {
		parseCheck("'hello'+' '+'world'", "(('hello' + ' ') + 'world')");
	}

	@Test1
	public void testMathOperatorsSubtract01() {
		parseCheck("5-4", "(5 - 4)");
	}

	@Test1
	public void testMathOperatorsMultiply01() {
		parseCheck("7*4", "(7 * 4)");
	}

	@Test1
	public void testMathOperatorsDivide01() {
		parseCheck("8/4", "(8 / 4)");
	}

	@Test1
	public void testMathOperatorModulus01() {
		parseCheck("7 % 4", "(7 % 4)");
	}

	// mixed operators
	@Test1
	public void testMixedOperators01() {
		parseCheck("true and 5>3", "(true and (5 > 3))");
	}

	// collection processors
	// public void testCollectionProcessorsCount01() {
	// parseCheck("new String[] {'abc','def','xyz'}.count()");
	// }

	// public void testCollectionProcessorsCount02() {
	// parseCheck("new int[] {1,2,3}.count()");
	// }
	//
	// public void testCollectionProcessorsMax01() {
	// parseCheck("new int[] {1,2,3}.max()");
	// }
	//
	// public void testCollectionProcessorsMin01() {
	// parseCheck("new int[] {1,2,3}.min()");
	// }
	//
	// public void testCollectionProcessorsAverage01() {
	// parseCheck("new int[] {1,2,3}.average()");
	// }
	//
	// public void testCollectionProcessorsSort01() {
	// parseCheck("new int[] {3,2,1}.sort()");
	// }
	//
	// public void testCollectionProcessorsNonNull01() {
	// parseCheck("{'a','b',null,'d',null}.nonNull()");
	// }
	//
	// public void testCollectionProcessorsDistinct01() {
	// parseCheck("{'a','b','a','d','e'}.distinct()");
	// }

	// references
	@Test1
	public void testReferences01() {
		parseCheck("@foo");
		parseCheck("@'foo.bar'");
		parseCheck("@\"foo.bar.goo\"","@'foo.bar.goo'");
	}

	@Test1
	public void testReferences03() {
		parseCheck("@$$foo");
	}

	// properties
	@Test1
	public void testProperties01() {
		parseCheck("name");
	}

	@Test1
	public void testProperties02() {
		parseCheck("placeofbirth.CitY");
	}

	@Test1
	public void testProperties03() {
		parseCheck("a.b.c.d.e");
	}

	// inline list creation
	@Test1
	public void testInlineListCreation01() {
		parseCheck("{1, 2, 3, 4, 5}", "{1,2,3,4,5}");
	}

	@Test1
	public void testInlineListCreation02() {
		parseCheck("{'abc','xyz'}", "{'abc','xyz'}");
	}

	// inline map creation
	@Test1
	public void testInlineMapCreation01() {
		parseCheck("{'key1':'Value 1','today':DateTime.Today}");
	}

	@Test1
	public void testInlineMapCreation02() {
		parseCheck("{1:'January',2:'February',3:'March'}");
	}

	// methods
	@Test1
	public void testMethods01() {
		parseCheck("echo(12)");
	}

	@Test1
	public void testMethods02() {
		parseCheck("echo(name)");
	}

	@Test1
	public void testMethods03() {
		parseCheck("age.doubleItAndAdd(12)");
	}

	// constructors
	@Test1
	public void testConstructors01() {
		parseCheck("new String('hello')");
	}

	// public void testConstructors02() {
	// parseCheck("new String[3]");
	// }

	// array construction
	// public void testArrayConstruction01() {
	// parseCheck("new int[] {1, 2, 3, 4, 5}", "new int[] {1,2,3,4,5}");
	// }
	//
	// public void testArrayConstruction02() {
	// parseCheck("new String[] {'abc','xyz'}", "new String[] {'abc','xyz'}");
	// }

	// variables and functions
	@Test1
	public void testVariables01() {
		parseCheck("#foo");
	}

	@Test1
	public void testFunctions01() {
		parseCheck("#fn(1,2,3)");
	}

	@Test1
	public void testFunctions02() {
		parseCheck("#fn('hello')");
	}

	// projections and selections
	// public void testProjections01() {
	// parseCheck("{1,2,3,4,5,6,7,8,9,10}.!{#isEven()}");
	// }

	// public void testSelections01() {
	// parseCheck("{1,2,3,4,5,6,7,8,9,10}.?{#isEven(#this) == 'y'}",
	// "{1,2,3,4,5,6,7,8,9,10}.?{(#isEven(#this) == 'y')}");
	// }

	// public void testSelectionsFirst01() {
	// parseCheck("{1,2,3,4,5,6,7,8,9,10}.^{#isEven(#this) == 'y'}",
	// "{1,2,3,4,5,6,7,8,9,10}.^{(#isEven(#this) == 'y')}");
	// }

	// public void testSelectionsLast01() {
	// parseCheck("{1,2,3,4,5,6,7,8,9,10}.${#isEven(#this) == 'y'}",
	// "{1,2,3,4,5,6,7,8,9,10}.${(#isEven(#this) == 'y')}");
	// }

	// assignment
	@Test1
	public void testAssignmentToVariables01() {
		parseCheck("#var1='value1'");
	}


	// ternary operator

	@Test1
	public void testTernaryOperator01() {
		parseCheck("1>2?3:4","(1 > 2) ? 3 : 4");
	}

	// public void testTernaryOperator01() {
	// parseCheck("{1}.#isEven(#this) == 'y'?'it is even':'it is odd'",
	// "({1}.#isEven(#this) == 'y') ? 'it is even' : 'it is odd'");
	// }

	//
	// public void testLambdaMax() {
	// parseCheck("(#max = {|x,y| $x > $y ? $x : $y }; #max(5,25))", "(#max={|x,y| ($x > $y) ? $x : $y };#max(5,25))");
	// }
	//
	// public void testLambdaFactorial() {
	// parseCheck("(#fact = {|n| $n <= 1 ? 1 : $n * #fact($n-1) }; #fact(5))",
	// "(#fact={|n| ($n <= 1) ? 1 : ($n * #fact(($n - 1))) };#fact(5))");
	// } // 120

	// Type references
	@Test1
	public void testTypeReferences01() {
		parseCheck("T(java.lang.String)");
	}

	@Test1
	public void testTypeReferences02() {
		parseCheck("T(String)");
	}

	@Test1
	public void testInlineList1() {
		parseCheck("{1,2,3,4}");
	}

	/**
	 * Parse the supplied expression and then create a string representation of the resultant AST, it should be the same
	 * as the original expression.
	 *
	 * @param expression the expression to parse *and* the expected value of the string form of the resultant AST
	 */
	public void parseCheck(String expression) {
		parseCheck(expression, expression);
	}

	/**
	 * Parse the supplied expression and then create a string representation of the resultant AST, it should be the
	 * expected value.
	 *
	 * @param expression the expression to parse
	 * @param expectedStringFormOfAST the expected string form of the AST
	 */
	public void parseCheck(String expression, String expectedStringFormOfAST) {
		try {
			SpelExpression e = parser.parseRaw(expression);
			if (e != null && !e.toStringAST().equals(expectedStringFormOfAST)) {
				SpelUtilities.printAbstractSyntaxTree(System.err, e);
			}
			if (e == null) {
				fail("Parsed exception was null");
			}
			assertEquals("String form of AST does not match expected output", expectedStringFormOfAST, e.toStringAST());
		} catch (ParseException ee) {
			ee.printStackTrace();
			fail("Unexpected Exception: " + ee.getMessage());
		}
	}

}
