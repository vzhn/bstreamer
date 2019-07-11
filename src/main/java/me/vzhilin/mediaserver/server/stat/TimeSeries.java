package me.vzhilin.mediaserver.server.stat;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TimeSeries {
    private final long capacityMillis;
    private final Deque<TimeSeriesEntry> entries = new LinkedList<>();
    private long lastTime;
    private long lastPayload;
    private int lastPackets;

    public TimeSeries(long capacity, TimeUnit capacityUint) {
        capacityMillis = TimeUnit.MILLISECONDS.convert(capacity, capacityUint);
    }

    public synchronized void put(int npackets, long payloadSize) {
        long now = System.currentTimeMillis();
        while (!entries.isEmpty() && now - entries.peek().getTimeMillis() > capacityMillis) {
            entries.poll();
        }
        if (lastTime / 1000 != now / 1000) {
            entries.add(new TimeSeriesEntry(lastTime / 1000 * 1000, lastPackets, lastPayload));
            lastTime = now;
            lastPayload = 0;
            lastPackets = 0;
        } else {
            lastPackets += npackets;
            lastPayload += payloadSize;
        }
    }

    public synchronized void drain(List<TimeSeriesEntry> collection) {
        collection.addAll(entries);
    }

    public final static class TimeSeriesEntry {
        private final long timeMillis;
        private final long payloadSize;
        private final int npackets;

        public TimeSeriesEntry(long timeMillis, int npackets, long payloadSize) {
            this.npackets = npackets;
            this.timeMillis = timeMillis;
            this.payloadSize = payloadSize;
        }

        public long getTimeMillis() {
            return timeMillis;
        }

        public long getPayloadSize() {
            return payloadSize;
        }

        public int getNpackets() {
            return npackets;
        }
    }
}
