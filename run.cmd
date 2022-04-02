@echo off
rem set JAVA_HOME=C:\Program Files\Java\jdk-11.0
set JAVA_HOME=C:\Program Files\Java\jdk-17.0
echo run application using %JAVA_HOME%:

rem we need the properties file HERE:
echo copying required resources to right place:
for %%f in (application.properties banner.txt logback.xml) do copy src\main\resources\%%f target\
for %%f in (logback-test.xml) do copy src\test\resources\%%f target\

cd target
"%JAVA_HOME%\bin\java" -cp . -jar kis-monitoring-1.0-SNAPSHOT.jar U:\Documents\eclipse\workspace_Vaadin\KIS-Monitoring\default.kmc
cd ..
