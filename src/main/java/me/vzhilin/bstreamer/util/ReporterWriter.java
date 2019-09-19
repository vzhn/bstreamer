package me.vzhilin.bstreamer.util;

import java.io.PrintStream;
import java.util.stream.Stream;

public final class ReporterWriter {
    private final Column[] columns;
    private final int size;
    private final int width;

    public final static class Column {
        private final String name;
        private final int width;

        public Column(String name, int width) {
            this.name = name;
            this.width = width;
        }

        public String getName() {
            return name;
        }

        public int getWidth() {
            return width;
        }
    }

    public ReporterWriter(Column... columns) {
        this.columns = columns;
        this.size = columns.length;
        this.width = Stream
                .of(columns)
                .map(Column::getWidth)
                .reduce((a, b) -> a + b + 3).get();
    }

    public void writeHeader(PrintStream out) {
        writeLine(out, extractNames());
        out.println();
        writeDelimiter(out, '=');
    }

    public void writeLine(PrintStream out, String... vs) {
        StringBuilder sb = new StringBuilder();
        sb.append('\r');
        for (int i = 0; i < vs.length; i++) {
            String v = vs[i];
            Column c = columns[i];
            sb.append(v);
            for (int r = 0; r < c.width -  v.length(); ++r) {
                sb.append(' ');
            }
            if (i < size - 1) {
                sb.append(" | ");
            }
        }
        out.print(sb);
    }

    private String[] extractNames() {
        String[] names = new String[size];
        for (int i = 0; i < size; i++) {
            names[i] = columns[i].name;
        }
        return names;
    }

    private void writeDelimiter(PrintStream out, char ch) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) {
            sb.append(ch);
        }
        out.println(sb);
    }
}
