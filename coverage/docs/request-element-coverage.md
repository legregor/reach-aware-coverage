# Request Element Coverage

Request element coverage measures how many of the **request elements** (path parameters and request body fields)
that a caller *can* send over a connection were actually sent with a value during the integration tests.

It answers the question: *"The tests called this endpoint — but did they exercise all the input the calling service
can pass to it?"*

For the kinds of values sent, see [request value partition coverage](request-value-partition-coverage.md).

## Key terms

| Term | Meaning |
|---|---|
| **Connection** | `(calling service, called endpoint)`, e.g. `ts-cancel-service → PUT /api/v1/orderservice/order`. The same endpoint called by two services gives two connections. |
| **Request element** | A path parameter or a field of the request body of the called endpoint. |
| **Reachable** | The calling service's code can send the element with a non-null value (static analysis). Only reachable elements are coverage items. |
| **Executed / covered** | The element had a non-null value in at least one recorded call during the tests (traces). |
| **Called connection** | A connection with at least one recorded call. Request element coverage is only computed for called connections; uncalled connections are already reported by connection coverage. |

## Element names

| Element | Name |
|---|---|
| Path parameter `{orderId}` of the endpoint | `path:orderId` |
| Top-level body field | `body:from` |
| Nested field | `body:trip.tripId` |
| Field of list items | `body:items[].name` |
| Body that is a single value (string, number, list of strings) | `body` (list items: `body[].x`) |

Path parameters are named after the **called endpoint's** template, not the caller's URL (a caller may use
`/users/id/{orderId}` for the endpoint `/users/id/{userId}`; the element is `path:userId`).

## How it is computed

### 1. Possible elements (OpenAPI spec)

`RequestElementTreeBuilder` builds an element tree per connection:

- **Path parameters** from the endpoint's path template. They are required and always reachable.
- **Body fields** from the request body schema in the OpenAPI spec of the called service
  (`--openapi-dir`, e.g. `<service>.json` or `<service>-slim.json`, JSON or YAML). `SchemaFieldReader` resolves `$ref`, `allOf` and arrays and stops at
  cycles and at depth 5. A field is *required* if it is in the schema's `required` list.
