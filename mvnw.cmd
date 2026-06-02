@echo off
SET JAVA_HOME=C:\Program Files\Java\jdk-23
SET MVN=C:\Users\ahmad\Downloads\apache-maven-3.9.16\bin\mvn.cmd
"%MVN%" -f "%~dp0pom.xml" %*

