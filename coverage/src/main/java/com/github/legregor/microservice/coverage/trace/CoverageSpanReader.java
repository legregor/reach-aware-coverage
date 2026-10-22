package com.github.legregor.microservice.coverage.trace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.legregor.microservice.coverage.trace.model.CoverageSpan;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class CoverageSpanReader {

    private final ObjectMapper mapper;

    public CoverageSpanReader() {
        this.mapper = new ObjectMapper();
    }

    public List<CoverageSpan> read(Path filePath) throws IOException {
        List<CoverageSpan> spans = mapper.readValue(filePath.toFile(), new TypeReference<List<CoverageSpan>>() {} );

        return spans;
    }


}