- **`readOnly` properties** of the spec are only sent in responses and are no request elements (and no interface
  mismatch if the caller's DTO has them).
- **No spec for the body:** the top-level properties of the caller's DTO are used, and the connection is marked
  `bodySpecMissing`.

### 2. Reachable elements (static analysis of the caller)

`RequestBodyReachabilityAnalyzer` looks at each `RestTemplate.exchange` call and finds where the body object comes from:

| Origin of the body object | Reachable top-level properties |
|---|---|
| Created locally (`new T()`) | Properties set by setters / field assignments (not `null`), constructor arguments, primitive fields, fields with an initializer or set in the no-arg constructor |
| Builder chain | Properties named in the chain |
| Parameter of the sending method | Analysed at each call site in the service (depth 3), union |
| Anything else (method return value, REST response, class field, …), or the object is passed to another method | All properties |

The analysis over-approximates: an element is only **not reachable** if the code clearly never gives it a value.
Nested fields inherit the reachability of their top-level parent. Several calls of one connection are merged
(union). Spec fields that are missing in the caller's DTO are not reachable; spec and DTO differences are reported as
an **interface mismatch** (`onlyInSpec`, `onlyInCaller`).

### 3. Executed elements (traces)

`ExecutedRequestElementsCollector` uses the recorded calls of the connection:

- **Path parameters** are executed as soon as the connection was called (a matched call always fills them).
- **Body fields** are read from the request body captured in the **server span** of the called service
  (`http.request.body`, recorded by the Hypertrace agent). A field is executed if it is present and not `null`.
  An empty string, `0` or an empty list/object count as a value (but the children of an empty list/object do not).
  `body:items[].name` is executed if at least one item has a name.

### 4. Coverage values per connection (`RequestElementCoverage`)

| Value | Definition |
|---|---|
| **Pooled** | covered reachable elements / reachable elements (every element has the same weight) |
| **Hierarchical** | coverage of an element = mean of its own value (1/0) and the values of its reachable children; connection value = mean over the top-level elements. A large nested object does not outweigh its siblings. |
| **Optional only (pooled)** | pooled over the elements that are not required (required elements are always present in a valid request) |

Example for the hierarchical value — top level `a` (covered), `b` (not covered), `address` (covered) with children
`zip` (not covered) and `city` (covered): `address = (1 + 0 + 1) / 3 = 2/3`, connection `= (1 + 0 + 2/3) / 3 ≈ 55.6 %`
(pooled: 3 of 5 = 60 %).

### 5. Not applicable (n/a)

A connection is n/a if no element can be judged. It is then excluded from all totals. The reason is reported:

| Reason | When |
|---|---|
| `NO_ELEMENTS` | The endpoint has no path parameters and no request body |
| `NONE_REACHABLE` | Elements exist, but the caller can never send any of them |
| `CALLEE_NOT_TRACED` | The called service produced no server span (e.g. it is a WireMock stub), so no body was recorded |
| `BODY_NOT_CAPTURED` | Server spans exist but contain no request body (body capture off) |

If only the body is missing from the traces but the connection has path parameters, the path parameters still count
and the body elements are listed as **not recorded** (`bodyNotRecordedReason`).

### 6. Totals (`CoverageReport`, called connections only)

| Value | Definition |
|---|---|
| Request element coverage (pooled) | all covered elements / all reachable elements, over all applicable connections |
| Mean over connections (pooled / hierarchical) | mean of the per-connection values |
| Optional elements only (pooled) | pooled over the optional elements of all connections |
| Lowest / median connection value | distribution of the per-connection pooled values |
| Counts | connections with request elements, n/a connections (per reason), connections whose body was not recorded, connections with an interface mismatch |

## Output

For each called connection, the reports (`coverage-report.txt` / `.md` / `.json`) show:

- **Covered** elements
- **Uncovered** elements: reachable but never sent with a value — **these are the test gaps**
- **Not reachable** elements: the caller never sends them; not counted against the tests
- **Not recorded** elements: body elements that could not be judged, with the reason
- **Sent but not in the spec** (`unexpectedElements`): fields in the traced bodies that the spec does not know
- interface mismatches and a missing body spec

`microservice-system.json` stores, per connection, the elements and the reachable / not-reachable lists from the static
analysis, plus where the body comes from (`requestBodyOrigins`).

## Requirements for a run

- `--openapi-dir` pointing to the OpenAPI specs (file names: see the root README). Without it, body elements come from the caller's DTOs only.
- The called services run with the Hypertrace agent and request body capture enabled
  (`dataCapture.httpBody.request: true`). Otherwise body elements are n/a.
- Exactly one root service folder for the static analysis.

```bash
./run-coverage.sh <MICROSERVICES_ROOT>/ts-cancel-service \
  <MICROSERVICES_ROOT>/ts-cancel-service/src/test/java/cancel/integration/get/CancelOrderTest.java \
  --config CancelConfig --openapi-dir coverage/src/test/resources/slim_open_api
```

## Limitations

- Only `RestTemplate.exchange` calls are analysed; query parameters and headers are not request elements yet.
- Reachability is decided for top-level body properties only; nested fields inherit it.
- Over-approximation: conditionally set properties count as reachable; a helper method used by several callers gets
  one result for all of them.
- JSON does not distinguish `null` from an absent field (Jackson writes `null` fields), so both mean "no value".
- Without `required` lists in the spec, every body field counts as optional.

## Code map

| Step | Class |
|---|---|
| Static reachability | `analysis/trace/RequestBodyReachabilityAnalyzer` (+ `ReachingDefinitions`) |
| Elements per connection | `analysis/CommunicationPathConverter` → `RestConnection.requestElements` / `reachableRequestElements` / `notReachableRequestElements` |
| Schema reading | `openapi/SchemaFieldReader`, `openapi/SchemaField` |
| Element tree | `evaluation/requestElement/RequestElementTreeBuilder`, `RequestElementTree`, `RequestElement` |
| Executed elements | `evaluation/requestElement/ExecutedRequestElementsCollector`, `ExecutedRequestElements` |
| Per-connection values | `evaluation/requestElement/RequestElementCoverage`, `NotApplicableReason` |
| Totals and output | `evaluation/overallCoverage/CoverageReport`, `output/TextCoverageReportWriter`, `output/MarkdownCoverageReportWriter` |
