package staxparser.xml;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Callback interface used with {@link XMLStreamReaderTemplate}
 * @author David Turanski
 *
 */
public interface XMLStreamReaderCallback {
    public Object execute(XMLStreamReader xmlStreamReader) throws XMLStreamException;
}
