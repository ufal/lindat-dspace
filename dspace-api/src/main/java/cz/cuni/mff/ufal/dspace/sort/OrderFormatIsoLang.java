/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.sort;

import org.dspace.sort.OrderFormatDelegate;

import cz.cuni.mff.ufal.IsoLangCodes;

/**
 * 
 * @author LINDAT/CLARIN team
 *
 */

public class OrderFormatIsoLang implements OrderFormatDelegate{

	@Override
	public String makeSortString(String value, String language) {
		String langName = IsoLangCodes.getLangForCode(value);
		if(langName != null){
			value = langName;
		}
		return value;
	}

}
