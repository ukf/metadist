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

package edu.internet2.middleware.shibboleth.metadata;

import java.util.Iterator;

/**
 * Corresponds to SAML Metadata Schema "SPSSODescriptorType".
 * 
 * @author Scott Cantor
 */
public interface SPSSODescriptor extends SSODescriptor {

	public boolean getAuthnRequestsSigned();

	public boolean getWantAssertionsSigned();

	public EndpointManager getAssertionConsumerServiceManager();

	public Iterator /* <AttributeConsumingService> */getAttributeConsumingServices();

	public AttributeConsumingService getDefaultAttributeConsumingService();

	public AttributeConsumingService getAttributeConsumingServiceByID(String id);
}