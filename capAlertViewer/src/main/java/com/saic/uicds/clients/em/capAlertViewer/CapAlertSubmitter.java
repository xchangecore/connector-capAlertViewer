package com.saic.uicds.clients.em.capAlertViewer;

import java.lang.reflect.Method;

import gov.ucore.ucore.x20.DigestMetadataType;
import gov.ucore.ucore.x20.DigestType;
import gov.ucore.ucore.x20.EventType;

import x1.oasisNamesTcEmergencyCap1.AlertDocument;
import x1.oasisNamesTcEmergencyCap1.AlertDocument.Alert;
import x1.oasisNamesTcEmergencyCap1.AlertDocument.Alert.Info;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import com.saic.uicds.clients.em.async.UicdsCore;
import com.saic.uicds.clients.em.async.UicdsIncident;
import com.saic.uicds.clients.em.async.UicdsWorkProduct;


import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
//import org.jdom.Document;
//import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.uicds.incident.IncidentDocument;
import org.uicds.incident.UICDSIncidentType;

import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import x0.messageStructure1.DataOwnerMetadataType;
import x0.messageStructure1.PackageMetadataType;
import x0.messageStructure1.StructuredPayloadType;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.base.IdentifierType;
import com.saic.precis.x2009.x06.base.ProcessingStateType;
import com.saic.precis.x2009.x06.base.ProcessingStatusType;
import com.saic.precis.x2009.x06.base.ProcessingStateType.Enum;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;
import com.saic.precis.x2009.x06.base.NamespaceMapItemType;
import com.saic.precis.x2009.x06.base.NamespaceMapType;
import com.saic.uicds.clients.util.WebServiceClient;

import org.uicds.alertService.GetListOfAlertsRequestDocument;
import org.uicds.alertService.GetListOfAlertsRequestDocument.GetListOfAlertsRequest;
import org.uicds.alertService.GetListOfAlertsResponseDocument;
import org.uicds.alertService.GetListOfAlertsResponseDocument.GetListOfAlertsResponse;

import org.uicds.incidentManagementService.GetIncidentListRequestDocument;
import org.uicds.incidentManagementService.GetIncidentListResponseDocument;
import org.uicds.incidentManagementService.GetIncidentListResponseDocument.GetIncidentListResponse;

import org.uicds.incidentManagementService.CreateIncidentFromCapRequestDocument;
import org.uicds.incidentManagementService.CreateIncidentFromCapRequestDocument.CreateIncidentFromCapRequest;
import org.uicds.incidentManagementService.CreateIncidentFromCapResponseDocument;

import org.uicds.workProductService.WorkProductListDocument.WorkProductList;

import org.uicds.workProductService.GetProductRequestDocument;
import org.uicds.workProductService.GetProductResponseDocument;

import org.uicds.workProductService.AssociateWorkProductToInterestGroupRequestDocument;
import org.uicds.workProductService.AssociateWorkProductToInterestGroupRequestDocument.AssociateWorkProductToInterestGroupRequest;
import org.uicds.workProductService.AssociateWorkProductToInterestGroupResponseDocument;
import org.uicds.workProductService.AssociateWorkProductToInterestGroupResponseDocument.AssociateWorkProductToInterestGroupResponse;

import x0.messageStructure1.StructuredPayloadType;
import javax.xml.namespace.QName;
import com.saic.uicds.clients.util.Common;


public class CapAlertSubmitter {

    Logger log = LoggerFactory.getLogger(this.getClass());

    //private WebServiceClient webServiceClient;
    private UicdsCore uicdsCore;
    
    private String uicdsID;

    private WorkProduct workProduct;
    
    private HashMap<String, WorkProduct> incidents = 
        new HashMap<String, WorkProduct>();

    private HashMap<String, WorkProduct> alerts = 
        new HashMap<String, WorkProduct>();

    public void setUicdsCore(UicdsCore core) {
        uicdsCore = core;
    }
	
    public String[][] getListOfAlerts() {
		System.out.println("in CapAlertSubmitter::getListOfAlerts()");

		System.out.println("keyset=" + alerts.keySet().toString());
        WorkProductList list = getAlertWorkProduct();


		String dataValues[][];
		int size = 0;
		size = alerts.size();
        System.out.println("number of alerts = " + size);
		dataValues = new String[size][2];
		

		int i=0;
        for (String alertID : alerts.keySet()) {
            
            AlertDocument doc = getAlertDocFromWP(alerts.get(alertID));

            int sizeOfInfo = doc.getAlert().sizeOfInfoArray();
            if (sizeOfInfo > 0) {
                Info[] infos = doc.getAlert().getInfoArray();
                dataValues[i][0]=infos[0].getEvent();
                dataValues[i][1]=infos[0].getHeadline();
            }
            i++;
        }
        return dataValues;
    }
	

