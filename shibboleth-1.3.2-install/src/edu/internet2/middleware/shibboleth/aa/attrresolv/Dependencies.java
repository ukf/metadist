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

package edu.internet2.middleware.shibboleth.aa.attrresolv;

import java.util.HashMap;
import java.util.Map;

import javax.naming.directory.Attributes;

/**
 * Contains resolved <code>ResolutionPlugIn</code> objects that a particular Attribute Definition PlugIn relies upon
 * for its resolution.
 * 
 * @author Walter Hoehn (wassa@columbia.edu)
 */
public class Dependencies {

	protected Map resolvedAttributeDependencies = new HashMap();
	protected Map resolvedConnectorDependencies = new HashMap();

	public Attributes getConnectorResolution(String id) {

		if (resolvedConnectorDependencies.containsKey(id)) {
			return (Attributes) resolvedConnectorDependencies.get(id);
		} else {
			return null;
		}
	}

	void addConnectorResolution(String id, Attributes attrs) {

		resolvedConnectorDependencies.put(id, attrs);
	}

	public ResolverAttribute getAttributeResolution(String id) {

		if (resolvedAttributeDependencies.containsKey(id)) {
			return (ResolverAttribute) resolvedAttributeDependencies.get(id);
		} else {
			return null;
		}
	}

	void addAttributeResolution(String id, ResolverAttribute attr) {

		resolvedAttributeDependencies.put(id, attr);
	}
}
