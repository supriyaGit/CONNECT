/*
 * Copyright (c) 2012, United States Government, as represented by the Secretary of Health and Human Services. 
 * All rights reserved. 
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met: 
 *     * Redistributions of source code must retain the above 
 *       copyright notice, this list of conditions and the following disclaimer. 
 *     * Redistributions in binary form must reproduce the above copyright 
 *       notice, this list of conditions and the following disclaimer in the documentation 
 *       and/or other materials provided with the distribution. 
 *     * Neither the name of the United States Government nor the 
 *       names of its contributors may be used to endorse or promote products 
 *       derived from this software without specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE UNITED STATES GOVERNMENT BE LIABLE FOR ANY 
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */
package gov.hhs.fha.nhinc.docquery.entity;

import gov.hhs.fha.nhinc.gateway.executorservice.NhinCallableRequest;
import gov.hhs.fha.nhinc.gateway.executorservice.NhinTaskExecutor;

import gov.hhs.fha.nhinc.orchestration.OutboundDelegate;
import gov.hhs.fha.nhinc.orchestration.OutboundResponseProcessor;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.bind.JAXBContext;
import gov.hhs.fha.nhinc.transform.marshallers.JAXBContextHandler;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import gov.hhs.fha.nhinc.common.auditlog.AdhocQueryResponseMessageType;
import gov.hhs.fha.nhinc.common.nhinccommon.AssertionType;
import gov.hhs.fha.nhinc.common.nhinccommon.HomeCommunityType;
import gov.hhs.fha.nhinc.common.nhinccommon.NhinTargetCommunitiesType;
import gov.hhs.fha.nhinc.common.nhinccommon.NhinTargetSystemType;
import gov.hhs.fha.nhinc.common.nhinccommon.QualifiedSubjectIdentifierType;
import gov.hhs.fha.nhinc.gateway.executorservice.ExecutorServiceHelper;
import gov.hhs.fha.nhinc.docquery.DocQueryAuditLog;
import gov.hhs.fha.nhinc.nhinclib.NhincConstants;
import gov.hhs.fha.nhinc.nhinclib.NullChecker;
import gov.hhs.fha.nhinc.properties.PropertyAccessor;
import gov.hhs.fha.nhinc.common.nhinccommonentity.RespondingGatewayCrossGatewayQuerySecuredRequestType;
import gov.hhs.fha.nhinc.connectmgr.ConnectionManagerCache;
import gov.hhs.fha.nhinc.connectmgr.NhinEndpointManager;
import gov.hhs.fha.nhinc.connectmgr.UrlInfo;
import gov.hhs.fha.nhinc.gateway.executorservice.ExecutorServiceHelper;
import gov.hhs.fha.nhinc.util.HomeCommunityMap;
import gov.hhs.fha.nhinc.common.nhinccommonadapter.CheckPolicyRequestType;
import gov.hhs.fha.nhinc.common.nhinccommonadapter.CheckPolicyResponseType;
import gov.hhs.fha.nhinc.policyengine.PolicyEngineChecker;
import gov.hhs.fha.nhinc.policyengine.adapter.proxy.PolicyEngineProxy;
import gov.hhs.fha.nhinc.policyengine.adapter.proxy.PolicyEngineProxyObjectFactory;
import gov.hhs.fha.nhinc.common.eventcommon.AdhocQueryRequestEventType;
import gov.hhs.fha.nhinc.common.eventcommon.AdhocQueryRequestMessageType;
import gov.hhs.fha.nhinc.util.format.PatientIdFormatUtil;
import gov.hhs.fha.nhinc.transform.document.DocumentTransformConstants;

import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.AdhocQueryType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotListType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryErrorList;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ValueListType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Orchestrates the Entity (i.e. from Adapter) DocQuery transaction
 * 
 * @author Neil Webb
 * @author paul.eftis (updated 10/15/2011 to implement new concurrent request handling/fanout)
 * @author paul.eftis (updated 01/15/2011 to implement new multispec delegate)
 */
public class EntityDocQueryOrchImpl {

    private Log log = null;
    private ExecutorService regularExecutor = null;
    private ExecutorService largejobExecutor = null;

    private static final String ContextPath = "oasis.names.tc.ebxml_regrep.xsd.rim._3";


