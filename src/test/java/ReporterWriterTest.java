import me.vzhilin.bstreamer.util.ReporterWriter;
import org.junit.Test;

public class ReporterWriterTest {
    @Test
    public void testReporter() {
        ReporterWriter.Column[] cols = new ReporterWriter.Column[] {
            new ReporterWriter.Column("time", 10),
            new ReporterWriter.Column("server connections", 20),
            new ReporterWriter.Column("bandwidth", 10)
        };
        ReporterWriter rp = new ReporterWriter(cols);
        rp.writeHeader(System.out);
        rp.writeLine(System.out, "12:34:56", "3000 [+12; -13]", " 21.5 GB");
    }
}
