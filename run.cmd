@echo off
set KISMON_HOME=U:\Documents\eclipse\workspace_Vaadin\KIS-Monitoring
cd %KISMON_HOME%

set JAVA_HOME=C:\Program Files\Java\jdk-17.0
echo run application using %JAVA_HOME%:
set KISMON_JAR=kis-monitoring-1.1-SNAPSHOT.jar

rem we need the properties file HERE:
echo copying required resources to right place:
for %%f in (application.properties banner.txt logback.xml) do copy src\main\resources\%%f target\
for %%f in (logback-test.xml) do copy src\test\resources\%%f target\
for %%f in (*.kmc *.p12) do copy %%f target\
rem TLS logging utility:
for %%f in (jSSLKeyLog.jar) do copy %%f target\

cd target

rem useful Java options:
rem "-Djavax.net.debug=ssl:handshake" 
rem -Djdk.httpclient.allowRestrictedHeaders=host,connection" 

rem standard version - with args:
rem "%JAVA_HOME%\bin\java" -cp . -jar %KISMON_JAR% "%KISMON_HOME%\ksta-old-VMs.kmc"
rem "%JAVA_HOME%\bin\java" -cp . -jar %KISMON_JAR% "%KISMON_HOME%\ksta.kmc" "%KISMON_HOME%\test.kmc"
rem no args to use the file specified in application.properties:
rem "%JAVA_HOME%\bin\java" -cp . -jar %KISMON_JAR% "%KISMON_HOME%\frobi.kmc"
rem "%JAVA_HOME%\bin\java" -cp . -jar %KISMON_JAR% "%KISMON_HOME%\ksta.kmc" "%KISMON_HOME%\zhservices.kmc"
"%JAVA_HOME%\bin\java" "-Djdk.httpclient.allowRestrictedHeaders=host,connection" -cp . -jar %KISMON_JAR% "%KISMON_HOME%\zhservices.kmc"
rem with TLS logging for WireShark:
rem "%JAVA_HOME%\bin\java" -javaagent:jSSLKeyLog.jar==%USERPROFILE%\Desktop\kismon_logfile.log -cp . -jar "%KISMON_JAR%" "%KISMON_HOME%\ksta.kmc" "%KISMON_HOME%\test.kmc"

cd ..
