@echo off
cd U:\Documents\eclipse\workspace_Vaadin\KIS-Monitoring

rem set JAVA_HOME=C:\Program Files\Java\jdk-11.0
set JAVA_HOME=C:\Program Files\Java\jdk-17.0
echo run application using %JAVA_HOME%:

rem we need the properties file HERE:
echo copying required resources to right place:
for %%f in (application.properties banner.txt logback.xml) do copy src\main\resources\%%f target\
for %%f in (logback-test.xml) do copy src\test\resources\%%f target\
for %%f in (*.kmc) do copy %%f target\
rem TLS logging utility:
for %%f in (jSSLKeyLog.jar) do copy %%f target\

cd target

rem standard version - with args:
rem "%JAVA_HOME%\bin\java" -cp . -jar kis-monitoring-1.0-SNAPSHOT.jar "U:\Documents\eclipse\workspace_Vaadin\KIS-Monitoring\ksta-old-VMs.kmc"
rem "%JAVA_HOME%\bin\java" -cp . -jar kis-monitoring-1.0-SNAPSHOT.jar U:\Documents\eclipse\workspace_Vaadin\KIS-Monitoring\ksta.kmc U:\Documents\eclipse\workspace_Vaadin\KIS-Monitoring\test.kmc
rem no args to use the file specified in application.properties:
"%JAVA_HOME%\bin\java" -cp . -jar kis-monitoring-1.0-SNAPSHOT.jar
rem with TLS logging for WireShark:
rem "%JAVA_HOME%\bin\java" -javaagent:jSSLKeyLog.jar==%USERPROFILE%\Desktop\kismon_logfile.log -cp . -jar kis-monitoring-1.0-SNAPSHOT.jar U:\Documents\eclipse\workspace_Vaadin\KIS-Monitoring\ksta.kmc U:\Documents\eclipse\workspace_Vaadin\KIS-Monitoring\test.kmc

cd ..
