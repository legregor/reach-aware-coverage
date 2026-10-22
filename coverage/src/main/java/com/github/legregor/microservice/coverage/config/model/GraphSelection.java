package com.github.legregor.microservice.coverage.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphSelection {

    private List<String> selectedServices = new ArrayList<>();
    private List<String> selectedConnections = new ArrayList<>();

    public void setSelectedServices(List<String> selectedServices) {
        this.selectedServices = normalize(selectedServices);
    }

    public void setSelectedConnections(List<String> selectedConnections) {
        this.selectedConnections = normalize(selectedConnections);
    }

    private static List<String> normalize(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return new ArrayList<>(normalized);
    }
}
