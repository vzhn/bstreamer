package me.vzhilin.mediaserver.util.metric;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PeriodCounter {
    private final Record /* last */ minute;
    private final Record /* last */ hour;
    private final Record /* last */ day;
    private final Record /* last */ week;
    private final List<Record> records = new ArrayList<>();

    private long totalCounter;

    public PeriodCounter() {
        minute = new Record(60, 1, TimeUnit.SECONDS);
        hour = new Record(60, 1, TimeUnit.MINUTES);
        day = new Record(24, 1, TimeUnit.HOURS);
        week = new Record(7, 1, TimeUnit.DAYS);

        records.add(minute);
        records.add(hour);
        records.add(day);
        records.add(week);
    }

    public Snapshot getMinute() {
        return minute.snapshot(System.currentTimeMillis());
    }

    public Snapshot getHour() {
        return hour.snapshot(System.currentTimeMillis());
    }

    public Snapshot getDay() {
        return day.snapshot(System.currentTimeMillis());
    }

    public Snapshot getWeek() {
        return week.snapshot(System.currentTimeMillis());
    }

    public void start(long startTimeMillis) {
        for (int i = 0; i < records.size(); i++) {
            records.get(i).start(startTimeMillis);
        }
    }

    public void inc(long timeMillis, long c) {
        synchronized (this) {
            totalCounter += c;
        }
        for (int i = 0; i < records.size(); i++) {
            records.get(i).inc(timeMillis, c);
        }
    }

    public long total() {
        synchronized (this) {
            return totalCounter;
        }
    }

    private static class Record {
        private final int period;
        private final TimeUnit periodUnit;
        private final int maxSize;
        private final long tickDurationMillis;
        private long startMillis = Long.MIN_VALUE;
        private final Deque<Sample> deque;
        private final Function<Sample, DateSample> sampleToDate;

        public Record(int maxSize, int period, TimeUnit periodUnit) {
            this.maxSize = maxSize;
            this.period = period;
            this.periodUnit = periodUnit;
            this.deque = new ArrayDeque<>(maxSize);
            this.tickDurationMillis = period * periodUnit.toMillis(period);
            sampleToDate = sample -> sample.asDate(startMillis, tickDurationMillis);
        }

        public synchronized void inc(long timeMillis, long c) {
            if (startMillis == Long.MIN_VALUE) {
                startMillis = timeMillis;
            }
            final long deltaMillis = timeMillis - startMillis;
            long ticks = periodUnit.convert(deltaMillis, TimeUnit.MILLISECONDS) / period;
            if (deque.isEmpty()) {
                deque.addLast(new Sample(ticks, c));
            } else {
                Sample last = deque.getLast();
                final long lastTicks = last.getTicks();
                if (last.ticks == ticks) {
                    last.inc(c);
                } else {
                    for (int i = 1; i < maxSize && i < (ticks - lastTicks); i++) {
                        deque.addLast(new Sample(lastTicks + i, 0));
                    }
                    while (deque.size() >= maxSize) {
                        deque.removeFirst();
                    }
                    deque.addLast(new Sample(ticks, c));
                }
            }
        }

        public synchronized void start(long timeMillis) {
            this.deque.clear();
            this.startMillis = timeMillis;
        }

        public synchronized Snapshot snapshot(long timeMillis) {
            inc(timeMillis, 0);
            List<DateSample> dateSamples = deque.stream().map(sampleToDate).collect(Collectors.toList());
            return new Snapshot(period, periodUnit, dateSamples);
        }
    }

    private static class Sample {
        private final long ticks;
        private long c;

        public Sample(long ticks, long c) {
            this.ticks = ticks;
            this.c = c;
        }

        public void inc(long v) {
            this.c += v;
        }

        public long getTicks() {
            return ticks;
        }

        public long getCount() {
            return c;
        }

        public DateSample asDate(long startMillis, long tickDurationMillis) {
            Instant instant = Instant.ofEpochMilli(startMillis).plusMillis(ticks * tickDurationMillis);
            LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return new DateSample(time, c);
        }
    }

    private static class DateSample {
        private final LocalDateTime time;
        private final long count;

        private DateSample(LocalDateTime time, long count) {
            this.time = time;
            this.count = count;
        }
    }

    public static class Snapshot {
        private final List<DateSample> samples;
        private final int period;
        private final TimeUnit periodUnit;

        public Snapshot(int period, TimeUnit periodUnit, List<DateSample> samples) {
            this.period = period;
            this.periodUnit = periodUnit;
            this.samples = samples;
        }

        private DateSample getLastEntireSample() {
            if (samples.size() >= 2) {
                return samples.get(samples.size() - 2);
            } else {
                return null;
            }
        }

        public long getLastEntireSampleCount() {
            DateSample s = getLastEntireSample();
            return s == null ? 0 : s.count;
        }

        public long getLastSampleCount() {
            if (!samples.isEmpty()) {
                return samples.get(0).count;
            } else {
                return 0;
            }
        }
    }
}
