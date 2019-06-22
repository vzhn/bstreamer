package me.vzhilin.mediaserver.server.strategy.seq;

import me.vzhilin.mediaserver.media.MediaPacketSourceDescription;
import me.vzhilin.mediaserver.media.MediaPacketSourceFactory;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategy;
import me.vzhilin.mediaserver.server.strategy.StreamingStrategyFactory;

public class SequencedStrategyFactory implements StreamingStrategyFactory {
    @Override
    public StreamingStrategy getStrategy(MediaPacketSourceFactory sf) {
        return null;
    }

    @Override
    public MediaPacketSourceDescription describe(MediaPacketSourceFactory sourceFactory) {
        return null;
    }
}
