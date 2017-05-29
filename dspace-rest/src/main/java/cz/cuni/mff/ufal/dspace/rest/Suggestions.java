package cz.cuni.mff.ufal.dspace.rest;

import org.dspace.core.Context;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.rest.Resource;
import org.dspace.utils.DSpace;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.sql.SQLException;

@Path("/suggestions")
public class Suggestions extends Resource {

    @GET
    @Path("/{facetField}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSuggestion(@PathParam("facetField") String facetField){
        DiscoverQuery queryArgs = new DiscoverQuery();
        queryArgs.setQuery("*:*");
        int facetLimit = -1;
        //Using the api that's there; we don't actually need "real" DiscoverFacetField object; TYPE_STANDARD should not modify the facetField, but TYPE_AC would add "_ac"
        queryArgs.addFacetField(new DiscoverFacetField(facetField, DiscoveryConfigurationParameters.TYPE_STANDARD, facetLimit, DiscoveryConfigurationParameters.SORT.COUNT));
        SearchService searchService = new DSpace().getServiceManager().getServiceByName(SearchService.class.getName(), SearchService.class);
        InputStream JSONStream = null;
        Context context = null;
        try{
            context = new Context(Context.READ_ONLY);
            JSONStream = searchService.searchJSON(context, queryArgs, null);
            context.complete();
        }catch (SearchServiceException | SQLException e){
            processException("Error while retrieving JSON from discovery. " + e.getMessage(), context);
        }finally {
            processFinally(context);
        }
        return Response.ok(JSONStream).build();
    }
}

