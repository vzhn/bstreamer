heat dir target\appassembler\repo -dr RepoDir -cg RepoGroup -gg -sfrag -template fragment -out repo.wxs
candle src\wix\mediaserver.wxs repo.wxs
light -b target\appassembler\repo repo.wixobj mediaserver.wixobj -o mediaserver.msi