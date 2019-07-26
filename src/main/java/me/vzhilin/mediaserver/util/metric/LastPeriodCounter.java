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

public class LastPeriodCounter {
    private final Record /* last */ minute;
    private final Record /* last */ hour;
    private final Record /* last */ day;
    private final Record /* last */ week;
    private final List<Record> records = new ArrayList<>();

    public LastPeriodCounter() {
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
        return minute.snapshot();
    }

    public Snapshot getHour() {
        return hour.snapshot();
    }

    public Snapshot getDay() {
        return day.snapshot();
    }

    public Snapshot getWeek() {
        return week.snapshot();
    }

    public void start(long startTimeMillis) {
        for (int i = 0; i < records.size(); i++) {
            records.get(i).start(startTimeMillis);
        }
    }

    public void inc(long timeMillis, int c) {
        for (int i = 0; i < records.size(); i++) {
            records.get(i).inc(timeMillis, c);
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

        public void inc(long timeMillis, int c) {
            if (startMillis == Long.MIN_VALUE) {
                startMillis = timeMillis;
            }
            final long deltaMillis = timeMillis - startMillis;
            long ticks = periodUnit.convert(deltaMillis, TimeUnit.MILLISECONDS) / period;
            if (!deque.isEmpty() && deque.getLast().ticks == ticks) {
                deque.getLast().inc(c);
            } else {
                while (deque.size() >= maxSize) {
                    deque.removeFirst();
                }
                deque.addLast(new Sample(ticks, c));
            }
        }

        public void start(long timeMillis) {
            this.deque.clear();
            this.startMillis = timeMillis;
        }

        public Snapshot snapshot() {
            List<DateSample> dateSamples = deque.stream().map(sampleToDate).collect(Collectors.toList());
            return new Snapshot(period, periodUnit, dateSamples);
        }
    }

    private static class Sample {
        private final long ticks;
        private int c;

        public Sample(long ticks, int c) {
            this.ticks = ticks;
            this.c = c;
        }

        public void inc(int v) {
            this.c += v;
        }

        public long getTicks() {
            return ticks;
        }

        public int getC() {
            return c;
        }

        public DateSample asDate(long startMillis, long tickDurationMillis) {
            Instant instant = Instant.ofEpochMilli(startMillis).plusMillis(tickDurationMillis);
            LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return new DateSample(time, ticks);
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

    private static class Snapshot {
        private final List<DateSample> samples;
        private final int period;
        private final TimeUnit periodUnit;

        public Snapshot(int period, TimeUnit periodUnit, List<DateSample> samples) {
            this.period = period;
            this.periodUnit = periodUnit;
            this.samples = samples;
        }
    }
}
