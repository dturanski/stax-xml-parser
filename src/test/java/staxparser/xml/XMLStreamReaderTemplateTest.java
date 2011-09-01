package staxparser.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.NoSuchElementException;
import java.util.Properties;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


import staxparser.util.ResourceUtils;
import staxparser.xml.XMLStreamReaderCallback;
import staxparser.xml.XMLStreamReaderTemplate;

public class XMLStreamReaderTemplateTest {

    private String xml;
    private XMLStreamReaderTemplate simpleTemplate;

    @Before
    public void setUp() throws IOException {
        XMLUnit.setIgnoreWhitespace(true);
        xml = ResourceUtils.classPathResourceAsString(getClass(), "/distribute-order-request.xml");
        simpleTemplate = new XMLStreamReaderTemplate(xml);
    }

    private void setUpWithResource(String path) throws IOException {
        xml = ResourceUtils.classPathResourceAsString(getClass(), path);
        simpleTemplate = new XMLStreamReaderTemplate(xml);
    }

    @Test
    public void testGetRoot() throws IOException, XMLStreamException, SAXException {

        assertTrue(simpleTemplate.nextElement("DistributeOrderRequest"));
        Writer writer = new StringWriter();
        simpleTemplate.writeElement(writer);

        XMLAssert.assertXMLEqual(xml, writer.toString());
    }

    @Test
    public void testGetElementAsString() throws SAXException, IOException, XMLStreamException {

        XMLAssert.assertXMLEqual(
                "<ord:quantity xmlns:ord=\"http://acme.com/nouns/order\">438</ord:quantity>",
                simpleTemplate.getNextElement(String.class, "quantity"));
    }

    public void testGetElementAsNode() throws SAXException, IOException, XMLStreamException {
        Element element = simpleTemplate.getNextElement(Element.class, "quantity");
        assertEquals("ord:quantity", element.getNodeName());
        assertEquals("quantity", element.getLocalName());
        assertEquals("http://acme.com/nouns/order", element.getNamespaceURI());
        assertEquals("438", element.getNodeValue());
    }

    @Test
    public void testGetNextElementAsString2() throws XMLStreamException, SAXException, IOException {
        String appInfo = simpleTemplate.getNextElement(String.class, "AppInfo");

        XMLAssert
                .assertXMLEqual(
                        "<app:AppInfo xmlns:app=\"http://acme.com/common/appinfo\">"
                                + "<app:applicationId>applicationId</app:applicationId><app:globalTransactionId>4ca2682b-879a-4de2-be4b-f67f9ef9cc7b</app:globalTransactionId>"
                                + "<app:requestTime>2011-01-14T15:42:07.875-05:00</app:requestTime><app:endUserId>endUserId</app:endUserId></app:AppInfo>",
                        appInfo);
    }

    @Test
    public void testElementDoesNotExist() throws XMLStreamException {
        String val = simpleTemplate.getNextElement(String.class, "BadElement");
        assertNull(val);
        try {
            simpleTemplate.getElement(String.class);
            fail("should throw exception");
        } catch (NoSuchElementException e) {
            assertEquals("END_DOCUMENT reached: no more elements on the stream.", e.getMessage());
        }
    }

    @Test
    public void testGetMultiple() throws XMLStreamException, SAXException, IOException {

        // No writer - position cursor only
        simpleTemplate.nextElement("item");
        simpleTemplate.nextElement("item");

        Writer writer = new StringWriter();
        for (int i = 0; i < 3; i++) {
            simpleTemplate.nextElement("item");
            simpleTemplate.writeElement(writer);
        }

        XMLAssert
                .assertXMLEqual(
                        "<foo>"
                                + "<ord:item xmlns:ord=\"http://acme.com/nouns/order\"><ord:quantity>149</ord:quantity><ord:sku>005012</ord:sku><ord:description>description for 005012</ord:description></ord:item>"
                                + "<ord:item xmlns:ord=\"http://acme.com/nouns/order\"><ord:quantity>327</ord:quantity><ord:sku>091146</ord:sku><ord:description>description for 091146</ord:description></ord:item>"
                                + "<ord:item xmlns:ord=\"http://acme.com/nouns/order\"><ord:quantity>40</ord:quantity><ord:sku>048562</ord:sku><ord:description>description for 048562</ord:description></ord:item>"
                                + "</foo>", "<foo>" + writer.toString() + "</foo>");
    }

