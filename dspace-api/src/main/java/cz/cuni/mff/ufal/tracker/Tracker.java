/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.tracker;

import javax.servlet.http.HttpServletRequest;

public interface Tracker
{
    public void trackDownload(HttpServletRequest request);
    public void trackPage(HttpServletRequest request, String pageName);
}