    /**
     * Add default constructor that is used by test cases Note that implementations should always use constructor that
     * takes the executor services as input
     */
    public EntityDocQueryOrchImpl() {
        // for this default test case, we just create default executor services
        // with a thread pool of 1
        regularExecutor = Executors.newFixedThreadPool(1);
        largejobExecutor = Executors.newFixedThreadPool(1);
    }

    /**
     * We construct the orch impl class with references to both executor services that could be used for this particular
     * orchestration instance. Determination of which executor service to use (largejob or regular) is based on the size
     * of the correlationsResult and configs
     */
    public EntityDocQueryOrchImpl(ExecutorService e, ExecutorService le) {
        log = createLogger();
        regularExecutor = e;
        largejobExecutor = le;
    }

    protected Log createLogger() {
        return ((log != null) ? log : LogFactory.getLog(getClass()));
    }

    /**
     * 
     * @param adhocQueryRequest
     * @param assertion
     * @param targets
     * @return <code>AdhocQueryResponse</code>
     */
    @SuppressWarnings("static-access")
    public AdhocQueryResponse respondingGatewayCrossGatewayQuery(AdhocQueryRequest adhocQueryRequest,
            AssertionType assertion, NhinTargetCommunitiesType targets) {
        log.debug("EntityDocQueryOrchImpl.respondingGatewayCrossGatewayQuery...");

        AdhocQueryResponse response = new AdhocQueryResponse();
        
        // quick rig for testing to switch between a0 and a1
        // note that a0 and a1 would be handled by different methods if they were different
        boolean responseIsSpecA0 = true;
        NhinEndpointManager nem = new NhinEndpointManager();
        NhincConstants.GATEWAY_API_LEVEL gatewayLevel = nem.getApiVersion(
                getLocalHomeCommunityId(), NhincConstants.NHIN_SERVICE_NAMES.DOCUMENT_QUERY);
        switch (gatewayLevel) {
        case LEVEL_g0: {
            responseIsSpecA0 = true;
            break;
        }
        case LEVEL_g1: {
            responseIsSpecA0 = false;
            break;
        }
        default: {
            responseIsSpecA0 = true;
            break;
        }
        }
        log.debug("EntityDocQueryOrchImpl set responseIsSpecA0=" + responseIsSpecA0);

        List<UrlInfo> urlInfoList = null;
        boolean isTargeted = false;

        // audit initial request
        DocQueryAuditLog auditLog = new DocQueryAuditLog();
        RespondingGatewayCrossGatewayQuerySecuredRequestType request = new RespondingGatewayCrossGatewayQuerySecuredRequestType();
        request.setAdhocQueryRequest(adhocQueryRequest);
        request.setNhinTargetCommunities(targets);
        String targetHomeCommunityId = HomeCommunityMap.getCommunityIdFromTargetCommunities(targets);
        auditInitialEntityRequest(request, assertion, auditLog, targetHomeCommunityId);

        try {
            if (targets != null && NullChecker.isNotNullish(targets.getNhinTargetCommunity())) {
                isTargeted = true;
            }

            // Obtain all the URLs for the targets being sent to
            try {
                urlInfoList = ConnectionManagerCache.getInstance().getEndpointURLFromNhinTargetCommunities(targets,
                        NhincConstants.DOC_QUERY_SERVICE_NAME);
            } catch (Exception ex) {
                log.error("EntityDocQueryOrchImpl Failed to obtain target URLs", ex);
            }

            // Validate that the message is not null
            if (adhocQueryRequest != null && adhocQueryRequest.getAdhocQuery() != null
                    && NullChecker.isNotNullish(adhocQueryRequest.getAdhocQuery().getSlot())) {
                List<SlotType1> slotList = adhocQueryRequest.getAdhocQuery().getSlot();
                String localAA = new EntityDocQueryHelper().getLocalAssigningAuthority(slotList);
                String uniquePatientId = new EntityDocQueryHelper().getUniquePatientId(slotList);
                log.debug("EntityDocQueryOrchImpl uniquePatientId: " + uniquePatientId + " and localAA=" + localAA);

                List<QualifiedSubjectIdentifierType> correlationsResult = new EntityDocQueryHelper()
                        .retreiveCorrelations(slotList, urlInfoList, assertion, isTargeted, getLocalHomeCommunityId());

                if (NullChecker.isNotNullish(correlationsResult)) {

                    List<NhinCallableRequest<OutboundDocQueryOrchestratable>> callableList = new ArrayList<NhinCallableRequest<OutboundDocQueryOrchestratable>>();
                    String transactionId = (UUID.randomUUID()).toString();

                    // we hold the error messages for any failed policy checks in policyErrList
                    RegistryErrorList policyErrList = new RegistryErrorList();

                    for (QualifiedSubjectIdentifierType identifier : correlationsResult) {
                        log.debug("EntityDocQueryOrchImpl correlated target aaid=" + identifier.getAssigningAuthorityIdentifier());
                        NhinTargetSystemType target = new NhinTargetSystemType();

                        HomeCommunityType targetCommunity = new EntityDocQueryHelper().lookupHomeCommunityId(
                                identifier.getAssigningAuthorityIdentifier(), localAA, getLocalHomeCommunityId());
                        String sTargetHomeCommunityId = null;
                        if (targetCommunity != null) {
                            target.setHomeCommunity(targetCommunity);
                            sTargetHomeCommunityId = targetCommunity.getHomeCommunityId();
                        }                        
                        log.debug("EntityDocQueryOrchImpl correlated target hcid=" + targetCommunity.getHomeCommunityId());

                        if (isValidPolicy(adhocQueryRequest, assertion, targetCommunity)) {
                            OutboundDelegate nd = new OutboundDocQueryDelegate();
                            OutboundResponseProcessor np = null;
                            if (responseIsSpecA0) {
                                np = new OutboundDocQueryProcessor(NhincConstants.GATEWAY_API_LEVEL.LEVEL_g0);
                            } else {
                                np = new OutboundDocQueryProcessor(NhincConstants.GATEWAY_API_LEVEL.LEVEL_g1);
                            }

                            // clone the request
                            AdhocQueryRequest clonedRequest = cloneRequest(adhocQueryRequest);
                            // replace the patient id slot
                            String formattedPatientId = PatientIdFormatUtil.hl7EncodePatientId(
                                    identifier.getSubjectIdentifier(), identifier.getAssigningAuthorityIdentifier());
                            List<SlotType1> slotType1 = clonedRequest.getAdhocQuery().getSlot();
                            Iterator<SlotType1> iterSlotType1 = slotType1.iterator();
                            while (iterSlotType1.hasNext()) {
                                SlotType1 slot = iterSlotType1.next();
                                if ((slot.getName() != null)
                                        && (slot.getName().equals(DocumentTransformConstants.EBXML_DOCENTRY_PATIENT_ID))) {
                                    ValueListType slotValueList = new ValueListType();
                                    slotValueList.getValue().add(formattedPatientId);
                                    slot.setValueList(slotValueList);
                                }
                            }
                            // set the home community id to the target hcid
                            if (NullChecker.isNotNullish(sTargetHomeCommunityId)) {
                                clonedRequest.getAdhocQuery().setHome(sTargetHomeCommunityId);
                            }
                            
                            OutboundDocQueryOrchestratable message = new OutboundDocQueryOrchestratable(nd, np, null,
                                    null, assertion, NhincConstants.DOC_QUERY_SERVICE_NAME, target, 
                                    clonedRequest);
                            callableList.add(new NhinCallableRequest<OutboundDocQueryOrchestratable>(message));

                            log.debug("EntityDocQueryOrchImpl added NhinCallableRequest" + " for hcid="
                                    + target.getHomeCommunity().getHomeCommunityId() + " with formattedPatientId=" + formattedPatientId);
                        } else {
                            RegistryError regErr = new RegistryError();
                            regErr.setCodeContext("Policy Check Failed for homeId="
                                    + target.getHomeCommunity().getHomeCommunityId() + " and aaId="
                                    + identifier.getAssigningAuthorityIdentifier());
                            regErr.setErrorCode("XDSRepositoryError");
                            regErr.setSeverity("Error");
                            policyErrList.getRegistryError().add(regErr);
                        }
                    }

                    // note that if responseIsSpecA0 taskexecutor is set to return OutboundDocQueryOrchestratable_a0
                    // else taskexecutor set to return OutboundDocQueryOrchestratable_a1
                    OutboundDocQueryOrchestratable_a0 orchResponse_g0 = null;
                    OutboundDocQueryOrchestratable_a1 orchResponse_g1 = null;                    
                    if (responseIsSpecA0) {
                        if (callableList.size() > 0) {
                            NhinTaskExecutor<OutboundDocQueryOrchestratable_a0, OutboundDocQueryOrchestratable> dqexecutor = new NhinTaskExecutor<OutboundDocQueryOrchestratable_a0, OutboundDocQueryOrchestratable>(
                                    ExecutorServiceHelper.getInstance().checkExecutorTaskIsLarge(
                                            correlationsResult.size()) ? largejobExecutor : regularExecutor,
                                    callableList, transactionId);
                            dqexecutor.executeTask();
                            orchResponse_g0 = (OutboundDocQueryOrchestratable_a0) dqexecutor.getFinalResponse();
                            response = orchResponse_g0.getCumulativeResponse();
                        }
                        
                        // add any errors from policyErrList to response
                        if (response != null && policyErrList.getRegistryError() != null
                                && !policyErrList.getRegistryError().isEmpty()) {
                            if (response.getRegistryErrorList() == null) {
                                response.setRegistryErrorList(policyErrList);
                            } else if (response.getRegistryErrorList().getRegistryError() != null) {
                                response.getRegistryErrorList().getRegistryError()
                                        .addAll(policyErrList.getRegistryError());
                            }
                        }
                    } else {
                        if (callableList.size() > 0) {
                            NhinTaskExecutor<OutboundDocQueryOrchestratable_a1, OutboundDocQueryOrchestratable> dqexecutor = new NhinTaskExecutor<OutboundDocQueryOrchestratable_a1, OutboundDocQueryOrchestratable>(
                                    ExecutorServiceHelper.getInstance().checkExecutorTaskIsLarge(
                                            correlationsResult.size()) ? largejobExecutor : regularExecutor,
                                    callableList, transactionId);
                            dqexecutor.executeTask();
                            orchResponse_g1 = (OutboundDocQueryOrchestratable_a1) dqexecutor.getFinalResponse();
                            response = orchResponse_g1.getCumulativeResponse();
                        }
                        
                        // add any errors from policyErrList to response
                        if (response != null && policyErrList.getRegistryError() != null
                                && !policyErrList.getRegistryError().isEmpty()) {
                            if (response.getRegistryErrorList() == null) {
                                response.setRegistryErrorList(policyErrList);
                            } else if (response.getRegistryErrorList().getRegistryError() != null) {
                                response.getRegistryErrorList().getRegistryError()
                                        .addAll(policyErrList.getRegistryError());
                            }
                        }
                    }

                    log.debug("EntityDocQueryOrchImpl taskexecutor done and received response");
                } else {
                    log.error("No patient correlations found.");
                    response = createErrorResponse("No patient correlations found.");
                }
            } else {
                log.error("Incomplete doc query message");
                response = createErrorResponse("Incomplete/empty adhocquery message");
            }
        } catch (Exception e) {
            log.error("Error occured processing doc query on entity interface: " + e.getMessage(), e);
            response = createErrorResponse("Fault encountered processing internal document query" + " exception="
                    + e.getMessage());
        }
        auditDocQueryResponse(response, assertion, auditLog, targetHomeCommunityId);
        log.debug("Exiting EntityDocQueryOrchImpl.respondingGatewayCrossGatewayQuery...");
        return response;
    }

