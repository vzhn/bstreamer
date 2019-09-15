set REPODIR=target\appassembler\repo
set BINDIR=target\appassembler\bin
set CONFDIR=src\conf

heat dir %REPODIR% -dr RepoDir -cg RepoGroup -gg -sfrag -template fragment -out repo.wxs
heat dir %BINDIR% -dr BinDir -cg BinGroup -gg -sfrag -template fragment -out bin.wxs
heat dir %CONFDIR% -dr ConfDir -cg ConfGroup -gg -sfrag -template fragment -out conf.wxs

candle src\wix\bstreamer.wxs repo.wxs bin.wxs conf.wxs
light -b %BINDIR% -b %REPODIR% -b %CONFDIR% ^
  conf.wixobj repo.wixobj bin.wixobj bstreamer.wixobj -o bstreamer.msi