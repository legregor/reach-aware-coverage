# Status Code Class Coverage

Status code class coverage measures whether the integration tests triggered the **kinds of responses** a connection
should produce, typically a success and an error.

It answers the question: *"The tests called this connection — but did they only ever see the happy path, or also the
error responses the caller has to handle?"*

The expected kinds of responses are configured in a [coverage config file](#configuration): HTTP status codes
(`404`), HTTP status code classes (`4xx`) or, for systems that report the outcome of a call in the response body,
**body outcomes** (`status=0`).

## Key terms

| Term | Meaning |
|---|---|
| **Expected entry** | One kind of response a connection should return during the tests: an HTTP status code (`404`), an HTTP status code class (`4xx`) or a body outcome (`status=0`). The entries come from the coverage config, globally or per connection; without a config, every connection expects `2xx` and `4xx`. |
| **Covered entry** | At least one call of the connection returned a matching response. |
| **Body outcome** | `<field>=<value>`: a value of a field in the response body that tells whether the call succeeded, e.g. `status=0`. For systems that always answer HTTP 200 and report success or failure in the body. |

## How it is computed

### 1. Recorded responses

For every call of a connection, the tool records

- the **HTTP status code**, from the client span of the calling service (also for calls to a mocked service; a call
  without an answer, e.g. a connection error, has the code 0 and matches no entry);
- the **response body**, from the server span of the called service (only for traced services; a mocked service has
  no server span, so its bodies are not recorded).

### 2. Matching an expected entry

| Entry | Example | Covered if at least one call … |
|---|---|---|
| HTTP status code | `404` | returned exactly this status code |
| HTTP status code class | `4xx`, `4XX` | returned a status code of this class (`4xx` = 400–499); any code of the class is enough |
| Body outcome | `status=0`, `result.code=OK` | returned a JSON body whose field has this value |

Body outcomes in detail:

- The field is a property of the response body; nested fields are written with dots (`result.code`).
- Numbers are compared by value (`status=1` matches `1` and `1.0`); everything else is compared as text
  (`code=OK` matches `"OK"`, `ok=true` matches `true`); `field=null` matches an explicit `null`.
- A body that is not JSON, or does not contain the field, matches no body outcome.
- Body outcomes need recorded response bodies: for a mocked callee they can never be covered.

HTTP entries and body outcomes can be mixed in one list (e.g. `["2xx", "status=0"]`); each entry counts once.

### 3. Coverage values

Per connection:

| Value | Definition |
|---|---|
| **Status code class coverage** | covered entries / expected entries of the connection |
| **Missing entries** | the expected entries that no call covered, listed per connection with the status codes and body outcomes the connection did return |

Example: a connection expects `["2xx", "4xx"]` and only ever returned 200 → 1 of 2 = 50 %, missing `4xx`
(seen: `200`).

Over the system (`CoverageReport`):

| Value | Definition |
|---|---|
| **All connections** | mean over all reachable connections; a connection that was never called counts with 0 % (its gap is already a gap of connection coverage) |
| **Called connections only** | mean over the called connections (the main value, as for the other metrics: a gap is reported once, at the level where it occurs) |
| **Lowest / median connection value** | over the called connections |
| **Connections missing expected entries** | every called connection with at least one missing entry, lowest coverage first |

Server errors (`5xx`) only count if they are expected entries (e.g. `"500"`), which is rarely useful: they point to
implementation or infrastructure problems rather than to test quality. The report notes how many connections returned
a server error.

## Configuration

### Where the config file comes from

The coverage config file is resolved in this order:

1. `--config-file <path>`;
2. `storage/configs/<name>.json` for `--config <name>` (the file the web UI saves);
3. otherwise the defaults: every connection expects `2xx` and `4xx`.

The log says which file was used, and the file is copied into the run folder (`coverage-config.json`).

### Structure

```json
{
  "configName": "MyConfig",
  "globalExpectedStatusCodes": ["2xx", "4xx"],
  "connectionOverrides": [
    {
      "sourceService": "shop-order-service",
      "destinationService": "shop-pricing-service",
      "method": "GET",
      "path": "/api/v1/pricing/discounts/{code}",
      "expectedStatusCodes": ["200", "400", "404"]
    }
  ]
}
```

| Field | Meaning |
|---|---|
| `configName` | Optional name of the config. |
| `globalExpectedStatusCodes` | The expected entries of every connection that no override matches. Missing or empty: `["2xx", "4xx"]`. |
| `connectionOverrides` | Expected entries for specific connections (optional). |
| `sourceService`, `destinationService` | Calling and called service, by service (folder) name. Left out: any service. |
| `method` | HTTP method of the call (case-insensitive). Left out: any method. |
| `path` | The called endpoint's path template (`/api/v1/pricing/discounts/{code}`), or the called URL or its path. Left out: any path. |
| `expectedStatusCodes` | The expected entries of the matching connections. |

Rules:

- An override applies to every connection it matches; the **first** matching override with expected entries wins.
  A connection that no override matches uses `globalExpectedStatusCodes`.
- Unknown field names or invalid JSON stop the run **before** the tests start, with the field and its position in the
  message (so a typo does not waste a test run).
- Entries that are neither an HTTP status code, a class nor a body outcome (e.g. `success`, `4xxx`, `600`) are
  reported as warnings; they can never be covered.
- Other fields that the web UI writes (`services`, `tests`) are ignored. Saving a config with the same name in the web
  UI overwrites the file, including manually added fields.

### Example 1: HTTP status codes (REST best practice)

Every connection should be tested with a success and a client error; for the discount lookup, the tests should
trigger the specific codes 200, 400 (invalid code) and 404 (unknown code):

```json
{
  "globalExpectedStatusCodes": ["2xx", "4xx"],
  "connectionOverrides": [
    { "sourceService": "shop-order-service", "destinationService": "shop-pricing-service",
      "method": "GET", "path": "/api/v1/pricing/discounts/{code}",
      "expectedStatusCodes": ["200", "400", "404"] }
  ]
}
```

A specific code is stricter than a class: `4xx` is covered by any client error, `["400", "404"]` requires both.

### Example 2: outcomes in the response body (custom status codes)

TrainTicket answers every call with HTTP 200 and reports the outcome in the body field `status`:
`{"status": 1, "msg": "Success", "data": {...}}` or `{"status": 0, "msg": "Order Not Found", "data": null}`. HTTP
classes would rate every connection 50 % (`2xx` covered, `4xx` never); body outcomes measure success and failure:

```json
{
  "globalExpectedStatusCodes": ["status=1", "status=0"]
}
```

### Example 3: mixed

Most services use HTTP status codes, one legacy service reports errors in the body:

```json
{
  "globalExpectedStatusCodes": ["2xx", "4xx"],
  "connectionOverrides": [
    { "destinationService": "legacy-billing-service",
      "expectedStatusCodes": ["result.code=OK", "result.code=ERROR"] }
  ]
}
```

## Output

- **Summary:** section *Status Code Class Coverage Statistics* in the text and Markdown reports: the values over all
  connections and over the called connections, lowest and median value, and the connections missing expected entries
  (with what they did return, e.g. `missing status=0 (seen: 200, status=1)`).
- **Per connection:** the expected entries, the HTTP status code classes covered, the body outcomes seen (for the
  fields of body outcome entries), the coverage value and the count of every status code.
- **JSON:** `expectedStatusCodes`, `statusCodes`, `statusCodeClassCoveragePercentage`, `missingExpectedStatusCodes` and
  `observedBodyOutcomes` of every connection report.

## Relation to the other metrics

- **Response field coverage** groups the responses by **HTTP** status code (each status code has its own response
  schema), but counts only responses of a relevant class: a body outcome entry selects the responses with that value
  (a `"status": 2` response does not count when `status=1` and `status=0` are expected). Response value partition
  coverage uses the same responses. See
  [response-field-coverage.md](response-field-coverage.md).
- Connections that were never called are gaps of connection coverage; status code class coverage concentrates on the
  called connections.

## Limitations

- Body outcomes need recorded response bodies (`dataCapture.httpBody` of the tracing agent) and a traced callee;
  for a mocked callee only HTTP entries can be covered.
- A body outcome tests one field for one value; combinations (e.g. HTTP 200 **and** `status=0`) and value ranges are
  not supported.
- Only fields of a JSON object body are supported (no top-level arrays, no headers).
- The expected entries are configured by the developer; the tool does not derive them from the OpenAPI spec (a spec
  that documents many error codes would otherwise produce expectations no test is meant to trigger).