    private void auditInitialEntityRequest(RespondingGatewayCrossGatewayQuerySecuredRequestType request,
            AssertionType assertion, DocQueryAuditLog auditLog, String targetHomeCommunityId) {

        if (auditLog != null) {
            auditLog.auditDQRequest(request.getAdhocQueryRequest(), assertion, targetHomeCommunityId,
                    NhincConstants.AUDIT_LOG_INBOUND_DIRECTION, NhincConstants.AUDIT_LOG_ENTITY_INTERFACE);
        }
    }

    private void auditDocQueryResponse(AdhocQueryResponse response, AssertionType assertion, DocQueryAuditLog auditLog,
            String targetHomeCommunityId) {

        if (auditLog != null) {
            AdhocQueryResponseMessageType auditMsg = new AdhocQueryResponseMessageType();
            auditMsg.setAdhocQueryResponse(response);
            auditMsg.setAssertion(assertion);
            auditLog.auditDQResponse(response, assertion, NhincConstants.AUDIT_LOG_OUTBOUND_DIRECTION,
                    NhincConstants.AUDIT_LOG_ENTITY_INTERFACE, targetHomeCommunityId);
        }
    }


    private AdhocQueryRequest cloneRequest(AdhocQueryRequest request){
        AdhocQueryRequest newRequest = new AdhocQueryRequest();
        AdhocQueryType currentQuery = request.getAdhocQuery();

        AdhocQueryType newQuery = new AdhocQueryType();
        List<SlotType1> newslotList = new ArrayList<SlotType1>();
        List<SlotType1> slotList = currentQuery.getSlot();
        for(SlotType1 slot : slotList){
            SlotType1 newSlot = new SlotType1();
            newSlot.setName(slot.getName());
            newSlot.setSlotType(slot.getSlotType());

            ValueListType valueListType = new ValueListType();
            List<String> valueList = new ArrayList<String>();
            for(String value : slot.getValueList().getValue()){
                valueList.add(value);
            }
            valueListType.getValue().addAll(valueList);
            newSlot.setValueList(valueListType);

            newslotList.add(newSlot);
        }
        newQuery.getSlot().addAll(newslotList);

        newQuery.getClassification().addAll(currentQuery.getClassification());
        newQuery.getExternalIdentifier().addAll(currentQuery.getExternalIdentifier());

        newQuery.setDescription(currentQuery.getDescription());
        newQuery.setHome(currentQuery.getHome());
        newQuery.setId(currentQuery.getId());
        newQuery.setLid(currentQuery.getLid());
        newQuery.setName(currentQuery.getName());
        newQuery.setObjectType(currentQuery.getObjectType());
        newQuery.setQueryExpression(currentQuery.getQueryExpression());
        newQuery.setStatus(currentQuery.getStatus());
        newQuery.setVersionInfo(currentQuery.getVersionInfo());
        newRequest.setAdhocQuery(newQuery);

        newRequest.setComment(request.getComment());
        // newRequest.setFederated(request.isFederated());
        newRequest.setFederation(request.getFederation());
        newRequest.setId(request.getId());
        newRequest.setMaxResults(request.getMaxResults());
        newRequest.setResponseOption(request.getResponseOption());
        newRequest.setStartIndex(request.getStartIndex());
        
        log.debug("EntityDocQueryOrchImpl::cloneRequest generated new AdhocQueryRequest");
        return newRequest;
    }


