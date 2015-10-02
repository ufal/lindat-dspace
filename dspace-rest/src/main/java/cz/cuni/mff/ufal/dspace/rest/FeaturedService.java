/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.rest;

import java.util.Hashtable;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "featuredService")
public class FeaturedService{
    public String name;
    public String url;
    public String description;
    
    public Map<String, String> links;

    public FeaturedService() {}

    public FeaturedService(String name, String url, String description){
        this.name = name;
        this.url = url;
        this.description = description;
        links = new Hashtable<String, String>();
    }
    
    public void addLink(String key, String value) {
    	links.put(key, value);
    }
}
