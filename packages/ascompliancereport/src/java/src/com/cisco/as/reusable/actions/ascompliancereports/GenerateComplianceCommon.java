package com.cisco.as.reusable.actions.ascompliancereports;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.tailf.conf.Conf;
import com.tailf.conf.ConfBuf;
import com.tailf.conf.ConfEnumeration;
import com.tailf.conf.ConfException;
import com.tailf.conf.ConfKey;
import com.tailf.conf.ConfPath;
import com.tailf.conf.ConfValue;
import com.tailf.conf.ConfXMLParam;
import com.tailf.conf.ConfXMLParamValue;
import com.tailf.maapi.Maapi;
import com.tailf.navu.NavuContainer;
import com.tailf.navu.NavuList;
import com.tailf.ncs.ns.Ncs;
import com.tailf.ncs.template.Template;
import com.tailf.ncs.template.TemplateVariables;

public class GenerateComplianceCommon {
	
    private static Logger logger  = Logger.getLogger(GenerateComplianceCommon.class);	
   
    
    /**
     * To add device list to the device group
     * @param devicesList - list of devices
     */
    /**
     * This method will add the given list of devices to the device group.
     * @param devicesList
     * @param deviceGroup
     * @param maapi
     * @throws IOException
     * @throws ConfException
     */
    static void addDevicesToGroup(String devicesList,String deviceGroup,Maapi maapi) throws IOException,ConfException{
        logger.debug("Adding devices to device Group ");
        int th = 0;
            
        th = maapi.startTrans(Conf.DB_RUNNING, Conf.MODE_READ_WRITE);
        NavuContainer ncsContainer = GenerateComplianceReportActions.getNcsContainer(maapi, th);
        
        //gets the device group container
        NavuList devlist = ncsContainer.container(Ncs._devices).list(Ncs._device_group_);
        NavuContainer devGrpContainer = devlist.elem(deviceGroup);
        
        
        if(devGrpContainer == null){
            // if device group does not exists, it will create.
            devGrpContainer  = devlist.create(deviceGroup);
            // adds list of devices to device group
            devGrpContainer.leaf(Ncs._device_name_).set(devicesList);
            maapi.applyTrans(th, false);
        }else {
            
            //
            
            //If the report already exists delete it and create new one.
            /*devlist.delete(deviceGroup);
            devGrpContainer  = devlist.create(deviceGroup);*/
  

            // adds list of devices to device group
            devGrpContainer.leaf(Ncs._device_name_).set(devicesList);
            maapi.applyTrans(th, false);
        }
        maapi.finishTrans(th);        
    }
    
    /**
     * This method will set the below command
     * Ex: set compliance reports report **report-name** compare-template **template-name**  **device-groupn-name**" 
     * @param templateArray -List of templates
     * @throws IOException
     * @throws ConfException
     */
    /**
     * 
     * @param templateArray
     * @param deviceGroup
     * @param reportName
     * @param maapi
     * @throws IOException
     * @throws ConfException
     */
    static void setReportName(String[] templateArray,String deviceGroup,String reportName,Maapi maapi) throws IOException, ConfException {
        logger.debug("setting reportName ");
        int th = 0;
        th = maapi.startTrans(Conf.DB_RUNNING, Conf.MODE_READ_WRITE);
        NavuContainer container = GenerateComplianceReportActions.getNcsContainer(maapi, th);
   
        NavuList report = container.container(Ncs._compliance).container(Ncs._reports_).list(Ncs._report_);
        NavuContainer reportContainer = report.elem(reportName);
        
        if(reportContainer == null) {
            reportContainer = report.create(reportName);
        }else{
        	//If the report already exists delete it and create new one.
        	report.delete(reportName);
        	reportContainer = report.create(reportName);
        }
        
        //for every template use the report container created above to add the templates. commit transaction at once. 
        for(String templateName:templateArray){ 
            NavuList compareTempList = reportContainer.list(Ncs._compare_template_);
                
            int numOfKeyElements = 2;
            ConfBuf[] compositeKeyElems = new ConfBuf[numOfKeyElements];
            compositeKeyElems[0] = new ConfBuf(templateName);
            compositeKeyElems[1] = new ConfBuf(deviceGroup);
            
            if(compareTempList.elem(new ConfKey(compositeKeyElems)) == null) {
                compareTempList.create(new ConfKey(compositeKeyElems));
            }
        }
        
        maapi.applyTrans(th, false);
        maapi.finishTrans(th);
     }
    
    /**
     * To get the compliance report
     * request compliance reports report **report-name** run outformat html
     * @param reportName
     * @return
     * @throws IOException
     * @throws ConfException
     */
    static String getComplianceReport(String reportName,String reportTitle,int ordinalValue,
            Maapi maapi) throws IOException, ConfException {
        logger.debug("To get the compliance report ");
        String location = null;
        
        ConfXMLParam[] params = new ConfXMLParam[2];
        params[0] = new ConfXMLParamValue(
                "ncs", "title", new ConfBuf(reportTitle));
        
        params[1] = new ConfXMLParamValue(
                "ncs", "outformat", new ConfEnumeration(ordinalValue));
        
        //request for compliance report
        ConfXMLParam[] complianceParamsOut = maapi.requestAction(
                params,
                "/ncs:compliance/reports/report{%s}/run",
                reportName);
        //return url value
        for (int i = 0; i < complianceParamsOut.length; i++) {
         if (complianceParamsOut[i].getTag().equals(Ncs._location_)) {
             location = complianceParamsOut[i].getValue().toString();
         }
        }
        
        return location;
    }

}
