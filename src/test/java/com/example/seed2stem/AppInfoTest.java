package com.example.seed2stem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppInfoTest {

    @Test
    void usesTheBuiltVersion() {
        assertEquals("1.2.0", AppInfo.clean("1.2.0", null));
        assertEquals("v1.2.0", new AppInfo("1.2.0").getLabel());
    }

    @Test
    void fallsBackWhenThePropertyWasntFilledIn() {
        assertEquals("1.2.0", AppInfo.clean("@project.version@", "1.2.0"));
        assertEquals("dev", AppInfo.clean("@project.version@", null));
        assertEquals("dev", new AppInfo("").getLabel());
    }
}
