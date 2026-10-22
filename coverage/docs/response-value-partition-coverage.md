# Response Value Partition Coverage

Response value partition coverage measures how many **classes of values** (value partitions) of the returned response
fields the integration tests made the called services return.

It answers the question: *"The callee returned this field — but did the tests drive it into the interesting kinds of
values, such as every enum value, the boundaries of a range, an empty list or no value at all?"* These are the values
whose handling in the calling service the tests exercise.

It builds on [response field coverage](response-field-coverage.md) (same connections, same status codes, same field
names) and uses the same partitions and classification as
[request value partition coverage](request-value-partition-coverage.md).

## Key terms

| Term | Meaning |
|---|---|
| **Covered response field** | A field of a judged status code that was returned with a non-null value at least once (covered by response field coverage). **Only covered fields are in scope**: a field that was never returned is already a gap of response field coverage. |
| **Value partition** | A class of values of one response field that the response schema allows, written as `<status>:<path>#<partition>`, e.g. `201:estimatedDelivery#none`, `400:fieldErrors#size-0`, `200:currency#enum=CHF`. |
| **Covered partition** | At least one value of the field, in a response with that status code, falls into the partition. |
| **Partition source** | `CONSTRAINT` if a schema constraint defines the partition, `TYPE_DEFAULT` if it only comes from the type. |

## How it is computed

1. **Scope:** the called connections whose response field coverage is applicable; per judged status code (returned,
   expected, documented, body captured) the covered response fields.
2. **Partitions:** `PartitionGenerator` on the field's schema in the callee's response schema of that status code —
   the same groups as for requests (no value, enum, boolean, range, sign, length, empty, format, pattern, size,
   variant). `#none` exists for optional or nullable fields (the response schema's `required` lists).
3. **Values:** the response bodies of that status code from the callee's server spans, only of the responses that
   response field coverage counts (those of a relevant response class, e.g. not a `"status": 2` response when only
   `status=1` and `status=0` are expected), collected along the response schema (`ElementValueCollector`): an absent field and an explicit `null` both mean "no value"; fields of list items
   are collected from every item of every response; fields below an absent object are not visited. The same field
   under different status codes (e.g. `400:message` and `404:message`) has separate values and partitions.
4. **Classification:** `ValueClassifier`, as for requests. A value covers one partition in every group of its field. A
   value the schema does not allow covers no partition and is listed as **returned outside the spec** (value, reason,
   count) — a defect of the callee or of its spec — but not counted.
5. **Reachability:** every partition of a covered field is a coverage item. There is no analysis of the callee's code
   yet (e.g. a field it always sets to the same constant).

## Coverage values

Per connection (`ResponseValuePartitionCoverage`):

| Value | Definition |
|---|---|
| **Pooled** | covered / all partitions of the covered fields of all judged status codes |
| **Hierarchical** | per status code, a field's own partitions count as one value (covered / all), averaged with the values of its covered children; the value of a status code is the mean over its top-level fields, the connection value the mean over its status codes |
| **Spec constraints only (pooled)** | pooled over the partitions with source `CONSTRAINT` |

Example (unit test `ResponseValuePartitionCoverageTest`): a quote response `200` with `quoteId` (uuid), `currency`
(enum EUR, CHF, USD), `total` (minimum 0), optional `discountApplied` (0–50) and a list `items` of `sku` (string) and
`quantity` (1–99), returned twice:

| Field | Partitions | Covered |
|---|---|---|
| `200:quoteId` | `#format` | 1/1 |
| `200:currency` | `#enum=EUR`, `#enum=CHF`, `#enum=USD` | 1/3 |
| `200:total` | `#min`, `#above` | 2/2 |
| `200:discountApplied` | `#none`, `#min`, `#interior`, `#max` | 2/4 |
| `200:items` | `#size-0`, `#size-1`, `#size-many` (type default) | 2/3 |
| `200:items[].sku` | `#empty`, `#non-empty` (type default) | 2/2 |
| `200:items[].quantity` | `#min`, `#interior`, `#max` | 2/3 |

Pooled 12/18 = 66.67 %; constraints only 8/13 = 61.54 %; hierarchical: `items` = (2/3 + 1 + 2/3) / 3 = 7/9, status
`200` = (1 + 1/3 + 1 + 1/2 + 7/9) / 5 = 72.22 %.

A connection is **n/a** (excluded from the totals) with a reason: the reasons of response field coverage (callee not
traced, no expected status code returned, no response schema, no response fields, body not captured), and
additionally *no covered response fields* (responses were judged, but no field had a value) and *no partitions* (the
covered fields have no partitions, e.g. only required objects).

Totals (`CoverageReport`, applicable called connections only): pooled, pooled over spec constraints only, the share of
partitions from spec constraints, mean pooled and mean hierarchical, lowest and median connection value, the n/a
connections per reason and the number of values returned outside the spec.

## Output

- **Summary:** section *Response Value Partition Coverage Statistics* (after response field coverage) in the text and
  Markdown reports.
- **Per connection:** one line or table row per covered field (`<status>:<path>`) with covered and uncovered
  partitions and the values returned outside the spec.
- **JSON:** `responseValuePartitionCoverage` of every connection report (values, partitions per status code and
  field, n/a reason).

## Prerequisites and limitations

- The called services' OpenAPI specs must describe the response bodies (as for response field coverage). The more
  constraints the response schemas declare, the more meaningful the partitions; without constraints only type-based
  partitions exist, some of which a callee can never return (e.g. a negative price for a `number` without
  `minimum`). The constraint-only value and the constraint share show how much of the result rests on the spec.
- Every partition of a covered field counts as reachable: partitions the callee can never produce are not excluded
  yet (no static analysis of the callee).
- Absent and `null` are one "no value" partition, as for requests.
- The limitations of the partition groups (one partition per `pattern`, no partitions for `default` values and
  `additionalProperties`, "each choice" instead of combinations) are those of
  [request value partition coverage](request-value-partition-coverage.md#limitations).
