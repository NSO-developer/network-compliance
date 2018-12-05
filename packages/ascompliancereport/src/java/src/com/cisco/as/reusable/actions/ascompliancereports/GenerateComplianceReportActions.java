package com.cisco.as.reusable.actions.ascompliancereports;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;



import com.cisco.as.reusable.actions.ascompliancereports.namespaces.ascompliancereport;
import com.tailf.conf.ConfEnumeration;
import com.tailf.conf.Conf;
import com.tailf.conf.ConfBuf;
import com.tailf.conf.ConfException;
import com.tailf.conf.ConfXMLParam;
import com.tailf.conf.ConfXMLParamValue;
import com.tailf.dp.DpActionTrans;
import com.tailf.dp.DpCallbackException;
import com.tailf.maapi.Maapi;
import com.tailf.maapi.MaapiUserSessionFlag;
import com.tailf.navu.NavuContainer;
import com.tailf.navu.NavuNode;


import java.net.Socket;

import com.tailf.navu.NavuContext;
import com.tailf.cdb.Cdb;
import com.tailf.cdb.CdbDBType;
import com.tailf.cdb.CdbSession;
import com.tailf.navu.NavuException;
import com.tailf.ncs.NcsMain;
import com.tailf.ncs.ns.Ncs;


/**
 * This Action will automate the on-boarding of AG/SAG/SUR devices.
 * @author root
 *
 */
public class GenerateComplianceReportActions {
    
    private static Logger LOGGER  = Logger.getLogger(GenerateComplianceReportActions.class);


    public List<ConfXMLParam> output;
    private String xmlPrefix;
    public ConfXMLParam [] params;
    
    Maapi maapi;
    NavuContainer navuContainer;
    int trans  = 0;
    String deviceGroup = "plist-devgroup";
    String reportName;
    String reportTitle;
    
    /**
     * Constuctor to initialize maapi and other default values
     * @param paramList
     */
	public GenerateComplianceReportActions(ConfXMLParam[] paramList) {
	    xmlPrefix = ascompliancereport.prefix+":";
	    this.params = paramList;
        this.output = new ArrayList<ConfXMLParam>();
        try {
            maapi = getNewMaapi();
            maapi.startUserSession("admin",
                    InetAddress.getByName("localhost"),
                    "maapi", new String[] {"admin"},
                    MaapiUserSessionFlag.PROTO_TCP);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ConfException e) {
            e.printStackTrace();
        } 
        
	}

