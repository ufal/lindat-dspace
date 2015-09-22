/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.rest;

import org.glassfish.jersey.server.ResourceConfig;

public class Application extends ResourceConfig{
    public Application(){
        packages("org.dspace.rest","cz.cuni.mff.ufal.dspace.rest");
    }
}
