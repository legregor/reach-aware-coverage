# Request Value Partition Coverage

Request value partition coverage measures how many **classes of input values** (value partitions) of the request
elements the integration tests exercised.

It answers the question: *"The tests sent this field — but did they send the interesting kinds of values, such as the
boundaries of a range, every enum value, an empty list or no value at all?"*

It builds on [request element coverage](request-element-coverage.md): same connections, same request elements
(path parameters and request body fields), same element names.

## Key terms

| Term | Meaning |
|---|---|
| **Value partition** | A class of values of one request element that the spec allows, e.g. "the minimum of the range", "enum value FIRST", "no value". Written as `<element>#<partition>`, e.g. `body:quantity#min`. |
| **Executed element** | A request element that was sent with a non-null value at least once (covered by request element coverage). **Only executed elements are in scope**: an element that was never sent is already a gap of request element coverage. |
| **Reachable partition** | A partition the caller can produce (static analysis). Only reachable partitions are coverage items. |
| **Covered partition** | At least one value sent during the tests falls into the partition. |
| **Partition source** | `CONSTRAINT` if a spec constraint defines the partition, `TYPE_DEFAULT` if it only comes from the type (fallback when the spec has no constraints). |

## How it is computed

### 1. Partitions from the spec

`PartitionGenerator` derives **groups** of partitions from the schema of each element. Each constraint gives its own
group; a value covers one partition in **every** group of its element ("each choice"), so the partitions of an element
add up instead of multiplying.

| Group | When | Partitions | Source |
|---|---|---|---|
| No value | element is optional or nullable (never for path parameters) | `#none` (absent or `null`) | constraint if the object declares a `required` list or the field is nullable, else type default |
| Enum | `enum` or `const` | `#enum=<value>` for each value; replaces all type groups below | constraint |
| Boolean | `boolean` | `#true`, `#false` | type default |
| Range | `integer` / `number` with `minimum` and/or `maximum` | `#min`, `#interior`, `#max`; one-sided: `#min`, `#above` or `#below`, `#max` | constraint |
| Sign | `integer` / `number` without range | `#negative`, `#zero`, `#positive` | type default |
| Length | `string` with `minLength` and/or `maxLength` | `#len-min`, `#len-interior`, `#len-max` (without `minLength`, `#len-min` is the empty string) | constraint |
| Empty | `string` without length bounds, format or pattern | `#empty`, `#non-empty` | type default |
| Format | `string` with `format` | `#format` (one valid-format partition) | constraint |
| Pattern | `string` with `pattern` | `#pattern` (one partition) | constraint |
| Size | `array` | `#size-0`, `#size-1`, `#size-many`; with `minItems` / `maxItems`: `#size-min`, `#size-interior`, `#size-max` | type default / constraint |
| Variant | `oneOf` / `anyOf` | `#variant=<name>` for each alternative | constraint |

Boundary details: an exclusive integer bound becomes the first value inside (`exclusiveMinimum: 0` → `#min` is 1); an
exclusive decimal bound has no first value, so it gets no boundary partition. An interior partition only exists if a
value lies strictly between the bounds. Objects only have no-value and variant partitions; an object without them has
no own partitions (only its children count). `readOnly` properties are never sent in requests and are no request
elements at all.

Swagger 2.0, OpenAPI 3.0 and 3.1 notations are normalised first (`x-nullable` / `nullable` / `type: [..., "null"]`,
boolean or numeric `exclusiveMinimum`, `const`, `$ref` to `definitions` or `components`, keywords next to `$ref`).

Example: `quantity: integer, minimum 1, maximum 99`, optional → `#none`, `#min`, `#interior`, `#max` (4 partitions).

### 2. Values sent (traces)

- **Body fields:** from the request bodies captured in the server spans of the called service, for every field of the
  body schema. An absent field and an explicit `null` both count as "no value". Fields of list items
  (`body:items[].name`) are collected from every item of every call; fields of an absent object are not visited.
- **Path parameters:** from the URLs of the recorded calls, matched against the endpoint's path template, URL-decoded
  and converted to the declared type (`"42"` → 42 for an integer parameter).

### 3. Classifying a value

