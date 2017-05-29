/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.authenticate.shibboleth;

import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

/**
 * Try to refactor the Shibboleth mess.
 * 
 * Get groups a user should be put into according to several Shibboleth headers
 * and default configuration values.
 */
public class ShibGroup
{
    
    // constants
    //
    private static String defaultRoles = ConfigurationManager.getProperty(
            "authentication-shibboleth","default-roles");
    private static String roleHeader = ConfigurationManager.getProperty(
            "authentication-shibboleth","role-header");
    private static boolean ignoreScope = ConfigurationManager.getBooleanProperty(
            "authentication-shibboleth","role-header.ignore-scope", true);
    private static boolean ignoreValue = ConfigurationManager.getBooleanProperty(
            "authentication-shibboleth","role-header.ignore-value", false);
    

    // variables
    //
    private static Logger logger_ = cz.cuni.mff.ufal.Logger.getLogger(ShibGroup.class);
    private ShibHeaders shib_headers_ = null;
    private Context context_ = null;

    // ctor
    //
    
    public ShibGroup(ShibHeaders shib_headers, Context context)
    {
        shib_headers_ = shib_headers;
        context_ = context;
        
        if (ignoreScope && ignoreValue) 
        {
            throw new IllegalStateException(
                    "Both config parameters for ignoring attributes scope and value are turned on, " +
                    "this is not a permissable configuration. (Note: ignore-scope defaults to true) " +
                    "The configuration parameters are: 'authentication.shib.role-header.ignore-scope' " +
                    "and 'authentication.shib.role-header.ignore-value'");
        }
    }
    
    /**
     * This is again a bit messy but the purpose is to find out into which groups an EPerson belongs; hence,
     * authorisation part from AAI. 
     * 
     * 
     */
    
    public int[] get()
    {
        try {
            logger_.debug("Starting to determine special groups");

            /* <UFAL>
             * lets be evil and hack the email to the entitlement field
             */
            List<String> affiliations = new ArrayList<String>(); 
            affiliations.addAll(
                    get_affilations_from_roles(roleHeader));
            affiliations.addAll(
                    get_affilations_from_shib_mappings());

            /* </UFAL> */
            
            
            if (affiliations.isEmpty()) {
                if (defaultRoles != null)
                    affiliations = Arrays.asList(defaultRoles.split(","));
                logger_.debug("Failed to find Shibboleth role header, '"+roleHeader+"', " +
                        "falling back to the default roles: '"+defaultRoles+"'");
            } else {
                logger_.debug("Found Shibboleth role header: '"+roleHeader+"' = '"+affiliations+"'");
            }

            // Loop through each affiliation
            //
            Set<Integer> groups = new HashSet<Integer>();
            if (affiliations != null) 
            {
                for ( String affiliation : affiliations) 
                {
                    // populate the organisation name
                    affiliation = populate_affiliation(affiliation, ignoreScope, ignoreValue);
                    // try to get the group names from authentication-shibboleth.cfg
                    String groupNames = get_group_names_from_affiliation(affiliation); 

                    if (groupNames == null) {
                        logger_.debug("Unable to find role mapping for the value, '"+affiliation+"', " +
                                "there should be a mapping in the dspace.cfg:  authentication.shib.role."+affiliation+" = <some group name>");
                        continue;
                    } else {
                        logger_.debug("Mapping role affiliation to DSpace group: '"+groupNames+"'");
                    }
                    
                    // get the group ids
                    groups.addAll(string2groups(groupNames));

                } // foreach affiliations
            } // if affiliations

            //attribute -> group mapping
            //check shibboleth attribute ATTR and put users having value ATTR_VALUE1 and ATTR_VALUE2 to GROUP1
            //users having ATTR_VALUE3 to GROUP2
            //groups must exist
            //header.ATTR=ATTR_VALUE1=>GROUP1,ATTR_VALUE2=>GROUP1,ATTR_VALUE3=>GROUP2
            final String lookFor = "header.";
            ConfigurationService configurationService = new DSpace().getConfigurationService();
            Properties allShibbolethProperties = ConfigurationManager.getProperties("authentication-shibboleth");
            for(String propertyName : allShibbolethProperties.stringPropertyNames()){
                //look for properties in authentication shibboleth that start with "header."
                if(propertyName.startsWith(lookFor)){
                    String headerName = propertyName.substring(lookFor.length());
                    List<String> presentHeaderValues = shib_headers_.get(headerName);
                    if(!presentHeaderValues.isEmpty()) {
                        //if shibboleth sent any attributes under the headerName
                        String[] values2groups = configurationService.getPropertyAsType(
                                "authentication-shibboleth." + propertyName, String[].class);
                        for (String value2group : values2groups) {
                            String[] value2groupParts = value2group.split("=>", 2);
                            String headerValue = value2groupParts[0].trim();
                            String assignedGroup = value2groupParts[1].trim();
                            if(presentHeaderValues.contains(headerValue)){
                                //our configured header value is present so add a group
                                groups.addAll(string2groups(assignedGroup));
                            }
                        }
                    }
                }
            }

            /* <UFAL>
             * Default group for shib authenticated users
             */
            Group default_group = get_default_group();
            if ( null != default_group ) {
                groups.add(default_group.getID());
            }
            /* </UFAL> */
            


            logger_.info("Added current EPerson to special groups: " + groups);

            // Convert from a Java Set to primitive int array
            int groupIds[] = new int[groups.size()];
            Iterator<Integer> it = groups.iterator();
            for (int i = 0; it.hasNext(); i++) {
                groupIds[i] = it.next();
            }

            return groupIds;
        } catch (Throwable t) {
            logger_.error(
                    "Unable to validate any special groups this user may belong too because of an exception.",t);
            return new int[0];
        }       
    }
    
