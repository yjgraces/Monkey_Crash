@echo off & setlocal enabledelayedexpansion

set LIB_JARS=""
cd ..\lib
for %%i in (*) do set LIB_JARS=!LIB_JARS!;..\lib\%%i
cd ..\bin


java -Xms64m -Xmx1024m -XX:MaxPermSize=64M -Djava.net.preferIPv4Stack=true -classpath ..\;..\config;%LIB_JARS% com.yd.container.Main %1
goto end

:end
pause