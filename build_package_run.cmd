echo build using Java-11:
set JAVA_HOME=C:\Program Files\AdoptOpenJDK\jdk-11.0.8.10-hotspot

echo run maven build:
rem mvn clean package -Pproduction

rem we need the properties file HERE:
echo copying resources to right place:
for %%f in (application.properties banner.txt logback.xml) do copy target\classes\%%f target\
for %%f in (default.kmc) do copy %%f target\

echo "<ENTER> to run application, ^C to quit:"
pause

echo run application:
cd target
"%JAVA_HOME%\bin\java" -jar kis-monitoring-1.0-SNAPSHOT.jar

pause
