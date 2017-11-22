/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.generation.AbstractGenerator;
import org.apache.cocoon.xml.dom.DOMStreamer;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

import org.dspace.utils.DSpace;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.isBlank;

public class DiscoJuiceFeeds extends AbstractGenerator {
    /**
     * log4j logger.
     */
    private static Logger log = Logger.getLogger(DiscoJuiceFeeds.class);

    private static final String discojuiceURL = "https://static.discojuice.org/feeds/";

    private static String feedsContent;
    private static ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static ScheduledFuture<?> future;
    private static boolean aggressiveDelay = true;

    static{
        future = executor.scheduleWithFixedDelay(new Updater(), 30, 60, TimeUnit.SECONDS);
    }

    private static final LookupService locationService;
    /**
     * contains entityIDs of idps we wish to set the country to something different than discojuice feeds suggests
     **/
    private static final Set<String> rewriteCountries;

    static {
        String dbfile = ConfigurationManager.getProperty("usage-statistics", "dbfile");
        LookupService service = null;
        if (dbfile != null) {
            try {
                service = new LookupService(dbfile,
                        LookupService.GEOIP_STANDARD);
            } catch (FileNotFoundException fe) {
                log.error("The GeoLite Database file is missing (" + dbfile + ")! Solr Statistics cannot generate location based reports! Please see the DSpace installation instructions for instructions to install this file.", fe);
            } catch (IOException e) {
                log.error("Unable to load GeoLite Database file (" + dbfile + ")! You may need to reinstall it. See the DSpace installation instructions for more details.", e);
            }
        } else {
            log.error("The required 'dbfile' configuration is missing in solr-statistics.cfg!");
        }
        locationService = service;

        rewriteCountries = new HashSet<String>();
        String propRewriteCountries = ConfigurationManager.getProperty("discojuice", "rewriteCountries");
        for (String country : propRewriteCountries.split(",")) {
            country = country.trim();
            rewriteCountries.add(country);
        }
    }

    public static void update(){
        log.info("DiscoJuiceFeeds::update called");
        lock.writeLock().lock();
        try{
            String newFeedsContent = createFeedsContent();
            if(isNotBlank(newFeedsContent)) {
                feedsContent = newFeedsContent;
            }
            if(aggressiveDelay && isNotBlank(feedsContent)){
                // apply the configured refresh rate, when we've successfully obtained feeds
                future.cancel(true);
                future = executor.scheduleWithFixedDelay(new Updater(), 0,
                        ConfigurationManager.getLongProperty("discojuice", "refresh"),
                        TimeUnit.HOURS);
                aggressiveDelay = false;
            }

        }finally {
            lock.writeLock().unlock();
        }
    }

    public String getContentType(boolean jsonp) {
        if(jsonp) {
            return "application/javascript;charset=utf-8";
        }else{
            return "application/json";
        }
    }

