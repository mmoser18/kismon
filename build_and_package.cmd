@echo off

cd U:\Documents\eclipse\workspace_Vaadin\KIS-Monitoring
set JAVA_HOME=C:\Program Files\Java\jdk-21.0
echo building using %JAVA_HOME%:
rem echo ========================
rem echo JAVA_HOME=%JAVA_HOME%
rem echo JAVACMD=%JAVACMD%
rem echo ========================

echo run maven build: 
rem For this to work flawlessly the env-variables "M2=C:\Program Files\Apache\Maven\bin" and "M2_HOME=C:\Program Files\Apache\Maven" 
rem should be set! 
rem "call" because command 'mvn' is actually a cmd-file with an exit at the end. Without the call this also terminates *us* ||-(
call mvn clean verify -Pproduction

pause