    //
    //
    private List<String> get_affilations_from_roles(String roleHeader) 
    {
        List<String> roleHeaderValues = shib_headers_.get(roleHeader);
        List<String> affiliations = new ArrayList<String>();

        // Get the Shib supplied affiliation or use the default affiliation
        // e.g., we can use 'entitlement' shibboleth header
        if(roleHeaderValues!=null) {
            for(String roleHeaderValue : roleHeaderValues) {
                affiliations.addAll(string2values(roleHeaderValue));
            }
        }
        return affiliations;
    }
    
    private List<String> get_affilations_from_shib_mappings() 
    {
        List<String> ret = new ArrayList<String>(); 
        String organization = shib_headers_.get_idp();
        // Try to get email based on utilities mapping database table 
        // 
        if(organization != null) 
        {
            String email_header = ShibEPerson.emailHeader;
            if (email_header != null)
            {
                String email = shib_headers_.get_single(email_header);
                if (email != null) {
                    ret = string2values(email);
                }
            }
        }
        if ( ret == null ) {
            return new ArrayList<String>();
        }
        
        return ret;
    }
    
    private String populate_affiliation(String affiliation, boolean ignoreScope, boolean ignoreValue) 
    {
        // If we ignore the affilation's scope then strip the scope if it exists.
        if (ignoreScope) {
            int index = affiliation.indexOf('@');
            if (index != -1) {
                affiliation = affiliation.substring(0, index);
            }
        } 
        // If we ignore the value, then strip it out so only the scope remains.
        if (ignoreValue) {
            int index = affiliation.indexOf('@');
            if (index != -1) {
                affiliation = affiliation.substring(index+1, affiliation.length());
            }
        }
        
        return affiliation;
    }
    
    private String get_group_names_from_affiliation(String affiliation)
    {
        String groupNames = ConfigurationManager.getProperty(
                "authentication-shibboleth","role." + affiliation);
        if (groupNames == null || groupNames.trim().length() == 0) 
        {
            groupNames = ConfigurationManager.getProperty(
                "authentication-shibboleth", "role." + affiliation.toLowerCase());
        }
        return groupNames;
    }
    
    private List<Integer> string2groups(String groupNames)
    {
        List<Integer> groups = new ArrayList<Integer>();
        // Add each group to the list.
        String[] names = groupNames.split(",");
        for (int i = 0; i < names.length; i++) {
            try {
                
                Group group = Group.findByName(context_, names[i].trim());
                if (group != null) {
                    groups.add(group.getID());
                }else { 
                    logger_.debug("Unable to find group: '"+names[i].trim()+"'");
                }
                
            } catch (SQLException sqle) {
                logger_.error(
                    "Exception thrown while trying to lookup affiliation role for group name: '"+names[i].trim()+"'",
                    sqle);
            }
        } // for each groupNames
        return groups;
    }

    private Group get_default_group()
    {
        String defaultAuthGroup = ConfigurationManager.getProperty(
                "authentication-shibboleth", "default.auth.group");
        if(defaultAuthGroup != null && defaultAuthGroup.trim().length()!=0){
            try {
                Group group = Group.findByName(context_,defaultAuthGroup.trim());
                if (group != null) {
                    return group;
                }else { 
                    logger_.debug("Unable to find default group: '"+defaultAuthGroup.trim()+"'");
                }
            } catch (SQLException sqle) {
                logger_.error("Exception thrown while trying to lookup shibboleth " +
                        "default authentication group with name: '"+defaultAuthGroup.trim()+"'",sqle);
            }
        }
        
        return null;
    }
    
    // helpers
    //
    
    private static List<String> string2values(String string) {
        if ( string == null ) {
            return null;
        }
        return Arrays.asList(string.split(",|;"));
    }
    

}