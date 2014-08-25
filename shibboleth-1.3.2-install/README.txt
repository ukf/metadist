                             Internet 2's Shibboleth Identity Provider
                                           Verion 1.3.1
                                           Jan-08-2007

Introduction
=================================
Shibboleth is a federated web authentication and attribute exchange system
based on SAML developed by Internet2 and MACE.

Shibboleth is divided into identity and service provider components, with the
IdP in Java and the SP in C and C++.

Please review the terms described in the LICENSE file before using this
code. It is now the Apache 2.0 license.



Documentation
=================================
Website (includes mailing list information):
    http://shibboleth.internet2.edu/

Distributions:
    http://shibboleth.internet2.edu/downloads/
    
Wiki: (most up to date documentation)
    https://spaces.internet2.edu/display/SHIB/WebHome
    
Bug/Issue Tracker:
    http://bugzilla.internet2.edu/
    


Quick Installation
=================================
$ export JAVA_HOME=(the path to your java installation)
$ cd shibboleth-idp-install
$ ./ant install

For in-depth installation and configuration instructions refer to the Shibboleth Wiki.