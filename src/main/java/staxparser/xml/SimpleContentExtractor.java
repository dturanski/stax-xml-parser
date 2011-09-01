package staxparser.xml;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
/**
 * A wrapper for {@link PathExpressionXMLContentExtractor} providing convenience methods 
 * to extract single values from XML input
 * 
 * @see PathExpressionXMLContentExtractor
 * 
 * @author David Turanski
 *
 */
public class SimpleContentExtractor {
    private final PathExpressionXMLContentExtractor pathExpressionExtractor;
    private final PathExpression expression;
	
    /**
     * @param selector A String used to create a {@link PathExpression}
     */
    public SimpleContentExtractor(String selector) {
		expression = new PathExpression(selector);
		pathExpressionExtractor = new PathExpressionXMLContentExtractor(
				Collections.singleton(expression));
	}
    
    /**
     * Extract the first occurrence of the element matching the selector and return its 
     * contents as a String
     * @param xml The XML input string
     * @return The target element as a String
     * 
     * @throws XMLStreamException
     */
    public String extractElement(String xml) throws XMLStreamException {
		return extractElement(new StringReader(xml), String.class);
	}
    
    /**
     * Extract the first occurrence of the element matching the selector and return its 
     * contents as a String 
     * @param reader A Reader for an XML input stream
     * @return The target element
     * @throws XMLStreamException
     */
    public String extractElement(Reader reader) throws XMLStreamException {
    	return extractElement(reader,String.class);
    }
    
	
    /**
     * Extract the first occurrence of the element matching the selector and return its 
     * contents as the required type
     * @param xml The XML input string
     * @param requiredType The return type: String.class, Node.class or Element.class 
     * @return The target element
     * @throws XMLStreamException
     */
	public <T> T extractElement(String xml, Class<T> requiredType) throws XMLStreamException {
		return extractElement(new StringReader(xml), requiredType);
	}
	
	/**
	 * Extract the first occurrence of the element matching the selector and return its 
     * contents as the required type
	 * @param reader A Reader for an XML input stream
	 * @param requiredType The return type: String.class, Node.class or Element.class
	 * @return The target element
	 * @throws XMLStreamException
	 */
	public <T> T extractElement(Reader reader, Class<T> requiredType ) throws XMLStreamException {
		Map<PathExpression,T> results = pathExpressionExtractor.extractElements(
				reader, requiredType);
		return results.get(expression);
	}
	
	/**
	 * Extract the text contents of the first occurrence of the element matching the 
	 * selector 
	 * 
	 * @param xml The XML input string
	 * @return The element text
	 * @throws XMLStreamException
	 */
	public String extractElementText(String xml) throws XMLStreamException {
		return extractElementText(new StringReader(xml));
	}
	
	/**
	 * Extract the text contents of the first occurrence of the element matching the 
	 * selector 
	 * 
	 * @param reader A Reader for an XML input stream
	 * @return The element text
	 * @throws XMLStreamException
	 */
	public String extractElementText(Reader reader) throws XMLStreamException {
		Map<PathExpression,String> results = pathExpressionExtractor.extractElementText(reader);
		return results.get(expression);
	}
}
