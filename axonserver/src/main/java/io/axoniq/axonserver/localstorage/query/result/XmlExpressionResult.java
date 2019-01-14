package io.axoniq.axonserver.localstorage.query.result;

import io.axoniq.axonserver.KeepNames;
import io.axoniq.axonserver.localstorage.query.ExpressionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.io.StringWriter;
import java.util.Collections;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * @author Marc Gathier
 */
@KeepNames
public class XmlExpressionResult implements AbstractMapExpressionResult  {
    private static final Logger logger = LoggerFactory.getLogger(XmlExpressionResult.class);
    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private final Element value;

    public XmlExpressionResult(Element value) {
        this.value = value;
    }

    @Override
    public Element getValue() {
        return value;
    }

    @Override
    public int compareTo( ExpressionResult o) {
        return value.toString().compareTo(o.toString());
    }

    @Override
    public String toString() {
        StringWriter writer = new StringWriter();
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(value), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException e) {
            logger.warn("Transformer failed to write to string", e);
        }
        return super.toString();
    }

    @Override
    public Element asXml() {
        return value;
    }

    @Override
    public Iterable<String> getColumnNames() {
        return Collections.singleton("xml");
    }
}
