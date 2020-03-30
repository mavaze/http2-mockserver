package io.github.mavaze.mockserver;

import org.junit.Ignore;
import org.junit.Test;

public class ApplicationTest {

    @Test
    public void test1() throws Exception {
        Application.main(new String[] {"-f", "src/test/resources/input.yaml", "-f", "src/test/resources/input.yaml"});
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