    protected String getLocalHomeCommunityId() {
        String sHomeCommunity = null;
        try {
            sHomeCommunity = PropertyAccessor.getProperty(NhincConstants.GATEWAY_PROPERTY_FILE,
                    NhincConstants.HOME_COMMUNITY_ID_PROPERTY);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        return sHomeCommunity;
    }

    /**
     * Policy Check verification done here
     * 
     * @param queryRequest
     * @param assertion
     * @return boolean
     */
    private boolean isValidPolicy(AdhocQueryRequest queryRequest, AssertionType assertion,
            HomeCommunityType targetCommunity) {
        boolean isValid = false;
        AdhocQueryRequestEventType checkPolicy = new AdhocQueryRequestEventType();
        AdhocQueryRequestMessageType checkPolicyMessage = new AdhocQueryRequestMessageType();
        checkPolicyMessage.setAdhocQueryRequest(queryRequest);
        checkPolicyMessage.setAssertion(assertion);
        checkPolicy.setMessage(checkPolicyMessage);
        checkPolicy.setDirection(NhincConstants.POLICYENGINE_OUTBOUND_DIRECTION);
        checkPolicy.setInterface(NhincConstants.AUDIT_LOG_ENTITY_INTERFACE);
        checkPolicy.setReceivingHomeCommunity(targetCommunity);
        PolicyEngineChecker policyChecker = new PolicyEngineChecker();
        CheckPolicyRequestType policyReq = policyChecker.checkPolicyAdhocQuery(checkPolicy);
        PolicyEngineProxyObjectFactory policyEngFactory = new PolicyEngineProxyObjectFactory();
        PolicyEngineProxy policyProxy = policyEngFactory.getPolicyEngineProxy();
        CheckPolicyResponseType policyResp = policyProxy.checkPolicy(policyReq, assertion);
        /* if response='permit' */
        if (policyResp.getResponse().getResult().get(0).getDecision().value().equals(NhincConstants.POLICY_PERMIT)) {
            isValid = true;
        }
        return isValid;
    }


    private AdhocQueryResponse createErrorResponse(String codeContext) {
        AdhocQueryResponse response = new AdhocQueryResponse();
        RegistryErrorList regErrList = new RegistryErrorList();
        response.setRegistryErrorList(regErrList);
        response.setStatus("urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Failure");
        RegistryError regErr = new RegistryError();
        regErrList.getRegistryError().add(regErr);
        regErr.setCodeContext(codeContext);
        regErr.setErrorCode("XDSRepositoryError");
        regErr.setSeverity("Error");
        return response;
    }

}
