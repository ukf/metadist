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
 * FilterSupportImpl.java
 * 
 * Provide access to the Filter to configuration information
 * and Session data.
 */
package edu.internet2.middleware.shibboleth.serviceprovider;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.opensaml.SAMLException;

import x0.maceShibbolethTargetConfig1.SessionsDocument.Sessions;

import edu.internet2.middleware.shibboleth.aap.AAP;
import edu.internet2.middleware.shibboleth.aap.AttributeRule;
import edu.internet2.middleware.shibboleth.resource.FilterSupport;
import edu.internet2.middleware.shibboleth.serviceprovider.ServiceProviderConfig.ApplicationInfo;

/**
 * Provide access from the Filter to the /shibboleth configuration and Sessions.
 * 
 * @author Howard Gilbert
 */
public class FilterSupportImpl implements FilterSupport {
    
    private static ServiceProviderContext context = ServiceProviderContext.getInstance();
    private static Logger log = Logger.getLogger(ContextListener.SHIBBOLETH_SERVICE);

    /**
     * Given a Resource URL, go to the RequestMap logic to find an applicationId.
     * 
     * @param url The URL of the Resource presented by the browser
     * @return applicationId string
     */
    public String getApplicationId(String url) {
        ServiceProviderConfig config = context.getServiceProviderConfig();
        String applicationId = config.mapRequest(url);
        return applicationId;
    }
    
    /**
     * Get the "providerId" (site name) of the ServiceProvider
     * 
     * @param applicationId 
     * @return providerId string
     */
    public String getProviderId(String applicationId) {
        ServiceProviderConfig config = context.getServiceProviderConfig();
        ApplicationInfo application = config.getApplication(applicationId);
        String providerId = application.getApplicationConfig().getProviderId();
        return providerId;
    }
    
    /**
     * Get the URL of the local AssertionConsumerServlet.
     * 
     * @param applicationId
     * @return URL string
     */
    public String getShireUrl(String applicationId) {
        ServiceProviderConfig config = context.getServiceProviderConfig();
        ApplicationInfo application = config.getApplication(applicationId);
        String shireUrl = application.getApplicationConfig().getSessions().getShireURL();
        return shireUrl;
    }
    
    /**
     * Get the URL to which the Browser should be initially redirected.
     * 
     * @param applicationId
     * @return URL string
     */
    public String getWayfUrl(String applicationId) {
        ServiceProviderConfig config = context.getServiceProviderConfig();
        ApplicationInfo application = config.getApplication(applicationId);
        String wayfUrl = application.getApplicationConfig().getSessions().getWayfURL();
        return wayfUrl;
    }
    
	/**
	 * Does the requested resource require Shibboleth authentication?
	 * 
	 * @param url  request url
	 * @return     true if Shibboleth is required
	 */
	public boolean isProtected(String url) {
		//TODO: get info from requestmap
	    return true;
	}

	/**
	 * Get attributes for this Session 
	 * 
	 * @param sessionId
	 * @param applicationId
	 * @return Map of (attribute,value) pairs
	 */
    public Map /*<String,String>*/ 
    getSessionAttributes(String sessionId, String applicationId) {
        SessionManager sm = context.getSessionManager();
        Session session = 
            sm.findSession(sessionId, applicationId);
        if (session==null)
            return null;
        Map /*<String,String>*/ attributes = SessionManager.mapAttributes(session);
        return attributes;
    }

    /**
     * Map attribute name to pseudo-HTTP-Headers
     * 
     * @param attributeName
     * @param applicationId
     * @return null or Header name string
     */
    public String getHeader(String attributeName, String applicationId) {
        ServiceProviderConfig config = context.getServiceProviderConfig();
        ApplicationInfo application = config.getApplication(applicationId);
        AAP[] providers = application.getAAPProviders();
        for (int i=0;i<providers.length;i++) {
            AAP aap = providers[i];
            AttributeRule rule = aap.lookup(attributeName, null);
            if (rule!=null)
                return rule.getHeader();
        }
        return null;
    }

    /**
     * @param ipaddr
     * @param request
     * @param applicationId
     * @param shireURL
     * @param providerId
     * @return
     */
    public String createSessionFromPost(
            String ipaddr, 
            HttpServletRequest request, 
            String applicationId, 
            String shireURL, 
            String providerId,
            String emptySessionId) {
        String sessionid;
        try {
            sessionid = AssertionConsumerServlet.createSessionFromPost(
                    ipaddr, request, applicationId, shireURL, providerId,emptySessionId);
        } catch (SAMLException e) {
        	log.error("Invalid POST data submitted by RM "+e);
            return null;
        }
        log.info("Session created from POST submitted by RM: "+sessionid);
        return sessionid;
    }


    public boolean getShireSSL(String applicationId) {
        ServiceProviderConfig config = context.getServiceProviderConfig();
        ApplicationInfo appinfo = config.getApplication(applicationId);
        Sessions appSessionValues = appinfo.getApplicationConfig().getSessions();
        return appSessionValues.getShireSSL();
    }

    /**
     * Create empty Session so SessionID can be written as a Cookie
     * before redirecting the Browser to the IDP.
     * 
     * @param applicationId
     * @return SessionId of empty session
     */
    public String createSession(String applicationId) {
        String id = context.getSessionManager().reserveSession(applicationId);
        return id;
    }
}
