package staxparser.util;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Common XML Utilities
 * 
 * @author David Turanski
 *
 */
public class XMLUtils {
    private XMLUtils(){}
    
    private final static DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    
    {
    	documentBuilderFactory.setNamespaceAware(true);
    }
    
    /**
     * Convert and xml String to an org.w3c.dom.Element
     * @param xml - the XML
     * @return - the Element
     */
    public static Element stringToElement(String xml){
    	Element element = null;
    	try {
    		Document document = getDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    		element = (Element)document.getDocumentElement().cloneNode(true);
		} catch ( Exception e) {
			 throw new RuntimeException(e);
		}
		
		return element; 	
    }
   
   /**
    * Transform a org.w3c.dom.Node to a String
    * @param node - the Node
    * @param omitXmlDeclaration - omit XML declaration
    * @param prettyPrint - apply indentation
    * @return the String result
    * @throws TransformerException
    */
   public static String elementToString(Node node, boolean omitXmlDeclaration, boolean prettyPrint) {      
     String result = null;
     try {
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer transformer = transFactory.newTransformer();
        StringWriter buffer = new StringWriter();
        if (omitXmlDeclaration){
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        } 
        if (prettyPrint) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        } 
        transformer.transform(new DOMSource(node),
              new StreamResult(buffer));
        result = buffer.toString();
     } catch (TransformerException e ) {
         throw new RuntimeException(e);
     }
     return result;
    }
   
	private static synchronized DocumentBuilder getDocumentBuilder() {
		try {
			return documentBuilderFactory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) {
			throw new RuntimeException("failed to create a new DocumentBuilder", e);
		}
	}
}
