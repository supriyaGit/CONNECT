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
package gov.hhs.fha.nhinc.docretrieve.nhin;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import gov.hhs.fha.nhinc.common.nhinccommon.AssertionType;
import gov.hhs.fha.nhinc.common.nhinccommon.NhinTargetSystemType;
import gov.hhs.fha.nhinc.docretrieve.audit.DocRetrieveAuditLogger;
import gov.hhs.fha.nhinc.nhinclib.NhincConstants;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import java.util.Properties;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 *
 * @author mweaver
 */
public class NhinDocRetrieveAuditTransformer_g0Test {

    NhinTargetSystemType nhinTargetSystemType = null;
    private InboundDocRetrieveOrchestratable mockMessage = null;
    private DocRetrieveAuditLogger docAuditLogger = null;
    private InboundDocRetrieveAuditTransformer_g0 transform = null;

    @Before
    public void Setup() {
        mockMessage = mock(InboundDocRetrieveOrchestratable.class);
        docAuditLogger = mock(DocRetrieveAuditLogger.class);
        transform = new InboundDocRetrieveAuditTransformer_g0(docAuditLogger);
    }

    /**
     * Test of transformRequest method, of class NhinDocRetrieveAuditTransformer_g0.
     */
    @Test
    public void testTransformRequest() {

        transform.transformRequest(mockMessage);
        verify(mockMessage, never()).getAssertion();
        verify(mockMessage, never()).getRequest();
        verify(docAuditLogger, never()).auditRequestMessage(Mockito.any(RetrieveDocumentSetRequestType.class), Mockito.any(AssertionType.class), eq(nhinTargetSystemType), eq(NhincConstants.AUDIT_LOG_INBOUND_DIRECTION), eq(NhincConstants.AUDIT_LOG_NHIN_INTERFACE), eq(Boolean.FALSE), Mockito.any(Properties.class), eq(NhincConstants.DOC_RETRIEVE_SERVICE_NAME));

    }

    /**
     * Test of transformResponse method, of class NhinDocRetrieveAuditTransformer_g0.
     */
    @Test
    public void testTransformResponse() {

        transform.transformResponse(mockMessage);
        verify(docAuditLogger, never()).auditResponseMessage(Mockito.any(RetrieveDocumentSetRequestType.class), Mockito.any(RetrieveDocumentSetResponseType.class), Mockito.any(AssertionType.class), eq(nhinTargetSystemType), eq(NhincConstants.AUDIT_LOG_OUTBOUND_DIRECTION), eq(NhincConstants.AUDIT_LOG_NHIN_INTERFACE), eq(Boolean.FALSE), Mockito.any(Properties.class), eq(NhincConstants.DOC_RETRIEVE_SERVICE_NAME));

    }

}