`ValueClassifier` (also used for the caller's constants, see below):

1. No value → `#none`; if the element is required and not nullable, the value is **outside the spec**.
2. Wrong type (e.g. `"abc"` for an integer) → outside the spec.
3. Any violated constraint (enum, range, length, pattern, format, size, alternatives) → outside the spec. Such a value
   covers **no** partition, not even in groups it satisfies.
4. Otherwise the value covers one partition in every group of the element.

Values outside the spec are **not counted** in the coverage; they are listed per element as developer information
(value, reason, how often), e.g. `0 (below minimum 1), 2x`.

### 4. Reachable partitions (static analysis)

By default every partition of an executed element is reachable. The static analysis narrows this down where it is
easy to decide:

- **Constants:** if every assignment of a top-level body property in the caller's code is a constant, only the
  partitions of these constants are reachable. Recognised: literals (also negative numbers), enum constants of the
  field's type, `static final` constants, conditional expressions of constants, local variables whose every assignment
  is a constant, field initializers and the default of primitive fields. Builder steps and `@AllArgsConstructor`
  arguments overwrite the defaults. If any assignment is something else (method result, parameter, object from
  elsewhere, `+=`), the property can have any value. Several call sites: constants are united, "any value" wins.
  Example: a caller that always sends `seatClass = SeatClass.FIRST` can only reach `#enum=FIRST`.
- **Never null:** primitive fields (`int`, `boolean`, ...) of the caller's DTO are always written by Jackson, so
  `#none` is not reachable for them.
- **Path parameters** can never be empty (an empty segment does not match the path template), so `#empty` (and
  `#len-min` without `minLength`) is not reachable for them.

Caller constants that the spec does not allow are listed as developer information.

### 5. Coverage values

Per connection (`RequestValuePartitionCoverage`):

| Value | Definition |
|---|---|
| **Pooled** | covered / reachable partitions of all executed elements |
| **Hierarchical** | an element's own partitions count as one value (covered / reachable), averaged with the values of its executed children; the connection value is the mean over the top-level elements. Elements with neither own partitions nor executed children are skipped. |
| **Spec constraints only (pooled)** | pooled over the partitions with source `CONSTRAINT` (without type defaults) |

Example (the aggregation example of the paper): `customerId` 2 of 4, `flag` 2 of 2, `address` 1 of 2 own partitions
with the children `zip` 1 of 4 and `city` 2 of 2 → pooled 8 / 14 = 57 %; `address` = (0.5 + 0.25 + 1) / 3 = 0.58,
hierarchical = (0.5 + 1 + 0.58) / 3 = 69 %.

A connection is **n/a** (excluded from the totals) with a reason: the request element reasons (no elements, none
reachable, callee not traced, body not captured) and additionally `NO_EXECUTED_ELEMENTS` (no element was sent with a
value) and `NO_PARTITIONS` (the executed elements have no partitions).

Totals (`CoverageReport`, applicable called connections only): pooled, pooled over spec constraints only, the share of
partitions that come from spec constraints, mean pooled and mean hierarchical, lowest and median connection value,
the n/a connections per reason and the number of values sent outside the spec.

The share of constraint partitions shows how much of the value is backed by the spec: for TrainTicket (types, a few
formats, no ranges or `required` lists) it is around 6 %, i.e. the metric mostly measures the type-based fallbacks.

## Output

For each called connection, the reports (`coverage-report.txt` / `.md` / `.json`) list per executed element:

- **covered** and **uncovered** partitions — the uncovered ones are the test gaps
- **not reachable** partitions (static analysis) and the constants the caller sends ("caller only sends")
- values sent **outside the spec** and caller constants outside the spec

`microservice-system.json` stores per connection the constants (`requestElementConstants`) and the never-null
elements (`neverNullRequestElements`) found by the static analysis.

## Requirements for a run

- `--openapi-dir` with the specs of the called services; without a body schema the body elements have no partitions
  (path parameters then count as plain strings).
- Request body capture in the traces (as for request element coverage).

## Limitations

**Scope**

- Only path parameters and request body fields; query parameters and headers are no request elements yet.
- The items of lists of plain values (`List<String>`) have no partitions (only the list itself has size partitions).
- No partitions for `default` values and `additionalProperties` yet.
- Combinations of groups are not measured ("each choice", not "all combinations").

**Partitions and classification**

- Absent and `null` are one "no value" partition (Jackson writes `null` fields by default, so the traces cannot tell
  them apart for Java callers).
- A `pattern` is one partition; alternatives inside the regular expression are not split. Patterns are checked with
  Java regular expressions (JSON Schema uses ECMA-262; the differences rarely matter).
- Formats are checked for date, date-time, time, uuid, email, uri, uri-reference, hostname, ipv4, ipv6, byte, int32
  and int64 (with simple checks for email and ipv6); other formats count as valid.
- Without a discriminator, `oneOf` / `anyOf` alternatives are recognised by their required properties only; a value
  that matches several alternatives covers all of them.
- With a weak spec, most partitions are type-based fallbacks; they are tagged and reported separately.

**Static reachability**

- Over-approximation: a partition is only excluded when the code clearly never produces it.
- Constants only for top-level body properties; nested fields and path parameters can always have any value.
- No data flow beyond local variables of the same method (method results, parameters and fields of other objects count
  as any value).
- A primitive field set by a setter keeps its default as a possible value (the setter may not run before the object
  is sent); only builders and `@AllArgsConstructor` overwrite defaults for sure.
- Only `RestTemplate.exchange` calls and Java / Jackson / Lombok conventions are analysed.

## Code map

| Step | Class |
|---|---|
| Schema with constraints (Swagger 2.0 / OpenAPI 3.x) | `openapi/SchemaFieldReader`, `SchemaField`, `SchemaConstraints`, `SchemaVariant`, `OpenApiRequestBodyResolver` |
| Partitions and classification (direction-neutral, reusable for responses) | `evaluation/valuePartition/PartitionGenerator`, `ValueClassifier`, `ValuePartition`, `PartitionGroup`, `PartitionSource`, `IntegerBounds`, `DecimalBounds`, `FormatChecker` |
| Values per field from JSON bodies | `evaluation/valuePartition/ElementValueCollector` |
| Constants and never-null fields (static analysis) | `analysis/trace/RequestBodyReachabilityAnalyzer`, `ConstantText`, `analysis/model/BodyConstants` → `RestConnection.requestElementConstants`, `neverNullRequestElements` |
| Values per element of a connection | `evaluation/requestValuePartition/ExecutedRequestValuesCollector` |
| Per-element and per-connection values | `evaluation/requestValuePartition/ElementPartitionCoverage`, `RequestValuePartitionCoverage`, `PartitionStatus`, `OutOfSpecValue` |
| Totals and output | `evaluation/overallCoverage/CoverageReport`, `output/TextCoverageReportWriter`, `output/MarkdownCoverageReportWriter` |
