/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import org.dspace.core.ConfigurationManager;
import org.dspace.health.Report;
import org.junit.Test;

import javax.security.auth.login.Configuration;
import java.lang.reflect.Field;
import java.util.Properties;

public class ReportTest
{
    @Test
    public void testReportHandleResolutionStatistics() throws IllegalAccessException {
        String args[] = {"-c", "0"};
        Report.main(args);
    }
}