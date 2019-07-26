package me.vzhilin.mediaserver.util.metric;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class TemporalCounter {
    private final Record minute;
    private final Record hour;
    private final Record day;
    private final Record week;

    private List<Record> records = new ArrayList<>();
    public TemporalCounter() {
        minute = new Record(60, ChronoUnit.SECONDS);
        hour = new Record(60, ChronoUnit.MINUTES);
        day = new Record(24, ChronoUnit.HOURS);
        week = new Record(7, ChronoUnit.DAYS);

        records.add(minute);
        records.add(hour);
        records.add(day);
        records.add(week);
    }

    public void inc(long epochMillis, int v) {
        ZoneOffset offset = OffsetDateTime.now().getOffset();
        int nanoOfSecond = (int) TimeUnit.MILLISECONDS.toNanos(epochMillis % 1000);
        long epochSecond = TimeUnit.MILLISECONDS.toSeconds(epochMillis);
        LocalDateTime time = LocalDateTime.ofEpochSecond(epochSecond, nanoOfSecond, offset);
        records.forEach(record -> record.inc(time, v));
    }

    public static class Record {
        private final int maxSize;
        private final TemporalUnit periodUnit;
        private final Deque<Sample> deque;

        public Record(int n, TemporalUnit periodUnit) {
            this.maxSize = n;
            this.periodUnit = periodUnit;
            deque = new ArrayDeque<>(n);
        }

        public void inc(LocalDateTime dateTime, int value) {
            LocalDateTime time = dateTime.truncatedTo(periodUnit);
            if (deque.isEmpty() || !deque.getLast().ts.equals(time)) {
                if (deque.size() == maxSize) {
                    deque.removeFirst();
                }
                deque.addLast(new Sample(time, value));
            } else {
                deque.getLast().inc(value);
            }
        }
    }

    private static class Sample {
        public LocalDateTime ts;
        public long sum;

        public Sample(LocalDateTime ts, int value) {
            this.ts = ts;
            this.sum = value;
        }

        public void inc(int value) {
            sum += value;
        }
    }
}
