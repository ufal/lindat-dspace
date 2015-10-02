/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.tracker;

import org.apache.log4j.Logger;

public class PiwikTrackerFactory
{
    /** log4j category */
    private static Logger log = Logger.getLogger(PiwikTrackerFactory.class);

    public static Tracker createInstance(TrackingSite site)
    {                
        switch(site) {
            case OAI:
                return new PiwikOAITracker();                
            case BITSTREAM:
                return new PiwikBitstreamTracker();                                          
        }       
        throw new IllegalArgumentException("Unknown site: " + site);
    }
}
