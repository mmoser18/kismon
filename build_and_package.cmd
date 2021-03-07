echo build using Java-11:
set JAVA_HOME=C:\Program Files\Java\jdk-11.0

echo run maven build:  ("call" because that's a cmd-file with an exit at the end. Without the call this also terminates *us*)
call mvn package -Pproduction

rem we need the properties file HERE:
echo copying resources to right place:
for %%f in (application.properties banner.txt logback.xml) do copy src\main\resources\%%f target\
for %%f in (default.kmc) do copy %%f target\
