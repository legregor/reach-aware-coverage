package com.github.legregor.microservice.coverage.evaluation.requestValuePartition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElement;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.PartitionGenerator;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.PartitionGroup;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.PartitionSource;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.ValueClassification;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.ValueClassifier;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.ValuePartition;
import com.github.legregor.microservice.coverage.openapi.SchemaField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Value partition coverage of one executed request element: its partitions with their status and the values sent
 * that the spec does not allow.
 */
public class ElementPartitionCoverage {

    private static final String NO_VALUE_TEXT = "(no value)";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String element;
    private final List<PartitionStatus> partitions;
    private final List<OutOfSpecValue> outOfSpecValues;
    private final List<String> staticConstants;
    private final boolean neverNull;
    private final List<OutOfSpecValue> outOfSpecConstants;

    ElementPartitionCoverage(String element, List<PartitionStatus> partitions, List<OutOfSpecValue> outOfSpecValues) {
        this(element, partitions, outOfSpecValues, null, false, List.of());
    }

    ElementPartitionCoverage(String element, List<PartitionStatus> partitions, List<OutOfSpecValue> outOfSpecValues,
                             List<String> staticConstants, boolean neverNull, List<OutOfSpecValue> outOfSpecConstants) {
        this.element = element;
        this.partitions = List.copyOf(partitions);
        this.outOfSpecValues = List.copyOf(outOfSpecValues);
        this.staticConstants = staticConstants == null ? null : List.copyOf(staticConstants);
        this.neverNull = neverNull;
        this.outOfSpecConstants = List.copyOf(outOfSpecConstants);
    }

    /**
     * Value partition coverage of a plain schema field, without static analysis: every partition is reachable. Used
     * for response fields (the callee's values; there is no analysis of the callee yet).
     *
     * @param name   the item name for reports, e.g. {@code 201:estimatedDelivery}
     * @param field  the field's schema; null gives an element without partitions
     * @param values the values observed (absent / null included); may be empty
     */
    public static ElementPartitionCoverage ofField(String name, SchemaField field, List<JsonNode> values,
                                                   PartitionGenerator generator, ValueClassifier classifier) {
        if (field == null) {
            return new ElementPartitionCoverage(name, List.of(), List.of());
        }
        Observed observed = observe(field, values, false, classifier);
        List<PartitionStatus> statuses = new ArrayList<>();
        for (ValuePartition partition : generator.partitions(field)) {
            statuses.add(new PartitionStatus(partition.id(), partition.group(), partition.source(), true,
                    observed.covered().contains(partition)));
        }
        return new ElementPartitionCoverage(name, statuses, observed.outOfSpec());
    }

    /**
     * Classifies the values of an element. Path parameter values are text from the URL and are converted to the
     * declared type first.
     *
     * @param values the values sent (absent / null included); may be empty
     */
    static ElementPartitionCoverage of(RequestElement element, List<JsonNode> values, PartitionGenerator generator,
                                       ValueClassifier classifier) {
        SchemaField field = element.getSchemaField();
        if (field == null) {
            return new ElementPartitionCoverage(element.getName(), List.of(), List.of());
        }
        boolean fromText = element.getKind() == RequestElement.Kind.PATH_PARAMETER;
        Observed observed = observe(field, values, fromText, classifier);
        // static analysis: constants the caller assigns (only these partitions are reachable) and primitive fields
        Set<ValuePartition> constantPartitions = null;
        List<OutOfSpecValue> outOfSpecConstants = new ArrayList<>();
        if (element.getConstants() != null) {
            constantPartitions = new LinkedHashSet<>();
            for (String constant : element.getConstants()) {
                ValueClassification classification = classifier.classify(field, parse(constant));
                constantPartitions.addAll(classification.covered());
                if (classification.isOutOfSpec()) {
                    outOfSpecConstants.add(new OutOfSpecValue(constant, classification.outOfSpecReason(), 1));
                }
            }
        }

        List<PartitionStatus> statuses = new ArrayList<>();
        for (ValuePartition partition : generator.partitions(field)) {
            boolean reachable;
            if (partition.group() == PartitionGroup.NO_VALUE) {
                reachable = !element.isNeverNull();
            } else if (fromText && isEmptyString(partition, field)) {
                reachable = false;
            } else {
                reachable = constantPartitions == null || constantPartitions.contains(partition);
            }
            statuses.add(new PartitionStatus(partition.id(), partition.group(), partition.source(), reachable,
                    observed.covered().contains(partition)));
        }
        return new ElementPartitionCoverage(element.getName(), statuses, observed.outOfSpec(), element.getConstants(),
                element.isNeverNull(), outOfSpecConstants);
    }