    private WorkProductList getAlertWorkProduct()
    {
        System.out.println("in CapAlertSubmitter::getAlertWorkProduct()");

        GetListOfAlertsRequestDocument requestDoc = 
            GetListOfAlertsRequestDocument.Factory.newInstance();
        GetListOfAlertsRequest request = 
            requestDoc.addNewGetListOfAlertsRequest();
        request.setQueryString("");

        NamespaceMapType map = NamespaceMapType.Factory.newInstance();
        NamespaceMapItemType mapItem = map.addNewItem();
        // mapItem.setPrefix("urn");
        // mapItem.setURI("oasis:names:tc:emergency:cap:1.1");

        request.setNamespaceMap(map);

        GetListOfAlertsResponseDocument responseDoc = 
            (GetListOfAlertsResponseDocument) uicdsCore.
                marshalSendAndReceive(requestDoc);

        //System.out.println("responseDoc= " + responseDoc);

        GetListOfAlertsResponse response = responseDoc.getGetListOfAlertsResponse();

        WorkProductList list = response.getWorkProductList();
        for (WorkProduct wp:list.getWorkProductArray()) {
            // System.out.println("product=\n" + product);

            IdentificationType id = Common.getIdentificationElement(wp);
            // String wpId = id.getIdentifier().getStringValue();
            WorkProduct newWP = getWorkProduct(id);

            if (newWP.sizeOfStructuredPayloadArray() != 0) {
                AlertDocument doc = getAlertDocFromWP(newWP);

                int sizeOfInfo = doc.getAlert().sizeOfInfoArray();
                if (sizeOfInfo > 0) {
                    Info[] infos = doc.getAlert().getInfoArray();
                    alerts.put(infos[0].getHeadline(), newWP);
                }
            }
        }

        return list;
    }

    private static AlertDocument getAlertDocFromWP(WorkProduct workproduct)
    {
        //System.out.println("in CapAlertSubmitter::getAlertDocFromWP()");
        StructuredPayloadType payload = workproduct.getStructuredPayloadArray(0);
        AlertDocument doc = null;

        XmlObject[] orgs = payload.selectChildren(new QName(
            AlertDocument.type.getDocumentElementName().getNamespaceURI(), 
            AlertDocument.type.getDocumentElementName().getLocalPart()));

        try {
            doc = AlertDocument.Factory.parse(orgs[0].getDomNode());
        }
        catch(XmlException e) {
            e.printStackTrace();
        }

        return doc;
    }


    private WorkProduct getWorkProduct(IdentificationType workProductID)
    {
        // System.out.println("in CapAlertSubmitter::getWorkProduct()");

        GetProductRequestDocument request = 
            GetProductRequestDocument.Factory.newInstance();
        request.addNewGetProductRequest().setWorkProductIdentification(workProductID);
        try {
            GetProductResponseDocument response = 
                (GetProductResponseDocument) uicdsCore.marshalSendAndReceive(request);
            WorkProduct wp = response.getGetProductResponse().getWorkProduct();

            return response.getGetProductResponse().getWorkProduct();
        } catch (ClassCastException e) {
            log.error("Error casting response to GetProductResponseDocument");
            return null;
        }
    }
	