    @Override
    public void generate() throws IOException, SAXException {
        HttpServletRequest request = ObjectModelHelper.getRequest(objectModel);
        HttpServletResponse response = ObjectModelHelper.getResponse(objectModel);
        String callback = request.getParameter("callback");
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            //the root should be ignored by TextSerializer
            Element root = doc.createElement("ignore_root");
            lock.readLock().lock();
            try {
                if (isBlank(feedsContent)) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to obtain feeds.");
                } else {
                    boolean jsonp = isNotBlank(callback);
                    CDATASection cdata;
                    if(jsonp){
                        cdata = doc.createCDATASection(callback + '(' + feedsContent + ')');
                    }else{
                        cdata = doc.createCDATASection(feedsContent);
                    }
                    root.appendChild(cdata);
                    doc.appendChild(root);
                    DOMStreamer streamer = new DOMStreamer(contentHandler, lexicalHandler);
                    streamer.stream(doc);
                }
            } finally {
                lock.readLock().unlock();
            }
        }catch (ParserConfigurationException e){
            log.error(e);
        }
    }

    public static String createFeedsContent(){
        String feedsConfig = ConfigurationManager.getProperty("discojuice", "feeds");
        String shibbolethDiscoFeedUrl = ConfigurationManager.getProperty("lr","lr.shibboleth.discofeed.url");
        return createFeedsContent(feedsConfig, shibbolethDiscoFeedUrl);
    }

    private static Map<String, JSONObject> toMap(JSONArray jsonArray){
        Map<String, JSONObject> map = new HashMap<>();
        for(Object entityO : jsonArray){
            JSONObject entity = (JSONObject) entityO;
            String entityID = (String) entity.get("entityID");
            if(!map.containsKey(entityID)){
                map.put(entityID, entity);
            }
        }
        return map;
    }

    private static JSONArray shrink(JSONArray jsonArray){
        for(Object entityO : jsonArray){
            JSONObject entity = (JSONObject) entityO;
            // if there are DisplayNames only the first one will be used in title copy the rest
            // to keywords
            // copy any value in Keywords and Description to keywords
            for(String key: new String[]{"DisplayNames", "Keywords", "Descriptions"}) {
                if (entity.containsKey(key)) {
                    JSONArray keyObjects = (JSONArray) entity.get(key);
                    List<String> values = getValues(keyObjects);
                    if (!values.isEmpty()) {
                        if("DisplayNames".equals(key)){
                            entity.put("title", values.remove(0));
                            if(values.isEmpty()){
                                continue;
                            }
                        }
                        if (entity.containsKey("keywords")) {
                            values.addAll((List<String>) entity.get("keywords"));
                        }
                        entity.put("keywords", values);
                    }
                }
            }

            // Logos (in contrast to icon) are currently unused by the fronted; they just eat bandwidth
            // The same for InformationURLs, Descriptions, PrivacyStatementURLs
            // Can be configured
            String[] toRemove = new DSpace().getConfigurationService().getPropertyAsType("discojuice" +
                    ".remove_from_shib_feed_object", new String[]{"Logos", "InformationURLs",
                    "Descriptions", "PrivacyStatementURLs", "DisplayNames", "Keywords"});
            for(String key : toRemove){
                entity.remove(key);
            }
        }
        return jsonArray;
    }

    private static List<String> getValues(JSONArray array){
        ArrayList<String> res = new ArrayList<>(array.size());
        for(Object obj : array){
            JSONObject jObj = (JSONObject) obj;
            if(jObj.containsKey("value")){
                res.add((String)jObj.get("value"));
            }

        }
        return res;
    }

    public static String createFeedsContent(String feedsConfig, String shibbolethDiscoFeedUrl){
        String old_value = System.getProperty("jsse.enableSNIExtension");
        System.setProperty("jsse.enableSNIExtension", "false");

        //Obtain shibboleths discofeed
        final Map<String,JSONObject> shibDiscoEntities = toMap(shrink(DiscoJuiceFeeds.downloadJSON(shibbolethDiscoFeedUrl)));

        //true is the default http://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html
        old_value = (old_value == null) ? "true" : old_value;
        System.setProperty("jsse.enableSNIExtension", old_value);

        Set<String> processedEntities = new HashSet<>();
        //loop through disco cdn feeds
        for(String feed : feedsConfig.split(",")){
            Map<String, JSONObject> feedMap = toMap(DiscoJuiceFeeds.downloadJSON(discojuiceURL + feed.trim()));
            //loop through entities in one feed
            for (Map.Entry<String, JSONObject> entry: feedMap.entrySet()){
                String entityID = entry.getKey();
                JSONObject cdnEntity = entry.getValue();
                //keep only entities from shibboleth, add only once, but copy geo, icon, country
                if(shibDiscoEntities.containsKey(entityID) && !processedEntities.contains(entityID)){
                    JSONObject geo = (JSONObject) cdnEntity.get("geo");
                    String icon = (String) cdnEntity.get("icon");
                    String country = (String) cdnEntity.get("country");
                    JSONObject shibEntity = shibDiscoEntities.get(entityID);
                    if(geo != null){
                            shibEntity.put("geo", geo);
                    }
                    if(icon != null){
                            shibEntity.put("icon", icon);
                    }
                    if(country != null){
                            shibEntity.put("country", country);
                    }
                    processedEntities.add(entityID);
                }
            }
        }

        //loop through shib entities, we show these...
        for (JSONObject shibEntity : shibDiscoEntities.values()){
            //rewrite or guess countries
            if(rewriteCountries.contains(shibEntity.get("entityID")) || isBlank((String)shibEntity.get("country"))){
                String old_country = (String)shibEntity.remove("country");
                String new_country = guessCountry(shibEntity);
                shibEntity.put("country", new_country);
                log.info(String.format("For %s changed country from %s to %s", shibEntity.get("entityID"), old_country, new_country));
            }
        }

        if(shibDiscoEntities.isEmpty()){
            return null;
        }else {
            JSONArray ret = new JSONArray();
            ret.addAll(shibDiscoEntities.values());
            return ret.toJSONString();
        }
    }

    private static JSONArray downloadJSON(String url){
        JSONParser parser = new JSONParser();
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            //Caution does not follow redirects, and even if you set it to http->https is not possible
            Object obj = parser.parse(new InputStreamReader(conn.getInputStream()));
            return (JSONArray) obj;
        }catch (IOException|ParseException e){
            log.error("Failed to obtain/parse "+ url + "\nCheck timeouts, redirects, shibboleth config.\n" + e);
        }
        return new JSONArray();
    }

    private static String guessCountry(JSONObject entity){
    	if(locationService != null && entity.containsKey("InformationURLs")){
    		JSONArray informationURLs = (JSONArray)entity.get("InformationURLs");
    		if(informationURLs.size() > 0){
    			String informationURL = (String)((JSONObject)informationURLs.get(0)).get("value");
    			try{
    				Location location = locationService.getLocation(java.net.InetAddress.getByName(new URL(informationURL).getHost()));
    				if(location != null && location.countryCode != null){
    					return location.countryCode;
    				}else{
    					log.info("Country or location is null for " + informationURL);
    				}
    			}catch(MalformedURLException e){
    				
    			}catch(java.net.UnknownHostException e){
    				
    			}
    		}
    	}
    	String entityID = (String)entity.get("entityID");
        //entityID not necessarily an URL
        try{
            URL url = new URL(entityID);
            String host = url.getHost();
            String topLevel = host.substring(host.lastIndexOf('.')+1);
            if(topLevel.length() == 2 && !topLevel.equalsIgnoreCase("eu")){
                //assume country code
                return topLevel.toUpperCase();
            }
        }catch(MalformedURLException e){

        }
        return "_all_"; //by default add "_all_", better search in dj
    }

    //For testing
    public static void main(String[] args) throws Exception{
        long startTime = System.currentTimeMillis();
        String feeds = DiscoJuiceFeeds.createFeedsContent("edugain, dfn, cesnet, surfnet2, haka, kalmar", "https://lindat.mff.cuni.cz/Shibboleth.sso/DiscoFeed");
        long endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime)/1000);
        //System.out.println(feeds);
    }

    private static class Updater implements Runnable {
        @Override
        public void run() {
            DiscoJuiceFeeds.update();
        }
    }
}
