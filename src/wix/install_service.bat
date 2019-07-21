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
set PR_CLASSPATH=repo\${project.build.finalName}.jar;${maven.compile.classpath}

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
prunsrv.exe //IS//%SERVICE_NAME%
