@echo off
echo build using Java-11:
set JAVA_HOME=C:\Program Files\Java\jdk-11.0
rem echo ========================
rem echo JAVA_HOME=%JAVA_HOME%
rem echo JAVACMD=%JAVACMD%
rem echo ========================

echo run maven build: 
rem "call" because command 'mvn' is actually a cmd-file with an exit at the end. Without the call this also terminates *us* ||-(
rem call mvn clean package -Pproduction
call mvn clean install -Pproduction

pause