	public String assocWorkProductToIncident(WorkProduct wp, String incidentID) {
		try {
			//log.info("the incident you want to attach to is:"+
            //         incidents.get(incidentID).getIncidentDocument().toString());


		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Accepted";
	}


	public List getListOfAlertIncidents() {
        System.out.println("in CapAlertSubmitter::getListOfAlertIncidents()");
        ArrayList list = new ArrayList();
        GetIncidentListRequestDocument requestDoc = 
            GetIncidentListRequestDocument.Factory.newInstance();
        requestDoc.addNewGetIncidentListRequest();

        GetIncidentListResponseDocument responseDoc =
            (GetIncidentListResponseDocument) uicdsCore.marshalSendAndReceive(requestDoc);

        GetIncidentListResponse response = responseDoc.getGetIncidentListResponse();
        WorkProductList wpList = response.getWorkProductList();

        for (WorkProduct wp:wpList.getWorkProductArray()) {
            String incident = findAlertIncident(wp);
            if (incident != null) {
                incidents.put(incident, wp);            
                list.add(incident);
            }
        }

        return list;
    }

    private String findAlertIncident(WorkProduct wp)
    {
        // for each alert in the list, find the one that match the incident

        // System.out.println("in CapAlertSubmitter::findAlertIncident()");

        String identifier = "";
        String descriptor = "";
        DigestType digest = Common.getDigest(wp);
        ArrayList eventList = Common.getEvents(digest);
        for (int i=0; i<eventList.size(); i++) {
            EventType event = (EventType) eventList.get(i);
            descriptor = event.getDescriptor().stringValue();
            // System.out.println("\nlooking for descriptor = " + descriptor);

            if (event.sizeOfIdentifierArray() > 0) {
                identifier = event.getIdentifierArray(0).stringValue();
                return identifier;
                //System.out.println("identifier = " + identifier);
            }
        }

        /*
        int i=0;
        for (String alertID : alerts.keySet()) {
            AlertDocument doc = getAlertDocFromWP(alerts.get(alertID));
            int sizeOfInfo = doc.getAlert().sizeOfInfoArray();
            if (sizeOfInfo > 0) {
                Info[] infos = doc.getAlert().getInfoArray();
                // System.out.println("description = " + infos[0].getDescription());
                String description = infos[0].getDescription();
                if (descriptor.equals(description)) {
                    log.info("found the incident for alert: " + identifier);
                    return identifier;
                }
            }
            i++;
        }
       */
        return null;
    }

    public boolean createAlert(String alertID)
    {
        WorkProduct wp = alerts.get(alertID);
        if (wp != null) {
            AlertDocument alertDoc = getAlertDocFromWP(wp);
            Alert alert = alertDoc.getAlert();
            int sizeOfInfo = alert.sizeOfInfoArray();
            if (sizeOfInfo > 0) {
                Info[] infos = alert.getInfoArray();
                String incidentName = infos[0].getHeadline();
                if (incidentName.length() > 40) {
                    incidentName = incidentName.substring(0, 40);
                }
                infos[0].setEvent(incidentName);


            }
            return createIncidentFromAlert(alertDoc);
        }

        return false;

    }

    public boolean associateAlert(String alertID, String incidentID)
    {
        WorkProduct alertWp = alerts.get(alertID);
        WorkProduct incidentWp = incidents.get(incidentID);
        if (alertWp != null && incidentWp != null) {
            return associateAlertWithIncident(alertWp, incidentWp);
         }

        return false;

    }

    public boolean createIncidentFromAlert(AlertDocument alertDoc)
    {
        boolean accepted = false;
        CreateIncidentFromCapRequestDocument requestDoc = 
            CreateIncidentFromCapRequestDocument.Factory.newInstance();
        CreateIncidentFromCapRequest request = 
            requestDoc.addNewCreateIncidentFromCapRequest();
        request.setAlert(alertDoc.getAlert());

        CreateIncidentFromCapResponseDocument responseDoc = 
            (CreateIncidentFromCapResponseDocument) uicdsCore
            .marshalSendAndReceive(requestDoc);

        if (responseDoc.getCreateIncidentFromCapResponse()
            .getWorkProductPublicationResponse()
            .getWorkProductProcessingStatus().getStatus() == 
                ProcessingStateType.ACCEPTED) {
            log.info("Incident is created.");
            accepted = true;

        } else {
            log.error("Incident creation not accepted: "
                + responseDoc.getCreateIncidentFromCapResponse()
                         .getWorkProductPublicationResponse()
                         .getWorkProductProcessingStatus().getStatus());
        }

        return accepted;

    }

    public boolean associateAlertWithIncident(WorkProduct alertWp, WorkProduct incidentWp)
    {
        boolean accepted = false;
        AssociateWorkProductToInterestGroupRequestDocument requestDoc = 
            AssociateWorkProductToInterestGroupRequestDocument.Factory.newInstance();

        AssociateWorkProductToInterestGroupRequest request = 
            requestDoc.addNewAssociateWorkProductToInterestGroupRequest();

        
        IdentificationType identType = Common.getIdentificationElement(alertWp);

        IdentifierType idType = IdentifierType.Factory.newInstance();
        idType.setStringValue(identType.getIdentifier().getStringValue());

        request.setWorkProductID(idType);

        IdentifierType igid = 
            Common.getFirstAssociatedInterestGroup(incidentWp);
        request.setIncidentID(igid);

        AssociateWorkProductToInterestGroupResponseDocument responseDoc =
            (AssociateWorkProductToInterestGroupResponseDocument) uicdsCore
            .marshalSendAndReceive(requestDoc);

        XmlObject obj = responseDoc.getAssociateWorkProductToInterestGroupResponse();

        System.out.println(obj.getClass().getName());
        if (obj.getClass().getName().contains("AssociateWorkProductToInterestGroupResponse")) {
            accepted = true;
        }
        return accepted;
    }	
	
}
