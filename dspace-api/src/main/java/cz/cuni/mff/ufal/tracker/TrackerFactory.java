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
import org.dspace.core.ConfigurationManager;

public class TrackerFactory
{
    /** log4j category */
    private static Logger log = Logger.getLogger(TrackerFactory.class);

    public static Tracker createInstance(TrackingSite site)
    {
        String trackerType = ConfigurationManager
                .getProperty("lr", "lr.tracker.type");
        if (trackerType.equalsIgnoreCase("piwik"))
        {
            return PiwikTrackerFactory.createInstance(site);
        }
        throw new IllegalArgumentException("Invalid tracker type:" + trackerType);
    }
}
