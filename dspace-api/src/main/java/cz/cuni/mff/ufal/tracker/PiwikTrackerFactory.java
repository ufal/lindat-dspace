/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.tracker;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

import javax.servlet.http.HttpServletRequest;

public class PiwikTrackerFactory
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(PiwikTrackerFactory.class);
    private static final ConfigurationService configurationService = new DSpace().getConfigurationService();
    private static final Tracker noopTracker = new Tracker(){

        @Override
        public void trackDownload(HttpServletRequest request) {
            log.debug("NOOP: trackDownload");
        }

        @Override
        public void trackPage(HttpServletRequest request, String pageName) {
            log.debug("NOOP: trackPage");
        }
    };

    public static Tracker createOAITrackerInstance(){
        return new PiwikOAITracker();
    }

    public static Tracker createBitstreamTrackerInstance(Collection col){
        int defaultSiteId = configurationService.getPropertyAsType("lr.lr.tracker.bitstream.site_id", 0);

        if(col == null){
            //There might be no collection for example if we try downloading as editors during review; the item has
            // not been installed in any collection yet.
            return new PiwikBitstreamTracker(defaultSiteId);
        }

        boolean disabled =  configurationService.getPropertyAsType("lr.lr.tracker.collection."
                + col.getHandle() + ".disabled", false);
        if(disabled){
            return noopTracker;
        }else{
            int siteId = configurationService.getPropertyAsType(
                    "lr.lr.tracker.bitstream.from." + col.getHandle() +".site_id", defaultSiteId);
            return new PiwikBitstreamTracker(siteId);
        }
    }
}
