package com.github.legregor.microservice.coverage.analysis.model;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class RestCall {
    private String method;
    private String url;
    private String returnType;
}
