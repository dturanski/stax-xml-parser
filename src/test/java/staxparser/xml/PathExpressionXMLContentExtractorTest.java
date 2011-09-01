package staxparser.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;


import staxparser.util.ResourceUtils;
import staxparser.xml.PathExpression;
import staxparser.xml.PathExpressionXMLContentExtractor;
 

public class PathExpressionXMLContentExtractorTest {
 
	private String xml;
	private PathExpressionXMLContentExtractor pathExpressionExtractor;
    @Before
    public void setUp() throws IOException {
    	XMLUnit.setIgnoreWhitespace(true);
    	xml = ResourceUtils.streamToString(getClass().getResourceAsStream("/distribute-order-request.xml"));
    
    }
	 
 	 
	@Test
	public void testevaluatePathExpressions() throws XMLStreamException {
		Set<PathExpression> expressions = new HashSet<PathExpression>();
		Map<PathExpression, String> results;
		 
		expressions.clear();
		PathExpression px;
		px = new PathExpression("//{http://acme.com/common/appinfo}AppInfo/{http://acme.com/common/appinfo}applicationId");
		expressions.add(px);
		pathExpressionExtractor = new PathExpressionXMLContentExtractor(expressions);
		results = (Map<PathExpression, String>)pathExpressionExtractor.extractElementText(xml);
		for (Entry<PathExpression, String> result: results.entrySet()) {
			System.out.println("["+ result.getKey().toString() + "] = [" + result.getValue() + "]");
		}
		assertEquals(1,results.size());
		assertEquals("applicationId",results.get(px));
		 
		expressions.clear();
		px = new PathExpression("//DistributeOrderRequest/Order/id");
		expressions.add(px);
		pathExpressionExtractor = new PathExpressionXMLContentExtractor(expressions);
		results = (Map<PathExpression, String>)pathExpressionExtractor.extractElementText(xml);
		for (Entry<PathExpression, String> result: results.entrySet()) {
			System.out.println("["+ result.getKey().toString() + "] = [" + result.getValue() + "]");
		}
		assertEquals(1,results.size());	
		assertNotNull(results.entrySet().iterator().next().getValue());
	}
	
	@Test
	public void testevaluateRootPathExpression() throws XMLStreamException {
		Set<PathExpression> expressions = new HashSet<PathExpression>();
		Map<PathExpression, String> results;	 
		
		expressions.clear();
		PathExpression px = new PathExpression("/DistributeOrderRequest/Order/id");
		expressions.add(px);
		pathExpressionExtractor = new PathExpressionXMLContentExtractor(expressions);
		results = (Map<PathExpression, String>)pathExpressionExtractor.extractElementText(xml);
		for (Entry<PathExpression, String> result: results.entrySet()) {
			System.out.println("["+ result.getKey().toString() + "] = [" + result.getValue() + "]");
		}
		assertEquals(1,results.size());	
		assertNotNull(results.entrySet().iterator().next().getValue());
	}
	
	@Test
	public void testMultiplePathExpressions() throws XMLStreamException {
		Set<PathExpression> expressions = new HashSet<PathExpression>();
		Map<PathExpression, String> results;
		
		PathExpression px0 = new PathExpression("//Order/customerId");
		expressions.add(px0);
		PathExpression px1 = new PathExpression("//AppInfo/globalTransactionId");
		expressions.add(px1);
		PathExpression px2 = new PathExpression("//AppInfo/applicationId");
		expressions.add(px2);
		
		pathExpressionExtractor = new PathExpressionXMLContentExtractor(expressions);
		results = (Map<PathExpression, String>)pathExpressionExtractor.extractElementText(xml);
		for (Entry<PathExpression, String> result: results.entrySet()) {
			System.out.println("["+ result.getKey().toString() + "] = [" + result.getValue() + "]");
		}
		assertEquals(3,results.size());
		assertEquals("customerFor-5",results.get(px0));
		assertEquals("4ca2682b-879a-4de2-be4b-f67f9ef9cc7b",results.get(px1));
		assertEquals("applicationId",results.get(px2));	 
	}
	
	@Test
	public void testMultiplePathExpressionsAsElement() throws XMLStreamException {
		Set<PathExpression> expressions = new HashSet<PathExpression>();
		Map<PathExpression,Element> results;
		
		PathExpression px0 = new PathExpression("//Order/customerId");
		expressions.add(px0);
		PathExpression px1 = new PathExpression("//AppInfo/globalTransactionId");
		expressions.add(px1);
		PathExpression px2 = new PathExpression("//AppInfo/applicationId");
		expressions.add(px2);
		pathExpressionExtractor = new PathExpressionXMLContentExtractor(expressions);
		
		results = (Map<PathExpression,Element>)pathExpressionExtractor.extractElements(xml, Element.class);
		for (Entry<PathExpression,Element> result: results.entrySet()) {
			System.out.println("["+ result.getKey().toString() + "] = [" + result.getValue() + "]");
		}
		assertEquals(3,results.size());
		assertEquals("customerFor-5",results.get(px0).getFirstChild().getNodeValue());
		assertEquals("4ca2682b-879a-4de2-be4b-f67f9ef9cc7b",results.get(px1).getFirstChild().getNodeValue());
		assertEquals("applicationId",results.get(px2).getFirstChild().getNodeValue());	 
	}
	 
	@Test
	public void testMultiplePathExpressionsAsElementString() throws XMLStreamException {
		Set<PathExpression> expressions = new HashSet<PathExpression>();
		Map<PathExpression,String> results;
		
		PathExpression px0 = new PathExpression("//Order/customerId");
		expressions.add(px0);
		PathExpression px1 = new PathExpression("//AppInfo/globalTransactionId");
		expressions.add(px1);
		PathExpression px2 = new PathExpression("//AppInfo/applicationId");
		expressions.add(px2);
	 
		pathExpressionExtractor = new PathExpressionXMLContentExtractor(expressions);
		results = (Map<PathExpression,String>)pathExpressionExtractor.extractElements(xml,String.class);
		for (Entry<PathExpression,String> result: results.entrySet()) {
			System.out.println("["+ result.getKey().toString() + "] = [" + result.getValue() + "]");
		}
		assertEquals(3,results.size());
		assertEquals("<ord:customerId xmlns:ord=\"http://acme.com/nouns/order\">customerFor-5</ord:customerId>",results.get(px0) );
		assertEquals("<app:globalTransactionId xmlns:app=\"http://acme.com/common/appinfo\">4ca2682b-879a-4de2-be4b-f67f9ef9cc7b</app:globalTransactionId>",
				results.get(px1));
		assertEquals("<app:applicationId xmlns:app=\"http://acme.com/common/appinfo\">applicationId</app:applicationId>",
				results.get(px2));	 
	}
	
}
