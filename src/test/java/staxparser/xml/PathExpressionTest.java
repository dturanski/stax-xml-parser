package staxparser.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.xml.namespace.QName;

import org.junit.Test;

import staxparser.xml.PathExpression;

public class PathExpressionTest {

	@Test 
	public void testConstructor(){
		PathExpression expression = new PathExpression("Order");
		assertEquals("//Order",expression.toString());
		
		expression = new PathExpression("//Order");
		assertEquals("//Order",expression.toString());
		
		expression = new PathExpression("/Order/foo/bar");
		assertEquals("/Order/foo/bar",expression.toString());
		
		try {
			expression = new PathExpression("Order-id");
			fail("should throw exception");
		} catch (Exception e){

		}
		try {
			expression = new PathExpression("Order.id");
			fail("should throw exception");
		} catch (Exception e){

		}
		expression = new PathExpression("Order_id");
		assertEquals("//Order_id",expression.toString());
		
		expression = new PathExpression("{http://www.example.com/order}Order_id");
		assertEquals("//{http://www.example.com/order}Order_id",expression.toString());
	}
	
	@Test
	public void testMatch() {
		assertMatch("/foo","/foo");
		assertMatch("/foo/bar","/foo/bar");
		assertMatch("//foo","/foo");
		assertMatch("//foo","//foo");
		assertMatch("//foo","/bar/baz/go/foo");
		assertMatch("//go/foo","/bar/baz/go/foo");

		/**
		 * No namespace matches any namespace
		 */

		assertMatch("//{ns1:foo/bar}go/foo","/bar/baz/go/foo");
		assertMatch("//{ns1:foo/bar}go/foo","/bar/baz/go/{ns2:foo/bar}foo");
		assertMatch("/{http://acme.com/messages/request/distributeorder}DistributeOrderRequest/{http://acme.com/common/appinfo}AppInfo/{http://acme.com/common/appinfo}applicationId",
		"//AppInfo/applicationId");
		
		
	}
	
	@Test
	public void testMatchRoot(){
		assertMatch("/{ns1:foo/bar}go/foo","/go/foo");
	}

	@Test
	public void testRelative() {
		PathExpression px = new PathExpression("/");
		assertFalse(px.isRelative());
		assertEquals(0,px.getQNames().size());
	}

	@Test
	public void testNotMatch() {
		assertNotMatch("/foo","/bar");
		//assertNotMatch("/foo","/bar/foo");
		assertNotMatch("/a/b","/a/c/b");
		assertNotMatch("//a/b","/c/b");
		assertNotMatch("//{ns1:foo/bar}go/foo","/bar/baz/{ns2:foo/bar}go/foo");
	}
	
	@Test 
	public void testNotMatch2() {
		assertNotMatch("/foo","/bar/foo");
	}

	@Test
	public void testSimpleExpressions(){
		PathExpression.validateSelector("/");
		PathExpression.validateSelector("//foo");
		PathExpression.validateSelector("//foo/bar");
		PathExpression.validateSelector("/foo");
		PathExpression.validateSelector("/foo/bar");
		PathExpression.validateSelector("/foo/bar/car");
		PathExpression.validateSelector("/foo");
		PathExpression.validateSelector("/foo/bar/car");
		PathExpression.validateSelector("/foo/bar/car@attr");
	}

	@Test
	public void testExpressionsWithNamespace(){
		PathExpression.validateSelector("//{namespace}foo");
		PathExpression.validateSelector("//{http://com.example/order}foo/bar");
		PathExpression.validateSelector("/{http://com.example/order}foo/bar");
		PathExpression.validateSelector("//{http://com.example/order}foo/{http://com.example/orderitem}bar");
	}

	@Test 
	public void testInvalidExpressions() {
		testInvalidExpression("foo/bar");
		testInvalidExpression("foo");
		testInvalidExpression("foo/bar/car/");
		testInvalidExpression("/foo/bar/car/");
		testInvalidExpression("foo/bar/car*");
		testInvalidExpression("foo/bar/car@");
		testInvalidExpression("/foo/bar/./car");
		testInvalidExpression("///");
	}

