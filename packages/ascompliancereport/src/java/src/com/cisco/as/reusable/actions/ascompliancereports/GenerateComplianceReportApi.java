package com.cisco.as.reusable.actions.ascompliancereports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;



import com.cisco.as.reusable.actions.ascompliancereports.namespaces.ascompliancereport;
import com.tailf.conf.ConfObject;
import com.tailf.conf.ConfTag;
import com.tailf.conf.ConfXMLParam;
import com.tailf.dp.DpActionTrans;
import com.tailf.dp.DpCallbackException;
import com.tailf.dp.annotations.ActionCallback;
import com.tailf.dp.proto.ActionCBType;
/**
 * This Api class will be called when requested to get compliace report for device group.
 * @author root
 *
 */
public class GenerateComplianceReportApi {

    private static Logger logger  = Logger.getLogger(GenerateComplianceReportApi.class);

    public GenerateComplianceReportApi() {

    }
    
    /**
	 * This init method is called to get the call point for prefixList Compliance report.
	 * @param trans
	 *   Transaction
	 * @throws DpCallbackException
	 */
    @ActionCallback(callPoint="generate-compliance-report-action-point",
                    callType=ActionCBType.INIT)
    public void init(DpActionTrans trans) throws DpCallbackException {}
    
    /**
     * This action point invokes when prefixList Compliance report request is called.
     * @param trans
     * @param kp
     * @param params
     * @return
     * @throws DpCallbackException
     * @throws IOException
     */

    @ActionCallback(callPoint="generate-compliance-report-action-point",
                    callType=ActionCBType.ACTION)
    public ConfXMLParam[] action(DpActionTrans trans, ConfTag name,
			ConfObject[] kp, ConfXMLParam[] params) throws DpCallbackException,
			IOException {
		ConfXMLParam[] result = null;

		try {
			
			GenerateComplianceReportActions action = new GenerateComplianceReportActions(params);
		
			switch (name.getTagHash()) {
				case ascompliancereport._generate_compliance_report:
					trans.actionSetTimeout(1800);
					action.generateDcComplianceReport(trans,params);
					break;
				default:
					// Model inconsistency
					throw new DpCallbackException("got bad operation: " + name);
			}

			result = action.output.toArray(new ConfXMLParam[0]);
			
			
		} catch (Exception e) {
			logger.error(
					"Error in command processing for " + name + " "
							+ e.getMessage(), e.getCause());
			List<ConfXMLParam> output = new ArrayList<ConfXMLParam>();
			GenerateComplianceReportActions.addStrElem(ascompliancereport.hash,
                    ascompliancereport._error_message,e.getMessage(), output);
			result = output.toArray(new ConfXMLParam[0]);
		}
		return result;
	}
    
}
