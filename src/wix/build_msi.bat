heat dir target\appassembler\repo -dr RepoDir -cg RepoGroup -gg -sfrag -template fragment -out repo.wxs
candle src\wix\bstreamer.wxs repo.wxs
light -b target\appassembler\repo repo.wixobj bstreamer.wixobj -o bstreamer.msi