# filter_remoteAddr
Check if the RemoteAddr match the pattern


1. Principle
-------------
The filter verify that the RemoteAddr match the pattern. This is the same usage as the org.apache.catalina.valves.RequestFilterValve except that a filter can be setup for a specific URL, not all the tomcat
 
2. Installation
---------------------------
	2.1 copy the  RemoteAddrServletValve-1.0.jar to <TOMCAT>/webapps/bonita/WEB-INF/lib
	2.2 register in the web.xml
	
	
	To register the filter in Tomcat, edit the file <TOMCAT>/webapps/bonita/WEB-INF/web.xml

	<filter>
		<filter-name>RemoteAddr</filter-name>
		<filter-class>org.bonitasoft.filter.allowed.FilterRemoteAddr</filter-class>		
		<init-param>
      		<param-name>deny</param-name>
      		<param-value>111\.222\.333\.444</param-value>
    	</init-param>   
		<init-param>
      		<param-name>allow</param-name>
      		<param-value>127\.0\.0\.1</param-value>
    	</init-param>
	</filter>

	<filter-mapping>
		<filter-name>RemoteAddr</filter-name>
		<url-pattern>/serverAPI/*</url-pattern>
	</filter-mapping>
	

where :
allow	
A regular expression (using java.util.regex) that the remote client's IP address is compared to. If this attribute is specified, the remote address MUST match for this request to be accepted. If this attribute is not specified, all requests will be accepted UNLESS the remote address matches a deny pattern.

deny	
A regular expression (using java.util.regex) that the remote client's IP address is compared to. If this attribute is specified, the remote address MUST NOT match for this request to be accepted. If this attribute is not specified, request acceptance is governed solely by the allow attribute.


5. Use it 
-----------
Restart the server
