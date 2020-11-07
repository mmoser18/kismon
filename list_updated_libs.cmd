echo Checking for updated dependencies in repository
mvn versions:display-dependency-updates
pause
echo Checking for updated version numbers specified in properties section:
mvn versions:display-property-updates
