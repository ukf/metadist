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

package edu.internet2.middleware.shibboleth.utils;

import jargs.gnu.CmdLineParser;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.opensaml.SAMLException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.internet2.middleware.shibboleth.aa.AAAttribute;
import edu.internet2.middleware.shibboleth.aa.AAAttributeSet;
import edu.internet2.middleware.shibboleth.aa.AAAttributeSet.ShibAttributeIterator;
import edu.internet2.middleware.shibboleth.aa.arp.ArpEngine;
import edu.internet2.middleware.shibboleth.aa.arp.ArpException;
import edu.internet2.middleware.shibboleth.aa.arp.ArpProcessingException;
import edu.internet2.middleware.shibboleth.aa.attrresolv.AttributeResolver;
import edu.internet2.middleware.shibboleth.aa.attrresolv.AttributeResolverException;
import edu.internet2.middleware.shibboleth.common.LocalPrincipal;
import edu.internet2.middleware.shibboleth.common.ShibbolethConfigurationException;
import edu.internet2.middleware.shibboleth.idp.IdPConfig;
import edu.internet2.middleware.shibboleth.idp.IdPConfigLoader;
import edu.internet2.middleware.shibboleth.xml.Parser;

/**
 * Utility for testing an Attribute Resolver configuration.
 * 
 * @author Walter Hoehn
 * @author Noah Levitt
 */
public class ResolverTest {

	private static boolean debug = false;
	private static String resolverxml = null;
	private static String idpXml = null;
	private static String requester = null;
	private static String responder = null;
	private static String user = null;
	private static String resource = null;
	private static URL resourceUrl = null;
	private static AttributeResolver resolver = null;
	private static ArpEngine arpEngine = null;

	public static void main(String[] args) {

		parseCommandLine(args);
		initializeResolver();
		AAAttributeSet attributeSet = createAttributeSet();
		resolveAttributes(attributeSet);

		System.out.println("Received the following from the Attribute Resolver:");
		System.out.println();
		printAttributes(System.out, attributeSet);
	}

	private static void resolveAttributes(AAAttributeSet attributeSet) {

		Principal principal = new LocalPrincipal(user);

		resolver.resolveAttributes(principal, requester, responder, attributeSet);

		try {
			if (arpEngine != null) {
				arpEngine.filterAttributes(attributeSet, principal, requester, resourceUrl);
			}
		} catch (ArpProcessingException e) {
			System.err.println("Error applying Attribute Release Policy: " + e.getMessage());
			System.exit(1);
		}
	}

	private static void parseCommandLine(String[] args) {

		CmdLineParser parser = new CmdLineParser();

		CmdLineParser.Option helpOption = parser.addBooleanOption('h', "help");
		CmdLineParser.Option debugOption = parser.addBooleanOption('d', "debug");
		CmdLineParser.Option idpXmlOption = parser.addStringOption('\u0000', "idpXml");
		CmdLineParser.Option userOption = parser.addStringOption('u', "user");
		CmdLineParser.Option requesterOption = parser.addStringOption('r', "requester");
		CmdLineParser.Option responderOption = parser.addStringOption('i', "responder");
		CmdLineParser.Option resolverxmlOption = parser.addStringOption('\u0000', "resolverxml");
		CmdLineParser.Option fileOption = parser.addStringOption('f', "file"); // deprecated
		CmdLineParser.Option resourceOption = parser.addStringOption('\u0000', "resource");

		try {
			parser.parse(args);
		} catch (CmdLineParser.OptionException e) {
			System.out.println(e.getMessage());
			printUsage(System.out);
			System.exit(1);
		}

		Boolean helpEnabled = (Boolean) parser.getOptionValue(helpOption);
		if (helpEnabled != null && helpEnabled.booleanValue()) {
			printUsage(System.out);
			System.exit(0);
		}

		Boolean debugEnabled = ((Boolean) parser.getOptionValue(debugOption));
		if (debugEnabled != null) {
			debug = debugEnabled.booleanValue();
		}

		// if --resolverxml and --file both specified, silently use --resolverxml
		resolverxml = (String) parser.getOptionValue(resolverxmlOption);
		if (resolverxml == null) {
			resolverxml = (String) parser.getOptionValue(fileOption);
		}

		idpXml = (String) parser.getOptionValue(idpXmlOption);

		user = (String) parser.getOptionValue(userOption);
		requester = (String) parser.getOptionValue(requesterOption);
		responder = (String) parser.getOptionValue(responderOption);
		resource = (String) parser.getOptionValue(resourceOption);

		configureLogging(debug);
		checkRequired();
	}

	/**
	 * Ensures that all required parameters were specified and successfully parsed.
	 */
	private static void checkRequired() {

		if (user == null) {
			System.out.println("Missing required parameter --user.");
			System.out.println();
			printUsage(System.out);
			System.exit(1);
		}
		if ((resolverxml == null && idpXml == null) || (resolverxml != null && idpXml != null)) {
			System.out.println("Exactly one of --idpXml and --resolverxml is required.");
			System.out.println();
			printUsage(System.out);
			System.exit(1);
		}
	}

