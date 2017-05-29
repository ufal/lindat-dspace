package cz.cuni.mff.ufal.dspace.rest;

import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.message.filtering.EntityFilteringFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;

public class Application extends ResourceConfig{
    public Application(){
        packages("org.dspace.rest","cz.cuni.mff.ufal.dspace.rest");
        register(EntityFilteringFeature.class);
        EncodingFilter.enableFor(this, GZipEncoder.class);
    }
}
