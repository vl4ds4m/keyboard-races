set JAVA_HOME=C:\Program Files\Java

"%JAVA_HOME%\jdk-17\bin\java.exe" --module-path "%JAVA_HOME%\javafx-sdk-17.0.7\lib";^
.\client\target\client-1.0-SNAPSHOT.jar;.\game\target\game-1.0-SNAPSHOT.jar ^
--module org.vl4ds4m.keyboardraces.client/org.vl4ds4m.keyboardraces.client.Main