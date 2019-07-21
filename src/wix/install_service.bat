set CURDIR=%~dp0
set SERVICE_NAME=Mediaserver
set PR_INSTALL=%CURDIR%\prunsrv.exe

REM Service log configuration
set PR_LOGPREFIX=%SERVICE_NAME%
set PR_LOGPATH=%CURDIR%\logs
set PR_STDOUTPUT=%CURDIR%\logs\stdout.txt
set PR_STDERROR=%CURDIR%\logs\stderr.txt
set PR_LOGLEVEL=Error

REM Path to java installation
set PR_JVM=C:\Program Files\Java\jdk-12.0.2\bin\server\jvm.dll
set PR_CLASSPATH="repo\mediaserver-0.3-SNAPSHOT.jar;repo\ffmpeg-4.1-1.4.4.jar;repo\javacpp-1.4.4.jar;repo\ffmpeg-4.1-1.4.4-windows-x86_64.jar;repo\commons-daemon-1.2.0.jar;repo\netty-all-4.1.36.Final.jar;repo\log4j-1.2.17.jar;repo\slf4j-api-1.7.13.jar;repo\slf4j-log4j12-1.7.13.jar;repo\metrics-core-4.1.0.jar;repo\simpleclient_dropwizard-0.6.0.jar;repo\simpleclient_httpserver-0.6.0.jar;repo\simpleclient_common-0.6.0.jar;repo\simpleclient-0.6.0.jar;repo\jfreechart-1.5.0.jar;repo\jackson-dataformat-yaml-2.9.9.jar;repo\jackson-core-2.9.9.jar;repo\snakeyaml-1.24.jar;repo\junit-4.13-beta-3.jar;repo\hamcrest-core-1.3.jar"

REM Startup configuration
set PR_STARTPATH=%CURDIR%
set PR_STARTUP=auto
set PR_STARTMODE=jvm
set PR_STARTCLASS=me.vzhilin.mediaserver.EntryPoint
set PR_STARTMETHOD=start

REM Shutdown configuration
set PR_STOPMODE=jvm
set PR_STOPCLASS=me.vzhilin.mediaserver.EntryPoint
set PR_STOPMETHOD=stop

REM JVM configuration
set PR_JVMMS=64
set PR_JVMMX=256
set PR_JVMSS=256
set PR_JVMOPTIONS=

REM Install service
procrun.exe //IS//%SERVICE_NAME%
