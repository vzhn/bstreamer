heat dir target\appassembler\repo -dr RepoDir -cg RepoGroup -gg -sfrag -template fragment -out repo.wxs
heat dir target\appassembler\bin -dr BinDir -cg BinGroup -gg -sfrag -template fragment -out bin.wxs
candle src\wix\bstreamer.wxs repo.wxs bin.wxs
light -b target\appassembler\repo repo.wixobj bin.wixobj bstreamer.wixobj -o bstreamer.msi