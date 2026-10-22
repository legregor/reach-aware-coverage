# Microservice Integration Test Coverage Report

## Connection Coverage Statistics

| Metric | Value |
|--------|-------|
| Total REST connections | 4 |
| Covered REST connections | 4 |
| Uncovered REST connections | 0 |
| **REST Connection Coverage** | **100,00%** |

### Uncovered REST Connections

*No uncovered connections*


### Covered REST Connections

- **ts-basic-service** → **ts-route-service** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`
- **ts-basic-service** → **ts-train-service** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`
- **ts-basic-service** → **ts-price-service** `GET http://ts-price-service:16579/api/v1/priceservice/prices/{routeId}/{trainType}`
- **ts-basic-service** → **ts-station-service** `GET http://ts-station-service:12345/api/v1/stationservice/stations/id/{stationName}`

## Request Element Coverage Statistics

*Computed over the called connections only.*

| Coverage Metric | Value |
|-----------------|-------|
| **Request Element Coverage (pooled)** | **100,00%** |
| Mean over connections (pooled) | 100,00% |
| Mean over connections (hierarchical) | 100,00% |
| Optional elements only (pooled) | n/a |
| Lowest / median connection value (pooled) | 100,00% / 100,00% |
| Connections with request elements | 4 |
| Connections with request element coverage n/a | 0 |
| Connections whose request body was not recorded | 0 |
| Connections with interface mismatches | 0 |


## Request Value Partition Coverage Statistics

*Computed over the executed request elements of the called connections only.*

| Coverage Metric | Value |
|-----------------|-------|
| **Request Value Partition Coverage (pooled)** | **100,00%** |
| Pooled, spec constraints only | n/a |
| Share of partitions from spec constraints | 0,00% |
| Mean over connections (pooled) | 100,00% |
| Mean over connections (hierarchical) | 100,00% |
| Lowest / median connection value (pooled) | 100,00% / 100,00% |
| Connections with value partitions | 4 |
| Connections with value partition coverage n/a | 0 |
| Values sent outside the spec | 0 |


## Request Body Schema Validation Statistics

| Information | Value |
|-----------------|-------|
| Connections with body schema | 0 |
| Bodies checked against schema | 0 |
| Bodies matching schema | 0 |
| Bodies missing from traces | 0 |

## Status Code Class Coverage Statistics

| Coverage Metric | Value |
|-----------------|-------|
| **Status Code Class Coverage (all connections)** | **100,00%** |
| **Status Code Class Coverage (covered connections only)** | **100,00%** |
| Lowest / median connection value (called connections) | 100,00% / 100,00% |

### Connections Missing Expected Status Codes

None: every called connection returned all expected status codes.


## Response Field Coverage Statistics

*Computed over the called connections and their returned, expected status codes only.*

| Coverage Metric | Value |
|-----------------|-------|
| **Response Field Coverage (pooled)** | **n/a** |
| Mean over connections (pooled) | n/a |
| Mean over connections (hierarchical) | n/a |
| Optional fields only (pooled) | n/a |
| Lowest / median connection value (pooled) | n/a / n/a |
| Connections with response fields | 0 |
| Connections with response field coverage n/a | 4 (no response fields: 4) |
| Fields returned but not in the spec | 26 |


## Response Value Partition Coverage Statistics

*Computed over the covered response fields of the called connections only.*

| Coverage Metric | Value |
|-----------------|-------|
| **Response Value Partition Coverage (pooled)** | **n/a** |
| Pooled, spec constraints only | n/a |
| Share of partitions from spec constraints | n/a |
| Mean over connections (pooled) | n/a |
| Mean over connections (hierarchical) | n/a |
| Lowest / median connection value (pooled) | n/a / n/a |
| Connections with response value partitions | 0 |
| Connections with response value partition coverage n/a | 4 (no response fields: 4) |
| Values returned outside the spec | 0 |





# Detailed Statistics for Covered REST Connections

## ts-basic-service → ts-route-service

**Endpoint:** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 5 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (1 of 1) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:routeId`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:routeId` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1`, `status=0` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 200 | 5 |

### Response Field Coverage

*n/a (the documented responses have no fields)*

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 5 | none | none | `status`, `msg`, `data`, `data.id`, `data.stations`, `data.distances`, `data.startStationId`, `data.terminalStationId` |

### Response Value Partition Coverage

*n/a (the documented responses have no fields)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 5
- **Number of Unique Response Bodies:** 2

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"a3f256c1-0e43-4f7d-9c21-121bf258101f","stations":["nanjing","suzhou","shanghai"],"distances":[0,200,250],"startStationId":"nanjing","terminalStationId":"shanghai"}}
```

```json
{"status":0,"msg":"No content with the routeId","data":null}
```



---

## ts-basic-service → ts-train-service

**Endpoint:** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 5 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (1 of 1) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:id`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:id` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=0`, `status=1` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 200 | 5 |

### Response Field Coverage

*n/a (the documented responses have no fields)*

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 5 | none | none | `status`, `msg`, `data`, `data.id`, `data.economyClass`, `data.confortClass`, `data.averageSpeed` |

### Response Value Partition Coverage

*n/a (the documented responses have no fields)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 5
- **Number of Unique Response Bodies:** 3

**List of unique Response Bodies:**

```json
{"status":0,"msg":"here is no TrainType with the trainType id: NotAValidTrainType","data":null}
```

```json
{"status":1,"msg":"success","data":{"id":"GaoTieOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":250}}
```

```json
{"status":1,"msg":"success","data":{"id":"GaoTieTwo","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":200}}
```



---

## ts-basic-service → ts-price-service

**Endpoint:** `GET http://ts-price-service:16579/api/v1/priceservice/prices/{routeId}/{trainType}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 4 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (2 of 2) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:routeId`, `path:trainType`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (2 of 2) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:routeId` | 1 of 1 | `#non-empty` | none | `#empty` | none |
| `path:trainType` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1`, `status=0` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 200 | 4 |

### Response Field Coverage

*n/a (the documented responses have no fields)*

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 4 | none | none | `status`, `msg`, `data`, `data.id`, `data.trainType`, `data.routeId`, `data.basicPriceRate`, `data.firstClassPriceRate` |

### Response Value Partition Coverage

*n/a (the documented responses have no fields)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 4
- **Number of Unique Response Bodies:** 2

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"719287d6-d3e7-4b54-9a92-71d039748b22","trainType":"GaoTieOne","routeId":"a3f256c1-0e43-4f7d-9c21-121bf258101f","basicPriceRate":0.7,"firstClassPriceRate":1.0}}
```

```json
{"status":0,"msg":"No that config","data":null}
```



---

## ts-basic-service → ts-station-service

**Endpoint:** `GET http://ts-station-service:12345/api/v1/stationservice/stations/id/{stationName}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 18 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (1 of 1) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:stationNameForId`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:stationNameForId` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1`, `status=0` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 200 | 18 |

### Response Field Coverage

*n/a (the documented responses have no fields)*

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 18 | none | none | `status`, `msg`, `data` |

### Response Value Partition Coverage

*n/a (the documented responses have no fields)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 18
- **Number of Unique Response Bodies:** 4

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":"nanjing"}
```

```json
{"status":1,"msg":"Success","data":"shanghai"}
```

```json
{"status":0,"msg":"Not exists","data":"Start not existing"}
```

```json
{"status":0,"msg":"Not exists","data":"End not existing"}
```



---