    /** The partitions covered by the values and the values outside the spec (shared by requests and responses). */
    private record Observed(Set<ValuePartition> covered, List<OutOfSpecValue> outOfSpec) {
    }

    private static Observed observe(SchemaField field, List<JsonNode> values, boolean fromText,
                                    ValueClassifier classifier) {
        Set<ValuePartition> covered = new LinkedHashSet<>();
        Map<String, Integer> outOfSpecCounts = new LinkedHashMap<>();
        Map<String, String> outOfSpecReasons = new LinkedHashMap<>();
        for (JsonNode value : values) {
            ValueClassification classification = fromText && value != null && value.isTextual()
                    ? classifier.classifyText(field, value.asText())
                    : classifier.classify(field, value);
            covered.addAll(classification.covered());
            if (classification.isOutOfSpec()) {
                String text = value == null || value.isNull() || value.isMissingNode() ? NO_VALUE_TEXT
                        : fromText && value.isTextual() ? value.asText() : value.toString();
                String key = text + "\u0000" + classification.outOfSpecReason();
                outOfSpecCounts.merge(key, 1, Integer::sum);
                outOfSpecReasons.put(key, classification.outOfSpecReason());
            }
        }
        List<OutOfSpecValue> outOfSpec = new ArrayList<>();
        outOfSpecCounts.forEach((key, count) -> outOfSpec.add(new OutOfSpecValue(key.substring(0, key.indexOf('\u0000')),
                outOfSpecReasons.get(key), count)));
        return new Observed(covered, outOfSpec);
    }

    private static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            return com.fasterxml.jackson.databind.node.TextNode.valueOf(json);
        }
    }

    /**
     * The empty string as a partition: a path parameter can never be empty (an empty segment does not match the
     * endpoint's path template), so this partition is not reachable for path parameters. With length bounds, the
     * minimum length is the empty string if minLength is missing or 0.
     */
    private static boolean isEmptyString(ValuePartition partition, SchemaField field) {
        if (partition.group() == PartitionGroup.EMPTY) {
            return partition.name().equals("empty");
        }
        Integer minLength = field.getConstraints().getMinLength();
        return partition.group() == PartitionGroup.LENGTH && partition.name().equals("len-min")
                && (minLength == null || minLength <= 0);
    }

    /** Element name, e.g. {@code body:quantity}. */
    public String getElement() {
        return element;
    }

    public List<PartitionStatus> getPartitions() {
        return partitions;
    }

    public List<OutOfSpecValue> getOutOfSpecValues() {
        return outOfSpecValues;
    }

    /**
     * The only values the caller ever assigns (static analysis, JSON text), or null if it can send any value. Only
     * the partitions of these constants are reachable.
     */
    public List<String> getStaticConstants() {
        return staticConstants;
    }

    /** The caller can never send this element without a value (primitive field): no-value is not reachable. */
    public boolean isNeverNull() {
        return neverNull;
    }

    /** Constants of the caller that the spec does not allow (developer information). */
    public List<OutOfSpecValue> getOutOfSpecConstants() {
        return outOfSpecConstants;
    }

    public int getReachableCount() {
        return count(PartitionStatus::reachable);
    }

    public int getCoveredCount() {
        return count(status -> status.reachable() && status.covered());
    }

    /** Reachable partitions from spec constraints only (without type defaults). */
    public int getConstraintReachableCount() {
        return count(status -> status.reachable() && status.source() == PartitionSource.CONSTRAINT);
    }

    public int getConstraintCoveredCount() {
        return count(status -> status.reachable() && status.covered() && status.source() == PartitionSource.CONSTRAINT);
    }

    /** Covered / reachable partitions (0-100), null if the element has no reachable partition. */
    public Double getCoveragePercentage() {
        int reachable = getReachableCount();
        return reachable == 0 ? null : getCoveredCount() * 100.0 / reachable;
    }

    public List<String> getCoveredPartitions() {
        return ids(status -> status.reachable() && status.covered());
    }

    public List<String> getUncoveredPartitions() {
        return ids(status -> status.reachable() && !status.covered());
    }

    public List<String> getNotReachablePartitions() {
        return ids(status -> !status.reachable());
    }

    private int count(Predicate<PartitionStatus> filter) {
        return (int) partitions.stream().filter(filter).count();
    }

    private List<String> ids(Predicate<PartitionStatus> filter) {
        return partitions.stream().filter(filter).map(PartitionStatus::id).toList();
    }
}
