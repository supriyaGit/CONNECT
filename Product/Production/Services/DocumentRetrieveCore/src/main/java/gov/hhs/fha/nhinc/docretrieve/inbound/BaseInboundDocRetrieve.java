/*
 * Copyright (c) 2009-2015, United States Government, as represented by the Secretary of Health and Human Services.
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
package gov.hhs.fha.nhinc.docretrieve.inbound;

import java.util.Properties;

import gov.hhs.fha.nhinc.aspect.InboundProcessingEvent;
import gov.hhs.fha.nhinc.common.nhinccommon.AssertionType;
import gov.hhs.fha.nhinc.docretrieve.aspect.RetrieveDocumentSetRequestTypeDescriptionBuilder;
import gov.hhs.fha.nhinc.docretrieve.aspect.RetrieveDocumentSetResponseTypeDescriptionBuilder;
import gov.hhs.fha.nhinc.docretrieve.audit.DocRetrieveAuditLogger;
import gov.hhs.fha.nhinc.docretrieve.nhin.InboundDocRetrieveOrchestratable;
import gov.hhs.fha.nhinc.nhinclib.NhincConstants;
import gov.hhs.fha.nhinc.orchestration.CONNECTInboundOrchestrator;
import gov.hhs.fha.nhinc.orchestration.Orchestratable;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;

/**
 * @author akong
 *
 */
public abstract class BaseInboundDocRetrieve implements InboundDocRetrieve {

    /**
     * Returns the orchestrator.
     *
     * @return orchestrator instance
     */
    abstract CONNECTInboundOrchestrator getOrchestrator();

    DocRetrieveAuditLogger docRetrieveAuditLogger = new DocRetrieveAuditLogger();

    /**
     * Creates the inbound orchestratable instance to be used by the orchestrator.
     *
     * @param body
     * @param assertion
     */
    abstract public InboundDocRetrieveOrchestratable createInboundOrchestrable(RetrieveDocumentSetRequestType body,
        AssertionType assertion, Properties webContextProperties);

    public void auditResponse(Orchestratable message) {
        if (message instanceof InboundDocRetrieveOrchestratable) {
            InboundDocRetrieveOrchestratable message_InboundDocRetrieveOrchestratable = (InboundDocRetrieveOrchestratable) message;
            docRetrieveAuditLogger.auditResponseMessage(message_InboundDocRetrieveOrchestratable.getRequest(),
                message_InboundDocRetrieveOrchestratable.getResponse(), message.getAssertion(), null,
                NhincConstants.AUDIT_LOG_OUTBOUND_DIRECTION, NhincConstants.AUDIT_LOG_NHIN_INTERFACE,
                Boolean.FALSE, message_InboundDocRetrieveOrchestratable.getWebContextProperties(),
                NhincConstants.DOC_RETRIEVE_SERVICE_NAME);

        }
    }

    @InboundProcessingEvent(beforeBuilder = RetrieveDocumentSetRequestTypeDescriptionBuilder.class,
        afterReturningBuilder = RetrieveDocumentSetResponseTypeDescriptionBuilder.class,
        serviceType = "Retrieve Document", version = "")
    public RetrieveDocumentSetResponseType respondingGatewayCrossGatewayRetrieve(RetrieveDocumentSetRequestType body,
        AssertionType assertion, Properties webContextProperties) {

        InboundDocRetrieveOrchestratable inboundOrchestrable = createInboundOrchestrable(body, assertion, webContextProperties);

        InboundDocRetrieveOrchestratable orchResponse = (InboundDocRetrieveOrchestratable) getOrchestrator().process(
            inboundOrchestrable);
        auditResponse(orchResponse);

        return orchResponse.getResponse();
    }
}
