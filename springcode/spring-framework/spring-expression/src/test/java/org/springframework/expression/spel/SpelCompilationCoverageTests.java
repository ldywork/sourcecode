/*
 * Copyright 2002-2015 the original author or authors.
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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.junit.Test;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.OpLT;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.expression.spel.ast.Ternary;
import org.springframework.expression.spel.standard.SpelCompiler;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.testdata.PersonInOtherPackage;

import static org.junit.Assert.*;

/**
 * Checks the behaviour of the SpelCompiler. This should cover compilation all compiled node types.
 *
 * @author Andy Clement
 * @since 4.1
 */
public class SpelCompilationCoverageTests extends AbstractExpressionTests {

	private Expression expression;
	private SpelNodeImpl ast;

	/*
	 * Further TODOs for compilation:
	 *
	 * - OpMinus with a single literal operand could be treated as a negative literal. Will save a
	 *   pointless loading of 0 and then a subtract instruction in code gen.
	 * - allow other accessors/resolvers to participate in compilation and create their own code
	 * - A TypeReference followed by (what ends up as) a static method invocation can really skip
	 *   code gen for the TypeReference since once that is used to locate the method it is not
	 *   used again.
	 * - The opEq implementation is quite basic. It will compare numbers of the same type (allowing
	 *   them to be their boxed or unboxed variants) or compare object references. It does not
	 *   compile expressions where numbers are of different types or when objects implement
	 *   Comparable.
     *
	 * Compiled nodes:
	 *
	 * TypeReference
	 * OperatorInstanceOf
	 * StringLiteral
	 * NullLiteral
	 * RealLiteral
	 * IntLiteral
	 * LongLiteral
	 * BooleanLiteral
	 * FloatLiteral
	 * OpOr
	 * OpAnd
	 * OperatorNot
	 * Ternary
	 * Elvis
	 * VariableReference
	 * OpLt
	 * OpLe
	 * OpGt
	 * OpGe
	 * OpEq
	 * OpNe
	 * OpPlus
	 * OpMinus
	 * OpMultiply
	 * OpDivide
	 * MethodReference
	 * PropertyOrFieldReference
	 * Indexer
	 * CompoundExpression
	 * ConstructorReference
	 * FunctionReference
	 * InlineList
	 * OpModulus
	 *
	 * Not yet compiled (some may never need to be):
	 * Assign
	 * BeanReference
	 * Identifier
	 * OpDec
	 * OpBetween
	 * OpMatches
	 * OpPower
	 * OpInc
	 * Projection
	 * QualifiedId
	 * Selection
	 */

	@Test1
	public void typeReference() throws Exception {
		expression = parse("T(String)");
		assertEquals(String.class,expression.getValue());
		assertCanCompile(expression);
		assertEquals(String.class,expression.getValue());

		expression = parse("T(java.io.IOException)");
		assertEquals(IOException.class,expression.getValue());
		assertCanCompile(expression);
		assertEquals(IOException.class,expression.getValue());

		expression = parse("T(java.io.IOException[])");
		assertEquals(IOException[].class,expression.getValue());
		assertCanCompile(expression);
		assertEquals(IOException[].class,expression.getValue());

		expression = parse("T(int[][])");
		assertEquals(int[][].class,expression.getValue());
		assertCanCompile(expression);
		assertEquals(int[][].class,expression.getValue());

		expression = parse("T(int)");
		assertEquals(Integer.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Integer.TYPE,expression.getValue());

		expression = parse("T(byte)");
		assertEquals(Byte.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Byte.TYPE,expression.getValue());

		expression = parse("T(char)");
		assertEquals(Character.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Character.TYPE,expression.getValue());

		expression = parse("T(short)");
		assertEquals(Short.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Short.TYPE,expression.getValue());

		expression = parse("T(long)");
		assertEquals(Long.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Long.TYPE,expression.getValue());

		expression = parse("T(float)");
		assertEquals(Float.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Float.TYPE,expression.getValue());

		expression = parse("T(double)");
		assertEquals(Double.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Double.TYPE,expression.getValue());

		expression = parse("T(boolean)");
		assertEquals(Boolean.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Boolean.TYPE,expression.getValue());

		expression = parse("T(Missing)");
		assertGetValueFail(expression);
		assertCantCompile(expression);
	}

	@SuppressWarnings("unchecked")
	@Test1
	public void operatorInstanceOf() throws Exception {
		expression = parse("'xyz' instanceof T(String)");
		assertEquals(true,expression.getValue());
		assertCanCompile(expression);
		assertEquals(true,expression.getValue());

		expression = parse("'xyz' instanceof T(Integer)");
		assertEquals(false,expression.getValue());
		assertCanCompile(expression);
		assertEquals(false,expression.getValue());

		List<String> list = new ArrayList<String>();
		expression = parse("#root instanceof T(java.util.List)");
		assertEquals(true,expression.getValue(list));
		assertCanCompile(expression);
		assertEquals(true,expression.getValue(list));

		List<String>[] arrayOfLists = new List[]{new ArrayList<String>()};
		expression = parse("#root instanceof T(java.util.List[])");
		assertEquals(true,expression.getValue(arrayOfLists));
		assertCanCompile(expression);
		assertEquals(true,expression.getValue(arrayOfLists));

		int[] intArray = new int[]{1,2,3};
		expression = parse("#root instanceof T(int[])");
		assertEquals(true,expression.getValue(intArray));
		assertCanCompile(expression);
		assertEquals(true,expression.getValue(intArray));

		String root = null;
		expression = parse("#root instanceof T(Integer)");
		assertEquals(false,expression.getValue(root));
		assertCanCompile(expression);
		assertEquals(false,expression.getValue(root));

		// root still null
		expression = parse("#root instanceof T(java.lang.Object)");
		assertEquals(false,expression.getValue(root));
		assertCanCompile(expression);
		assertEquals(false,expression.getValue(root));

		root = "howdy!";
		expression = parse("#root instanceof T(java.lang.Object)");
		assertEquals(true,expression.getValue(root));
		assertCanCompile(expression);
		assertEquals(true,expression.getValue(root));
	}

	@Test1
	public void stringLiteral() throws Exception {
		expression = parser.parseExpression("'abcde'");
		assertEquals("abcde",expression.getValue(new TestClass1(),String.class));
		assertCanCompile(expression);
		String resultC = expression.getValue(new TestClass1(),String.class);
		assertEquals("abcde",resultC);
		assertEquals("abcde",expression.getValue(String.class));
		assertEquals("abcde",expression.getValue());
		assertEquals("abcde",expression.getValue(new StandardEvaluationContext()));
		expression = parser.parseExpression("\"abcde\"");
		assertCanCompile(expression);
		assertEquals("abcde",expression.getValue(String.class));
	}

	@Test1
	public void nullLiteral() throws Exception {
		expression = parser.parseExpression("null");
		Object resultI = expression.getValue(new TestClass1(),Object.class);
		assertCanCompile(expression);
		Object resultC = expression.getValue(new TestClass1(),Object.class);
		assertEquals(null,resultI);
		assertEquals(null,resultC);
		assertEquals(null,resultC);
	}

	@Test1
	public void realLiteral() throws Exception {
		expression = parser.parseExpression("3.4d");
		double resultI = expression.getValue(new TestClass1(),Double.TYPE);
		assertCanCompile(expression);
		double resultC = expression.getValue(new TestClass1(),Double.TYPE);
		assertEquals(3.4d,resultI,0.1d);
		assertEquals(3.4d,resultC,0.1d);

		assertEquals(3.4d,expression.getValue());
	}

	@SuppressWarnings("rawtypes")
	@Test1
	public void inlineList() throws Exception {
		expression = parser.parseExpression("'abcde'.substring({1,3,4}[0])");
		Object o = expression.getValue();
		assertEquals("bcde",o);
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals("bcde", o);

		expression = parser.parseExpression("{'abc','def'}");
		List<?> l = (List) expression.getValue();
		assertEquals("[abc, def]", l.toString());
		assertCanCompile(expression);
		l = (List) expression.getValue();
		assertEquals("[abc, def]", l.toString());

		expression = parser.parseExpression("{'abc','def'}[0]");
		o = expression.getValue();
		assertEquals("abc",o);
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals("abc", o);

		expression = parser.parseExpression("{'abcde','ijklm'}[0].substring({1,3,4}[0])");
		o = expression.getValue();
		assertEquals("bcde",o);
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals("bcde", o);

		expression = parser.parseExpression("{'abcde','ijklm'}[0].substring({1,3,4}[0],{1,3,4}[1])");
		o = expression.getValue();
		assertEquals("bc",o);
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals("bc", o);
	}

	@SuppressWarnings("rawtypes")
	@Test1
	public void nestedInlineLists() throws Exception {
		Object o = null;

		expression = parser.parseExpression("{{1,2,3},{4,5,6},{7,8,9}}");
		o = expression.getValue();
		assertEquals("[[1, 2, 3], [4, 5, 6], [7, 8, 9]]",o.toString());
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals("[[1, 2, 3], [4, 5, 6], [7, 8, 9]]",o.toString());

		expression = parser.parseExpression("{{1,2,3},{4,5,6},{7,8,9}}.toString()");
		o = expression.getValue();
		assertEquals("[[1, 2, 3], [4, 5, 6], [7, 8, 9]]",o);
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals("[[1, 2, 3], [4, 5, 6], [7, 8, 9]]",o);

		expression = parser.parseExpression("{{1,2,3},{4,5,6},{7,8,9}}[1][0]");
		o = expression.getValue();
		assertEquals(4,o);
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals(4,o);

		expression = parser.parseExpression("{{1,2,3},'abc',{7,8,9}}[1]");
		o = expression.getValue();
		assertEquals("abc",o);
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals("abc",o);

		expression = parser.parseExpression("'abcde'.substring({{1,3},1,3,4}[0][1])");
		o = expression.getValue();
		assertEquals("de",o);
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals("de", o);

		expression = parser.parseExpression("'abcde'.substring({{1,3},1,3,4}[1])");
		o = expression.getValue();
		assertEquals("bcde",o);
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals("bcde", o);

		expression = parser.parseExpression("{'abc',{'def','ghi'}}");
		List<?> l = (List) expression.getValue();
		assertEquals("[abc, [def, ghi]]", l.toString());
		assertCanCompile(expression);
		l = (List) expression.getValue();
		assertEquals("[abc, [def, ghi]]", l.toString());

		expression = parser.parseExpression("{'abcde',{'ijklm','nopqr'}}[0].substring({1,3,4}[0])");
		o = expression.getValue();
		assertEquals("bcde",o);
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals("bcde", o);

		expression = parser.parseExpression("{'abcde',{'ijklm','nopqr'}}[1][0].substring({1,3,4}[0])");
		o = expression.getValue();
		assertEquals("jklm",o);
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals("jklm", o);

		expression = parser.parseExpression("{'abcde',{'ijklm','nopqr'}}[1][1].substring({1,3,4}[0],{1,3,4}[1])");
		o = expression.getValue();
		assertEquals("op",o);
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals("op", o);
	}

	@Test1
	public void intLiteral() throws Exception {
		expression = parser.parseExpression("42");
		int resultI = expression.getValue(new TestClass1(),Integer.TYPE);
		assertCanCompile(expression);
		int resultC = expression.getValue(new TestClass1(),Integer.TYPE);
		assertEquals(42,resultI);
		assertEquals(42,resultC);

		expression = parser.parseExpression("T(Integer).valueOf(42)");
		expression.getValue(Integer.class);
		assertCanCompile(expression);
		assertEquals(new Integer(42),expression.getValue(null,Integer.class));

		// Code gen is different for -1 .. 6 because there are bytecode instructions specifically for those
		// values

		// Not an int literal but an opminus with one operand:
//		expression = parser.parseExpression("-1");
//		assertCanCompile(expression);
//		assertEquals(-1,expression.getValue());
		expression = parser.parseExpression("0");
		assertCanCompile(expression);
		assertEquals(0,expression.getValue());
		expression = parser.parseExpression("2");
		assertCanCompile(expression);
		assertEquals(2,expression.getValue());
		expression = parser.parseExpression("7");
		assertCanCompile(expression);
		assertEquals(7,expression.getValue());
	}

	@Test1
	public void longLiteral() throws Exception {
		expression = parser.parseExpression("99L");
		long resultI = expression.getValue(new TestClass1(),Long.TYPE);
		assertCanCompile(expression);
		long resultC = expression.getValue(new TestClass1(),Long.TYPE);
		assertEquals(99L,resultI);
		assertEquals(99L,resultC);
	}

	@Test1
	public void booleanLiteral() throws Exception {
		expression = parser.parseExpression("true");
		boolean resultI = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertTrue(SpelCompiler.compile(expression));
		boolean resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultC);