	/**
	 * This method will receive the input request from Northbound/Service/CLI, process it to send the 
	 * requested report for device compliance.
	 * @param trans
	 *    - Transaction
	 * @param params
	 *    - params from the request
	 * @return
	 * @throws DpCallbackException 
	 */
	public String generateDcComplianceReport(DpActionTrans trans, ConfXMLParam[] params) throws DpCallbackException {
	   
	    try {
	       	        
	        Document doc = ConfXMLParam.toDOM(params);
            doc.getDocumentElement().normalize();
            
            String jobName = null;
            String deviceList = null;
            String templateList = null;
            String makeCompliant = null;
            String outformat = null;
            int ordinalValue = -1;
            //Toretrieve the information from xml
            for(int i =0; i < params.length;i++){
                if(ascompliancereport._jobname_.equals(params[i].getTag())){
                    jobName =  params[i].getValue().toString();
                }else if(ascompliancereport._report_title_.equals(params[i].getTag())){
                    reportTitle =  params[i].getValue().toString(); 
                }else if(ascompliancereport._report_name_.equals(params[i].getTag())){
                    reportName =  params[i].getValue().toString(); 
                }    
                
                else if(ascompliancereport._device_name_.equals(params[i].getTag())) {                   
                    if(deviceList == null)
                        deviceList =  params[i].getValue().toString();
                    else
                        deviceList =  deviceList + " " + params[i].getValue().toString();
                    
                }else if(ascompliancereport._template_name_.equals(params[i].getTag())){
                    if(templateList == null)
                        templateList =  params[i].getValue().toString();
                    else
                        templateList =  templateList + " " + params[i].getValue().toString();
                }     

                else if(ascompliancereport._make_complaint_.equals(params[i].getTag())){
                    makeCompliant =  params[i].getValue().toString();
                }else if(ascompliancereport._outformat_.equals(params[i].getTag())){
                    outformat =  params[i].getValue().toString();
                    ordinalValue = getEnumStringParam(params,ascompliancereport._outformat_);
                    LOGGER.debug("ordinalValue " +ordinalValue);
                }
            }
	       
            //validations to check null values
            if(jobName == null || jobName.trim().length() == 0){
            	throw new DpCallbackException("Mandatory field Job Name is missing. Please provide the same");
            }
            
            if(deviceList == null || deviceList.trim().length() == 0){
            	throw new DpCallbackException("Mandatory field Devices is missing. Please provide the same");
            }
            
            if(templateList == null || templateList.trim().length() == 0){
            	throw new DpCallbackException("Mandatory field Templates is missing. Please provide the same");
            }
	       
	        String[] templateArray =  templateList.split(" ");
            LOGGER.debug("jobName " +jobName + " reportName " + reportName + " deviceList " + deviceList
                    + " templateList " + templateList + " makeCompliant " + makeCompliant + " reportTitle "+ reportTitle );
            //validate whether template exist in device template path
            validateTemplates(deviceList,templateArray);
            //adds devices to the device group
            GenerateComplianceCommon.addDevicesToGroup(deviceList,deviceGroup,maapi);
            //sets the report name after comparing
            GenerateComplianceCommon.setReportName(templateArray,deviceGroup,reportName,maapi);
            //apply the templates to devices when flag is true
            makeCompliant(makeCompliant,templateArray);
            //get the report 
            String location = GenerateComplianceCommon.getComplianceReport(reportName,reportTitle,ordinalValue,maapi);
            
            
            
            
            //set the output paramaters
           addStrElem(ascompliancereport.hash,
                    ascompliancereport._jobname,jobName, output);
            addStrElem(ascompliancereport.hash,
                   ascompliancereport._url,location, output);
             
        } catch (IOException e) {
           throw new DpCallbackException(e.getMessage());
        } catch (ConfException e) {
            throw new DpCallbackException(e.getMessage());
        } finally {
        	closeMaapiSock(maapi);
        }
            
      
        return "Success";
	}
	
	/**
	 * This method validate the input templates whether it exist in NSO device template path
	 * @param templateArray
	 * @throws IOException
	 * @throws ConfException
	 */
    private void validateTemplates(String deviceList,String[] templateArray) throws IOException, ConfException {
        
       if(deviceList == null){
           throw new DpCallbackException("Please provide devices list" ); 
       }
       
       if(templateArray.length == 0){
           throw new DpCallbackException("Please provide templates list" ); 
       }
       
       int th = maapi.startTrans(Conf.DB_RUNNING, Conf.MODE_READ_WRITE);
       NavuContainer ncsContainer = getNcsContainer(maapi, th);
       //check template existence
       for(String template:templateArray){
           String expression = "/devices/template[name='"+template+"']";
           Collection<NavuNode> nodes = readDataFromCDBUsingXpathAndNcsContainer(ncsContainer, expression);
           if(nodes.size()==0) {
               throw new DpCallbackException("Template "+template +" does not exists in cdb" );
           }
       }
        
    }
    
    /**
     * This function will read data from CDB using xpath
     * 
     * @param xPathExpression
     *            xpath expression to search data
     * @return Collection<NavuNode> 
     *           Collection of Navu nodes for searched data
     * @throws ConfException
     *             
     */

