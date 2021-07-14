:start
echo run application using Java-11:
set JAVA_HOME=C:\Program Files\Java\jdk-11.0

rem goto directory in which THIS .cmd file sits:
cd  %~p0
rem start the application using default config:
"%JAVA_HOME%\bin\java" -jar kis-monitoring-1.0-SNAPSHOT.jar default.kmc

pause
goto start
