package io.github.mavaze.mockserver;

import org.junit.Ignore;
import org.junit.Test;

public class ApplicationTest {

    @Test
    @Ignore
    public void test1() throws Exception {
        Application.main(new String[] {"-f", "config.yaml", "-f", "config.yaml"});
        Thread.sleep(69*60*1000); // FIXME: I don't need of 1 hour of waiting
    }

    /**
     * Execution is ambiguous as no stub mappings provided.
     */
    @Ignore
    @Test(expected = IllegalArgumentException.class)
    public void test2() throws Exception {
        Application.main(new String[] {});
    }
}
