package cz.cuni.mff.ufal.dspace.rest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "featuredServices")
public class FeaturedServices {
    @XmlElement(name = "featuredService")
    private List<FeaturedService> featuredServiceList;

    public FeaturedServices(){
        featuredServiceList = new ArrayList<>();
    }

    public void add(String name, String url, String description){
        featuredServiceList.add(new FeaturedService(name, url, description));
    }
}

@XmlRootElement(name = "featuredService")
class FeaturedService{
    public String name;
    public String url;
    public String description;

    public FeaturedService() {}

    public FeaturedService(String name, String url, String description){
        this.name = name;
        this.url = url;
        this.description = description;
    }
}
