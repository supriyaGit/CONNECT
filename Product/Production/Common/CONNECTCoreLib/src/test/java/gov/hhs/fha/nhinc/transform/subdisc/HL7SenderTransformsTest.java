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
package gov.hhs.fha.nhinc.transform.subdisc;

import org.hl7.v3.MCCIMT000100UV01Sender;
import org.hl7.v3.MCCIMT000200UV01Sender;
import org.hl7.v3.MCCIMT000300UV01Sender;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/**
 * @author achidambaram
 *
 */
public class HL7SenderTransformsTest {

    @Test
    public void createMCCIMT000200UV01Sender() {
        MCCIMT000200UV01Sender sender;
        String OID = null;
        HL7SenderTransforms transforms = new HL7SenderTransforms();
        sender = transforms.createMCCIMT000200UV01Sender(OID);
        assertNull(sender.getDevice().getId().get(0).getRoot());
        assertNull(sender.getDevice().getAsAgent().getValue().getRepresentedOrganization().getValue().getId().get(0)
                .getRoot());
    }

    @Test
    public void createMCCIMT000100UV01Sender() {
        MCCIMT000100UV01Sender sender;
        String OID = null;
        HL7SenderTransforms transforms = new HL7SenderTransforms();
        sender = transforms.createMCCIMT000100UV01Sender(OID);
        assertNull(sender.getDevice().getId().get(0).getRoot());
        assertNull(sender.getDevice().getAsAgent().getValue().getRepresentedOrganization().getValue().getId().get(0)
                .getRoot());
    }

    @Test
    public void createMCCIMT000300UV01Sender() {
        MCCIMT000300UV01Sender sender;
        String OID = null;
        HL7SenderTransforms transforms = new HL7SenderTransforms();
        sender = transforms.createMCCIMT000300UV01Sender(OID);
        assertNull(sender.getDevice().getId().get(0).getRoot());
        assertNull(sender.getDevice().getAsAgent().getValue().getRepresentedOrganization().getValue().getId().get(0)
                .getRoot());
    }

}
