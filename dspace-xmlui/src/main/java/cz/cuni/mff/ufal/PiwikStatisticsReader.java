package cz.cuni.mff.ufal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PiwikStatisticsReader extends AbstractReader {
	
	private static Logger log = Logger.getLogger(PiwikStatisticsReader.class);
	
    /** The Cocoon response */
    protected Response response;

    /** The Cocoon request */
    protected Request request;
    
    /** Item requesting statistics */
    private Item item = null;

    /** True if user agent making this request was identified as spider. */
    private boolean isSpider = false;
    
    private String rest = "";
    
    
    /** Piwik configurations */
    private static final String PIWIK_API_URL = ConfigurationManager.getProperty("lr", "lr.statistics.api.url");
    private static final String PIWIK_AUTH_TOKEN = ConfigurationManager.getProperty("lr", "lr.statistics.api.auth.token");
    private static final String PIWIK_SITE_ID = ConfigurationManager.getProperty("lr", "lr.statistics.api.site_id");
    private static final String PIWIK_DOWNLOAD_SITE_ID = ConfigurationManager.getProperty("lr", "lr.tracker.bitstream.site_id");
    
    /**
     * Set up the PiwikStatisticsReader
     *
     * See the class description for information on configuration options.
     */
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, par);
        
        try
        {
        	
            Context context = ContextUtil.obtainContext(objectModel);
        	
            this.request = ObjectModelHelper.getRequest(objectModel);
            this.response = ObjectModelHelper.getResponse(objectModel);
            
            rest = par.getParameter("rest", null);
            
            String handle = par.getParameter("handle", null);
            
            this.isSpider = par.getParameter("userAgent", "").equals("spider");
            
         	// Reference by an item's handle.
            DSpaceObject dso = dso = HandleManager.resolveToObject(context, handle);

            if (dso instanceof Item) {
                item = (Item)dso;                
            } else {
            	throw new ResourceNotFoundException("Unable to locate item");
            }

            EPerson eperson = context.getCurrentUser();
            
            if(!AuthorizeManager.isAdmin(context)) {
            	throw new AuthorizeException();
            }
            
        } catch (AuthorizeException | SQLException | IllegalStateException e) {
            throw new ProcessingException("Unable to read piwik statistics", e);
        }

    }


	@Override
	public void generate() throws IOException, SAXException, ProcessingException {
		try {
			String queryString = request.getQueryString();
			
			String module = request.getParameter("module");
			String method = request.getParameter("method");
			String format = request.getParameter("format");
			
			if(format==null || format.isEmpty()) {
				format = "xml";
			}
			
			if(module!=null && !module.equals("API")) {
				throw new Exception("Only piwik module=API requests are processed.");
			}
			
			
			if(method!=null && method.equals("API.get")) {
				Calendar cal = Calendar.getInstance();
				Date today = cal.getTime();
				cal.add(Calendar.DATE, -7);
				Date weekBefore = cal.getTime();
				
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				
				String url = PIWIK_API_URL + rest + "?" + queryString + "&date=" + df.format(weekBefore) + "," + df.format(today) + "&idSite=" + PIWIK_SITE_ID + "&token_auth=" + PIWIK_AUTH_TOKEN + "&segment=pageUrl=@" + item.getHandle();																			
				String downloadURL = PIWIK_API_URL + rest + "?" + queryString + "&date=" + df.format(weekBefore) + "," + df.format(today) + "&idSite=" + PIWIK_DOWNLOAD_SITE_ID + "&token_auth=" + PIWIK_AUTH_TOKEN + "&segment=pageUrl=@" + item.getHandle();				
				String report = readFromURL(url);
				String downloadReport = readFromURL(downloadURL);
				
				String merge = "";

				if(format.equalsIgnoreCase("xml")) {
					merge = mergeXML(report, downloadReport);
				} else
				if(format.equalsIgnoreCase("json")) {
					merge = mergeJSON(report, downloadReport);
				}
				
				out.write(merge.getBytes());
			} else {
				String url = PIWIK_API_URL + rest + "?" + queryString  + "&idSite=" + PIWIK_SITE_ID + "&token_auth=" + PIWIK_AUTH_TOKEN + "&segment=pageUrl=@" + item.getHandle();
				String report = readFromURL(url);
				out.write(report.getBytes());
			}
			
			out.flush();
			
		} catch (Exception e) {
			throw new ProcessingException("Unable to read piwik statisitcs", e);
		}
	}	
	
	private String mergeXML(String report, String downloadReport) throws Exception {
		Document reportDoc = loadXMLFromString(report);
		Document downloadReportDoc = loadXMLFromString(downloadReport);
		
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		XPathExpression resExpr = xpath.compile("//result");
		XPathExpression pageViewsExpr = xpath.compile("//nb_pageviews");
		XPathExpression uniqPageViewsExpr = xpath.compile("//nb_uniq_pageviews");
		XPathExpression downExpr = xpath.compile("//nb_downloads");
		XPathExpression uniqDownExpr = xpath.compile("//nb_uniq_downloads");		
				
		NodeList rRows = (NodeList)resExpr.evaluate(reportDoc, XPathConstants.NODESET);
		NodeList dRows = (NodeList)resExpr.evaluate(downloadReportDoc, XPathConstants.NODESET);
				
		for(int i=0;i<rRows.getLength();i++) {
			Node rRow = rRows.item(i);
			Node dRow = dRows.item(i);
			if(!dRow.hasChildNodes()) continue;			
			if(!rRow.hasChildNodes()) {
				Element nb_visits = reportDoc.createElement("nb_visits");
				nb_visits.setNodeValue("0");
				Element nb_uniq_visitors = reportDoc.createElement("nb_uniq_visitors");
				nb_uniq_visitors.setNodeValue("0");
				Element nb_pageviews = reportDoc.createElement("nb_pageviews");
				nb_pageviews.setNodeValue("0");
				Element nb_uniq_pageviews = reportDoc.createElement("nb_uniq_pageviews");
				nb_uniq_pageviews.setNodeValue("0");
				Element nb_downloads = reportDoc.createElement("nb_downloads");
				nb_downloads.setNodeValue("0");
				Element nb_uniq_downloads = reportDoc.createElement("nb_uniq_downloads");
				nb_uniq_downloads.setNodeValue("0");
				rRow.appendChild(nb_visits);
				rRow.appendChild(nb_uniq_visitors);
				rRow.appendChild(nb_pageviews);
				rRow.appendChild(nb_uniq_pageviews);
				rRow.appendChild(nb_downloads);
				rRow.appendChild(nb_uniq_downloads);
			}
			Node dv = (Node)pageViewsExpr.evaluate(dRow, XPathConstants.NODE);
			Node duv = (Node)uniqPageViewsExpr.evaluate(dRow, XPathConstants.NODE);
			Node rd = (Node)downExpr.evaluate(rRow, XPathConstants.NODE);
			Node rud = (Node)uniqDownExpr.evaluate(rRow, XPathConstants.NODE);
			
			rd.setTextContent(dv.getTextContent());
			rud.setTextContent(duv.getTextContent());

		}		
		Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		Writer out = new StringWriter();
		tf.transform(new DOMSource(reportDoc), new StreamResult(out));
		return out.toString();
	}
	
	private String mergeJSON(String report, String downloadReport) throws Exception {
		JSONParser parser = new JSONParser();
		JSONObject reportJSON = (JSONObject)parser.parse(report);
		JSONObject downloadReportJSON = (JSONObject)parser.parse(downloadReport);
		for(Object key : reportJSON.keySet()) {			
			JSONObject rRow = null;
			JSONObject dRow = null;
			try{
				dRow = (JSONObject)downloadReportJSON.get(key);
			} catch (ClassCastException e) {
				continue;
			}
			try {
				rRow = (JSONObject)reportJSON.get(key);
			} catch (ClassCastException e) {
				rRow = new JSONObject();
				reportJSON.put(key, rRow);
			}			
			rRow.put("nb_downloads", dRow.get("nb_pageviews"));
			rRow.put("nb_uniq_downloads", dRow.get("nb_uniq_pageviews"));
		}
		return reportJSON.toJSONString();
	}
	
	private String readFromURL(String url) throws IOException {
		StringBuilder output = new StringBuilder();		
		URL widget = new URL(url);
        String old_value = "false";
        try{
            old_value = System.getProperty("jsse.enableSNIExtension");
            System.setProperty("jsse.enableSNIExtension", "false");

            BufferedReader in = new BufferedReader(new InputStreamReader(widget.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                output.append(inputLine).append("\n");
            }
            in.close();
        }finally {
        	//true is the default http://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html
        	old_value = (old_value == null) ? "true" : old_value;
            System.setProperty("jsse.enableSNIExtension", old_value);
        }
		return output.toString();
	}
	
	private static Document loadXMLFromString(String xml) throws Exception {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    InputSource is = new InputSource(new StringReader(xml));
	    return builder.parse(is);
	}

}


