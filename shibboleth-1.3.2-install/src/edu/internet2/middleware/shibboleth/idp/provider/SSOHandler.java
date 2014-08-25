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

package edu.internet2.middleware.shibboleth.idp.provider;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.opensaml.SAMLException;
import org.opensaml.SAMLNameIdentifier;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.LocalPrincipal;
import edu.internet2.middleware.shibboleth.common.NameIdentifierMapping;
import edu.internet2.middleware.shibboleth.common.NameIdentifierMappingException;
import edu.internet2.middleware.shibboleth.common.NameMapper;
import edu.internet2.middleware.shibboleth.common.RelyingParty;
import edu.internet2.middleware.shibboleth.common.ShibbolethConfigurationException;
import edu.internet2.middleware.shibboleth.common.provider.CryptoShibHandle;
import edu.internet2.middleware.shibboleth.idp.IdPProtocolHandler;
import edu.internet2.middleware.shibboleth.idp.IdPProtocolSupport;
import edu.internet2.middleware.shibboleth.idp.InvalidClientDataException;
import edu.internet2.middleware.shibboleth.metadata.EntityDescriptor;
import edu.internet2.middleware.shibboleth.metadata.SPSSODescriptor;

/**
 * @author Walter Hoehn
 */
public abstract class SSOHandler extends BaseHandler implements IdPProtocolHandler {

	private static Logger log = Logger.getLogger(BaseHandler.class.getName());

	/**
	 * Required DOM-based constructor.
	 */
	public SSOHandler(Element config) throws ShibbolethConfigurationException {

		super(config);

	}

	public static void validateEngineData(HttpServletRequest req) throws InvalidClientDataException {

		if ((req.getRemoteAddr() == null) || (req.getRemoteAddr().equals(""))) { throw new InvalidClientDataException(
				"Unable to obtain client address."); }
	}

	protected Date getAuthNTime(HttpServletRequest request) throws SAMLException {

		// Determine, if possible, when the authentication actually happened
		String suppliedAuthNInstant = request.getHeader("SAMLAuthenticationInstant");
		if (suppliedAuthNInstant != null && !suppliedAuthNInstant.equals("")) {
			try {
				return new SimpleDateFormat().parse(suppliedAuthNInstant);
			} catch (ParseException e) {
				log.error("An error was encountered while receiving authentication "
						+ "instant from authentication mechanism: " + e);
				throw new SAMLException(SAMLException.RESPONDER, "General error processing request.");
			}
		} else {
			return new Date(System.currentTimeMillis());
		}
	}
	
