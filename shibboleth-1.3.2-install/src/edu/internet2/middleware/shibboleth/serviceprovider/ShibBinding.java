/*
 * Copyright [2005] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * ShibBinding.java
 * 
 * Corresponds to ShibBinding.cpp
 * 
 * A Shibboleth wrapper around the services of SAMLSOAPBinding,
 * this class adds processing from the Shibboleth configuration 
 * to the process of sending a SAMLRequest and getting a SAMLResponse.
 * In particular, the caller of a ShibBinding provides arguments
 * that identify the target of the request from the Metadata, and
 * the caller passes an implementation of Trust so that signatures
 * can be validated.
 */
package edu.internet2.middleware.shibboleth.serviceprovider;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.opensaml.NoSuchProviderException;
import org.opensaml.SAMLAssertion;
import org.opensaml.SAMLAuthorityBinding;
import org.opensaml.SAMLBinding;
import org.opensaml.SAMLBindingFactory;
import org.opensaml.SAMLCondition;
import org.opensaml.SAMLException;
import org.opensaml.SAMLRequest;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLSOAPHTTPBinding;
import org.opensaml.TrustException;
import edu.internet2.middleware.shibboleth.common.Trust;
import edu.internet2.middleware.shibboleth.metadata.AttributeAuthorityDescriptor;
import edu.internet2.middleware.shibboleth.metadata.Endpoint;
import edu.internet2.middleware.shibboleth.serviceprovider.ServiceProviderConfig.ApplicationInfo;

/**
 * Wrapper for a SAMLBinding send/receive operation.
 * 
 * <p>A ServiceProvider creates a ShibBinding object and then calls
 * its send() method. The logic is certainly capable of generating any
 * SAML Request/Response sequence. However, the variables have been
 * declared to have more specific types than the general logic, so this
 * version can only be used by a Service Provider to make an attribute query
 * to the AA.</p>
 * 
 * <p>The ShibBinding doesn't hold any important resources. The
 * identity of the AA isn't passed until the send() method and could change
 * across calls, so there aren't any persistent network resources. Nothing
 * prevents a ShibBinding object from being reused, but normally it is
 * just a transient object as in resp=(new ShibBinding(appid)).send(req,...)</p>
 * 
 * @author Howard Gilbert
 */
public class ShibBinding {
	
	private static Logger log = Logger.getLogger(ShibBinding.class);
	
	private static ServiceProviderContext context = ServiceProviderContext.getInstance();
	
	private String applicationId = null;
	
	/**
	 * While the C++ constructor takes iterators over the Trust and 
	 * Metadata, here we provide the key of an ApplicationInfo object
	 * that contains them.
	 * 
	 * @param applicationId
	 * @throws NoSuchProviderException
	 */
	public 
	ShibBinding(
			String applicationId)  {
		this.applicationId=applicationId;
	}

	/**
	 * Send a SAMLRequest and get back a SAMLResponse.
	 * 
	 * <p>Although this logic could be generalized, this version
	 * declares the arguments to be of specific types (an AA role)
	 * so it can only be used to send the Attribute Query and get back
	 * the Attribute Assertions.
	 * 
	 * @param req        SAMLRequest to send
	 * @param role       AttributeAuthorityRole representing destination
	 * @param audiences  Audience strings to check SAML conditions
	 * @param bindings   Stupid idea. Don't use this parameter
	 * @return           The SAMLResponse
	 * @throws SAMLException
	 */
	public 
			SAMLResponse 
	send (
			SAMLRequest req,
			AttributeAuthorityDescriptor role,
			String[] audiences,
			SAMLAuthorityBinding[] bindings,
            Trust trust) 
	throws SAMLException {
		
		// For the duration of the request, get local references to
		// configuration objects that might change between requests
		ServiceProviderConfig config = context.getServiceProviderConfig();
		ApplicationInfo appinfo = config.getApplication(applicationId);
		
        SAMLBinding sbinding = null;
		SAMLResponse resp = null;
		String prevBinding = null;
	
		/*
		 * Try any inline bindings provided by 1.0/1.1 IdPs. 
		 */
		if (bindings!=null) {
			for (int ibinding=0;ibinding<bindings.length;ibinding++) {
				try {
					SAMLAuthorityBinding binding = bindings[ibinding];
					if (!binding.getBinding().equals(prevBinding)) {
						prevBinding = binding.getBinding();
                        sbinding = SAMLBindingFactory.getInstance(binding.getBinding());
                    }
					resp=sbinding.send(binding.getLocation(),req);
					validateResponseSignatures(role, appinfo, resp);
					return resp;
                } catch (TrustException e) {
                    log.error("Unable to validate signatures on attribute response: " + e);
                    continue;
                } catch (SAMLException e) {
                    log.error("Unable to query attributes: " + e);
                    continue;
                }
			}
		}
		
		/*
		 * Try each metadata endpoint...
		 */
		Iterator ends = role.getAttributeServiceManager().getEndpoints();
        while (ends.hasNext()) {
            Endpoint endpoint = (Endpoint)ends.next();
            try {
                if (!endpoint.getBinding().equals(prevBinding)) {
                    prevBinding = endpoint.getBinding();
                    sbinding = SAMLBindingFactory.getInstance(endpoint.getBinding());
                }
                if (sbinding instanceof SAMLSOAPHTTPBinding) {
                    SAMLSOAPHTTPBinding httpbind = (SAMLSOAPHTTPBinding)sbinding;
                    httpbind.addHook(new ShibHttpHook(role,trust));
                }
                resp=sbinding.send(endpoint.getLocation(),req);
                validateResponseSignatures(role, appinfo, resp);
                return resp;
            } catch (TrustException e) {
                log.error("Unable to validate signatures on attribute response: " + e);
                continue;
            } catch (SAMLException e) {
                log.error("Unable to query attributes: " + e);
                continue;
            }
        }
        return null;
	}

	/**
	 * Given a SAMLResponse, check the Response itself and every Assertion it 
     * contains for a digital signature. If signed, call Trust to verify the
     * signature against the configured Certificates for this Role in the Metadata.
	 * 
	 * @param role     OriginSite
	 * @param appinfo  Application data
	 * @param resp     SAML response
	 * @throws TrustException on failure
	 */
	public static void 
	validateResponseSignatures(
			AttributeAuthorityDescriptor role, 
			ApplicationInfo appinfo, 
			SAMLResponse resp) 
	throws TrustException {
		
        // If the entire Response is signed, check it
		if (resp.isSigned()&& 
            !appinfo.validate(resp,role)) {
			throw new TrustException("Unable to validate signature of response");
		}
		
        // Now check each Assertion in the Response for a signature
		Iterator assertions = resp.getAssertions();
		while (assertions.hasNext()) {
			SAMLAssertion assertion = (SAMLAssertion) assertions.next();
			
			Iterator conditions = assertion.getConditions();
			while (conditions.hasNext()) {
				SAMLCondition condition = (SAMLCondition) conditions.next();
				// TODO C++ only seems to validate that the audience string is present
			}
			
			if (assertion.isSigned() && 
				!appinfo.validate(assertion,role)) {
				throw new TrustException("Unable to validate signature of assertion in response");
			}
		}
	}
}
