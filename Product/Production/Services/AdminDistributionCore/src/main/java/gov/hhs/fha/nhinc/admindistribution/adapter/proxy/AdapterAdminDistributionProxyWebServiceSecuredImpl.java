/*
 * Copyright (c) 2009-2016, United States Government, as represented by the Secretary of Health and Human Services.
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
package gov.hhs.fha.nhinc.admindistribution.adapter.proxy;

import gov.hhs.fha.nhinc.adapteradmindistribution.AdapterAdministrativeDistributionSecuredPortType;
import gov.hhs.fha.nhinc.admindistribution.AdminDistributionHelper;
import gov.hhs.fha.nhinc.admindistribution.adapter.proxy.service.AdapterAdminDistributionSecuredServicePortDescriptor;
import gov.hhs.fha.nhinc.admindistribution.aspect.EDXLDistributionEventDescriptionBuilder;
import gov.hhs.fha.nhinc.aspect.AdapterDelegationEvent;
import gov.hhs.fha.nhinc.common.nhinccommon.AssertionType;
import gov.hhs.fha.nhinc.common.nhinccommonadapter.RespondingGatewaySendAlertMessageSecuredType;
import gov.hhs.fha.nhinc.event.DefaultEventDescriptionBuilder;
import gov.hhs.fha.nhinc.messaging.client.CONNECTCXFClientFactory;
import gov.hhs.fha.nhinc.messaging.client.CONNECTClient;
import gov.hhs.fha.nhinc.messaging.service.port.ServicePortDescriptor;
import gov.hhs.fha.nhinc.nhinclib.NhincConstants;
import gov.hhs.fha.nhinc.nhinclib.NhincConstants.ADAPTER_API_LEVEL;
import gov.hhs.fha.nhinc.nhinclib.NullChecker;
import gov.hhs.fha.nhinc.webserviceproxy.WebServiceProxyHelper;
import oasis.names.tc.emergency.edxl.de._1.EDXLDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dunnek
 */
public class AdapterAdminDistributionProxyWebServiceSecuredImpl implements AdapterAdminDistributionProxy {

    private static final Logger LOG = LoggerFactory.getLogger(AdapterAdminDistributionProxyWebServiceSecuredImpl.class);
    private AdminDistributionHelper adminDistributionHelper;

    /**
     * Constructor.
     */
    public AdapterAdminDistributionProxyWebServiceSecuredImpl() {
        adminDistributionHelper = getHelper();
    }

    /**
     * @return an instance of AdminDistributionHelper.
     */
    protected AdminDistributionHelper getHelper() {
        return new AdminDistributionHelper();
    }

    /**
     * This method returns CXFClient to implement AdpaterAdmin Dist Secured Service.
     *
     * @param portDescriptor comprises of NameSpaceUri, WSDLFile to read,Port, ServiceName and WS_ADDRESSING_ACTION.
     * @param url targetCommunity Url received.
     * @param assertion Assertion received.
     * @return CXFClient for AdapterAdminDist Secured Service.
     */
    protected CONNECTClient<AdapterAdministrativeDistributionSecuredPortType> getCONNECTClientSecured(
            ServicePortDescriptor<AdapterAdministrativeDistributionSecuredPortType> portDescriptor, String url,
            AssertionType assertion) {

        return CONNECTCXFClientFactory.getInstance().getCONNECTClientSecured(portDescriptor, url, assertion);
    }

    protected String getUrl() {
        return adminDistributionHelper.getAdapterUrl(NhincConstants.ADAPTER_ADMIN_DIST_SECURED_SERVICE_NAME,
                ADAPTER_API_LEVEL.LEVEL_a0);
    }

    /**
     * This method implements SendAlertMessage for AdminDist.
     *
     * @param body Emergency Message Distribution Element transaction message body received.
     * @param assertion Assertion received.
     */
    @AdapterDelegationEvent(beforeBuilder = EDXLDistributionEventDescriptionBuilder.class, afterReturningBuilder = DefaultEventDescriptionBuilder.class, serviceType = "Admin Distribution", version = "")
    @Override
    public void sendAlertMessage(EDXLDistribution body, AssertionType assertion) {
        LOG.debug("Begin sendAlertMessage");
        String url = getUrl();

        if (NullChecker.isNotNullish(url)) {
            try {
                RespondingGatewaySendAlertMessageSecuredType message = new RespondingGatewaySendAlertMessageSecuredType();
                message.setEDXLDistribution(body);

                ServicePortDescriptor<AdapterAdministrativeDistributionSecuredPortType> portDescriptor = new AdapterAdminDistributionSecuredServicePortDescriptor();

                CONNECTClient<AdapterAdministrativeDistributionSecuredPortType> client = getCONNECTClientSecured(
                        portDescriptor, url, assertion);
                client.enableMtom();

                client.invokePort(AdapterAdministrativeDistributionSecuredPortType.class, "sendAlertMessage", message);
            } catch (Exception ex) {
                LOG.error("Unable to send message: " + ex.getMessage(), ex);
            }
        } else {
            LOG.error("Failed to call the web service (" + NhincConstants.ADAPTER_ADMIN_DIST_SECURED_SERVICE_NAME
                    + ").  The URL is null.");
        }
    }

    /**
     * @return an instance of webServiceProxyHelper.
     */
    protected WebServiceProxyHelper getWebServiceProxyHelper() {
        return new WebServiceProxyHelper();
    }

}
