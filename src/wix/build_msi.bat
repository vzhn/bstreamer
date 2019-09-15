set REPODIR=target\appassembler\repo
set BINDIR=target\appassembler\bin
set CONFDIR=src\conf
set VIDEODIR=src\video

IF NOT "%VIDEO_LOCATION%"=="" (set VIDEODIR=%VIDEO_LOCATION%)

heat dir %REPODIR% -dr RepoDir -cg RepoGroup -gg -sfrag -template fragment -out repo.wxs
heat dir %BINDIR% -dr BinDir -cg BinGroup -gg -sfrag -template fragment -out bin.wxs
heat dir %CONFDIR% -dr ConfDir -cg ConfGroup -gg -sfrag -template fragment -out conf.wxs
heat dir %VIDEODIR% -dr VideoDir -cg VideoGroup -gg -sfrag -template fragment -out video.wxs

candle src\wix\bstreamer.wxs repo.wxs bin.wxs conf.wxs video.wxs
light -b %BINDIR% -b %REPODIR% -b %CONFDIR% -b %VIDEODIR% ^
  conf.wixobj repo.wixobj bin.wixobj video.wixobj bstreamer.wixobj -o bstreamer.msi