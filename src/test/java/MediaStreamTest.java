import me.vzhilin.mediaserver.stream.Cursor;
import me.vzhilin.mediaserver.stream.ItemFactory;
import me.vzhilin.mediaserver.stream.Stream;
import org.junit.Test;

public class MediaStreamTest {
    @Test
    public void test()  {
        Stream<Integer> s = new Stream<>(new ItemFactory<Integer>() {
            private int v = 0;
            @Override
            public Integer next() {
                return v++;
            }

            @Override
            public void free(Integer item) {
                System.err.println("free: " + item);
            }
        });


        Cursor<Integer> c1 = s.cursor();
        Cursor<Integer> c2 = s.cursor();
        Cursor<Integer> c3 = s.cursor();

        System.err.println(c1.next());
        System.err.println(c1.next());
        System.err.println(c2.next());
        System.err.println(c2.next());

        System.err.println(c1.next());
        System.err.println(c1.next());
        System.err.println(c2.next());
        System.err.println(c2.next());

        c3.close();

        System.err.println(c1.next());
        System.err.println(c1.next());
        System.err.println(c2.next());
        System.err.println(c2.next());
    }

}
