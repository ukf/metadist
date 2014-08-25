<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html 
	PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" 
	"DTD/xhtml1-strict.dtd">
	<%@ taglib uri="/WEB-INF/tlds/struts-logic.tld" prefix="logic" %>
	<%@ taglib uri="/WEB-INF/tlds/struts-bean.tld" prefix="bean" %>
	
	<jsp:useBean id="requestURL" scope="request" class="java.lang.String"/>
	<jsp:useBean id="errorText" scope="request" class="java.lang.String"/>
	
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<link rel="stylesheet" type="text/css" href="main.css" />
	<title>Shibboleth Identity Provider Failure</title>
</head>

<body>

<p class="error">Identity Provider failure at (<bean:write name="requestURL" />)</p>

<p><bean:write name="errorText" /></p>

<p>A malfunction occurred in the application or service you were attempting to
access, preventing the login request from being processed. Please contact the
support staff associated with that application or service to report the problem.</p>

<p>You will generally need to provide them with detailed information on how you
accessed the web site for them to determine the cause of the error.
Any bookmarks used or the initial URL you accessed may be helpful to them.</p>

</body>
</html>