		expression = parser.parseExpression("false");
		resultI = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultI);
		assertTrue(SpelCompiler.compile(expression));
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultC);
	}

	@Test1
	public void floatLiteral() throws Exception {
		expression = parser.parseExpression("3.4f");
		float resultI = expression.getValue(new TestClass1(),Float.TYPE);
		assertCanCompile(expression);
		float resultC = expression.getValue(new TestClass1(),Float.TYPE);
		assertEquals(3.4f,resultI,0.1f);
		assertEquals(3.4f,resultC,0.1f);

		assertEquals(3.4f,expression.getValue());
	}

	@Test1
	public void opOr() throws Exception {
		Expression expression = parser.parseExpression("false or false");
		boolean resultI = expression.getValue(1,Boolean.TYPE);
		SpelCompiler.compile(expression);
		boolean resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultI);
		assertEquals(false,resultC);

		expression = parser.parseExpression("false or true");
		resultI = expression.getValue(1,Boolean.TYPE);
		assertCanCompile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);

		expression = parser.parseExpression("true or false");
		resultI = expression.getValue(1,Boolean.TYPE);
		assertCanCompile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);

		expression = parser.parseExpression("true or true");
		resultI = expression.getValue(1,Boolean.TYPE);
		assertCanCompile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);

		TestClass4 tc = new TestClass4();
		expression = parser.parseExpression("getfalse() or gettrue()");
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCanCompile(expression);
		resultC = expression.getValue(tc,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);

		// Can't compile this as we aren't going down the getfalse() branch in our evaluation
		expression = parser.parseExpression("gettrue() or getfalse()");
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCantCompile(expression);

		expression = parser.parseExpression("getA() or getB()");
		tc.a = true;
		tc.b = true;
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCantCompile(expression); // Haven't yet been into second branch
		tc.a = false;
		tc.b = true;
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCanCompile(expression); // Now been down both
		assertTrue(resultI);

		boolean b = false;
		expression = parse("#root or #root");
		Object resultI2 = expression.getValue(b);
		assertCanCompile(expression);
		assertFalse((Boolean)resultI2);
		assertFalse((Boolean)expression.getValue(b));
	}

	@Test1
	public void opAnd() throws Exception {
		Expression expression = parser.parseExpression("false and false");
		boolean resultI = expression.getValue(1,Boolean.TYPE);
		SpelCompiler.compile(expression);
		boolean resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultI);
		assertEquals(false,resultC);

		expression = parser.parseExpression("false and true");
		resultI = expression.getValue(1,Boolean.TYPE);
		SpelCompiler.compile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultI);
		assertEquals(false,resultC);

		expression = parser.parseExpression("true and false");
		resultI = expression.getValue(1,Boolean.TYPE);
		SpelCompiler.compile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultI);
		assertEquals(false,resultC);

		expression = parser.parseExpression("true and true");
		resultI = expression.getValue(1,Boolean.TYPE);
		SpelCompiler.compile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);

		TestClass4 tc = new TestClass4();

		// Can't compile this as we aren't going down the gettrue() branch in our evaluation
		expression = parser.parseExpression("getfalse() and gettrue()");
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCantCompile(expression);

		expression = parser.parseExpression("getA() and getB()");
		tc.a = false;
		tc.b = false;
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCantCompile(expression); // Haven't yet been into second branch
		tc.a = true;
		tc.b = false;
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCanCompile(expression); // Now been down both
		assertFalse(resultI);
		tc.a = true;
		tc.b = true;
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertTrue(resultI);

		boolean b = true;
		expression = parse("#root and #root");
		Object resultI2 = expression.getValue(b);
		assertCanCompile(expression);
		assertTrue((Boolean)resultI2);
		assertTrue((Boolean)expression.getValue(b));
	}

	@Test1
	public void operatorNot() throws Exception {
		expression = parse("!true");
		assertEquals(false,expression.getValue());
		assertCanCompile(expression);
		assertEquals(false,expression.getValue());

		expression = parse("!false");
		assertEquals(true,expression.getValue());
		assertCanCompile(expression);
		assertEquals(true,expression.getValue());

		boolean b = true;
		expression = parse("!#root");
		assertEquals(false,expression.getValue(b));
		assertCanCompile(expression);
		assertEquals(false,expression.getValue(b));

		b = false;
		expression = parse("!#root");
		assertEquals(true,expression.getValue(b));
		assertCanCompile(expression);
		assertEquals(true,expression.getValue(b));
	}

	@Test1
	public void ternary() throws Exception {
		Expression expression = parser.parseExpression("true?'a':'b'");
		String resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(String.class);
		assertEquals("a",resultI);
		assertEquals("a",resultC);

		expression = parser.parseExpression("false?'a':'b'");
		resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		resultC = expression.getValue(String.class);
		assertEquals("b",resultI);
		assertEquals("b",resultC);

		expression = parser.parseExpression("false?1:'b'");
		// All literals so we can do this straight away
		assertCanCompile(expression);
		assertEquals("b",expression.getValue());

		boolean root = true;
		expression = parser.parseExpression("(#root and true)?T(Integer).valueOf(1):T(Long).valueOf(3L)");
		assertEquals(1,expression.getValue(root));
		assertCantCompile(expression); // Have not gone down false branch
		root = false;
		assertEquals(3L,expression.getValue(root));
		assertCanCompile(expression);
		assertEquals(3L,expression.getValue(root));
		root = true;
		assertEquals(1,expression.getValue(root));
	}

	@Test1
	public void ternaryWithBooleanReturn() { // SPR-12271
		expression = parser.parseExpression("T(Boolean).TRUE?'abc':'def'");
		assertEquals("abc",expression.getValue());
		assertCanCompile(expression);
		assertEquals("abc",expression.getValue());

		expression = parser.parseExpression("T(Boolean).FALSE?'abc':'def'");
		assertEquals("def",expression.getValue());
		assertCanCompile(expression);
		assertEquals("def",expression.getValue());
	}

	@Test1
	public void elvis() throws Exception {
		Expression expression = parser.parseExpression("'a'?:'b'");
		String resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(String.class);
		assertEquals("a",resultI);
		assertEquals("a",resultC);

		expression = parser.parseExpression("null?:'a'");
		resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		resultC = expression.getValue(String.class);
		assertEquals("a",resultI);
		assertEquals("a",resultC);

		String s = "abc";
		expression = parser.parseExpression("#root?:'b'");
		assertCantCompile(expression);
		resultI = expression.getValue(s,String.class);
		assertEquals("abc",resultI);
		assertCanCompile(expression);
	}

	@Test1
	public void variableReference_root() throws Exception {
		String s = "hello";
		Expression expression = parser.parseExpression("#root");
		String resultI = expression.getValue(s,String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(s,String.class);
		assertEquals(s,resultI);
		assertEquals(s,resultC);

		expression = parser.parseExpression("#root");
		int i = (Integer)expression.getValue(42);
		assertEquals(42,i);
		assertCanCompile(expression);
		i = (Integer)expression.getValue(42);
		assertEquals(42,i);
	}

	public static String concat(String a, String b) {
		return a+b;
	}

	public static String join(String...strings) {
		StringBuilder buf = new StringBuilder();
		for (String string: strings) {
			buf.append(string);
		}
		return buf.toString();
	}

	@Test1
	public void compiledExpressionShouldWorkWhenUsingCustomFunctionWithVarargs() throws Exception {
		StandardEvaluationContext context = null;

		// Here the target method takes Object... and we are passing a string
		expression = parser.parseExpression("#doFormat('hey %s', 'there')");
		context = new StandardEvaluationContext();
		context.registerFunction("doFormat",
				DelegatingStringFormat.class.getDeclaredMethod("format", String.class,
						Object[].class));
		((SpelExpression) expression).setEvaluationContext(context);

		assertEquals("hey there", expression.getValue(String.class));
		assertTrue(((SpelNodeImpl) ((SpelExpression) expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("hey there", expression.getValue(String.class));

		expression = parser.parseExpression("#doFormat([0], 'there')");
		context = new StandardEvaluationContext(new Object[] { "hey %s" });
		context.registerFunction("doFormat",
				DelegatingStringFormat.class.getDeclaredMethod("format", String.class,
						Object[].class));
		((SpelExpression) expression).setEvaluationContext(context);

		assertEquals("hey there", expression.getValue(String.class));
		assertTrue(((SpelNodeImpl) ((SpelExpression) expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("hey there", expression.getValue(String.class));

		expression = parser.parseExpression("#doFormat([0], #arg)");
		context = new StandardEvaluationContext(new Object[] { "hey %s" });
		context.registerFunction("doFormat",
				DelegatingStringFormat.class.getDeclaredMethod("format", String.class,
						Object[].class));
		context.setVariable("arg", "there");
		((SpelExpression) expression).setEvaluationContext(context);

		assertEquals("hey there", expression.getValue(String.class));
		assertTrue(((SpelNodeImpl) ((SpelExpression) expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("hey there", expression.getValue(String.class));
	}

	@Test1
	public void functionReference() throws Exception {
		EvaluationContext ctx = new StandardEvaluationContext();
		Method m = this.getClass().getDeclaredMethod("concat",String.class,String.class);
		ctx.setVariable("concat",m);

		expression = parser.parseExpression("#concat('a','b')");
		assertEquals("ab",expression.getValue(ctx));
		assertCanCompile(expression);
		assertEquals("ab",expression.getValue(ctx));

		expression = parser.parseExpression("#concat(#concat('a','b'),'c').charAt(1)");
		assertEquals('b',expression.getValue(ctx));
		assertCanCompile(expression);
		assertEquals('b',expression.getValue(ctx));

		expression = parser.parseExpression("#concat(#a,#b)");
		ctx.setVariable("a", "foo");
		ctx.setVariable("b", "bar");
		assertEquals("foobar",expression.getValue(ctx));
		assertCanCompile(expression);
		assertEquals("foobar",expression.getValue(ctx));
		ctx.setVariable("b", "boo");
		assertEquals("fooboo",expression.getValue(ctx));

		m = Math.class.getDeclaredMethod("pow",Double.TYPE,Double.TYPE);
		ctx.setVariable("kapow",m);
		expression = parser.parseExpression("#kapow(2.0d,2.0d)");
		assertEquals("4.0",expression.getValue(ctx).toString());
		assertCanCompile(expression);
		assertEquals("4.0",expression.getValue(ctx).toString());
	}

	// Confirms visibility of what is being called.
	@Test1
	public void functionReferenceVisibility_SPR12359() throws Exception {
		StandardEvaluationContext context = new StandardEvaluationContext(new  Object[] { "1" });
		context.registerFunction("doCompare", SomeCompareMethod.class.getDeclaredMethod(
				"compare", Object.class, Object.class));
		context.setVariable("arg", "2");
		// type nor method are public
		expression = parser.parseExpression("#doCompare([0],#arg)");
		assertEquals("-1",expression.getValue(context, Integer.class).toString());
		assertCantCompile(expression);

		// type not public but method is
		context = new StandardEvaluationContext(new  Object[] { "1" });
		context.registerFunction("doCompare", SomeCompareMethod.class.getDeclaredMethod(
				"compare2", Object.class, Object.class));
		context.setVariable("arg", "2");
		expression = parser.parseExpression("#doCompare([0],#arg)");
		assertEquals("-1",expression.getValue(context, Integer.class).toString());
		assertCantCompile(expression);
	}

	@Test1
	public void functionReferenceNonCompilableArguments_SPR12359() throws Exception {
		StandardEvaluationContext context = new StandardEvaluationContext(new  Object[] { "1" });
		context.registerFunction("negate", SomeCompareMethod2.class.getDeclaredMethod(
				"negate", Integer.TYPE));
		context.setVariable("arg", "2");
		int[] ints = new int[]{1,2,3};
		context.setVariable("ints",ints);

		expression = parser.parseExpression("#negate(#ints.?[#this<2][0])");
		assertEquals("-1",expression.getValue(context, Integer.class).toString());
		// Selection isn't compilable.
		assertFalse(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
	}

	@Test1
	public void functionReferenceVarargs_SPR12359() throws Exception {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.registerFunction("append",
				SomeCompareMethod2.class.getDeclaredMethod("append", String[].class));
		context.registerFunction("append2",
				SomeCompareMethod2.class.getDeclaredMethod("append2", Object[].class));
		context.registerFunction("append3",
				SomeCompareMethod2.class.getDeclaredMethod("append3", String[].class));
		context.registerFunction("append4",
				SomeCompareMethod2.class.getDeclaredMethod("append4", String.class, String[].class));
		context.registerFunction("appendChar",
				SomeCompareMethod2.class.getDeclaredMethod("appendChar", char[].class));
		context.registerFunction("sum",
				SomeCompareMethod2.class.getDeclaredMethod("sum", int[].class));
		context.registerFunction("sumDouble",
				SomeCompareMethod2.class.getDeclaredMethod("sumDouble", double[].class));
		context.registerFunction("sumFloat",
				SomeCompareMethod2.class.getDeclaredMethod("sumFloat", float[].class));
		context.setVariable("stringArray", new String[]{"x","y","z"});
		context.setVariable("intArray", new int[]{5,6,9});
		context.setVariable("doubleArray", new double[]{5.0d,6.0d,9.0d});
		context.setVariable("floatArray", new float[]{5.0f,6.0f,9.0f});

		expression = parser.parseExpression("#append('a','b','c')");
		assertEquals("abc",expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("abc",expression.getValue(context).toString());

		expression = parser.parseExpression("#append('a')");
		assertEquals("a",expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("a",expression.getValue(context).toString());

		expression = parser.parseExpression("#append()");
		assertEquals("",expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("",expression.getValue(context).toString());

		expression = parser.parseExpression("#append(#stringArray)");
		assertEquals("xyz",expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("xyz",expression.getValue(context).toString());

		// This is a methodreference invocation, to compare with functionreference
		expression = parser.parseExpression("append(#stringArray)");
		assertEquals("xyz",expression.getValue(context,new SomeCompareMethod2()).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("xyz",expression.getValue(context,new SomeCompareMethod2()).toString());

		expression = parser.parseExpression("#append2('a','b','c')");
		assertEquals("abc",expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("abc",expression.getValue(context).toString());

		expression = parser.parseExpression("append2('a','b')");
		assertEquals("ab",expression.getValue(context, new SomeCompareMethod2()).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("ab",expression.getValue(context, new SomeCompareMethod2()).toString());

		expression = parser.parseExpression("#append2('a','b')");
		assertEquals("ab",expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("ab",expression.getValue(context).toString());

		expression = parser.parseExpression("#append2()");
		assertEquals("",expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("",expression.getValue(context).toString());

		expression = parser.parseExpression("#append3(#stringArray)");
		assertEquals("xyz",expression.getValue(context, new SomeCompareMethod2()).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("xyz",expression.getValue(context, new SomeCompareMethod2()).toString());

		// TODO fails due to conversionservice handling of String[] to Object...
//		expression = parser.parseExpression("#append2(#stringArray)");
//		assertEquals("xyz",expression.getValue(context).toString());
//		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
//		assertCanCompile(expression);
//		assertEquals("xyz",expression.getValue(context).toString());

		expression = parser.parseExpression("#sum(1,2,3)");
		assertEquals(6,expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals(6,expression.getValue(context));

		expression = parser.parseExpression("#sum(2)");
		assertEquals(2,expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals(2,expression.getValue(context));

		expression = parser.parseExpression("#sum()");
		assertEquals(0,expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals(0,expression.getValue(context));

		expression = parser.parseExpression("#sum(#intArray)");
		assertEquals(20,expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals(20,expression.getValue(context));

		expression = parser.parseExpression("#sumDouble(1.0d,2.0d,3.0d)");
		assertEquals(6,expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals(6,expression.getValue(context));

		expression = parser.parseExpression("#sumDouble(2.0d)");
		assertEquals(2,expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals(2,expression.getValue(context));

		expression = parser.parseExpression("#sumDouble()");
		assertEquals(0,expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals(0,expression.getValue(context));

		expression = parser.parseExpression("#sumDouble(#doubleArray)");
		assertEquals(20,expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals(20,expression.getValue(context));

		expression = parser.parseExpression("#sumFloat(1.0f,2.0f,3.0f)");
		assertEquals(6,expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals(6,expression.getValue(context));

		expression = parser.parseExpression("#sumFloat(2.0f)");
		assertEquals(2,expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals(2,expression.getValue(context));

		expression = parser.parseExpression("#sumFloat()");
		assertEquals(0,expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals(0,expression.getValue(context));

		expression = parser.parseExpression("#sumFloat(#floatArray)");
		assertEquals(20,expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals(20,expression.getValue(context));


		expression = parser.parseExpression("#appendChar('abc'.charAt(0),'abc'.charAt(1))");
		assertEquals("ab",expression.getValue(context));
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("ab",expression.getValue(context));


		expression = parser.parseExpression("#append4('a','b','c')");
		assertEquals("a::bc",expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("a::bc",expression.getValue(context).toString());

		expression = parser.parseExpression("#append4('a','b')");
		assertEquals("a::b",expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("a::b",expression.getValue(context).toString());

		expression = parser.parseExpression("#append4('a')");
		assertEquals("a::",expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("a::",expression.getValue(context).toString());

		expression = parser.parseExpression("#append4('a',#stringArray)");
		assertEquals("a::xyz",expression.getValue(context).toString());
		assertTrue(((SpelNodeImpl)((SpelExpression)expression).getAST()).isCompilable());
		assertCanCompile(expression);
		assertEquals("a::xyz",expression.getValue(context).toString());
	}

	@Test1
	public void functionReferenceVarargs() throws Exception {
		EvaluationContext ctx = new StandardEvaluationContext();
		Method m = this.getClass().getDeclaredMethod("join", String[].class);
		ctx.setVariable("join", m);
		expression = parser.parseExpression("#join('a','b','c')");
		assertEquals("abc",expression.getValue(ctx));
		assertCanCompile(expression);
		assertEquals("abc",expression.getValue(ctx));
	}

	@Test1
	public void variableReference_userDefined() throws Exception {
		EvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariable("target", "abc");
		expression = parser.parseExpression("#target");
		assertEquals("abc",expression.getValue(ctx));
		assertCanCompile(expression);
		assertEquals("abc",expression.getValue(ctx));
		ctx.setVariable("target", "123");
		assertEquals("123",expression.getValue(ctx));
		ctx.setVariable("target", 42);
		try {
			assertEquals(42,expression.getValue(ctx));
			fail();
		}
		catch (SpelEvaluationException see) {
			assertTrue(see.getCause() instanceof ClassCastException);
		}

		ctx.setVariable("target", "abc");
		expression = parser.parseExpression("#target.charAt(0)");
		assertEquals('a',expression.getValue(ctx));
		assertCanCompile(expression);
		assertEquals('a',expression.getValue(ctx));
		ctx.setVariable("target", "1");
		assertEquals('1',expression.getValue(ctx));
		ctx.setVariable("target", 42);
		try {
			assertEquals('4',expression.getValue(ctx));
			fail();
		}
		catch (SpelEvaluationException see) {
			assertTrue(see.getCause() instanceof ClassCastException);
		}
	}

	@Test1
	public void opLt() throws Exception {
		expression = parse("3.0d < 4.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("3446.0d < 1123.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("3 < 1");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("2 < 4");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("3.0f < 1.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("1.0f < 5.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("30L < 30L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("15L < 20L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		// Differing types of number, not yet supported
		expression = parse("1 < 3.0d");
		assertCantCompile(expression);

		expression = parse("T(Integer).valueOf(3) < 4");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("T(Integer).valueOf(3) < T(Integer).valueOf(3)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("5 < T(Integer).valueOf(3)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
	}

	@Test1
	public void opLe() throws Exception {
		expression = parse("3.0d <= 4.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("3446.0d <= 1123.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3446.0d <= 3446.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("3 <= 1");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("2 <= 4");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("3 <= 3");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("3.0f <= 1.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("1.0f <= 5.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("2.0f <= 2.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("30L <= 30L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("15L <= 20L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		// Differing types of number, not yet supported
		expression = parse("1 <= 3.0d");
		assertCantCompile(expression);

		expression = parse("T(Integer).valueOf(3) <= 4");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("T(Integer).valueOf(3) <= T(Integer).valueOf(3)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("5 <= T(Integer).valueOf(3)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
	}

	@Test1
	public void opGt() throws Exception {
		expression = parse("3.0d > 4.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3446.0d > 1123.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("3 > 1");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("2 > 4");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("3.0f > 1.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("1.0f > 5.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("30L > 30L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("15L > 20L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		// Differing types of number, not yet supported
		expression = parse("1 > 3.0d");
		assertCantCompile(expression);

		expression = parse("T(Integer).valueOf(3) > 4");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("T(Integer).valueOf(3) > T(Integer).valueOf(3)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("5 > T(Integer).valueOf(3)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
	}

	@Test1
	public void opGe() throws Exception {
		expression = parse("3.0d >= 4.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3446.0d >= 1123.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("3446.0d >= 3446.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("3 >= 1");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("2 >= 4");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3 >= 3");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("3.0f >= 1.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("1.0f >= 5.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3.0f >= 3.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("40L >= 30L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("15L >= 20L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("30L >= 30L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		// Differing types of number, not yet supported
		expression = parse("1 >= 3.0d");
		assertCantCompile(expression);

		expression = parse("T(Integer).valueOf(3) >= 4");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("T(Integer).valueOf(3) >= T(Integer).valueOf(3)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("5 >= T(Integer).valueOf(3)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
	}

	@Test1
	public void opEq() throws Exception {
		String tvar = "35";
		expression = parse("#root == 35");
		Boolean bb = (Boolean)expression.getValue(tvar);
		System.out.println(bb);
		assertFalse((Boolean)expression.getValue(tvar));
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue(tvar));

		expression = parse("35 == #root");
		expression.getValue(tvar);
		assertFalse((Boolean)expression.getValue(tvar));
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue(tvar));

		TestClass7 tc7 = new TestClass7();
		expression = parse("property == 'UK'");
		assertTrue((Boolean)expression.getValue(tc7));
		TestClass7.property = null;
		assertFalse((Boolean)expression.getValue(tc7));
		assertCanCompile(expression);
		TestClass7.reset();
		assertTrue((Boolean)expression.getValue(tc7));
		TestClass7.property = "UK";
		assertTrue((Boolean)expression.getValue(tc7));
		TestClass7.reset();
		TestClass7.property = null;
		assertFalse((Boolean)expression.getValue(tc7));
		expression = parse("property == null");
		assertTrue((Boolean)expression.getValue(tc7));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(tc7));

		expression = parse("3.0d == 4.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3446.0d == 3446.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("3 == 1");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3 == 3");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("3.0f == 1.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("2.0f == 2.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("30L == 30L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("15L == 20L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		// number types are not the same
		expression = parse("1 == 3.0d");
		assertCantCompile(expression);

		Double d = 3.0d;
		expression = parse("#root==3.0d");
		assertTrue((Boolean)expression.getValue(d));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(d));

		Integer i = 3;
		expression = parse("#root==3");
		assertTrue((Boolean)expression.getValue(i));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(i));

		Float f = 3.0f;
		expression = parse("#root==3.0f");
		assertTrue((Boolean)expression.getValue(f));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(f));

		long l = 300l;
		expression = parse("#root==300l");
		assertTrue((Boolean)expression.getValue(l));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(l));

		boolean b = true;
		expression = parse("#root==true");
		assertTrue((Boolean)expression.getValue(b));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(b));

		expression = parse("T(Integer).valueOf(3) == 4");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("T(Integer).valueOf(3) == T(Integer).valueOf(3)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("5 == T(Integer).valueOf(3)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("T(Float).valueOf(3.0f) == 4.0f");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("T(Float).valueOf(3.0f) == T(Float).valueOf(3.0f)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("5.0f == T(Float).valueOf(3.0f)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("T(Long).valueOf(3L) == 4L");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("T(Long).valueOf(3L) == T(Long).valueOf(3L)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("5L == T(Long).valueOf(3L)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("T(Double).valueOf(3.0d) == 4.0d");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("T(Double).valueOf(3.0d) == T(Double).valueOf(3.0d)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("5.0d == T(Double).valueOf(3.0d)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("false == true");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("T(Boolean).valueOf('true') == T(Boolean).valueOf('true')");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("T(Boolean).valueOf('true') == true");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("false == T(Boolean).valueOf('false')");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
	}

	@Test1
	public void opNe() throws Exception {
		expression = parse("3.0d != 4.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("3446.0d != 3446.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("3 != 1");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("3 != 3");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("3.0f != 1.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("2.0f != 2.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("30L != 30L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("15L != 20L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		// not compatible number types
		expression = parse("1 != 3.0d");
		assertCantCompile(expression);

		expression = parse("T(Integer).valueOf(3) != 4");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("T(Integer).valueOf(3) != T(Integer).valueOf(3)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("5 != T(Integer).valueOf(3)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("T(Float).valueOf(3.0f) != 4.0f");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("T(Float).valueOf(3.0f) != T(Float).valueOf(3.0f)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("5.0f != T(Float).valueOf(3.0f)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("T(Long).valueOf(3L) != 4L");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("T(Long).valueOf(3L) != T(Long).valueOf(3L)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("5L != T(Long).valueOf(3L)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("T(Double).valueOf(3.0d) == 4.0d");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("T(Double).valueOf(3.0d) == T(Double).valueOf(3.0d)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("5.0d == T(Double).valueOf(3.0d)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("false == true");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("T(Boolean).valueOf('true') == T(Boolean).valueOf('true')");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("T(Boolean).valueOf('true') == true");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("false == T(Boolean).valueOf('false')");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
	}

	@Test1
	public void opPlus() throws Exception {
		expression = parse("2+2");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4,expression.getValue());

		expression = parse("2L+2L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4L,expression.getValue());

		expression = parse("2.0f+2.0f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4.0f,expression.getValue());

		expression = parse("3.0d+4.0d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(7.0d,expression.getValue());

		expression = parse("+1");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1,expression.getValue());

		expression = parse("+1L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1L,expression.getValue());

		expression = parse("+1.5f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1.5f,expression.getValue());

		expression = parse("+2.5d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(2.5d,expression.getValue());

		expression = parse("+T(Double).valueOf(2.5d)");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(2.5d,expression.getValue());

		expression = parse("T(Integer).valueOf(2)+6");
		assertEquals(8,expression.getValue());
		assertCanCompile(expression);
		assertEquals(8,expression.getValue());

		expression = parse("T(Integer).valueOf(1)+T(Integer).valueOf(3)");
		assertEquals(4,expression.getValue());
		assertCanCompile(expression);
		assertEquals(4,expression.getValue());

		expression = parse("1+T(Integer).valueOf(3)");
		assertEquals(4,expression.getValue());
		assertCanCompile(expression);
		assertEquals(4,expression.getValue());

		expression = parse("T(Float).valueOf(2.0f)+6");
		assertEquals(8.0f,expression.getValue());
		assertCantCompile(expression);

		expression = parse("T(Float).valueOf(2.0f)+T(Float).valueOf(3.0f)");
		assertEquals(5.0f,expression.getValue());
		assertCanCompile(expression);
		assertEquals(5.0f,expression.getValue());

		expression = parse("3L+T(Long).valueOf(4L)");
		assertEquals(7L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(7L,expression.getValue());

		expression = parse("T(Long).valueOf(2L)+6");
		assertEquals(8L,expression.getValue());
		assertCantCompile(expression);

		expression = parse("T(Long).valueOf(2L)+T(Long).valueOf(3L)");
		assertEquals(5L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(5L,expression.getValue());

		expression = parse("1L+T(Long).valueOf(2L)");
		assertEquals(3L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(3L,expression.getValue());
	}

	@Test1
	public void opPlusString() throws Exception {
		expression = parse("'hello' + 'world'");
		assertEquals("helloworld",expression.getValue());
		assertCanCompile(expression);
		assertEquals("helloworld",expression.getValue());

		// Method with string return
		expression = parse("'hello' + getWorld()");
		assertEquals("helloworld",expression.getValue(new Greeter()));
		assertCanCompile(expression);
		assertEquals("helloworld",expression.getValue(new Greeter()));

		// Method with string return
		expression = parse("getWorld() + 'hello'");
		assertEquals("worldhello",expression.getValue(new Greeter()));
		assertCanCompile(expression);
		assertEquals("worldhello",expression.getValue(new Greeter()));

		// Three strings, optimal bytecode would only use one StringBuilder
		expression = parse("'hello' + getWorld() + ' spring'");
		assertEquals("helloworld spring",expression.getValue(new Greeter()));
		assertCanCompile(expression);
		assertEquals("helloworld spring",expression.getValue(new Greeter()));

		// Three strings, optimal bytecode would only use one StringBuilder
		expression = parse("'hello' + 3 + ' spring'");
		assertEquals("hello3 spring",expression.getValue(new Greeter()));
		assertCantCompile(expression);

		expression = parse("object + 'a'");
		assertEquals("objecta",expression.getValue(new Greeter()));
		assertCanCompile(expression);
		assertEquals("objecta",expression.getValue(new Greeter()));

		expression = parse("'a'+object");
		assertEquals("aobject",expression.getValue(new Greeter()));
		assertCanCompile(expression);
		assertEquals("aobject",expression.getValue(new Greeter()));

		expression = parse("'a'+object+'a'");
		assertEquals("aobjecta",expression.getValue(new Greeter()));
		assertCanCompile(expression);
		assertEquals("aobjecta",expression.getValue(new Greeter()));

		expression = parse("object+'a'+object");
		assertEquals("objectaobject",expression.getValue(new Greeter()));
		assertCanCompile(expression);
		assertEquals("objectaobject",expression.getValue(new Greeter()));

		expression = parse("object+object");
		assertEquals("objectobject",expression.getValue(new Greeter()));
		assertCanCompile(expression);
		assertEquals("objectobject",expression.getValue(new Greeter()));
	}

	@Test1
	public void opMinus() throws Exception {
		expression = parse("2-2");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(0,expression.getValue());

		expression = parse("4L-2L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(2L,expression.getValue());

		expression = parse("4.0f-2.0f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(2.0f,expression.getValue());

		expression = parse("3.0d-4.0d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(-1.0d,expression.getValue());

		expression = parse("-1");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(-1,expression.getValue());

		expression = parse("-1L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(-1L,expression.getValue());

		expression = parse("-1.5f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(-1.5f,expression.getValue());

		expression = parse("-2.5d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(-2.5d,expression.getValue());

		expression = parse("T(Integer).valueOf(2)-6");
		assertEquals(-4,expression.getValue());
		assertCanCompile(expression);
		assertEquals(-4,expression.getValue());

		expression = parse("T(Integer).valueOf(1)-T(Integer).valueOf(3)");
		assertEquals(-2,expression.getValue());
		assertCanCompile(expression);
		assertEquals(-2,expression.getValue());

		expression = parse("4-T(Integer).valueOf(3)");
		assertEquals(1,expression.getValue());
		assertCanCompile(expression);
		assertEquals(1,expression.getValue());

		expression = parse("T(Float).valueOf(2.0f)-6");
		assertEquals(-4.0f,expression.getValue());
		assertCantCompile(expression);

		expression = parse("T(Float).valueOf(8.0f)-T(Float).valueOf(3.0f)");
		assertEquals(5.0f,expression.getValue());
		assertCanCompile(expression);
		assertEquals(5.0f,expression.getValue());

		expression = parse("11L-T(Long).valueOf(4L)");
		assertEquals(7L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(7L,expression.getValue());

		expression = parse("T(Long).valueOf(9L)-6");
		assertEquals(3L,expression.getValue());
		assertCantCompile(expression);

		expression = parse("T(Long).valueOf(4L)-T(Long).valueOf(3L)");
		assertEquals(1L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(1L,expression.getValue());

		expression = parse("8L-T(Long).valueOf(2L)");
		assertEquals(6L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(6L,expression.getValue());
	}

	@Test1
	public void opMultiply() throws Exception {
		expression = parse("2*2");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4,expression.getValue());

		expression = parse("2L*2L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4L,expression.getValue());

		expression = parse("2.0f*2.0f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4.0f,expression.getValue());

		expression = parse("3.0d*4.0d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(12.0d,expression.getValue());

		expression = parse("T(Float).valueOf(2.0f)*6");
		assertEquals(12.0f,expression.getValue());
		assertCantCompile(expression);

		expression = parse("T(Float).valueOf(8.0f)*T(Float).valueOf(3.0f)");
		assertEquals(24.0f,expression.getValue());
		assertCanCompile(expression);
		assertEquals(24.0f,expression.getValue());

		expression = parse("11L*T(Long).valueOf(4L)");
		assertEquals(44L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(44L,expression.getValue());

		expression = parse("T(Long).valueOf(9L)*6");
		assertEquals(54L,expression.getValue());
		assertCantCompile(expression);

		expression = parse("T(Long).valueOf(4L)*T(Long).valueOf(3L)");
		assertEquals(12L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(12L,expression.getValue());

		expression = parse("8L*T(Long).valueOf(2L)");
		assertEquals(16L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(16L,expression.getValue());

		expression = parse("T(Float).valueOf(8.0f)*-T(Float).valueOf(3.0f)");
		assertEquals(-24.0f,expression.getValue());
		assertCanCompile(expression);
		assertEquals(-24.0f,expression.getValue());
	}

	@Test1
	public void opDivide() throws Exception {
		expression = parse("2/2");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1,expression.getValue());

		expression = parse("2L/2L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1L,expression.getValue());

		expression = parse("2.0f/2.0f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1.0f,expression.getValue());

		expression = parse("3.0d/4.0d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(0.75d,expression.getValue());

		expression = parse("T(Float).valueOf(6.0f)/2");
		assertEquals(3.0f,expression.getValue());
		assertCantCompile(expression);

		expression = parse("T(Float).valueOf(8.0f)/T(Float).valueOf(2.0f)");
		assertEquals(4.0f,expression.getValue());
		assertCanCompile(expression);
		assertEquals(4.0f,expression.getValue());

		expression = parse("12L/T(Long).valueOf(4L)");
		assertEquals(3L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(3L,expression.getValue());

		expression = parse("T(Long).valueOf(44L)/11");
		assertEquals(4L,expression.getValue());
		assertCantCompile(expression);

		expression = parse("T(Long).valueOf(4L)/T(Long).valueOf(2L)");
		assertEquals(2L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(2L,expression.getValue());

		expression = parse("8L/T(Long).valueOf(2L)");
		assertEquals(4L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(4L,expression.getValue());

		expression = parse("T(Float).valueOf(8.0f)/-T(Float).valueOf(4.0f)");
		assertEquals(-2.0f,expression.getValue());
		assertCanCompile(expression);
		assertEquals(-2.0f,expression.getValue());
	}

	@Test1
	public void opModulus_12041() throws Exception {
		expression = parse("2%2");
		assertEquals(0,expression.getValue());
		assertCanCompile(expression);
		assertEquals(0,expression.getValue());

		expression = parse("payload%2==0");
		assertTrue(expression.getValue(new GenericMessageTestHelper<Integer>(4),Boolean.TYPE));
		assertFalse(expression.getValue(new GenericMessageTestHelper<Integer>(5),Boolean.TYPE));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper<Integer>(4),Boolean.TYPE));
		assertFalse(expression.getValue(new GenericMessageTestHelper<Integer>(5),Boolean.TYPE));

		expression = parse("8%3");
		assertEquals(2,expression.getValue());
		assertCanCompile(expression);
		assertEquals(2,expression.getValue());

		expression = parse("17L%5L");
		assertEquals(2L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(2L,expression.getValue());

		expression = parse("3.0f%2.0f");
		assertEquals(1.0f,expression.getValue());
		assertCanCompile(expression);
		assertEquals(1.0f,expression.getValue());

		expression = parse("3.0d%4.0d");
		assertEquals(3.0d,expression.getValue());
		assertCanCompile(expression);
		assertEquals(3.0d,expression.getValue());

		expression = parse("T(Float).valueOf(6.0f)%2");
		assertEquals(0.0f,expression.getValue());
		assertCantCompile(expression);

		expression = parse("T(Float).valueOf(6.0f)%4");
		assertEquals(2.0f,expression.getValue());
		assertCantCompile(expression);

		expression = parse("T(Float).valueOf(8.0f)%T(Float).valueOf(3.0f)");
		assertEquals(2.0f,expression.getValue());
		assertCanCompile(expression);
		assertEquals(2.0f,expression.getValue());

		expression = parse("13L%T(Long).valueOf(4L)");
		assertEquals(1L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(1L,expression.getValue());

		expression = parse("T(Long).valueOf(44L)%12");
		assertEquals(8L,expression.getValue());
		assertCantCompile(expression);

		expression = parse("T(Long).valueOf(9L)%T(Long).valueOf(2L)");
		assertEquals(1L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(1L,expression.getValue());

		expression = parse("7L%T(Long).valueOf(2L)");
		assertEquals(1L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(1L,expression.getValue());

		expression = parse("T(Float).valueOf(9.0f)%-T(Float).valueOf(4.0f)");
		assertEquals(1.0f,expression.getValue());
		assertCanCompile(expression);
		assertEquals(1.0f,expression.getValue());
	}

	@Test1
	public void failsWhenSettingContextForExpression_SPR12326() {
		SpelExpressionParser parser = new SpelExpressionParser(
				new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, getClass().getClassLoader()));
		Person3 person = new Person3("foo", 1);
		SpelExpression expression = parser.parseRaw("#it?.age?.equals([0])");
		StandardEvaluationContext context = new StandardEvaluationContext(new Object[] { 1 });
		context.setVariable("it", person);
		expression.setEvaluationContext(context);
		assertTrue(expression.getValue(Boolean.class));
		assertTrue(expression.getValue(Boolean.class));
		assertCanCompile(expression);
		assertTrue(expression.getValue(Boolean.class));
	}


	/**
	 * Test variants of using T(...) and static/non-static method/property/field references.
	 */
	@Test1
	public void constructorReference_SPR13781() {
		// Static field access on a T() referenced type
		expression = parser.parseExpression("T(java.util.Locale).ENGLISH");
		assertEquals("en",expression.getValue().toString());
		assertCanCompile(expression);
		assertEquals("en",expression.getValue().toString());

		// The actual expression from the bug report. It fails if the ENGLISH reference fails
		// to pop the type reference for Locale off the stack (if it isn't popped then
		// toLowerCase() will be called with a Locale parameter). In this situation the
		// code generation for ENGLISH should notice there is something on the stack that
		// is not required and pop it off.
		expression = parser.parseExpression("#userId.toString().toLowerCase(T(java.util.Locale).ENGLISH)");
		StandardEvaluationContext context =
				new StandardEvaluationContext();
		context.setVariable("userId", "RoDnEy");
		assertEquals("rodney",expression.getValue(context));
		assertCanCompile(expression);
		assertEquals("rodney",expression.getValue(context));

		// Property access on a class object
		expression = parser.parseExpression("T(String).name");
		assertEquals("java.lang.String",expression.getValue());
		assertCanCompile(expression);
		assertEquals("java.lang.String",expression.getValue());

		// Now the type reference isn't on the stack, and needs loading
		context = new StandardEvaluationContext(String.class);
		expression = parser.parseExpression("name");
		assertEquals("java.lang.String",expression.getValue(context));
		assertCanCompile(expression);
		assertEquals("java.lang.String",expression.getValue(context));

		expression = parser.parseExpression("T(String).getName()");
		assertEquals("java.lang.String",expression.getValue());
		assertCanCompile(expression);
		assertEquals("java.lang.String",expression.getValue());
		
		// These tests below verify that the chain of static accesses (either method/property or field)
		// leave the right thing on top of the stack for processing by any outer consuming code.
		// Here the consuming code is the String.valueOf() function.  If the wrong thing were on
		// the stack (for example if the compiled code for static methods wasn't popping the 
		// previous thing off the stack) the valueOf() would operate on the wrong value.

		String shclass = StaticsHelper.class.getName();
		// Basic chain: property access then method access
		expression = parser.parseExpression("T(String).valueOf(T(String).name.valueOf(1))");
		assertEquals("1",expression.getValue());
		assertCanCompile(expression);
		assertEquals("1",expression.getValue());

		// chain of statics ending with static method
		expression = parser.parseExpression("T(String).valueOf(T("+shclass+").methoda().methoda().methodb())");
		assertEquals("mb",expression.getValue());
		assertCanCompile(expression);
		assertEquals("mb",expression.getValue());

		// chain of statics ending with static field
		expression = parser.parseExpression("T(String).valueOf(T("+shclass+").fielda.fielda.fieldb)");
		assertEquals("fb",expression.getValue());
		assertCanCompile(expression);
		assertEquals("fb",expression.getValue());

		// chain of statics ending with static property access
		expression = parser.parseExpression("T(String).valueOf(T("+shclass+").propertya.propertya.propertyb)");
		assertEquals("pb",expression.getValue());
		assertCanCompile(expression);
		assertEquals("pb",expression.getValue());

		// variety chain
		expression = parser.parseExpression("T(String).valueOf(T("+shclass+").fielda.methoda().propertya.fieldb)");
		assertEquals("fb",expression.getValue());
		assertCanCompile(expression);
		assertEquals("fb",expression.getValue());

		expression = parser.parseExpression("T(String).valueOf(fielda.fieldb)");
		assertEquals("fb",expression.getValue(StaticsHelper.sh));
		assertCanCompile(expression);
		assertEquals("fb",expression.getValue(StaticsHelper.sh));
		
		expression = parser.parseExpression("T(String).valueOf(propertya.propertyb)");
		assertEquals("pb",expression.getValue(StaticsHelper.sh));
		assertCanCompile(expression);
		assertEquals("pb",expression.getValue(StaticsHelper.sh));

		expression = parser.parseExpression("T(String).valueOf(methoda().methodb())");
		assertEquals("mb",expression.getValue(StaticsHelper.sh));
		assertCanCompile(expression);
		assertEquals("mb",expression.getValue(StaticsHelper.sh));
		
	}
	
	@Test1
	public void constructorReference_SPR12326() {
		String type = this.getClass().getName();
		String prefix = "new "+type+".Obj";

		expression = parser.parseExpression(prefix+"([0])");
		assertEquals("test", ((Obj) expression.getValue(new Object[] { "test" })).param1);
		assertCanCompile(expression);
		assertEquals("test", ((Obj) expression.getValue(new Object[] { "test" })).param1);

		expression = parser.parseExpression(prefix+"2('foo','bar').output");
		assertEquals("foobar", expression.getValue(String.class));
		assertCanCompile(expression);
		assertEquals("foobar", expression.getValue(String.class));

		expression = parser.parseExpression(prefix+"2('foo').output");
		assertEquals("foo", expression.getValue(String.class));
		assertCanCompile(expression);
		assertEquals("foo", expression.getValue(String.class));

		expression = parser.parseExpression(prefix+"2().output");
		assertEquals("", expression.getValue(String.class));
		assertCanCompile(expression);
		assertEquals("", expression.getValue(String.class));

		expression = parser.parseExpression(prefix+"3(1,2,3).output");
		assertEquals("123", expression.getValue(String.class));
		assertCanCompile(expression);
		assertEquals("123", expression.getValue(String.class));

		expression = parser.parseExpression(prefix+"3(1).output");
		assertEquals("1", expression.getValue(String.class));
		assertCanCompile(expression);
		assertEquals("1", expression.getValue(String.class));

		expression = parser.parseExpression(prefix+"3().output");
		assertEquals("", expression.getValue(String.class));
		assertCanCompile(expression);
		assertEquals("", expression.getValue(String.class));

		expression = parser.parseExpression(prefix+"3('abc',5.0f,1,2,3).output");
		assertEquals("abc:5.0:123", expression.getValue(String.class));
		assertCanCompile(expression);
		assertEquals("abc:5.0:123", expression.getValue(String.class));

		expression = parser.parseExpression(prefix+"3('abc',5.0f,1).output");
		assertEquals("abc:5.0:1", expression.getValue(String.class));
		assertCanCompile(expression);
		assertEquals("abc:5.0:1", expression.getValue(String.class));

		expression = parser.parseExpression(prefix+"3('abc',5.0f).output");
		assertEquals("abc:5.0:", expression.getValue(String.class));
		assertCanCompile(expression);
		assertEquals("abc:5.0:", expression.getValue(String.class));

		expression = parser.parseExpression(prefix+"4(#root).output");
		assertEquals("123", expression.getValue(new int[]{1,2,3},String.class));
		assertCanCompile(expression);
		assertEquals("123", expression.getValue(new int[]{1,2,3},String.class));
	}

	@Test1
	public void methodReferenceMissingCastAndRootObjectAccessing_SPR12326() {
		// Need boxing code on the 1 so that toString() can be called
		expression = parser.parseExpression("1.toString()");
		assertEquals("1", expression.getValue());
		assertCanCompile(expression);
		assertEquals("1", expression.getValue());

		expression = parser.parseExpression("#it?.age.equals([0])");
		Person person = new Person(1);
		StandardEvaluationContext context =
				new StandardEvaluationContext(new Object[] { person.getAge() });
		context.setVariable("it", person);
		assertTrue(expression.getValue(context, Boolean.class));
		assertCanCompile(expression);
		assertTrue(expression.getValue(context, Boolean.class));

		// Variant of above more like what was in the bug report:
		SpelExpressionParser parser = new SpelExpressionParser(
				new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE,
						this.getClass().getClassLoader()));

		SpelExpression ex = parser.parseRaw("#it?.age.equals([0])");
		context = new StandardEvaluationContext(new Object[] { person.getAge() });
		context.setVariable("it", person);
		assertTrue(ex.getValue(context, Boolean.class));
		assertTrue(ex.getValue(context, Boolean.class));

		PersonInOtherPackage person2 = new PersonInOtherPackage(1);
		ex = parser.parseRaw("#it?.age.equals([0])");
		context =
				new StandardEvaluationContext(new Object[] { person2.getAge() });
		context.setVariable("it", person2);
		assertTrue(ex.getValue(context, Boolean.class));
		assertTrue(ex.getValue(context, Boolean.class));

		ex = parser.parseRaw("#it?.age.equals([0])");
		context =
				new StandardEvaluationContext(new Object[] { person2.getAge() });
		context.setVariable("it", person2);
		assertTrue((Boolean)ex.getValue(context));
		assertTrue((Boolean)ex.getValue(context));
	}

	@Test1
	public void constructorReference() throws Exception {
		// simple ctor
		expression = parser.parseExpression("new String('123')");
		assertEquals("123",expression.getValue());
		assertCanCompile(expression);
		assertEquals("123",expression.getValue());

		String testclass8 = "org.springframework.expression.spel.SpelCompilationCoverageTests$TestClass8";
		// multi arg ctor that includes primitives
		expression = parser.parseExpression("new "+testclass8+"(42,'123',4.0d,true)");
		assertEquals(testclass8,expression.getValue().getClass().getName());
		assertCanCompile(expression);
		Object o = expression.getValue();
		assertEquals(testclass8,o.getClass().getName());
		TestClass8 tc8 = (TestClass8)o;
		assertEquals(42,tc8.i);
		assertEquals("123",tc8.s);
		assertEquals(4.0d,tc8.d,0.5d);
		assertEquals(true,tc8.z);

		// no-arg ctor
		expression = parser.parseExpression("new "+testclass8+"()");
		assertEquals(testclass8,expression.getValue().getClass().getName());
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals(testclass8,o.getClass().getName());

		// pass primitive to reference type ctor
		expression = parser.parseExpression("new "+testclass8+"(42)");
		assertEquals(testclass8,expression.getValue().getClass().getName());
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals(testclass8,o.getClass().getName());
		tc8 = (TestClass8)o;
		assertEquals(42,tc8.i);

		// private class, can't compile it
		String testclass9 = "org.springframework.expression.spel.SpelCompilationCoverageTests$TestClass9";
		expression = parser.parseExpression("new "+testclass9+"(42)");
		assertEquals(testclass9,expression.getValue().getClass().getName());
		assertCantCompile(expression);
	}

	@Test1
	public void methodReferenceReflectiveMethodSelectionWithVarargs() throws Exception {
		TestClass10 tc = new TestClass10();

		// Should call the non varargs version of concat
		// (which causes the '::' prefix in test output)
		expression = parser.parseExpression("concat('test')");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("::test",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("::test",tc.s);
		tc.reset();

		// This will call the varargs concat with an empty array
		expression = parser.parseExpression("concat()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("",tc.s);
		tc.reset();

		// Should call the non varargs version of concat
		// (which causes the '::' prefix in test output)
		expression = parser.parseExpression("concat2('test')");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("::test",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("::test",tc.s);
		tc.reset();

		// This will call the varargs concat with an empty array
		expression = parser.parseExpression("concat2()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("",tc.s);
		tc.reset();
	}

	@Test1
	public void methodReferenceVarargs() throws Exception {
		TestClass5 tc = new TestClass5();

		// varargs string
		expression = parser.parseExpression("eleven()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("",tc.s);
		tc.reset();

		// varargs string
		expression = parser.parseExpression("eleven('aaa')");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("aaa",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("aaa",tc.s);
		tc.reset();

		// varargs string
		expression = parser.parseExpression("eleven(stringArray)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("aaabbbccc",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("aaabbbccc",tc.s);
		tc.reset();

		// varargs string
		expression = parser.parseExpression("eleven('aaa','bbb','ccc')");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("aaabbbccc",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("aaabbbccc",tc.s);
		tc.reset();

		expression = parser.parseExpression("sixteen('aaa','bbb','ccc')");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("aaabbbccc",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("aaabbbccc",tc.s);
		tc.reset();

		// TODO Fails related to conversion service converting a String[] to satisfy Object...
//		expression = parser.parseExpression("sixteen(stringArray)");
//		assertCantCompile(expression);
//		expression.getValue(tc);
//		assertEquals("aaabbbccc",tc.s);
//		assertCanCompile(expression);
//		tc.reset();
//		expression.getValue(tc);
//		assertEquals("aaabbbccc",tc.s);
//		tc.reset();

		// varargs int
		expression = parser.parseExpression("twelve(1,2,3)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals(6,tc.i);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals(6,tc.i);
		tc.reset();

		expression = parser.parseExpression("twelve(1)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals(1,tc.i);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals(1,tc.i);
		tc.reset();

		// one string then varargs string
		expression = parser.parseExpression("thirteen('aaa','bbb','ccc')");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("aaa::bbbccc",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("aaa::bbbccc",tc.s);
		tc.reset();

		// nothing passed to varargs parameter
		expression = parser.parseExpression("thirteen('aaa')");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("aaa::",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("aaa::",tc.s);
		tc.reset();

		// nested arrays
		expression = parser.parseExpression("fourteen('aaa',stringArray,stringArray)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("aaa::{aaabbbccc}{aaabbbccc}",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("aaa::{aaabbbccc}{aaabbbccc}",tc.s);
		tc.reset();

		// nested primitive array
		expression = parser.parseExpression("fifteen('aaa',intArray,intArray)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("aaa::{112233}{112233}",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("aaa::{112233}{112233}",tc.s);
		tc.reset();

		// varargs boolean
		expression = parser.parseExpression("arrayz(true,true,false)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("truetruefalse",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("truetruefalse",tc.s);
		tc.reset();

		expression = parser.parseExpression("arrayz(true)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("true",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("true",tc.s);
		tc.reset();

		// varargs short
		expression = parser.parseExpression("arrays(s1,s2,s3)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("123",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("123",tc.s);
		tc.reset();

		expression = parser.parseExpression("arrays(s1)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("1",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("1",tc.s);
		tc.reset();

		// varargs double
		expression = parser.parseExpression("arrayd(1.0d,2.0d,3.0d)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("1.02.03.0",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("1.02.03.0",tc.s);
		tc.reset();

		expression = parser.parseExpression("arrayd(1.0d)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("1.0",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("1.0",tc.s);
		tc.reset();

		// varargs long
		expression = parser.parseExpression("arrayj(l1,l2,l3)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("123",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("123",tc.s);
		tc.reset();

		expression = parser.parseExpression("arrayj(l1)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("1",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("1",tc.s);
		tc.reset();

		// varargs char
		expression = parser.parseExpression("arrayc(c1,c2,c3)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("abc",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("abc",tc.s);
		tc.reset();

		expression = parser.parseExpression("arrayc(c1)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("a",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("a",tc.s);
		tc.reset();

		// varargs byte
		expression = parser.parseExpression("arrayb(b1,b2,b3)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("656667",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("656667",tc.s);
		tc.reset();

		expression = parser.parseExpression("arrayb(b1)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("65",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("65",tc.s);
		tc.reset();

		// varargs float
		expression = parser.parseExpression("arrayf(f1,f2,f3)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("1.02.03.0",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("1.02.03.0",tc.s);
		tc.reset();

		expression = parser.parseExpression("arrayf(f1)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("1.0",tc.s);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("1.0",tc.s);
		tc.reset();
	}

	@Test1
	public void methodReference() throws Exception {
		TestClass5 tc = new TestClass5();

		// non-static method, no args, void return
		expression = parser.parseExpression("one()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals(1,tc.i);
		tc.reset();

		// static method, no args, void return
		expression = parser.parseExpression("two()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals(1,TestClass5._i);
		tc.reset();

		// non-static method, reference type return
		expression = parser.parseExpression("three()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		assertEquals("hello",expression.getValue(tc));
		tc.reset();

		// non-static method, primitive type return
		expression = parser.parseExpression("four()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		assertEquals(3277700L,expression.getValue(tc));
		tc.reset();

		// static method, reference type return
		expression = parser.parseExpression("five()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		assertEquals("hello",expression.getValue(tc));
		tc.reset();

		// static method, primitive type return
		expression = parser.parseExpression("six()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		assertEquals(3277700L,expression.getValue(tc));
		tc.reset();

		// non-static method, one parameter of reference type
		expression = parser.parseExpression("seven(\"foo\")");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("foo",tc.s);
		tc.reset();

		// static method, one parameter of reference type
		expression = parser.parseExpression("eight(\"bar\")");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("bar",TestClass5._s);
		tc.reset();

		// non-static method, one parameter of primitive type
		expression = parser.parseExpression("nine(231)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals(231,tc.i);
		tc.reset();

		// static method, one parameter of primitive type
		expression = parser.parseExpression("ten(111)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals(111,TestClass5._i);
		tc.reset();

		// method that gets type converted parameters

		// Converting from an int to a string
		expression = parser.parseExpression("seven(123)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("123",tc.s);
		assertCantCompile(expression); // Uncompilable as argument conversion is occurring

		Expression expression = parser.parseExpression("'abcd'.substring(index1,index2)");
		String resultI = expression.getValue(new TestClass1(),String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(new TestClass1(),String.class);
		assertEquals("bc",resultI);
		assertEquals("bc",resultC);

		// Converting from an int to a Number
		expression = parser.parseExpression("takeNumber(123)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("123",tc.s);
		tc.reset();
		assertCanCompile(expression); // The generated code should include boxing of the int to a Number
		expression.getValue(tc);
		assertEquals("123",tc.s);

		// Passing a subtype
		expression = parser.parseExpression("takeNumber(T(Integer).valueOf(42))");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("42",tc.s);
		tc.reset();
		assertCanCompile(expression); // The generated code should include boxing of the int to a Number
		expression.getValue(tc);
		assertEquals("42",tc.s);

		// Passing a subtype
		expression = parser.parseExpression("takeString(T(Integer).valueOf(42))");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("42",tc.s);
		tc.reset();
		assertCantCompile(expression); // method takes a string and we are passing an Integer
	}

	@Test1
	public void errorHandling() throws Exception {
		TestClass5 tc = new TestClass5();

		// changing target

		// from primitive array to reference type array
		int[] is = new int[]{1,2,3};
		String[] strings = new String[]{"a","b","c"};
		expression = parser.parseExpression("[1]");
		assertEquals(2,expression.getValue(is));
		assertCanCompile(expression);
		assertEquals(2,expression.getValue(is));

		try {
			assertEquals(2,expression.getValue(strings));
			fail();
		}
		catch (SpelEvaluationException see) {
			assertTrue(see.getCause() instanceof ClassCastException);
		}
		SpelCompiler.revertToInterpreted(expression);
		assertEquals("b",expression.getValue(strings));
		assertCanCompile(expression);
		assertEquals("b",expression.getValue(strings));


		tc.field = "foo";
		expression = parser.parseExpression("seven(field)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("foo",tc.s);
		assertCanCompile(expression);
		tc.reset();
		tc.field="bar";
		expression.getValue(tc);

		// method with changing parameter types (change reference type)
		tc.obj = "foo";
		expression = parser.parseExpression("seven(obj)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("foo",tc.s);
		assertCanCompile(expression);
		tc.reset();
		tc.obj=new Integer(42);
		try {
			expression.getValue(tc);
			fail();
		}
		catch (SpelEvaluationException see) {
			assertTrue(see.getCause() instanceof ClassCastException);
		}


		// method with changing target
		expression = parser.parseExpression("#root.charAt(0)");
		assertEquals('a',expression.getValue("abc"));
		assertCanCompile(expression);
		try {
			expression.getValue(new Integer(42));
			fail();
		}
		catch (SpelEvaluationException see) {
			// java.lang.Integer cannot be cast to java.lang.String
			assertTrue(see.getCause() instanceof ClassCastException);
		}
	}

	@Test1
	public void methodReference_staticMethod() throws Exception {
		Expression expression = parser.parseExpression("T(Integer).valueOf(42)");
		int resultI = expression.getValue(new TestClass1(),Integer.TYPE);
		assertCanCompile(expression);
		int resultC = expression.getValue(new TestClass1(),Integer.TYPE);
		assertEquals(42,resultI);
		assertEquals(42,resultC);
	}

	@Test1
	public void methodReference_literalArguments_int() throws Exception {
		Expression expression = parser.parseExpression("'abcd'.substring(1,3)");
		String resultI = expression.getValue(new TestClass1(),String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(new TestClass1(),String.class);
		assertEquals("bc",resultI);
		assertEquals("bc",resultC);
	}

	@Test1
	public void methodReference_simpleInstanceMethodNoArg() throws Exception {
		Expression expression = parser.parseExpression("toString()");
		String resultI = expression.getValue(42,String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(42,String.class);
		assertEquals("42",resultI);
		assertEquals("42",resultC);
	}

	@Test1
	public void methodReference_simpleInstanceMethodNoArgReturnPrimitive() throws Exception {
		expression = parser.parseExpression("intValue()");
		int resultI = expression.getValue(new Integer(42),Integer.TYPE);
		assertEquals(42,resultI);
		assertCanCompile(expression);
		int resultC = expression.getValue(new Integer(42),Integer.TYPE);
		assertEquals(42,resultC);
	}

	@Test1
	public void methodReference_simpleInstanceMethodOneArgReturnPrimitive1() throws Exception {
		Expression expression = parser.parseExpression("indexOf('b')");
		int resultI = expression.getValue("abc",Integer.TYPE);
		assertCanCompile(expression);
		int resultC = expression.getValue("abc",Integer.TYPE);
		assertEquals(1,resultI);
		assertEquals(1,resultC);
	}

	@Test1
	public void methodReference_simpleInstanceMethodOneArgReturnPrimitive2() throws Exception {
		expression = parser.parseExpression("charAt(2)");
		char resultI = expression.getValue("abc",Character.TYPE);
		assertEquals('c',resultI);
		assertCanCompile(expression);
		char resultC = expression.getValue("abc",Character.TYPE);
		assertEquals('c',resultC);
	}

	@Test1
	public void compoundExpression() throws Exception {
		Payload payload = new Payload();
		expression = parser.parseExpression("DR[0]");
		assertEquals("instanceof Two",expression.getValue(payload).toString());
		assertCanCompile(expression);
		assertEquals("instanceof Two",expression.getValue(payload).toString());
		ast = getAst();
		assertEquals("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Two",ast.getExitDescriptor());

		expression = parser.parseExpression("holder.three");
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Three",expression.getValue(payload).getClass().getName());
		assertCanCompile(expression);
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Three",expression.getValue(payload).getClass().getName());
		ast = getAst();
		assertEquals("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Three",ast.getExitDescriptor());

		expression = parser.parseExpression("DR[0]");
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Two",expression.getValue(payload).getClass().getName());
		assertCanCompile(expression);
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Two",expression.getValue(payload).getClass().getName());
		assertEquals("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Two",getAst().getExitDescriptor());

		expression = parser.parseExpression("DR[0].three");
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Three",expression.getValue(payload).getClass().getName());
		assertCanCompile(expression);
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Three",expression.getValue(payload).getClass().getName());
		ast = getAst();
		assertEquals("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Three",ast.getExitDescriptor());

		expression = parser.parseExpression("DR[0].three.four");
		assertEquals(0.04d,expression.getValue(payload));
		assertCanCompile(expression);
		assertEquals(0.04d,expression.getValue(payload));
		assertEquals("D",getAst().getExitDescriptor());
	}

	@Test1
	public void mixingItUp_indexerOpEqTernary() throws Exception {
		Map<String, String> m = new HashMap<String,String>();
		m.put("andy","778");

		expression = parse("['andy']==null?1:2");
		System.out.println(expression.getValue(m));
		assertCanCompile(expression);
		assertEquals(2,expression.getValue(m));
		m.remove("andy");
		assertEquals(1,expression.getValue(m));
	}

	@Test1
	public void propertyReference() throws Exception {
		TestClass6 tc = new TestClass6();

		// non static field
		expression = parser.parseExpression("orange");
		assertCantCompile(expression);
		assertEquals("value1",expression.getValue(tc));
		assertCanCompile(expression);
		assertEquals("value1",expression.getValue(tc));

		// static field
		expression = parser.parseExpression("apple");
		assertCantCompile(expression);
		assertEquals("value2",expression.getValue(tc));
		assertCanCompile(expression);
		assertEquals("value2",expression.getValue(tc));

		// non static getter
		expression = parser.parseExpression("banana");
		assertCantCompile(expression);
		assertEquals("value3",expression.getValue(tc));
		assertCanCompile(expression);
		assertEquals("value3",expression.getValue(tc));

		// static getter
		expression = parser.parseExpression("plum");
		assertCantCompile(expression);
		assertEquals("value4",expression.getValue(tc));
		assertCanCompile(expression);
		assertEquals("value4",expression.getValue(tc));
	}

	@Test1
	public void propertyReferenceVisibility() { // SPR-12771
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariable("httpServletRequest", HttpServlet3RequestFactory.getOne());
		// Without a fix compilation was inserting a checkcast to a private type
		expression = parser.parseExpression("#httpServletRequest.servletPath");
		assertEquals("wibble",expression.getValue(ctx));
		assertCanCompile(expression);
		assertEquals("wibble",expression.getValue(ctx));
	}

	@SuppressWarnings("unchecked")
	@Test1
	public void indexer() throws Exception {
		String[] sss = new String[]{"a","b","c"};
		Number[] ns = new Number[]{2,8,9};
		int[] is = new int[]{8,9,10};
		double[] ds = new double[]{3.0d,4.0d,5.0d};
		long[] ls = new long[]{2L,3L,4L};
		short[] ss = new short[]{(short)33,(short)44,(short)55};
		float[] fs = new float[]{6.0f,7.0f,8.0f};
		byte[] bs = new byte[]{(byte)2,(byte)3,(byte)4};
		char[] cs = new char[]{'a','b','c'};

		// Access String (reference type) array
		expression = parser.parseExpression("[0]");
		assertEquals("a",expression.getValue(sss));
		assertCanCompile(expression);
		assertEquals("a",expression.getValue(sss));
		assertEquals("Ljava/lang/String",getAst().getExitDescriptor());

		expression = parser.parseExpression("[1]");
		assertEquals(8,expression.getValue(ns));
		assertCanCompile(expression);
		assertEquals(8,expression.getValue(ns));
		assertEquals("Ljava/lang/Number",getAst().getExitDescriptor());

		// Access int array
		expression = parser.parseExpression("[2]");
		assertEquals(10,expression.getValue(is));
		assertCanCompile(expression);
		assertEquals(10,expression.getValue(is));
		assertEquals("I",getAst().getExitDescriptor());

		// Access double array
		expression = parser.parseExpression("[1]");
		assertEquals(4.0d,expression.getValue(ds));
		assertCanCompile(expression);
		assertEquals(4.0d,expression.getValue(ds));
		assertEquals("D",getAst().getExitDescriptor());

		// Access long array
		expression = parser.parseExpression("[0]");
		assertEquals(2L,expression.getValue(ls));
		assertCanCompile(expression);
		assertEquals(2L,expression.getValue(ls));
		assertEquals("J",getAst().getExitDescriptor());

		// Access short array
		expression = parser.parseExpression("[2]");
		assertEquals((short)55,expression.getValue(ss));
		assertCanCompile(expression);
		assertEquals((short)55,expression.getValue(ss));
		assertEquals("S",getAst().getExitDescriptor());

		// Access float array
		expression = parser.parseExpression("[0]");
		assertEquals(6.0f,expression.getValue(fs));
		assertCanCompile(expression);
		assertEquals(6.0f,expression.getValue(fs));
		assertEquals("F",getAst().getExitDescriptor());

		// Access byte array
		expression = parser.parseExpression("[2]");
		assertEquals((byte)4,expression.getValue(bs));
		assertCanCompile(expression);
		assertEquals((byte)4,expression.getValue(bs));
		assertEquals("B",getAst().getExitDescriptor());

		// Access char array
		expression = parser.parseExpression("[1]");
		assertEquals('b',expression.getValue(cs));
		assertCanCompile(expression);
		assertEquals('b',expression.getValue(cs));
		assertEquals("C",getAst().getExitDescriptor());

		// Collections
		List<String> strings = new ArrayList<String>();
		strings.add("aaa");
		strings.add("bbb");
		strings.add("ccc");
		expression = parser.parseExpression("[1]");
		assertEquals("bbb",expression.getValue(strings));
		assertCanCompile(expression);
		assertEquals("bbb",expression.getValue(strings));
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());

		List<Integer> ints = new ArrayList<Integer>();
		ints.add(123);
		ints.add(456);
		ints.add(789);
		expression = parser.parseExpression("[2]");
		assertEquals(789,expression.getValue(ints));
		assertCanCompile(expression);
		assertEquals(789,expression.getValue(ints));
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());

		// Maps
		Map<String,Integer> map1 = new HashMap<String,Integer>();
		map1.put("aaa", 111);
		map1.put("bbb", 222);
		map1.put("ccc", 333);
		expression = parser.parseExpression("['aaa']");
		assertEquals(111,expression.getValue(map1));
		assertCanCompile(expression);
		assertEquals(111,expression.getValue(map1));
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());

		// Object
		TestClass6 tc = new TestClass6();
		expression = parser.parseExpression("['orange']");
		assertEquals("value1",expression.getValue(tc));
		assertCanCompile(expression);
		assertEquals("value1",expression.getValue(tc));
		assertEquals("Ljava/lang/String",getAst().getExitDescriptor());

		expression = parser.parseExpression("['peach']");
		assertEquals(34L,expression.getValue(tc));
		assertCanCompile(expression);
		assertEquals(34L,expression.getValue(tc));
		assertEquals("J",getAst().getExitDescriptor());

		// getter
		expression = parser.parseExpression("['banana']");
		assertEquals("value3",expression.getValue(tc));
		assertCanCompile(expression);
		assertEquals("value3",expression.getValue(tc));
		assertEquals("Ljava/lang/String",getAst().getExitDescriptor());

		// list of arrays

		List<String[]> listOfStringArrays = new ArrayList<String[]>();
		listOfStringArrays.add(new String[]{"a","b","c"});
		listOfStringArrays.add(new String[]{"d","e","f"});
		expression = parser.parseExpression("[1]");
		assertEquals("d e f",stringify(expression.getValue(listOfStringArrays)));
		assertCanCompile(expression);
		assertEquals("d e f",stringify(expression.getValue(listOfStringArrays)));
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());

		expression = parser.parseExpression("[1][0]");
		assertEquals("d",stringify(expression.getValue(listOfStringArrays)));
		assertCanCompile(expression);
		assertEquals("d",stringify(expression.getValue(listOfStringArrays)));
		assertEquals("Ljava/lang/String",getAst().getExitDescriptor());

		List<Integer[]> listOfIntegerArrays = new ArrayList<Integer[]>();
		listOfIntegerArrays.add(new Integer[]{1,2,3});
		listOfIntegerArrays.add(new Integer[]{4,5,6});
		expression = parser.parseExpression("[0]");
		assertEquals("1 2 3",stringify(expression.getValue(listOfIntegerArrays)));
		assertCanCompile(expression);
		assertEquals("1 2 3",stringify(expression.getValue(listOfIntegerArrays)));
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());

		expression = parser.parseExpression("[0][1]");
		assertEquals(2,expression.getValue(listOfIntegerArrays));
		assertCanCompile(expression);
		assertEquals(2,expression.getValue(listOfIntegerArrays));
		assertEquals("Ljava/lang/Integer",getAst().getExitDescriptor());

		// array of lists
		List<String>[] stringArrayOfLists = new ArrayList[2];
		stringArrayOfLists[0] = new ArrayList<String>();
		stringArrayOfLists[0].add("a");
		stringArrayOfLists[0].add("b");
		stringArrayOfLists[0].add("c");
		stringArrayOfLists[1] = new ArrayList<String>();
		stringArrayOfLists[1].add("d");
		stringArrayOfLists[1].add("e");
		stringArrayOfLists[1].add("f");
		expression = parser.parseExpression("[1]");
		assertEquals("d e f",stringify(expression.getValue(stringArrayOfLists)));
		assertCanCompile(expression);
		assertEquals("d e f",stringify(expression.getValue(stringArrayOfLists)));
		assertEquals("Ljava/util/ArrayList",getAst().getExitDescriptor());

		expression = parser.parseExpression("[1][2]");
		assertEquals("f",stringify(expression.getValue(stringArrayOfLists)));
		assertCanCompile(expression);
		assertEquals("f",stringify(expression.getValue(stringArrayOfLists)));
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());

		// array of arrays
		String[][] referenceTypeArrayOfArrays = new String[][]{new String[]{"a","b","c"},new String[]{"d","e","f"}};
		expression = parser.parseExpression("[1]");
		assertEquals("d e f",stringify(expression.getValue(referenceTypeArrayOfArrays)));
		assertCanCompile(expression);
		assertEquals("[Ljava/lang/String",getAst().getExitDescriptor());
		assertEquals("d e f",stringify(expression.getValue(referenceTypeArrayOfArrays)));
		assertEquals("[Ljava/lang/String",getAst().getExitDescriptor());

		expression = parser.parseExpression("[1][2]");
		assertEquals("f",stringify(expression.getValue(referenceTypeArrayOfArrays)));
		assertCanCompile(expression);
		assertEquals("f",stringify(expression.getValue(referenceTypeArrayOfArrays)));
		assertEquals("Ljava/lang/String",getAst().getExitDescriptor());

		int[][] primitiveTypeArrayOfArrays = new int[][]{new int[]{1,2,3},new int[]{4,5,6}};
		expression = parser.parseExpression("[1]");
		assertEquals("4 5 6",stringify(expression.getValue(primitiveTypeArrayOfArrays)));
		assertCanCompile(expression);
		assertEquals("4 5 6",stringify(expression.getValue(primitiveTypeArrayOfArrays)));
		assertEquals("[I",getAst().getExitDescriptor());

		expression = parser.parseExpression("[1][2]");
		assertEquals("6",stringify(expression.getValue(primitiveTypeArrayOfArrays)));
		assertCanCompile(expression);
		assertEquals("6",stringify(expression.getValue(primitiveTypeArrayOfArrays)));
		assertEquals("I",getAst().getExitDescriptor());

		// list of lists of reference types
		List<List<String>> listOfListOfStrings = new ArrayList<List<String>>();
		List<String> list = new ArrayList<String>();
		list.add("a");
		list.add("b");
		list.add("c");
		listOfListOfStrings.add(list);
		list = new ArrayList<String>();
		list.add("d");
		list.add("e");
		list.add("f");
		listOfListOfStrings.add(list);

		expression = parser.parseExpression("[1]");
		assertEquals("d e f",stringify(expression.getValue(listOfListOfStrings)));
		assertCanCompile(expression);
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());
		assertEquals("d e f",stringify(expression.getValue(listOfListOfStrings)));
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());

		expression = parser.parseExpression("[1][2]");
		assertEquals("f",stringify(expression.getValue(listOfListOfStrings)));
		assertCanCompile(expression);
		assertEquals("f",stringify(expression.getValue(listOfListOfStrings)));
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());

		// Map of lists
		Map<String,List<String>> mapToLists = new HashMap<String,List<String>>();
		list = new ArrayList<String>();
		list.add("a");
		list.add("b");
		list.add("c");
		mapToLists.put("foo", list);
		expression = parser.parseExpression("['foo']");
		assertEquals("a b c",stringify(expression.getValue(mapToLists)));
		assertCanCompile(expression);
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());
		assertEquals("a b c",stringify(expression.getValue(mapToLists)));
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());

		expression = parser.parseExpression("['foo'][2]");
		assertEquals("c",stringify(expression.getValue(mapToLists)));
		assertCanCompile(expression);
		assertEquals("c",stringify(expression.getValue(mapToLists)));
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());

		// Map to array
		Map<String,int[]> mapToIntArray = new HashMap<String,int[]>();
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.addPropertyAccessor(new CompilableMapAccessor());
		mapToIntArray.put("foo",new int[]{1,2,3});
		expression = parser.parseExpression("['foo']");
		assertEquals("1 2 3",stringify(expression.getValue(mapToIntArray)));
		assertCanCompile(expression);
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());
		assertEquals("1 2 3",stringify(expression.getValue(mapToIntArray)));
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());

		expression = parser.parseExpression("['foo'][1]");
		assertEquals(2,expression.getValue(mapToIntArray));
		assertCanCompile(expression);
		assertEquals(2,expression.getValue(mapToIntArray));

		expression = parser.parseExpression("foo");
		assertEquals("1 2 3",stringify(expression.getValue(ctx,mapToIntArray)));
		assertCanCompile(expression);
		assertEquals("1 2 3",stringify(expression.getValue(ctx,mapToIntArray)));
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());

		expression = parser.parseExpression("foo[1]");
		assertEquals(2,expression.getValue(ctx,mapToIntArray));
		assertCanCompile(expression);
		assertEquals(2,expression.getValue(ctx,mapToIntArray));

		expression = parser.parseExpression("['foo'][2]");
		assertEquals("3",stringify(expression.getValue(ctx,mapToIntArray)));
		assertCanCompile(expression);
		assertEquals("3",stringify(expression.getValue(ctx,mapToIntArray)));
		assertEquals("I",getAst().getExitDescriptor());

		// Map array
		Map<String,String>[] mapArray = new Map[1];
		mapArray[0] = new HashMap<String,String>();
		mapArray[0].put("key", "value1");
		expression = parser.parseExpression("[0]");
		assertEquals("{key=value1}",stringify(expression.getValue(mapArray)));
		assertCanCompile(expression);
		assertEquals("Ljava/util/Map",getAst().getExitDescriptor());
		assertEquals("{key=value1}",stringify(expression.getValue(mapArray)));
		assertEquals("Ljava/util/Map",getAst().getExitDescriptor());

		expression = parser.parseExpression("[0]['key']");
		assertEquals("value1",stringify(expression.getValue(mapArray)));
		assertCanCompile(expression);
		assertEquals("value1",stringify(expression.getValue(mapArray)));
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());
	}

	@Test1
	public void plusNeedingCheckcast_SPR12426() {
		expression = parser.parseExpression("object + ' world'");
		Object v = expression.getValue(new FooObject());
		assertEquals("hello world",v);
		assertCanCompile(expression);
		assertEquals("hello world",v);

		expression = parser.parseExpression("object + ' world'");
		v = expression.getValue(new FooString());
		assertEquals("hello world",v);
		assertCanCompile(expression);
		assertEquals("hello world",v);
	}

	@Test1
	public void mixingItUp_propertyAccessIndexerOpLtTernaryRootNull() throws Exception {
		Payload payload = new Payload();

		expression = parser.parseExpression("DR[0].three");
		Object v = expression.getValue(payload);
		assertEquals("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Three",getAst().getExitDescriptor());

		Expression expression = parser.parseExpression("DR[0].three.four lt 0.1d?#root:null");
		v = expression.getValue(payload);

		SpelExpression sExpr = (SpelExpression)expression;
		Ternary ternary = (Ternary)sExpr.getAST();
		OpLT oplt = (OpLT)ternary.getChild(0);
		CompoundExpression cExpr = (CompoundExpression)oplt.getLeftOperand();
		String cExprExitDescriptor = cExpr.getExitDescriptor();
		assertEquals("D",cExprExitDescriptor);
		assertEquals("Z",oplt.getExitDescriptor());

		assertCanCompile(expression);
		Object vc = expression.getValue(payload);
		assertEquals(payload,v);
		assertEquals(payload,vc);
		payload.DR[0].three.four = 0.13d;
		vc = expression.getValue(payload);
		assertNull(vc);
	}

	@Test1
	public void variantGetter() throws Exception {
		Payload2Holder holder = new Payload2Holder();
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.addPropertyAccessor(new MyAccessor());
		expression = parser.parseExpression("payload2.var1");
		Object v = expression.getValue(ctx,holder);
		assertEquals("abc",v);

//		// time it interpreted
//		long stime = System.currentTimeMillis();
//		for (int i=0;i<100000;i++) {
//			v = expression.getValue(ctx,holder);
//		}
//		System.out.println((System.currentTimeMillis()-stime));
//
		assertCanCompile(expression);
		v = expression.getValue(ctx,holder);
		assertEquals("abc",v);
//
//		// time it compiled
//		stime = System.currentTimeMillis();
//		for (int i=0;i<100000;i++) {
//			v = expression.getValue(ctx,holder);
//		}
//		System.out.println((System.currentTimeMillis()-stime));
	}

	@Test1
	public void compilerWithGenerics_12040() {
		expression = parser.parseExpression("payload!=2");
		assertTrue(expression.getValue(new GenericMessageTestHelper<Integer>(4),Boolean.class));
		assertCanCompile(expression);
		assertFalse(expression.getValue(new GenericMessageTestHelper<Integer>(2),Boolean.class));

		expression = parser.parseExpression("2!=payload");
		assertTrue(expression.getValue(new GenericMessageTestHelper<Integer>(4),Boolean.class));
		assertCanCompile(expression);
		assertFalse(expression.getValue(new GenericMessageTestHelper<Integer>(2),Boolean.class));

		expression = parser.parseExpression("payload!=6L");
		assertTrue(expression.getValue(new GenericMessageTestHelper<Long>(4L),Boolean.class));
		assertCanCompile(expression);
		assertFalse(expression.getValue(new GenericMessageTestHelper<Long>(6L),Boolean.class));

		expression = parser.parseExpression("payload==2");
		assertFalse(expression.getValue(new GenericMessageTestHelper<Integer>(4),Boolean.class));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper<Integer>(2),Boolean.class));

		expression = parser.parseExpression("2==payload");
		assertFalse(expression.getValue(new GenericMessageTestHelper<Integer>(4),Boolean.class));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper<Integer>(2),Boolean.class));

		expression = parser.parseExpression("payload==6L");
		assertFalse(expression.getValue(new GenericMessageTestHelper<Long>(4L),Boolean.class));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper<Long>(6L),Boolean.class));

		expression = parser.parseExpression("2==payload");
		assertFalse(expression.getValue(new GenericMessageTestHelper<Integer>(4),Boolean.class));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper<Integer>(2),Boolean.class));

		expression = parser.parseExpression("payload/2");
		assertEquals(2,expression.getValue(new GenericMessageTestHelper<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(3,expression.getValue(new GenericMessageTestHelper<Integer>(6)));

		expression = parser.parseExpression("100/payload");
		assertEquals(25,expression.getValue(new GenericMessageTestHelper<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(10,expression.getValue(new GenericMessageTestHelper<Integer>(10)));

		expression = parser.parseExpression("payload+2");
		assertEquals(6,expression.getValue(new GenericMessageTestHelper<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(8,expression.getValue(new GenericMessageTestHelper<Integer>(6)));

		expression = parser.parseExpression("100+payload");
		assertEquals(104,expression.getValue(new GenericMessageTestHelper<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(110,expression.getValue(new GenericMessageTestHelper<Integer>(10)));

		expression = parser.parseExpression("payload-2");
		assertEquals(2,expression.getValue(new GenericMessageTestHelper<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(4,expression.getValue(new GenericMessageTestHelper<Integer>(6)));

		expression = parser.parseExpression("100-payload");
		assertEquals(96,expression.getValue(new GenericMessageTestHelper<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(90,expression.getValue(new GenericMessageTestHelper<Integer>(10)));

		expression = parser.parseExpression("payload*2");
		assertEquals(8,expression.getValue(new GenericMessageTestHelper<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(12,expression.getValue(new GenericMessageTestHelper<Integer>(6)));

		expression = parser.parseExpression("100*payload");
		assertEquals(400,expression.getValue(new GenericMessageTestHelper<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(1000,expression.getValue(new GenericMessageTestHelper<Integer>(10)));

		expression = parser.parseExpression("payload/2L");
		assertEquals(2L,expression.getValue(new GenericMessageTestHelper<Long>(4L)));
		assertCanCompile(expression);
		assertEquals(3L,expression.getValue(new GenericMessageTestHelper<Long>(6L)));

		expression = parser.parseExpression("100L/payload");
		assertEquals(25L,expression.getValue(new GenericMessageTestHelper<Long>(4L)));
		assertCanCompile(expression);
		assertEquals(10L,expression.getValue(new GenericMessageTestHelper<Long>(10L)));

		expression = parser.parseExpression("payload/2f");
		assertEquals(2f,expression.getValue(new GenericMessageTestHelper<Float>(4f)));
		assertCanCompile(expression);
		assertEquals(3f,expression.getValue(new GenericMessageTestHelper<Float>(6f)));

		expression = parser.parseExpression("100f/payload");
		assertEquals(25f,expression.getValue(new GenericMessageTestHelper<Float>(4f)));
		assertCanCompile(expression);
		assertEquals(10f,expression.getValue(new GenericMessageTestHelper<Float>(10f)));

		expression = parser.parseExpression("payload/2d");
		assertEquals(2d,expression.getValue(new GenericMessageTestHelper<Double>(4d)));
		assertCanCompile(expression);
		assertEquals(3d,expression.getValue(new GenericMessageTestHelper<Double>(6d)));

		expression = parser.parseExpression("100d/payload");
		assertEquals(25d,expression.getValue(new GenericMessageTestHelper<Double>(4d)));
		assertCanCompile(expression);
		assertEquals(10d,expression.getValue(new GenericMessageTestHelper<Double>(10d)));
	}

	// The new helper class here uses an upper bound on the generic
	@Test1
	public void compilerWithGenerics_12040_2() {
		expression = parser.parseExpression("payload/2");
		assertEquals(2,expression.getValue(new GenericMessageTestHelper2<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(3,expression.getValue(new GenericMessageTestHelper2<Integer>(6)));

		expression = parser.parseExpression("9/payload");
		assertEquals(1,expression.getValue(new GenericMessageTestHelper2<Integer>(9)));
		assertCanCompile(expression);
		assertEquals(3,expression.getValue(new GenericMessageTestHelper2<Integer>(3)));

		expression = parser.parseExpression("payload+2");
		assertEquals(6,expression.getValue(new GenericMessageTestHelper2<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(8,expression.getValue(new GenericMessageTestHelper2<Integer>(6)));

		expression = parser.parseExpression("100+payload");
		assertEquals(104,expression.getValue(new GenericMessageTestHelper2<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(110,expression.getValue(new GenericMessageTestHelper2<Integer>(10)));

		expression = parser.parseExpression("payload-2");
		assertEquals(2,expression.getValue(new GenericMessageTestHelper2<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(4,expression.getValue(new GenericMessageTestHelper2<Integer>(6)));

		expression = parser.parseExpression("100-payload");
		assertEquals(96,expression.getValue(new GenericMessageTestHelper2<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(90,expression.getValue(new GenericMessageTestHelper2<Integer>(10)));

		expression = parser.parseExpression("payload*2");
		assertEquals(8,expression.getValue(new GenericMessageTestHelper2<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(12,expression.getValue(new GenericMessageTestHelper2<Integer>(6)));

		expression = parser.parseExpression("100*payload");
		assertEquals(400,expression.getValue(new GenericMessageTestHelper2<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(1000,expression.getValue(new GenericMessageTestHelper2<Integer>(10)));
	}

	// The other numeric operators
	@Test1
	public void compilerWithGenerics_12040_3() {
		expression = parser.parseExpression("payload >= 2");
		assertTrue(expression.getValue(new GenericMessageTestHelper2<Integer>(4),Boolean.TYPE));
		assertCanCompile(expression);
		assertFalse(expression.getValue(new GenericMessageTestHelper2<Integer>(1),Boolean.TYPE));

		expression = parser.parseExpression("2 >= payload");
		assertFalse(expression.getValue(new GenericMessageTestHelper2<Integer>(5),Boolean.TYPE));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper2<Integer>(1),Boolean.TYPE));

		expression = parser.parseExpression("payload > 2");
		assertTrue(expression.getValue(new GenericMessageTestHelper2<Integer>(4),Boolean.TYPE));
		assertCanCompile(expression);
		assertFalse(expression.getValue(new GenericMessageTestHelper2<Integer>(1),Boolean.TYPE));

		expression = parser.parseExpression("2 > payload");
		assertFalse(expression.getValue(new GenericMessageTestHelper2<Integer>(5),Boolean.TYPE));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper2<Integer>(1),Boolean.TYPE));

		expression = parser.parseExpression("payload <=2");
		assertTrue(expression.getValue(new GenericMessageTestHelper2<Integer>(1),Boolean.TYPE));
		assertCanCompile(expression);
		assertFalse(expression.getValue(new GenericMessageTestHelper2<Integer>(6),Boolean.TYPE));

		expression = parser.parseExpression("2 <= payload");
		assertFalse(expression.getValue(new GenericMessageTestHelper2<Integer>(1),Boolean.TYPE));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper2<Integer>(6),Boolean.TYPE));

		expression = parser.parseExpression("payload < 2");
		assertTrue(expression.getValue(new GenericMessageTestHelper2<Integer>(1),Boolean.TYPE));
		assertCanCompile(expression);
		assertFalse(expression.getValue(new GenericMessageTestHelper2<Integer>(6),Boolean.TYPE));

		expression = parser.parseExpression("2 < payload");
		assertFalse(expression.getValue(new GenericMessageTestHelper2<Integer>(1),Boolean.TYPE));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper2<Integer>(6),Boolean.TYPE));
	}

	@Test1
	public void indexerMapAccessor_12045() throws Exception {
		SpelParserConfiguration spc = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE,this.getClass().getClassLoader());
		SpelExpressionParser sep = new SpelExpressionParser(spc);
		expression=sep.parseExpression("headers[command]");
		MyMessage root = new MyMessage();
		assertEquals("wibble",expression.getValue(root));
		// This next call was failing because the isCompilable check in Indexer did not check on the key being compilable
		// (and also generateCode in the Indexer was missing the optimization that it didn't need necessarily need to call
		// generateCode for that accessor)
		assertEquals("wibble",expression.getValue(root));
		assertCanCompile(expression);

		// What about a map key that is an expression - ensure the getKey() is evaluated in the right scope
		expression=sep.parseExpression("headers[getKey()]");
		assertEquals("wobble",expression.getValue(root));
		assertEquals("wobble",expression.getValue(root));

		expression=sep.parseExpression("list[getKey2()]");
		assertEquals("wobble",expression.getValue(root));
		assertEquals("wobble",expression.getValue(root));

		expression = sep.parseExpression("ia[getKey2()]");
		assertEquals(3,expression.getValue(root));
		assertEquals(3,expression.getValue(root));
	}


	// helper methods

	private SpelNodeImpl getAst() {
		SpelExpression spelExpression = (SpelExpression)expression;
		SpelNode ast = spelExpression.getAST();
		return (SpelNodeImpl)ast;
	}

	private String stringify(Object object) {
		StringBuilder s = new StringBuilder();
		if (object instanceof List) {
			List<?> ls = (List<?>)object;
			for (Object l: ls) {
				s.append(l);
				s.append(" ");
			}
		}
		else if (object instanceof Object[]) {
			Object[] os = (Object[])object;
			for (Object o: os) {
				s.append(o);
				s.append(" ");
			}
		}
		else if (object instanceof int[]) {
			int[] is = (int[])object;
			for (int i: is) {
				s.append(i);
				s.append(" ");
			}
		}
		else {
			s.append(object.toString());
		}
		return s.toString().trim();
	}

	private void assertCanCompile(Expression expression) {
		assertTrue(SpelCompiler.compile(expression));
	}

	private void assertCantCompile(Expression expression) {
		assertFalse(SpelCompiler.compile(expression));
	}

	private Expression parse(String expression) {
		return parser.parseExpression(expression);
	}

	private void assertGetValueFail(Expression expression) {
		try {
			Object o = expression.getValue();
			fail("Calling getValue on the expression should have failed but returned "+o);
		}
		catch (Exception ex) {
			// success!
		}
	}


	// helper classes

	public interface Message<T> {

		MessageHeaders getHeaders();

		@SuppressWarnings("rawtypes")
		List getList();

		int[] getIa();
	}

	public static class MyMessage implements Message<String> {

		public MessageHeaders getHeaders() {
			MessageHeaders mh = new MessageHeaders();
			mh.put("command", "wibble");
			mh.put("command2", "wobble");
			return mh;
		}

		public int[] getIa() { return new int[]{5,3}; }

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public List getList() {
			List l = new ArrayList();
			l.add("wibble");
			l.add("wobble");
			return l;
		}

		public String getKey() {
			return "command2";
		}

		public int getKey2() {
			return 1;
		}
	}

	@SuppressWarnings("serial")
	public static class MessageHeaders extends HashMap<String,Object> {
	}

	public static class GenericMessageTestHelper<T> {

		private T payload;

		GenericMessageTestHelper(T value) {
			this.payload = value;
		}

		public T getPayload() {
			return payload;
		}
	}

	// This test helper has a bound on the type variable
	public static class GenericMessageTestHelper2<T extends Number> {

		private T payload;

		GenericMessageTestHelper2(T value) {
			this.payload = value;
		}

		public T getPayload() {
			return payload;
		}
	}

	static class MyAccessor implements CompilablePropertyAccessor {

		private Method method;

		public Class<?>[] getSpecificTargetClasses() {
			return new Class[]{Payload2.class};
		}

		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			// target is a Payload2 instance
			return true;
		}

		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			Payload2 payload2 = (Payload2)target;
			return new TypedValue(payload2.getField(name));
		}

		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return false;
		}

		public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
		}

		@Override
		public boolean isCompilable() {
			return true;
		}

		@Override
		public Class<?> getPropertyType() {
			return Object.class;
		}

		@Override
		public void generateCode(String propertyName, MethodVisitor mv,CodeFlow cf) {
			if (method == null) {
				try {
					method = Payload2.class.getDeclaredMethod("getField", String.class);
				}
				catch (Exception e) {
				}
			}
			String descriptor = cf.lastDescriptor();
			String memberDeclaringClassSlashedDescriptor = method.getDeclaringClass().getName().replace('.','/');
			if (descriptor == null) {
				cf.loadTarget(mv);
			}
			if (descriptor == null || !memberDeclaringClassSlashedDescriptor.equals(descriptor.substring(1))) {
				mv.visitTypeInsn(CHECKCAST, memberDeclaringClassSlashedDescriptor);
			}
			mv.visitLdcInsn(propertyName);
			mv.visitMethodInsn(INVOKEVIRTUAL, memberDeclaringClassSlashedDescriptor, method.getName(),CodeFlow.createSignatureDescriptor(method),false);
		}
	}

	static class CompilableMapAccessor implements CompilablePropertyAccessor {

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			Map<?,?> map = (Map<?,?>) target;
			return map.containsKey(name);
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			Map<?,?> map = (Map<?,?>) target;
			Object value = map.get(name);
			if (value == null && !map.containsKey(name)) {
				throw new MapAccessException(name);
			}
			return new TypedValue(value);
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
			Map<String,Object> map = (Map<String,Object>) target;
			map.put(name, newValue);
		}

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return new Class[] {Map.class};
		}

		@Override
		public boolean isCompilable() {
			return true;
		}

		@Override
		public Class<?> getPropertyType() {
			return Object.class;
		}

		@Override
		public void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf) {
			String descriptor = cf.lastDescriptor();
			if (descriptor == null) {
				cf.loadTarget(mv);
			}
			mv.visitLdcInsn(propertyName);
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get","(Ljava/lang/Object;)Ljava/lang/Object;",true);
		}
	}

	/**
	 * Exception thrown from {@code read} in order to reset a cached
	 * PropertyAccessor, allowing other accessors to have a try.
	 */
	@SuppressWarnings("serial")
	private static class MapAccessException extends AccessException {

		private final String key;

		public MapAccessException(String key) {
			super(null);
			this.key = key;
		}

		@Override
		public String getMessage() {
			return "Map does not contain a value for key '" + this.key + "'";
		}
	}


	// test classes

	public static class Greeter {

		public String getWorld() {
			return "world";
		}

		public Object getObject() {
			return "object";
		}
	}

	public static class FooObject {

		public Object getObject() { return "hello"; }
	}

	public static class FooString {

		public String getObject() { return "hello"; }
	}

	public static class Payload {

		Two[] DR = new Two[]{new Two()};

		public Two holder = new Two();

		public Two[] getDR() {
			return DR;
		}
	}

	public static class Payload2 {

		String var1 = "abc";
		String var2 = "def";

		public Object getField(String name) {
			if (name.equals("var1")) {
				return var1;
			}
			else if (name.equals("var2")) {
				return var2;
			}
			return null;
		}
	}

	public static class Payload2Holder {

		public Payload2 payload2 = new Payload2();
	}

	public class Person {

		private int age;

		public Person(int age) {
			this.age = age;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}

	public class Person3 {

		private int age;

		public Person3(String name, int age) {
			this.age = age;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}

	public static class Two {

		Three three = new Three();

		public Three getThree() {
			return three;
		}
		public String toString() {
			return "instanceof Two";
		}
	}

	public static class Three {

		double four = 0.04d;

		public double getFour() {
			return four;
		}
	}

	public static class TestClass1 {

		public int index1 = 1;
		public int index2 = 3;
		public String word = "abcd";
	}

	public static class TestClass4 {

		public boolean a,b;
		public boolean gettrue() { return true; }
		public boolean getfalse() { return false; }
		public boolean getA() { return a; }
		public boolean getB() { return b; }
	}

	public static class TestClass10 {

		public String s = null;

		public void reset() {
			s = null;
		}

		public void concat(String arg) {
			s = "::"+arg;
		}

		public void concat(String... vargs) {
			if (vargs==null) {
				s = "";
			}
			else {
				s = "";
				for (String varg: vargs) {
					s+=varg;
				}
			}
		}

		public void concat2(Object arg) {
			s = "::"+arg;
		}

		public void concat2(Object... vargs) {
			if (vargs==null) {
				s = "";
			}
			else {
				s = "";
				for (Object varg: vargs) {
					s+=varg;
				}
			}
		}
	}

	public static class TestClass5 {

		public int i = 0;
		public String s = null;
		public static int _i = 0;
		public static String _s = null;

		public static short s1 = (short)1;
		public static short s2 = (short)2;
		public static short s3 = (short)3;

		public static long l1 = 1L;
		public static long l2 = 2L;
		public static long l3 = 3L;

		public static float f1 = 1f;
		public static float f2 = 2f;
		public static float f3 = 3f;

		public static char c1 = 'a';
		public static char c2 = 'b';
		public static char c3 = 'c';

		public static byte b1 = (byte)65;
		public static byte b2 = (byte)66;
		public static byte b3 = (byte)67;

		public static String[] stringArray = new String[]{"aaa","bbb","ccc"};
		public static int[] intArray = new int[]{11,22,33};

		public Object obj = null;

		public String field = null;

		public void reset() {
			i = 0;
			_i=0;
			s = null;
			_s = null;
			field = null;
		}

		public void one() { i = 1; }

		public static void two() { _i = 1; }

		public String three() { return "hello"; }
		public long four() { return 3277700L; }

		public static String five() { return "hello"; }
		public static long six() { return 3277700L; }

		public void seven(String toset) { s = toset; }
//		public void seven(Number n) { s = n.toString(); }

		public void takeNumber(Number n) { s = n.toString(); }
		public void takeString(String s) { this.s = s; }
		public static void eight(String toset) { _s = toset; }

		public void nine(int toset) { i = toset; }
		public static void ten(int toset) { _i = toset; }

		public void eleven(String... vargs) {
			if (vargs==null) {
				s = "";
			}
			else {
				s = "";
				for (String varg: vargs) {
					s+=varg;
				}
			}
		}

		public void twelve(int... vargs) {
			if (vargs==null) {
				i = 0;
			}
			else {
				i = 0;
				for (int varg: vargs) {
					i+=varg;
				}
			}
		}

		public void thirteen(String a, String... vargs) {
			if (vargs==null) {
				s = a+"::";
			}
			else {
				s = a+"::";
				for (String varg: vargs) {
					s+=varg;
				}
			}
		}

		public void arrayz(boolean... bs) {
			s = "";
			if (bs != null) {
				s = "";
				for (boolean b: bs) {
					s+=Boolean.toString(b);
				}
			}
		}

		public void arrays(short... ss) {
			s = "";
			if (ss != null) {
				s = "";
				for (short s: ss) {
					this.s+=Short.toString(s);
				}
			}
		}

		public void arrayd(double... vargs) {
			s = "";
			if (vargs != null) {
				s = "";
				for (double v: vargs) {
					this.s+=Double.toString(v);
				}
			}
		}

		public void arrayf(float... vargs) {
			s = "";
			if (vargs != null) {
				s = "";
				for (float v: vargs) {
					this.s+=Float.toString(v);
				}
			}
		}

		public void arrayj(long... vargs) {
			s = "";
			if (vargs != null) {
				s = "";
				for (long v: vargs) {
					this.s+=Long.toString(v);
				}
			}
		}

		public void arrayb(byte... vargs) {
			s = "";
			if (vargs != null) {
				s = "";
				for (Byte v: vargs) {
					this.s+=Byte.toString(v);
				}
			}
		}

		public void arrayc(char... vargs) {
			s = "";
			if (vargs != null) {
				s = "";
				for (char v: vargs) {
					this.s+=Character.toString(v);
				}
			}
		}

		public void fourteen(String a, String[]... vargs) {
			if (vargs==null) {
				s = a+"::";
			}
			else {
				s = a+"::";
				for (String[] varg: vargs) {
					s+="{";
					for (String v: varg) {
						s+=v;
					}
					s+="}";
				}
			}
		}

		public void fifteen(String a, int[]... vargs) {
			if (vargs==null) {
				s = a+"::";
			}
			else {
				s = a+"::";
				for (int[] varg: vargs) {
					s+="{";
					for (int v: varg) {
						s+=Integer.toString(v);
					}
					s+="}";
				}
			}
		}

		public void sixteen(Object... vargs) {
			if (vargs==null) {
				s = "";
			}
			else {
				s = "";
				for (Object varg: vargs) {
					s+=varg;
				}
			}
		}
	}

	public static class TestClass6 {

		public String orange = "value1";
		public static String apple = "value2";

		public long peach = 34L;

		public String getBanana() {
			return "value3";
		}

		public static String getPlum() {
			return "value4";
		}
	}

	public static class TestClass7 {

		public static String property;

		static {
			String s = "UK 123";
			StringTokenizer st = new StringTokenizer(s);
			property = st.nextToken();
		}

		public static void reset() {
			String s = "UK 123";
			StringTokenizer st = new StringTokenizer(s);
			property = st.nextToken();
		}

	}

	public static class TestClass8 {

		public int i;
		public String s;
		public double d;
		public boolean z;

		public TestClass8(int i, String s, double d, boolean z) {
			this.i = i;
			this.s = s;
			this.d = d;
			this.z = z;
		}

		public TestClass8() {

		}

		public TestClass8(Integer i) {
			this.i = i;
		}

		@SuppressWarnings("unused")
		private TestClass8(String a, String b) {
			this.s = a+b;
		}
	}

	public static class Obj {

		private final String param1;

		public Obj(String param1){
			this.param1 = param1;
		}
	}

	public static class Obj2 {

		public final String output;

		public Obj2(String... params){
			StringBuilder b = new StringBuilder();
			for (String param: params) {
				b.append(param);
			}
			output = b.toString();
		}
	}

	public static class Obj3 {

		public final String output;

		public Obj3(int... params) {
			StringBuilder b = new StringBuilder();
			for (int param: params) {
				b.append(Integer.toString(param));
			}
			output = b.toString();
		}

		public Obj3(String s, Float f, int... ints) {
			StringBuilder b = new StringBuilder();
			b.append(s);
			b.append(":");
			b.append(Float.toString(f));
			b.append(":");
			for (int param: ints) {
				b.append(Integer.toString(param));
			}
			output = b.toString();
		}
	}

	public static class Obj4 {

		public final String output;

		public Obj4(int[] params) {
			StringBuilder b = new StringBuilder();
			for (int param: params) {
				b.append(Integer.toString(param));
			}
			output = b.toString();
		}
	}

	@SuppressWarnings("unused")
	private static class TestClass9 {

		public TestClass9(int i) {}
	}

	// These test classes simulate a pattern of public/private classes seen in Spring Security

	// final class HttpServlet3RequestFactory implements HttpServletRequestFactory
	static class HttpServlet3RequestFactory {

		static Servlet3SecurityContextHolderAwareRequestWrapper getOne() {
			HttpServlet3RequestFactory outer = new HttpServlet3RequestFactory();
			return outer.new Servlet3SecurityContextHolderAwareRequestWrapper();
		}

		// private class Servlet3SecurityContextHolderAwareRequestWrapper extends SecurityContextHolderAwareRequestWrapper
		private class Servlet3SecurityContextHolderAwareRequestWrapper extends SecurityContextHolderAwareRequestWrapper {
		}
	}

	// public class SecurityContextHolderAwareRequestWrapper extends HttpServletRequestWrapper
	static class SecurityContextHolderAwareRequestWrapper extends HttpServletRequestWrapper {
	}

	public static class HttpServletRequestWrapper {

		public String getServletPath() {
			return "wibble";
		}
	}

	// Here the declaring class is not public
	static class SomeCompareMethod {

		// method not public
		static int compare(Object o1, Object o2) {
			return -1;
		}

		// public
		public static int compare2(Object o1, Object o2) {
			return -1;
		}
	}

	public static class SomeCompareMethod2 {

		public static int negate(int i1) {
			return -i1;
		}

		public static String append(String... strings) {
			StringBuilder b = new StringBuilder();
			for (String string: strings) {
				b.append(string);
			}
			return b.toString();
		}

		public static String append2(Object... objects) {
			StringBuilder b = new StringBuilder();
			for (Object object: objects) {
				b.append(object.toString());
			}
			return b.toString();
		}

		public static String append3(String[] strings) {
			StringBuilder b = new StringBuilder();
			for (String string: strings) {
				b.append(string);
			}
			return b.toString();
		}

		public static String append4(String s, String... strings) {
			StringBuilder b = new StringBuilder();
			b.append(s).append("::");
			for (String string: strings) {
				b.append(string);
			}
			return b.toString();
		}

		public static String appendChar(char... values) {
			StringBuilder b = new StringBuilder();
			for (char ch: values) {
				b.append(ch);
			}
			return b.toString();
		}

		public static int sum(int... ints) {
			int total = 0;
			for (int i: ints) {
				total+=i;
			}
			return total;
		}

		public static int sumDouble(double... values) {
			int total = 0;
			for (double i: values) {
				total+=i;
			}
			return total;
		}

		public static int sumFloat(float... values) {
			int total = 0;
			for (float i: values) {
				total+=i;
			}
			return total;
		}
	}

	public static class DelegatingStringFormat {

		public static String format(String s, Object... args) {
			return String.format(s, args);
		}
	}

	public static class StaticsHelper {
		static StaticsHelper sh = new StaticsHelper();
		public static StaticsHelper methoda() {
			return sh;
		}
		public static String methodb() {
			return "mb";
		}
		
		public static StaticsHelper getPropertya() {
			return sh;
		}

		public static String getPropertyb() {
			return "pb";
		}
		

		public static StaticsHelper fielda = sh;
		public static String fieldb = "fb";
		
		public String toString() {
			return "sh";
		}
	}
}
