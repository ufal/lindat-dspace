package cz.cuni.mff.ufal.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.dspace.utils.DSpace;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class PageStructureUtil {
    public static Node interpolateVariables(Node node){
        if(node != null) {
            if (node.getNodeType() == Node.TEXT_NODE) {
                Map<String, String> valuesMap = new DSpace().getConfigurationService().getAllProperties();
                StrSubstitutor sub = new StrSubstitutor(valuesMap);
                String templateString = node.getTextContent();
                String interpolatedString = sub.replace(templateString);
                node.setTextContent(interpolatedString);
            } else if (node.hasChildNodes()) {
                NodeList children = node.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    interpolateVariables(child);
                }
            }
        }
        return node;
    }

    public static Node documentReadAndInterpolate(String documentPath) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new File(documentPath));
        return interpolateVariables(doc.getDocumentElement());
    }
}
