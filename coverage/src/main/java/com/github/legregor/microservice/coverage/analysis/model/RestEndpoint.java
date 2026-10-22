package com.github.legregor.microservice.coverage.analysis.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class RestEndpoint {
    private String method;
    private String path;
    private List<String> arguments;
    private String returnType;
}