    @Test
    public void testGetElementText() throws XMLStreamException {

        assertEquals("438", simpleTemplate.getNextElementText("quantity"));
        assertEquals("048793", simpleTemplate.getNextElementText("sku"));
        assertEquals("description for 048793", simpleTemplate.getNextElementText("description"));
    }

    @Test
    public void testGetRootElementTypeCB() throws XMLStreamException {

        XMLStreamReaderCallback callback = new XMLStreamReaderCallback() {
            @Override
            public Object execute(XMLStreamReader xmlStreamReader) throws XMLStreamException {
                xmlStreamReader.next();
                return xmlStreamReader.getName().toString();
            }
        };
        assertEquals("{http://acme.com/messages/request/distributeorder}DistributeOrderRequest",
                simpleTemplate.executeCallBack(callback));
    }

    @Test
    public void testGetRootElementType() throws XMLStreamException {
        assertEquals("{http://acme.com/messages/request/distributeorder}DistributeOrderRequest",
                simpleTemplate.getNextElementTypeAsString());
    }

    @Test
    public void testDefaultNS() throws IOException, XMLStreamException {
        this.setUpWithResource("/distribute-order-request-default-ns.xml");
        Writer writer = new StringWriter();
        simpleTemplate.nextElement("Order");

        simpleTemplate.writeElement(writer);
        String order = writer.toString();

        assertTrue("namespace not included",
                order.startsWith("<Order xmlns=\"http://acme.com/nouns/order\">"));
    }

    @Test
    public void testDefaultNS2() throws IOException, XMLStreamException {
        this.setUpWithResource("/another-test.xml");
        Writer writer = new StringWriter();
        simpleTemplate.nextElement("PayloadContext");

        simpleTemplate.writeElement(writer);
        String el = writer.toString();

        assertEquals(
                "<PayloadContext xmlns:ns2=\"http://acme.com/it/enterprise/data/v1\" xmlns=\"http://acme.com/it/enterprise/msg/v1\" xmlns:ns3=\"http://acme.com/it/enterprise/contract/createEvent/v1\"><MessageProfile></MessageProfile><ApplicationProfile></ApplicationProfile><TransactionProfile></TransactionProfile><UserArea></UserArea></PayloadContext>",
                el);

    }
    
    @Test public void testCDataParsing() throws IOException, XMLStreamException {
            this.setUpWithResource("/cdata-example.xml");
            final String[] expected = new String[]{
                    ResourceUtils.classPathResourceAsString(getClass(), "/expected-cdata-1.xml"),
                    ResourceUtils.classPathResourceAsString(getClass(), "/expected-cdata-2.xml")
            };
            
            Properties factoryProperties = new Properties();
            factoryProperties.put("http://java.sun.com/xml/stream/properties/report-cdata-event",Boolean.TRUE);
            XMLStreamReaderTemplate template = new XMLStreamReaderTemplate(xml, factoryProperties);
            template.executeCallBack(new XMLStreamReaderCallback() {
                
              
            @Override
       
            public Object execute(XMLStreamReader xmlStreamReader) throws XMLStreamException {
                int event;
                int count = 0;
                while ( (event = xmlStreamReader.next()) != XMLEvent.END_DOCUMENT) {
                    
                 
                    if (event == XMLEvent.CDATA){
                        try {
                           
                           XMLAssert.assertXMLEqual(expected[count++], xmlStreamReader.getText().trim()); 
                          
                        } catch (Exception e) {
                            System.out.println("expect: {"+expected+"}");
                            System.out.println("actual: {"+xmlStreamReader.getText().trim()+"}");
                            fail(e.getMessage());
                        }
                       
                    }
                }
                assertEquals(2,count);
                return null;
            }
            
        });
    }

}
