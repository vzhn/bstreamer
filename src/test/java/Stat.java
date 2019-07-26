import me.vzhilin.mediaserver.util.metric.PeriodCounter;

public class Stat {
    public static void main(String... argv) {
        new Stat().start();
    }

    private void start() {
        PeriodCounter c = new PeriodCounter();

        int k = 0;
        while (k ++ < 100000000) {
            c.inc(System.currentTimeMillis(), 1);
        }

        System.err.println(c);
    }
}
