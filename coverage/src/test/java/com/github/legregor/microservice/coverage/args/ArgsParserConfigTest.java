package com.github.legregor.microservice.coverage.args;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Tests for the coverage config arguments of {@link ArgsParser}. */
class ArgsParserConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void configFileIsUsedAndNotMistakenForAConfigName() throws IOException {
        Path file = Files.writeString(tempDir.resolve("my-config.json"), "{}");

        ArgsParser separate = new ArgsParser(new String[] {"--config-file", file.toString(), "--config", "Run1"});
        ArgsParser inline = new ArgsParser(new String[] {"--config-file=" + file});

        assertEquals(file.toAbsolutePath().normalize(), separate.getConfigPath());
        assertEquals("Run1", separate.getConfigName());
        assertEquals(file.toAbsolutePath().normalize(), inline.getConfigPath());
        assertNull(inline.getConfigName());
    }

    @Test
    void withoutConfigFileTheWebUiConfigOfTheNameIsUsedIfItExists() {
        // storage/configs/<name>.json relative to the working directory; this name does not exist there
        ArgsParser parser = new ArgsParser(new String[] {"--config", "NoSuchConfig-" + System.nanoTime()});

        assertNull(parser.getConfigPath());
    }

    @Test
    void noConfigAtAll() {
        assertNull(new ArgsParser(new String[] {}).getConfigPath());
    }
}
