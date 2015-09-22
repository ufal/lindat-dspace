/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.rest;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "featuredServices")
public class FeaturedServices {
    @XmlElement(name = "featuredService")
    private List<FeaturedService> featuredServiceList;

    public FeaturedServices(){
        featuredServiceList = new ArrayList<>();
    }

    public void add(FeaturedService featuredService){
       this.featuredServiceList.add(featuredService);
    }
}