    public static Collection<NavuNode> readDataFromCDBUsingXpathAndNcsContainer(NavuContainer ncsContainer,
            String xPathExpression) throws ConfException {
        Collection<NavuNode> nodes = ncsContainer.xPathSelect(xPathExpression);
        return nodes;
    }
    
    
    /**
     * This method will apply the template to the deviceGroup
     * 
     * request devices device-group **device-group-name** apply-template template-name **template-name**
     * 
     * @param makeCompliant
     * @param templateArray
     * @throws IOException
     * @throws ConfException
     */
   private void makeCompliant(String makeCompliant, String[] templateArray) throws IOException, ConfException {
       LOGGER.debug("MakeCompliant the device group with given templates ");
       //when flag is true then apply the templates to the devices.
       if(makeCompliant.equalsIgnoreCase("true")){
           for(String template: templateArray) {
            ConfXMLParam[] params = new ConfXMLParam[]{new ConfXMLParamValue("ncs",
                    "template-name", new ConfBuf(template))};
            maapi.requestAction(
                    params,
                    "/ncs:devices/device-group{%s}/apply-template",
                    deviceGroup);
            LOGGER.debug("Made Compliant with the template " + template);
           }
       }
   }
   
   
   /**
    * To get the enumeration value 
    * @param params
    *  - input parameters
    * @param name
    *   - name of the tag
    * @return
    *    - return index of enum
    */
   public static int getEnumStringParam(ConfXMLParam[] params, String name) {
       for (int i = 0; i < params.length; i++) {

           if (params[i].getTag().equals(name)) {
               ConfEnumeration confEnum = (ConfEnumeration) params[i].getValue();
               return confEnum.getOrdinalValue();
           }
       }
       return -1;
   }
   
   /**
    * Add output element to response
    * @param serviceHashVal
    *  - service hash value
    * @param hashVal
    *  - hash value
    * @param val
    *  - value
    * @param output
    *  - output
    */
   public static void addStrElem(int serviceHashVal, int hashVal, String val, List<ConfXMLParam> output) {
       LOGGER.debug("Inside addStrElem method.......");
       if (val.equals(null)) {
           LOGGER.debug("Value is null");
           return;
       }
       try {

           output.add(new ConfXMLParamValue(serviceHashVal, hashVal, new ConfBuf(val)));
       } catch (Exception e) {

           LOGGER.error("Error setting addStrElem in output message", e);
       }
   }
   
   ////
   //// NCS/MAAPI/CDB Convenience routines
   ////
   /**
    * To get the new NcsSock
    * @return
    * @throws IOException
    */
   public static Socket newNcsSock()
           throws IOException
       {
           NcsMain ncsServ = NcsMain.getInstance();
           return new Socket(ncsServ.getNcsHost(),ncsServ.getNcsPort());
       }
   
  
   /**
    * To get the new CDB operational session
    * @param caller
    * @return
    * @throws IOException
    * @throws ConfException
    */
   public static CdbSession getNewCdbOperSession(Class caller)
           throws IOException, ConfException
       {
           Cdb cdb = new Cdb(caller.getName()+"-cdb-operational", newNcsSock());
           return cdb.startSession(CdbDBType.CDB_OPERATIONAL);
       }
   /**
    * To get the new CDB operational connection
    * @param caller
    * @return
    * @throws IOException
    * @throws ConfException
    */
   public static Cdb getNewCdbConnection(String caller)
           throws IOException, ConfException
       {
       
       
        Cdb cdb = new Cdb(caller+"-cdb-operational",newNcsSock());
        return cdb;
       }
   /**
    * To close cdb connection
    * @param cdb
    */
   public static void closeCdbconnection(Cdb cdb) {
       if (cdb!=null) {
           Socket ncsSock = cdb.getSocket();
           try {
               cdb.close();
           } catch (Exception e) {
               if (LOGGER!=null)
                   LOGGER.warn("CDB is not closed: "+e.getMessage());
           }
           closeNcsSock(ncsSock);
       }
   }
   
   /**
    * To close the maapi socket
    * @param maapi
    */
   public static void closeMaapiSock(Maapi maapi) {
       if (maapi!=null)
           closeNcsSock(maapi.getSocket());
   }
   
