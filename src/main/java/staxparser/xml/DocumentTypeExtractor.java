package staxparser.xml;

import javax.xml.stream.XMLStreamException;
/**
 * Extract document type (Root element QName) from an XML document
 * @author David Turanski
 *
 */
public class DocumentTypeExtractor {
    /**
     * 
     * @param xml - XML document as String
     * @return - the fully qualified type of the root element as a QName String
     * @throws XMLStreamException
     */
    public String extractDocumentType(String xml) throws XMLStreamException {
        XMLStreamReaderTemplate template = null;
       try {
        template = new XMLStreamReaderTemplate(xml); 
        template.nextElement();
       } finally {
           if (template != null) template.close();
       }
       return template.getQNameAsString();
    }
}
