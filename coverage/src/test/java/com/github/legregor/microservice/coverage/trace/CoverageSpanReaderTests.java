package com.github.legregor.microservice.coverage.trace;

import com.github.legregor.microservice.coverage.trace.model.CoverageInternalSpan;
import com.github.legregor.microservice.coverage.trace.model.CoverageServerSpan;
import com.github.legregor.microservice.coverage.trace.model.CoverageSpan;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CoverageSpanReaderTests {

    @Test
    void readsCoverageSpansIntoCorrectSubclasses() throws Exception {
        String fileName = "processed-spans.json";
        String filePath = "src/test/resources/" + fileName;

        Path path = Paths.get(filePath);

        List<CoverageSpan> spans = new CoverageSpanReader().read(path);

        CoverageSpan span1 = spans.get(0);
        assertTrue(span1 instanceof CoverageInternalSpan);
        CoverageSpan span4 = spans.get(3);
        assertTrue(span4 instanceof CoverageServerSpan);

        // Check that the spans are read correctly and are of the expected subclasses

    }
}