   /**
    * To close the cdb session
    * @param session
    */
   public static void closeCdbSession(CdbSession session) {
       if (session!=null) {
           Socket ncsSock = session.getCdb().getSocket();
           try {
               session.endSession();
           } catch (Exception e) {
               if (LOGGER!=null)
                   LOGGER.warn("CDB session is not closed: "+e.getMessage());
           }
           closeNcsSock(ncsSock);
       }
   }
   /**
    * To close the NCS socket
    * @param ncsSock
    */
   public static void closeNcsSock(Socket ncsSock) {
       if (ncsSock != null)
           try {
               ncsSock.close();
           } catch (Exception e) {
               if (LOGGER != null)
                   LOGGER.warn("NCS socket is not closed: " + e.getMessage());
           }
   }


   
   /**
    * To get the new maapi
    * @return
    * @throws IOException
    * @throws ConfException
    */
   public static Maapi getNewMaapi() throws IOException, ConfException {
       return new Maapi(newNcsSock());
   }
   
   /**
    * To start maapi write transaction
    * @param maapi
    * @param createUserSession
    * @return
    * @throws IOException
    * @throws ConfException
    */
   public static int openMaapiWrite(Maapi maapi, boolean createUserSession)
           throws IOException, ConfException {
       maapi.startUserSession("admin", InetAddress.getLocalHost(), "maapi",
               new String[] { "admin" }, MaapiUserSessionFlag.PROTO_TCP);
       
       int th = maapi.startTrans(Conf.DB_RUNNING, Conf.MODE_READ_WRITE);

       return th;
   }

   /**
    * To open a maapi object
    * @param maapi
    * @return
    * @throws IOException
    * @throws ConfException
    */
   public static int openMaapiWrite(Maapi maapi) throws IOException,
           ConfException {
       return openMaapiWrite(maapi, true);

   }

   /**
    * To get the transaction of maapi
    * @param maapi
    * @return
    * @throws IOException
    * @throws ConfException
    */
   public static int openMaapiWriteExistingUserSession(Maapi maapi)
   // usid is existing user session id
           throws IOException, ConfException {
       int th = maapi.startTrans(Conf.DB_RUNNING, Conf.MODE_READ_WRITE);

       return th;
   }


   /***
    * To open the operational cdb session.
    * @return
    * @throws IOException
    * @throws ConfException
    */
   public static CdbSession openSession() throws IOException, ConfException {
       CdbSession wsess = null;
       try {
           Cdb cdb = new Cdb("cdb-operational", newNcsSock());
           wsess = cdb.startSession(CdbDBType.CDB_OPERATIONAL);

       } catch (IOException ioException) {
           throw ioException;
       } catch (ConfException confException) {
           throw confException;
       }
       return wsess;
   }

   /**
    * To close Cdb Session
    * @param cdbSession
    * @return
    * @throws IOException
    */
   public static boolean closeSession(CdbSession cdbSession)
           throws IOException {
       boolean result = false;
       if (cdbSession != null) {
           Socket ncsSock = cdbSession.getCdb().getSocket();
           try {
               cdbSession.endSession();
               result = true;
           } catch (Exception e) {
               System.out.println("CDB session is not closed: "
                       + e.getMessage());
           }
           closeNcsSock(ncsSock);
       }
       return result;
   }   
   
   
     /**
    * Closes Maapi connection socket
    * @param th
    *   TransactionId
    * @throws ConfException
    */
   public static void finishTransaction(Maapi maapi, int th) throws ConfException
   {
            
        try {
            maapi.finishTrans(th);
               
        } catch (Exception e) {

               LOGGER.error("Generic Exception while closing transaction : "
                       + e.toString());
               try {
                   maapi.abortTrans(th);
               } catch (IOException e1) {
                   e1.printStackTrace();
                   LOGGER.error("ServicePreCheck - IO Exception: "
                           + e1.toString());
               }
               
       }
   }   
   /**
    * This method gets the NCS container provided maapi and transaction id
    * @param maapi
    *   - Maapi instance
    * @param th
    *   -Transaction Id
    * @return
    *   - Instance of Navu Container
    */
   public static NavuContainer getNcsContainer(Maapi maapi, int th) {

       NavuContext ctx = new NavuContext(maapi, th);
       try {
           return new NavuContainer(ctx).container(new Ncs().hash());
       } catch (NavuException e) {
           e.printStackTrace();
       }
       return null;
   }
    
}
