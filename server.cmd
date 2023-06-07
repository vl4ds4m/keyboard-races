set JAVA_HOME=C:\Program Files\Java

"%JAVA_HOME%\jdk-17\bin\java.exe" ^
--module-path .\server\target\server-1.0-SNAPSHOT.jar;.\game\target\game-1.0-SNAPSHOT.jar ^
--module org.vl4ds4m.keyboardraces.server/org.vl4ds4m.keyboardraces.server.Server