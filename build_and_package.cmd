@echo off
echo build using Java-11:
set JAVA_HOME=C:\Program Files\Java\jdk-11.0
rem echo ========================
rem echo JAVA_HOME=%JAVA_HOME%
rem echo JAVACMD=%JAVACMD%
rem echo ========================

echo run maven build: 
rem "call" because that's a cmd-file with an exit at the end. Without the call this also terminates *us*
rem call mvn clean package -Pproduction
call mvn clean install -Pproduction

rem we need the properties file HERE:
echo copying resources to right place:
for %%f in (application.properties banner.txt logback.xml) do copy src\main\resources\%%f target\
for %%f in (default.kmc) do copy %%f target\
