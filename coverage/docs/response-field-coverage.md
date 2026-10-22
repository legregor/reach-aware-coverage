# Response Field Coverage

Response field coverage measures, per called connection, how many of the response fields documented in the called
service's OpenAPI spec were returned with a value during the tests. It answers: did the tests make the called service
return each of its response fields, in success and in error responses, so that the caller's handling of them was
exercised?

## Scope: which responses count

- **Connections:** only connections the tests called at least once (the executed connections). Uncalled connections
  are gaps of connection coverage.
- **Responses:** only responses of a **relevant response class**, i.e. responses that match at least one expected
  entry of the connection in the coverage config (default `2xx` and `4xx`): an HTTP entry by the status code (a class
  matches every code of the class, a code only itself), a body outcome entry (e.g. `status=0`) by the response body.
  With `["status=1", "status=0"]`, a response with `"status": 2` does not count, even with HTTP 200; a response with
  HTTP 500 and `"status": 0` does. A response whose class depends on the body, but whose body was not captured, is
  listed as *body not recorded* (see below).
- **Variants:** the relevant responses are grouped by their HTTP status code; each status code is one response
  variant with its own documented schema. A status code that no relevant response had contributes no fields. Whether
  that is a gap is decided by status code class coverage: an expected `404` that was never returned is a gap there; a
  `201` that was never returned while `2xx` is expected is no gap, because the class is satisfied by another code
  (e.g. 200). Status codes without a relevant response are listed as *ignored* (by default 5xx), also when only the
  caller saw them (e.g. a call to an untraced callee), unless their class depends on a body that is unknown.
- **Status code and body** are taken from the same server span of the called service. Calls to a mocked service have
  no server span; their responses are not recorded.

## Coverage items

For every relevant status code, the tool looks up the documented response in the called service's spec, in the order
OpenAPI defines: the exact code (`"409"`), then the range of its class (`"4XX"`, OpenAPI 3), then `default`. References
to `#/components/responses/` (OpenAPI 3) and `#/responses/` (Swagger 2.0) are followed.

The coverage items are all fields of that response schema, written as `status:path`:

| Path | Meaning |
| --- | --- |
| `201:reservationId` | top-level field of the 201 response |
| `400:address.zip` | nested object field |
| `400:fieldErrors`, `400:fieldErrors[].field` | a list and the fields of its items |
| `200:[]`, `200:[].sku` | a response whose body is a list, and the fields of its items |

- `writeOnly` fields are no response fields.
- Required or optional comes from the schema's `required` lists; a required field inside an optional object or list
  counts as required. Without `required` lists every field is optional.
- A field is **covered** if at least one response with its status code carried it with a non-null value. Absent and
  `null` count the same (Jackson writes unset fields as `null`).
- Fields returned but not in the schema are listed as *returned but not in the spec*; they are not counted.

## Coverage values

| Value | Definition |
| --- | --- |
| **Pooled** | covered fields / all fields of the judged status codes |
| **Hierarchical** | each status code's response is a tree, computed as for request elements (an element's own coverage averaged with its children, the tree value is the mean over the top-level fields); the connection value is the mean over its status codes, so every returned response weighs the same |
| **Optional only** | pooled over the optional fields (a valid response always carries its required fields) |

Totals over the called connections: pooled, mean of the pooled and of the hierarchical connection values, optional
only (pooled), lowest and median connection value, connections with response fields, n/a counts with their reasons,
and the number of fields returned but not in the spec.

## Not applicable

| Reason | When |
| --- | --- |
| callee not traced (mock) | no server span of the called service |
| no expected status code returned | every returned code is unexpected (e.g. only 5xx) |
| no response schema | the spec documents none of the returned codes, or there is no spec for the called service |
| no response fields | the documented responses have no fields (no body, or a body without properties) |
| body not captured | responses with fields were returned, but their bodies are not in the server spans |

A status code whose responses carried no captured body is not judged (listed as *body not recorded*), because it is
unknown which fields were returned. A captured body that is not JSON is judged; its fields are uncovered.

## Output

- **Summary:** section *Response Field Coverage Statistics (called connections only)* in the text and Markdown
  reports.
- **Per connection:** one line or table row per relevant status code with the spec key, the number of responses, the
  covered and uncovered fields, and the fields returned but not in the spec; ignored status codes are listed below.
- **JSON:** `responseFieldCoverage` of every connection report (values, per status code details, n/a reason).

## Example (sample-shop)

The order service reserves stock at the inventory service (`POST /api/v1/inventory/reservations`). The tests return
201, 400 and 409; the spec documents a schema for each:

| Status | Fields (covered / all) | Uncovered |
| --- | --- | --- |
| 201 | 4/4: `reservationId`, `orderId`, `createdAt`, optional `estimatedDelivery` | – |
| 400 | 4/5: `message`, optional `fieldErrors`, `fieldErrors[].field`, `fieldErrors[].message`, optional `hint` | `hint` |
| 409 | 2/2: `message`, optional `availableQuantity` | – |

Pooled 10/11 = 90.91 %; hierarchical (1 + 0.667 + 1) / 3 = 88.89 %; optional only 3/4 = 75 %.

## Prerequisites and limitations

- The called services' OpenAPI specs must describe the response bodies. Specs that only describe responses as an
  untyped `HttpEntity` (e.g. springfox output for TrainTicket's endpoints) make the metric not applicable.
- Only the responses that the tests triggered are judged; which further status codes should occur is configured in the
  coverage config and checked by status code class coverage.
- Interface mismatches between the response schemas and the caller's response classes are not reported yet.
- Headers are not response fields; `3xx` responses usually have no body and contribute no fields.
