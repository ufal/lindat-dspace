/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import cz.cuni.mff.ufal.DSpaceApi;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.administrative.controlpanel.AbstractControlPanelTab;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Site;
import org.dspace.core.ConfigurationManager;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ControlPanelEditConfigurationTab extends AbstractControlPanelTab {

	private static final Message T_DSPACE_HEAD = message("xmlui.administrative.ControlPanel.dspace_head");

	@Override
	public void addBody(Map objectModel, Division div) throws WingException, SQLException, AuthorizeException {
		if (!AuthorizeManager.isAdmin(context))
		{
			throw new AuthorizeException("You are not authorized to view this page.");
		}

		ConfigurationService configuration = new DSpace().getConfigurationService();
		div.addPara("alert", "alert alert-danger").addContent("Use at your own risk. This is only a call to ConfigurationService. It does not update values cached in any java class. ConfigurationManager is unaware of any value change.");

		div = div.addInteractiveDivision("edit-config-form", this
				.web_link, Division.METHOD_POST);
		Request request = ObjectModelHelper.getRequest(objectModel);

		String varName = request.getParameter("varName");
		String varValue = "";

		if(isNotBlank(varName) && "Show".equals(request.getParameter("submit-config-form"))){
			varValue = configuration.getProperty(varName);
		}

		if(isNotBlank(varName) && "Set".equals(request.getParameter("submit-config-form"))){
			varValue = request.getParameter("varValue");
			configuration.setProperty(varName, varValue);
		}

		div.setHead(T_DSPACE_HEAD);
		List list = div.addList("dspace");
		list.addLabel("Property name");
		list.addItem(null,null).addText("varName").setValue(varName);
		list.addLabel("Property value");
		list.addItem(null,null).addText("varValue").setValue(varValue);
		list.addItem(null, null).addButton("submit-config-form").setValue("Show");
		list.addItem(null, null).addButton("submit-config-form").setValue("Set");
	}

}