	/**
	 * Returns authenticated user making SSO request.
	 * @return false if response to client was required, so calling handler should exit
	 * @param usernamebuf	buffer to receieve username
	 * @throws SAMLException 
	 * @throws IOException 
	 */
	protected boolean getRemoteUser(StringBuffer usernamebuf, HttpServletRequest request, HttpServletResponse response, IdPProtocolSupport support) throws SAMLException, IOException {
		// First check for SSO trigger.
        if (support.getIdPConfig().getAuthHeaderName().equalsIgnoreCase("COOKIE")) {
            // Remote user may already be set if there's already a session.
            String username = request.getRemoteUser();
            if (username != null) {
                // If we're the protected version, we need to set the cookie.
                if (request.getServletPath().endsWith(support.getIdPConfig().getProtectedPath())) {
                    log.debug("Invoked with container authentication, preserving user identity in cookie.");
                    try {
                        CryptoShibHandle crypto =
                            (CryptoShibHandle)support.getNameMapper().getNameIdentifierMappingById(support.getIdPConfig().getCryptoHandleId());
                        if (crypto == null) {
                        	throw new NameIdentifierMappingException("CryptoShibHandle mapper not available with id (" +
                        			support.getIdPConfig().getCryptoHandleId() + ").");
                        }
                        Cookie cookie = new Cookie(support.getIdPConfig().getCookieName(),
                        		crypto.getName(new LocalPrincipal(username)));
                        cookie.setSecure(true);
                        response.addCookie(cookie);
                    }
                    catch (NameIdentifierMappingException e) {
                        log.error("Error converting principal to encrypted cookie: " + e);
                        throw new SAMLException("Error converting principal to encrypted cookie.", e);
                    }
                }
                usernamebuf.append(username);
                return true;
            }
            else {
                // This must be the naked servlet path. Check for the encrypted cookie.
                log.debug("Checking for SSO cookie.");
                Cookie[] cookies = request.getCookies();
                for (int i=0; cookies != null && i<cookies.length; i++) {
                    if (cookies[i].getName().equals(support.getIdPConfig().getCookieName())) {
                        log.debug("Found SSO cookie (" + cookies[i].getValue() + ").");
                        try {
                            CryptoShibHandle crypto =
                                (CryptoShibHandle)support.getNameMapper().getNameIdentifierMappingById(support.getIdPConfig().getCryptoHandleId());
                            if (crypto == null) {
                            	throw new NameIdentifierMappingException("CryptoShibHandle mapper not available with id (" +
                            			support.getIdPConfig().getCryptoHandleId() + ").");
                            }
                            username = crypto.getPrincipal(cookies[i].getValue()).getName();
                            log.debug("Recovered username (" + username + ") from cookie.");
                            usernamebuf.append(username);
                            return true;
                        }
                        catch (NameIdentifierMappingException e) {
                            log.error("Error while recovering username from cookie: " + e);
                        }
                    }
                }
                if (username == null) {
                    // Still no identity. Redirect request to fully-clothed version.
                    log.debug("No usable cookie, redirecting to protected copy of servlet.");
                    response.sendRedirect(support.getIdPConfig().getProtectedPath() + '?' + request.getQueryString());
                    return false;
                }
            }
        }
        else if (support.getIdPConfig().getAuthHeaderName().equalsIgnoreCase("REMOTE_USER")) {
            usernamebuf.append(request.getRemoteUser());
            return true;
        }

        usernamebuf.append(request.getHeader(support.getIdPConfig().getAuthHeaderName()));
        return true;
	}

	/**
	 * Constructs a SAML Name Identifier of a given principal that is most appropriate to the relying party.
	 * 
	 * @param mapper
	 *            name mapping facility
	 * @param principal
	 *            the principal represented by the name identifier
	 * @param relyingParty
	 *            the party that will consume the name identifier
	 * @param descriptor
	 *            metadata descriptor for the party that will consume the name identifier
	 * @return the SAML Name identifier
	 * @throws NameIdentifierMappingException
	 *             if a name identifier could not be created
	 */
	protected SAMLNameIdentifier getNameIdentifier(NameMapper mapper, LocalPrincipal principal,
			RelyingParty relyingParty, EntityDescriptor descriptor) throws NameIdentifierMappingException {

		String[] availableMappings = relyingParty.getNameMapperIds();

		// If we have preferred Name Identifier formats from the metadata, see if the we can find one that is configured
		// for this relying party
		SPSSODescriptor role;
		if (descriptor != null
				&& (role = descriptor.getSPSSODescriptor(org.opensaml.XML.SAML11_PROTOCOL_ENUM)) != null) {
			Iterator spPreferredFormats = role.getNameIDFormats();
			while (spPreferredFormats.hasNext()) {

				String preferredFormat = (String) spPreferredFormats.next();
				for (int i = 0; availableMappings != null && i < availableMappings.length; i++) {
					NameIdentifierMapping mapping = mapper.getNameIdentifierMappingById(availableMappings[i]);
					if (mapping != null && preferredFormat.equals(mapping.getNameIdentifierFormat().toString())) {
						log.debug("Found a supported name identifier format that "
								+ "matches the metadata for the relying party: ("
								+ mapping.getNameIdentifierFormat().toString() + ").");
						return mapping.getNameIdentifier(principal, relyingParty, relyingParty.getIdentityProvider());
					}
				}
			}
		}

		// If we didn't find any matches, then just use the default for the relying party
		String defaultNameMapping = null;
		if (availableMappings != null && availableMappings.length > 0) {
			defaultNameMapping = availableMappings[0];
		}
		SAMLNameIdentifier nameId = mapper.getNameIdentifier(defaultNameMapping, principal, relyingParty, relyingParty
				.getIdentityProvider());
		log.debug("Using the default name identifier format for this relying party: (" + nameId.getFormat());
		return nameId;
	}
}