	private static AAAttributeSet createAttributeSet() {

		String[] attributes = resolver.listRegisteredAttributeDefinitionPlugIns();
		AAAttributeSet attributeSet = new AAAttributeSet();

		for (int i = 0; i < attributes.length; i++) {
			try {
				attributeSet.add(new AAAttribute(attributes[i]));
			} catch (SAMLException e) {
				System.err.println("Error creating AAAttribute (" + attributes[i] + "): " + e.getMessage());
				System.exit(1);
			}
		}

		return attributeSet;
	}

	private static void initializeResolver() {

		if (idpXml != null) {
			try {
				Document idpConfig = IdPConfigLoader.getIdPConfig(idpXml);
				IdPConfig configuration = new IdPConfig(idpConfig.getDocumentElement());

				resolver = new AttributeResolver(configuration);

				NodeList itemElements = idpConfig.getDocumentElement().getElementsByTagNameNS(
						IdPConfig.configNameSpace, "ReleasePolicyEngine");

				if (itemElements.getLength() > 1) {
					System.err
							.println("Warning: encountered multiple <ReleasePolicyEngine> configuration elements in ("
									+ idpXml + "). Using first...");
				}

				if (itemElements.getLength() < 1) {
					arpEngine = new ArpEngine();
				} else {
					arpEngine = new ArpEngine((Element) itemElements.item(0));
				}

				if (resource != null) {
					resourceUrl = new URL(resource);
				}
			} catch (ShibbolethConfigurationException e) {
				System.err.println("Error loading IdP configuration file (" + idpXml + "): " + e.getMessage());
				System.exit(1);
			} catch (AttributeResolverException e) {
				System.err.println("Error initializing the Attribute Resolver: " + e.getMessage());
				System.exit(1);
			} catch (ArpException e) {
				System.err.println("Error initializing the ARP Engine: " + e.getMessage());
				System.exit(1);
			} catch (MalformedURLException e) {
				System.err.println("Specified resource URL is invalid: " + e.getMessage());
				System.exit(1);
			}
		} else {
			try {
				resolver = new AttributeResolver(resolverxml);
			} catch (AttributeResolverException e) {
				System.err.println("Error initializing the Attribute Resolver: " + e.getMessage());
				System.exit(1);
			}
		}
	}

	private static void printAttributes(PrintStream out, AAAttributeSet attributeSet) {

		try {
			for (ShibAttributeIterator iterator = attributeSet.shibAttributeIterator(); iterator.hasNext();) {
				AAAttribute attribute = iterator.nextShibAttribute();
				Node node = attribute.toDOM();

				ByteArrayOutputStream xml = new ByteArrayOutputStream();
				if (!(node instanceof Element)) {
					System.err.println("Received bad Element data from SAML library.");
					System.exit(1);
				}
				out.println(Parser.serialize(node));
				out.println();
			}
		} catch (SAMLException e) {
			System.err.println("Error creating SAML attribute: " + e.getMessage());
			System.exit(1);
		}
	}

	private static void configureLogging(boolean debugEnabled) {

		ConsoleAppender rootAppender = new ConsoleAppender();
		rootAppender.setWriter(new PrintWriter(System.out));
		rootAppender.setName("stdout");
		Logger.getRootLogger().addAppender(rootAppender);

		if (debugEnabled) {
			Logger.getRootLogger().setLevel(Level.DEBUG);
			rootAppender.setLayout(new PatternLayout("%-5p %-41X{serviceId} %d{ISO8601} (%c:%L) - %m%n"));
		} else {
			Logger.getRootLogger().setLevel(Level.INFO);
			Logger.getLogger("edu.internet2.middleware.shibboleth.aa.attrresolv").setLevel(Level.WARN);
			rootAppender.setLayout(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN));
		}
		Logger.getLogger("org.apache.xml.security").setLevel(Level.OFF);
	}

	private static void printUsage(PrintStream out) {

		// out.println("Tests an AA Attribute Resolver configuration.");
		out.println("Usage: resolvertest --user=USER {--idpXml=URL|--resolverxml=URL} [OPTION...]");
		out.println();
		out.println("Options:");
		out.println("  -h, --help                Print usage information");
		out.println("  -d, --debug               Run in debug mode");
		out.println("  --idpXml=FILEURL          URL of the IdP configuration file. Attributes");
		out.println("                              will be filtered according to the Attribute Release");
		out.println("                              Policy (ARP) specified in the configuration file");
		out.println("  --resolverxml=FILEURL     URL of the resolver configuration file. No ARP");
		out.println("                              filtering will be done");
		out.println("  --user=USER               User for whom attributes should be resolved");
		out.println("  --requester=REQUESTER     Name of the requester (SP). Emulates");
		out.println("                              unauthenticated requester if not specified");
		out.println("  --responder=RESPONDER     Name of the responder (IdP).");
		out.println("  --resource=URL            URL of the resource. Only attributes available");
		out.println("                              to any resource will be returned if not specified");
	}
}
