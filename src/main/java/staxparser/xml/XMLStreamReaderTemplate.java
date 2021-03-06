package staxparser.xml;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import staxparser.util.XMLUtils;


/**
 * A wrapper class providing convenience functions over {@link javax.xml.stream.XMLStreamReader}. This class is not 
 * thread safe (e.g., a new instance is required for each thread).
 * @author David Turanski
 *
 */
public class XMLStreamReaderTemplate {

	protected final Logger logger = Logger.getLogger(this.getClass());
	private final XMLInputFactory factory;
	private final Set<String> declaredNamespaces;
	protected XMLStreamReader xmlStreamReader;
	private static Set<Class<?>> SUPPORTED_TYPES = new HashSet<Class<?>>(Arrays.asList(
		new Class<?>[] { String.class, Node.class,Element.class }));

	/**
	 * 
	 * @param reader A Reader for the XML input stream
	 */
	public XMLStreamReaderTemplate(Reader reader) {
		 this(reader,null);
	}
	
	/**
     * 
     * @param reader A Reader for the XML input stream
     * @param factoryProperties XMLInputFactory properties
     */
    public XMLStreamReaderTemplate(Reader reader,Properties factoryProperties) {
        factory = XMLInputFactory.newInstance();
        if (factoryProperties != null){
            for ( Entry<Object, Object> property: factoryProperties.entrySet()){
            factory.setProperty(property.getKey().toString(), property.getValue());
            }
        }
        declaredNamespaces = new HashSet<String>();
        try {
            initializeStreamReader(reader);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

	/**
	 * Alternate constructor that creates a StringReader for the given input
	 * @param xml The XML input string
	 */
	public XMLStreamReaderTemplate(String xml) {
		this(new StringReader(xml),null);
	}
	
	/**
     * Alternate constructor that creates a StringReader for the given input
     * @param xml The XML input string
     * @param factoryProperties XMLInputFactory properties
     */
    public XMLStreamReaderTemplate(String xml, Properties factoryProperties) {
        this(new StringReader(xml),factoryProperties);
    }

	/**
	 * Returns text at the current element
	 * @return the text value
	 * @throws XMLStreamException
	 */
	public final String getElementText() throws XMLStreamException {
		return xmlStreamReader.getElementText();
	}

	/**
	 * Returns the local name of the current element
	 * @return local name
	 */
	public final String getLocalName() {
		return xmlStreamReader.getLocalName();
	}

	/**
	 * Returns the QName of the current element
	 * @return the {@link javax.xml.namespace.QName} 
	 */
	public final QName getName() {
		return xmlStreamReader.getName();
	}

	/**
	 * Returns the String representation of the QName of the current element
	 * @return QName as string
	 */
	public final String getQNameAsString() {
		return xmlStreamReader.getName().toString();
	}

	/**
	 * Execute the given callback implementation
	 * @param callback an implementation of {@link XMLStreamReaderCallback}. Usually an anonymous class
	 * @return the result of the callback execution
	 * @throws XMLStreamException
	 */
	public final Object executeCallBack(XMLStreamReaderCallback callback) throws XMLStreamException {
		return callback.execute(xmlStreamReader);
	}

	/**
	 * Get the current element 
	 * @param requiredType The result type one of: <code>String.class, Element.class, Node.class</code>
	 * @return the element
	 * @throws XMLStreamException
	 */
	@SuppressWarnings("unchecked")
	public final <T> T getElement(Class<T> requiredType) throws XMLStreamException {
		validateRequiredTypeIsSupported(requiredType);
		StringWriter writer = new StringWriter();
		writeElement(writer);

		String xml = writer.toString();

		if (requiredType.equals(String.class))
			return (T) xml;
		else
			return (T) XMLUtils.stringToElement(writer.toString());
	}

	/** 
	 * Position the cursor at the next element and return its {@link javax.xml.namespace.QName}
	 * @return the QName
	 * @throws XMLStreamException
	 */
	public final QName getNextElementType() throws XMLStreamException {
		nextElement();
		return getName();
	}

	/**
	 * Position the cursor at the next element and return the string representation of its
	 * {@link javax.xml.namespace.QName}
	 * @return the result
	 * @throws XMLStreamException
	 */
	public final String getNextElementTypeAsString() throws XMLStreamException {
		nextElement();
		return getQNameAsString();
	}

	/**
	 * Write the current element contents to a Writer
	 * @param writer The Writer used to access the output stream
	 * @throws XMLStreamException
	 */
	public final void writeElement(Writer writer) throws XMLStreamException {
		XMLOutputFactory oFactory = XMLOutputFactory.newInstance();
		XMLStreamWriter staxWriter = oFactory.createXMLStreamWriter(writer);
		declaredNamespaces.clear();
		String elementName = null;

		try {
			elementName = xmlStreamReader.getLocalName();
		} catch (Exception e) {
			String msg;
			if (xmlStreamReader.getEventType() == XMLEvent.END_DOCUMENT) {
				msg = "END_DOCUMENT reached: no more elements on the stream.";
			} else {
				msg = e.getMessage();
			}

			throw new NoSuchElementException(msg);
		}

		staxWriter.writeStartElement(xmlStreamReader.getName().getPrefix(), xmlStreamReader.getName().getLocalPart(),
				xmlStreamReader.getName().getNamespaceURI());
		String prefix = xmlStreamReader.getName().getPrefix();

		writeNamespaces(staxWriter);
		int event;
		int attrIndex = 0;
		boolean writing = true;
		while (writing && (event = xmlStreamReader.next()) != XMLEvent.END_DOCUMENT) {
			switch (event) {
			case XMLEvent.START_ELEMENT:
				attrIndex = 0;
				staxWriter.writeStartElement(xmlStreamReader.getName().getPrefix(), xmlStreamReader.getName()
						.getLocalPart(), xmlStreamReader.getName().getNamespaceURI());
				if (!prefix.equals(xmlStreamReader.getName().getPrefix())) {
					prefix = xmlStreamReader.getName().getPrefix();
					writeNamespaces(staxWriter);
				}
				break;
			case XMLEvent.END_ELEMENT:
				staxWriter.writeEndElement();
				writing = !xmlStreamReader.getLocalName().equals(elementName);
				break;
			case XMLEvent.ATTRIBUTE:
				staxWriter.writeAttribute(xmlStreamReader.getAttributeLocalName(attrIndex),
						xmlStreamReader.getAttributeValue(attrIndex));
				attrIndex++;
				break;
			case XMLEvent.CDATA:
				staxWriter.writeCData(xmlStreamReader.getText());
				break;
			case XMLEvent.CHARACTERS:
				if (!xmlStreamReader.isWhiteSpace()) {
					staxWriter.writeCharacters(xmlStreamReader.getText().trim());
				}
				break;
			}

		}
		staxWriter.close();
	}

	/**
	 * Position the cursor to the next element matching the given {@link javax.xml.namespace.QName} and get its text.
	 * A <code>null</code> argument matches the next element. A <code>null</code> value for <code>qname.getNamespaceURI()</code> matches on 
	 * <code>qname.getLocalPart()</code> for any namespace.
	 * @param qname The QName 
	 * @return The text
	 * @throws XMLStreamException 
	 */
	public String getNextElementText(QName qname) throws XMLStreamException {
		return (qname == null) ? getNextElementText(null, null) :
			getNextElementText(qname.getLocalPart(), qname.getNamespaceURI());
	}

	/**
	 * Position the cursor to the next element matching the given local name and get its text.
	 * A <code>null</code> argument matches the next element
	 * 
	 * @param localName The element local name
	 * @return The text
	 * @throws XMLStreamException 
	 */
	public String getNextElementText(String localName) throws XMLStreamException {
		return getNextElementText(localName, null);
	}

	/**
	 * Position the cursor to the next element matching the given local name and namespace URI and get its text.
	 * Both arguments <code>null</code> matches the next element. A <code>null</code> namespaceURI matches the local name for any namespace
	 * @param localName The element local name
	 * @param namespaceURI The element namespace URI
	 * @return The text
	 * @throws XMLStreamException 
	 */
	public String getNextElementText(String localName, String namespaceURI) throws XMLStreamException {
		if (!nextElement(localName, namespaceURI)) {
			return null;
		}
		return getElementText();
	}

	/**
	 * Position the cursor at the next element and return its text 
	 * @return The text
	 * @throws XMLStreamException
	 */
	public String getNextElementText() throws XMLStreamException {
		return getNextElementText(null, null);
	}

	/**
	 * Position the cursor at the next element
	 * @return <code>true</code> if an element was found
	 * @throws XMLStreamException
	 */
	public boolean nextElement() throws XMLStreamException {
		return nextElement(null, null);
	}

	/**
	 * Position the cursor to the next element matching the given {@link javax.xml.namespace.QName}.
	 * @param qname The qname
	 * @return <code>true</code> if an element was found
	 * @throws XMLStreamException
	 */
	public boolean nextElement(QName qname) throws XMLStreamException {
		return (qname == null) ? nextElement(null, null) :
			nextElement(qname.getLocalPart(), qname.getNamespaceURI());
	}

	/**
	 * Position the cursor to the next element matching the given local name
	 * @param localName The localName
	 * @return <code>true</code> if an element was found
	 * @throws XMLStreamException
	 */
	public boolean nextElement(String localName) throws XMLStreamException {
		return nextElement(localName, null);
	}

	/**
	 * Position the cursor to the next element matching the given local name and namespace URI
	 * @param localName The local name, or <code>null</code> to match any element
	 * @param namespaceURI The namespace URI, or <code>null</code> to match any name space
	 * @return <code>true</code> if an element was found
	 * @throws XMLStreamException
	 */
	public boolean nextElement(String localName, String namespaceURI) throws XMLStreamException {
		return nextStartElement(localName, namespaceURI);
	}

	/**
	 * Return a count of elements matching the given {@link javax.xml.namespace.QName}.
	 * @param qname The qname,or <code>null</code> to match all elements
	 * @return The number of elements
	 * @throws XMLStreamException
	 */
	public int count(QName qname) throws XMLStreamException {
		return (qname == null) ? count(null, null) :
			count(qname.getLocalPart(), qname.getNamespaceURI());
	}

    /**
	 * Return a count of elements matching the given local name.
     * @param localName The local name
     * @return The number of elements
     * @throws XMLStreamException
     */
	public int count(String localName) throws XMLStreamException {
		return count(localName, null);
	}

	/**	 
	* Return a count of elements matching the given local name and namespace URI.
 	* @param localName The local name, or <code>null</code> to match any name
	* @param namespaceURI The namespace URI, or <code>null</code> to match any namespace
	* @return The number of elements
	* @throws XMLStreamException
	*/
	public int count(String localName, String namespaceURI) throws XMLStreamException {
		int count = 0;
		while (nextElement(localName, namespaceURI)) {
			count++;
		}
		return count;
	}

	/**
	 * Position the cursor the the next element matching the given {@link javax.xml.namespace.QName} and write the 
	 * element to the Writer's stream
	 * @param writer The Writer used to access the content
	 * @param qname The qname, or <code>null</code> to match the next element
	 * @return <code>true</code> if an element was found
	 * @throws XMLStreamException
	 */
	public boolean writeNextElement(Writer writer, QName qname) throws XMLStreamException {
		return (qname == null) ? writeNextElement(writer, null, null) :
			writeNextElement(writer, qname.getLocalPart(), qname.getNamespaceURI());
	}

	/**
	 * Position the cursor the the next element matching the input parameters and write the 
	 * element to the Writer's stream
	 * @param writer The Writer used to access the content
	 * @param localName The local name, or <code>null</code> to match the next element
	 * @return <code>true</code> if an element was found
	 * @throws XMLStreamException
	 */
	public boolean writeNextElement(Writer writer, String localName) throws XMLStreamException {
		return writeNextElement(writer, localName, null);
	}

	/**
	 * Position the cursor the the next element matching the input parameters and write the 
	 * element to the Writer's stream
	 * @param writer The Writer used to access the content
	 * @param localName The local name, or <code>null</code> to match any element
	 * @param namespaceUri The namespaceURI, or <code>null</code> to match any namespace
	 * @return <code>true</code> if an element was found
	 * @throws XMLStreamException
	 */
	public boolean writeNextElement(Writer writer, String localName, String namespaceURI) throws XMLStreamException {
		Boolean found = nextElement(localName, namespaceURI);
		if (found) {
			writeElement(writer);
		}
		return found;
	}

	/**
	 * Return the next element matching the given {@link javax.xml.namespace.QName} 
	 * @param requiredType The required return type, one of: <code>String.class, Element.class, Node.class</code>
	 * @param qname The qname, or <code>null</code> to match the next element.
	 * @return The element
	 * @throws XMLStreamException
	 */

	public <T> T getNextElement(Class<T> requiredType, QName qname) throws XMLStreamException {
		return (qname == null) ? getNextElement(requiredType, null, null) :
			getNextElement(requiredType, qname.getLocalPart(), qname.getNamespaceURI());
	}
	
	/**
	 * Return the next element matching the given local name
	 * @param requiredType The required return type, one of: <code>String.class, Element.class, Node.class</code>
	 * @param localName The local name, or <code>null</code> to match the next element
	 * @return The element
	 * @throws XMLStreamException
	 */

	public <T> T getNextElement(Class<T> requiredType, String localName) throws XMLStreamException {
		return getNextElement(requiredType, localName, null);
	}

	/**
	 * Return the next element matching the given local name and namespace URI
	 * @param requiredType The required return type, one of: <code>String.class, Element.class, Node.class</code>
	 * @param localName The local name, or <code>null</code> to match the any name
	 * @param namespaceURI the namespace URI, or <code>null</code> to match any namespace
	 * @return The element
	 * @throws XMLStreamException
	 */
	public <T> T getNextElement(Class<T> requiredType, String localName, String namespaceURI) throws XMLStreamException {
		Boolean found = nextElement(localName, namespaceURI);
		if (found) {
			return getElement(requiredType);
		} else {
			return null;
		}

	}

    /**
     * Position cursor to the start of the next element matching the input parameters
     * @param localName The local name, or <code>null</code> to match any name
     * @param namespaceURI The namespace URI, <code>null</code> to match any namespace 
     * @return <code>true</code> if the element was found
     * @throws XMLStreamException
     */
	protected final boolean nextStartElement(String localName, String namespaceURI) throws XMLStreamException {

		int event;
		boolean elementFound = false;
		while (xmlStreamReader.hasNext() && !elementFound && (event = xmlStreamReader.next()) != XMLEvent.END_DOCUMENT) {
			if (event == XMLEvent.START_ELEMENT) {
				if (localName != null) {
					if (namespaceURI == null) {
						elementFound = xmlStreamReader.getLocalName().equals(localName);
					} else {
						elementFound = getName().equals(new QName(namespaceURI, localName));
					}
				} else {
					elementFound = true;
				}
			}
		}

		return elementFound;
	}

	/**
	 * Position to the next event
	 * @return XMLEvent
	 * @throws XMLStreamException
	 */
	protected final int next() throws XMLStreamException {
		return xmlStreamReader.next();
	}

	/**
	 * Close the reader
	 * @throws XMLStreamException
	 */
	protected final void close() throws XMLStreamException {
		xmlStreamReader.close();
	}

	/**
	 * Validate the required type is one of the supported types, otherwise throw a RuntimeException
	 * @param requiredType
	 */
	protected static final void validateRequiredTypeIsSupported(Class<?> requiredType) {
		if (!SUPPORTED_TYPES.contains(requiredType)) {
			throw new IllegalArgumentException("required type must be one of " + SUPPORTED_TYPES.toArray());
		}
	}

	/*
	 * Save the namespaces defined in the document
	 */
	private void writeNamespaces(XMLStreamWriter staxWriter) throws XMLStreamException {

		if (xmlStreamReader.getNamespaceCount() == 0) {
			if (!isNamespaceDeclared(xmlStreamReader.getName())) {
				staxWriter.writeNamespace(xmlStreamReader.getName().getPrefix(), xmlStreamReader.getName()
						.getNamespaceURI());
				declareNamespace(xmlStreamReader.getName());
			}
		} else {
			for (int i = 0; i < xmlStreamReader.getNamespaceCount(); i++) { 
				QName qname = new QName(xmlStreamReader.getNamespaceURI(i), "", 
				        (xmlStreamReader.getNamespacePrefix(i)==null?"":xmlStreamReader.getNamespacePrefix(i)));
				if (!isNamespaceDeclared(qname)) {
					staxWriter.writeNamespace(qname.getPrefix(), qname.getNamespaceURI());
					declareNamespace(qname);
				}
			}
		}

	}

	/*
	 * See if a namespace is already declared for a prefix
	 */
	private boolean isNamespaceDeclared(QName qname) {
		String ns = qname.getPrefix() + ":" + qname.getNamespaceURI();
		return declaredNamespaces.contains(ns);
	}

	/* 
	 * Save a namespace declaration
	 */
	private void declareNamespace(QName qname) {
		String ns = qname.getPrefix() + ":" + qname.getNamespaceURI();
		declaredNamespaces.add(ns);
	}

	/*
	 * Create a new XMLStreamReader
	 */
	private void initializeStreamReader(Reader reader) throws XMLStreamException {
		xmlStreamReader = factory.createXMLStreamReader(reader);		 
	}
	
	public boolean hasNext() {
		try {
			return xmlStreamReader.hasNext();
		}
		catch (XMLStreamException e) {
			 throw new RuntimeException(e);
		}
	}

}
