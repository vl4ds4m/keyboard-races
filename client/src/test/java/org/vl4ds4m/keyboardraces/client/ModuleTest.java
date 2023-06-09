package org.vl4ds4m.keyboardraces.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ModuleTest {
    @Test
    public void getAbsentResource() {
        String resource = "/random.abc";
        Assertions.assertThrows(RuntimeException.class, () -> Main.getURL(resource));
    }
}