	@Test
	public void testInvalidExpressionsWithNamespace(){
		testInvalidExpression("//{namespace{}}foo");
		testInvalidExpression("{http://com.example/order}/foo");
	}


	@Test
	public void testPop() {
		PathExpression expr =new PathExpression("/").pop();
		assertNull(expr);

		expr =new PathExpression("//foo").pop();
		assertEquals("/",expr.toString());

		expr = new PathExpression("/foo").pop();
		assertEquals("/",expr.toString());

		expr = new PathExpression("/foo/bar").pop();
		assertEquals("/foo",expr.toString());

		expr = new PathExpression("//{http://com.example/order}foo/{http://com.example/orderitem}bar").pop();
		assertEquals("//{http://com.example/order}foo",expr.toString());
	}

	@Test
	public void testPush() {
		PathExpression expr;
		expr = new PathExpression("/").push(QName.valueOf("{http://com.example/order}foo"));
		assertEquals("/{http://com.example/order}foo", expr.toString());
		expr = new PathExpression("/{http://com.example/order}foo").push(QName.valueOf("bar"));
		assertEquals("/{http://com.example/order}foo/bar", expr.toString());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testShouldFailOnWildcard() {
		new PathExpression(
 				"//DistributeSalesOrderRequest/Document/SalesOrderHeader/SalesOrderCategory/Name/*body");
	}

	@Test 
	public void testQName(){
		QName qname;
		qname = new QName("http://com.example/order","foo");
		assertEquals("foo",qname.getLocalPart());
		qname = new QName(null,"foo");
		assertEquals("foo",qname.getLocalPart());

		qname = QName.valueOf("{http://com.example/order}foo");
		assertEquals("foo",qname.getLocalPart());
		assertEquals("http://com.example/order",qname.getNamespaceURI());

		List<QName> qnames;
		qnames = PathExpression.parse("//{http://com.example/order}foo/{http://com.example/orderitem}bar");
		assertEquals(2, qnames.size());
		assertEquals("foo",qnames.get(0).getLocalPart());
		assertEquals("http://com.example/order",qnames.get(0).getNamespaceURI());
		assertEquals("bar",qnames.get(1).getLocalPart());
		assertEquals("http://com.example/orderitem",qnames.get(1).getNamespaceURI());


		qnames = PathExpression.parse("/foo/bar");
		assertEquals(2, qnames.size());
		assertEquals("foo",qnames.get(0).getLocalPart());
		assertEquals("",qnames.get(0).getNamespaceURI());
		assertEquals("bar",qnames.get(1).getLocalPart());
		assertEquals("",qnames.get(1).getNamespaceURI());

	}
	
	@Test
	public void testEquals(){
		PathExpression p1 = new PathExpression("/foo");
		PathExpression p2 = new PathExpression("/foo");
		assertTrue(p2.equals(p1));
		assertTrue(p1.equals(p2));
		
		p1 = new PathExpression("/order/orderItem/sku");
		p2 = new PathExpression("/order/orderItem/sku");
		assertTrue(p2.equals(p1));
		assertTrue(p1.equals(p2));
	}

	@Test 
	public void testHashCode(){
		PathExpression p1 = new PathExpression("/foo");
		PathExpression p2 = new PathExpression("/foo");
		assertTrue(p2.hashCode() == p1.hashCode());
	}
	
	private void testInvalidExpression(String expression){
		try {
			PathExpression.validateSelector("foo/bar/car/");
			fail("should throw exception");
		} catch (IllegalArgumentException e) {

		}
	}	


	private void assertMatch(String s1, String s2){
		PathExpression px1 = new PathExpression(s1);
		PathExpression px2 = new PathExpression(s2);
		assertTrue("should match ["+px1+"]["+px2+"]",px1.matches(px2));
		assertTrue("reflexive should match", px2.matches(px1));
	}

	private void assertNotMatch(String s1, String s2){
		PathExpression px1 = new PathExpression(s1);
		PathExpression px2 = new PathExpression(s2);
		assertFalse("should not match", px1.matches(px2));
		assertFalse("reflexive should not match", px2.matches(px1));
	}
	
	
	
}
