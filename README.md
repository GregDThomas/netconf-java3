netconf-java
============
Java library for NETCONF

Support
=======
This software is not officially supported by Juniper Networks, but by a team dedicated to helping customers,
partners, and the development community.  To report bug-fixes, issues, suggestions, please raise issues
or even better submit pull requests on GitHub.

Requirements
============
* Java 8

Logging
=======
* The library uses both the Log4j 2 API and the Slf4J libraries to log information. Suitable libraries should be 
included to capture this output (e.g. log4j-core-2,  log4j-to-slf4j-2, log4j-slf4j-impl).
* Under normal operation it should be sufficient to log both `net.juniper.netconf` and `org.apache.sshd` at `INFO` 
level. During initial development, it may be necessary to bump one or both of these up to `DEBUG` or even `TRACE` level.

Releases
========
Version 3.0 is a significant update to netconf-java with a different API to earlier versions. 
Please refer to [Version 2.0](https://github.com/Juniper/netconf-java/releases/tag/v2.1.1.6) for details of earlier 
versions.