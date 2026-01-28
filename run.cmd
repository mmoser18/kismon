@echo off

rem note: adapt to the location where the KISMON jar and other files are located:
set KISMON_HOME=U:\Documents\eclipse\workspace_Vaadin\KIS-Monitoring
cd %KISMON_HOME%

set JAVA_HOME=C:\Program Files\Java\jdk-21.0
echo running KISMON using %JAVA_HOME%:
set KISMON_JAR=kis-monitoring-1.1-SNAPSHOT.jar

rem we need the properties file and a few others HERE:
echo copying required resources to target:
for %%f in (application.properties banner.txt logback.xml) do copy src\main\resources\%%f target\
rem what was that intended for? It caused logback to always use the logback-test.xml which drove me nuts!
rem for %%f in (logback-test.xml) do copy src\test\resources\%%f target\
for %%f in (*.p12) do copy src\main\resources\%%f target\
rem disabled - taking config from parent dir to avoid overwriting possibly adapted config at each startup
for %%f in (*.kmc) do copy %%f target\

rem in case of TLS handshake issues you can enable jSSLKeyLog to be able to read and record TLS-messages using WireShark to analyze issues
rem TLS logging utility:
rem for %%f in (jSSLKeyLog.jar) do copy %%f target\

cd target
pwd

rem useful Java options (in case of handshake issues):
rem "-Djavax.net.debug=ssl:handshake" 


rem standard version - with config file names (with path) as args - e.g.:
rem set configFile=test.kmc
set configFile=home.kmc
"%JAVA_HOME%\bin\java" -cp . -jar "%KISMON_JAR%" "%KISMON_HOME%\%configFile%"

rem starting without args --> will use the config file specified in application.properties:
rem "%JAVA_HOME%\bin\java" -cp . -jar "%KISMON_JAR%"

rem with TLS logging enabled (for WireShark):
rem "%JAVA_HOME%\bin\java" -javaagent:jSSLKeyLog.jar==%USERPROFILE%\ssl-keys.log -cp . -jar "%KISMON_JAR%" "%KISMON_HOME%\%configFile%"

cd ..
pwd
