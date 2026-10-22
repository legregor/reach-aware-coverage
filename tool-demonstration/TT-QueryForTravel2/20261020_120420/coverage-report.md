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
- **ts-basic-service** → **ts-station-service** `GET http://ts-station-service:12345/api/v1/stationservice/stations/id/{stationName}`
- **ts-basic-service** → **ts-train-service** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`
- **ts-basic-service** → **ts-price-service** `GET http://ts-price-service:16579/api/v1/priceservice/prices/{routeId}/{trainType}`

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
| Pooled, spec constraints only | 100,00% |
| Share of partitions from spec constraints | 100,00% |
| Mean over connections (pooled) | 100,00% |
| Mean over connections (hierarchical) | 100,00% |
| Lowest / median connection value (pooled) | 100,00% / 100,00% |
| Connections with value partitions | 4 |
| Connections with value partition coverage n/a | 0 |
| Values sent outside the spec | 2 |


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
| **Response Field Coverage (pooled)** | **100,00%** |
| Mean over connections (pooled) | 100,00% |
| Mean over connections (hierarchical) | 100,00% |
| Optional fields only (pooled) | n/a |
| Lowest / median connection value (pooled) | 100,00% / 100,00% |
| Connections with response fields | 4 |
| Connections with response field coverage n/a | 0 |
| Fields returned but not in the spec | 0 |


## Response Value Partition Coverage Statistics

*Computed over the covered response fields of the called connections only.*

| Coverage Metric | Value |
|-----------------|-------|
| **Response Value Partition Coverage (pooled)** | **100,00%** |
| Pooled, spec constraints only | 100,00% |
| Share of partitions from spec constraints | 100,00% |
| Mean over connections (pooled) | 100,00% |
| Mean over connections (hierarchical) | 100,00% |
| Lowest / median connection value (pooled) | 100,00% / 100,00% |
| Connections with response value partitions | 4 |
| Connections with response value partition coverage n/a | 0 |
| Values returned outside the spec | 0 |





# Detailed Statistics for Covered REST Connections

## ts-basic-service → ts-route-service

**Endpoint:** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 6 |

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

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: 100,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:routeId` | 1 of 1 | `#format` | none | none | `invalid Id` (not a valid uuid, 1x) |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1`, `status=0` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 200 | 6 |

### Response Field Coverage

Pooled: 100,00% (8 of 8) | Hierarchical: 100,00% | Optional fields only: n/a

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 6 | `status`, `msg`, `data`, `data.id`, `data.stations`, `data.distances`, `data.startStationId`, `data.terminalStationId` | none | none |

### Response Value Partition Coverage

Pooled: 100,00% (14 of 14) | Hierarchical: 100,00% | Spec constraints only: 100,00%

| Field | Covered | Covered partitions | Uncovered partitions | Returned outside the spec |
|-------|---------|--------------------|----------------------|---------------------------|
| `200:status` | 2 of 2 | `#enum=0`, `#enum=1` | none | none |
| `200:msg` | 2 of 2 | `#enum=Success`, `#enum=No content with the routeId` | none | none |
| `200:data` | 1 of 1 | `#none` | none | none |
| `200:data.id` | 1 of 1 | `#format` | none | none |
| `200:data.stations` | 2 of 2 | `#size-min`, `#size-above` | none | none |
| `200:data.distances` | 2 of 2 | `#size-min`, `#size-above` | none | none |
| `200:data.startStationId` | 2 of 2 | `#len-min`, `#len-above` | none | none |
| `200:data.terminalStationId` | 2 of 2 | `#len-min`, `#len-above` | none | none |

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 6
- **Number of Unique Response Bodies:** 3

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"a3f256c1-0e43-4f7d-9c21-121bf258101f","stations":["nanjing","suzhou","shanghai"],"distances":[0,200,250],"startStationId":"nanjing","terminalStationId":"shanghai"}}
```

```json
{"status":0,"msg":"No content with the routeId","data":null}
```

```json
{"status":1,"msg":"Success","data":{"id":"7b1e4c52-2f0a-4c8e-9d3b-5a6f8e2c1d90","stations":["a","b"],"distances":[0,100],"startStationId":"a","terminalStationId":"b"}}
```



---

## ts-basic-service → ts-station-service

**Endpoint:** `GET http://ts-station-service:12345/api/v1/stationservice/stations/id/{stationName}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 22 |

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

Pooled: 100,00% (2 of 2) | Hierarchical: 100,00% | Spec constraints only: 100,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:stationNameForId` | 2 of 2 | `#len-min`, `#len-above` | none | none | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1`, `status=0` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 200 | 22 |

### Response Field Coverage

Pooled: 100,00% (3 of 3) | Hierarchical: 100,00% | Optional fields only: n/a

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 20 | `status`, `msg`, `data` | none | none |

### Response Value Partition Coverage

Pooled: 100,00% (6 of 6) | Hierarchical: 100,00% | Spec constraints only: 100,00%

| Field | Covered | Covered partitions | Uncovered partitions | Returned outside the spec |
|-------|---------|--------------------|----------------------|---------------------------|
| `200:status` | 2 of 2 | `#enum=0`, `#enum=1` | none | none |
| `200:msg` | 2 of 2 | `#enum=Success`, `#enum=Not exists` | none | none |
| `200:data` | 2 of 2 | `#len-min`, `#len-above` | none | none |

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 20
- **Number of Unique Response Bodies:** 6

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":"shanghai"}
```

```json
{"status":1,"msg":"Success","data":"nanjing"}
```

```json
{"status":0,"msg":"Not exists","data":"End not existing"}
```

```json
{"status":0,"msg":"Not exists","data":"Start not existing"}
```

```json
{"status":1,"msg":"Success","data":"a"}
```

```json
{"status":1,"msg":"Success","data":"b"}
```



---

## ts-basic-service → ts-train-service

**Endpoint:** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 6 |

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

Pooled: 100,00% (2 of 2) | Hierarchical: 100,00% | Spec constraints only: 100,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:id` | 2 of 2 | `#len-min`, `#len-above` | none | none | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1`, `status=0` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 200 | 6 |

### Response Field Coverage

Pooled: 100,00% (7 of 7) | Hierarchical: 100,00% | Optional fields only: n/a

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 6 | `status`, `msg`, `data`, `data.id`, `data.economyClass`, `data.confortClass`, `data.averageSpeed` | none | none |

### Response Value Partition Coverage

Pooled: 100,00% (12 of 12) | Hierarchical: 100,00% | Spec constraints only: 100,00%

| Field | Covered | Covered partitions | Uncovered partitions | Returned outside the spec |
|-------|---------|--------------------|----------------------|---------------------------|
| `200:status` | 2 of 2 | `#enum=0`, `#enum=1` | none | none |
| `200:msg` | 1 of 1 | `#pattern` | none | none |
| `200:data` | 1 of 1 | `#none` | none | none |
| `200:data.id` | 2 of 2 | `#len-min`, `#len-above` | none | none |
| `200:data.economyClass` | 2 of 2 | `#min`, `#above` | none | none |
| `200:data.confortClass` | 2 of 2 | `#min`, `#above` | none | none |
| `200:data.averageSpeed` | 2 of 2 | `#min`, `#above` | none | none |

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 6
- **Number of Unique Response Bodies:** 4

**List of unique Response Bodies:**

```json
{"status":1,"msg":"success","data":{"id":"GaoTieOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":250}}
```

```json
{"status":0,"msg":"here is no TrainType with the trainType id: NotAValidTrainType","data":null}
```

```json
{"status":1,"msg":"success","data":{"id":"GaoTieTwo","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":200}}
```

```json
{"status":1,"msg":"success","data":{"id":"X","economyClass":0,"confortClass":0,"averageSpeed":1}}
```



---

## ts-basic-service → ts-price-service

**Endpoint:** `GET http://ts-price-service:16579/api/v1/priceservice/prices/{routeId}/{trainType}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 5 |

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

Pooled: 100,00% (3 of 3) | Hierarchical: 100,00% | Spec constraints only: 100,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:routeId` | 1 of 1 | `#format` | none | none | `invalid Id` (not a valid uuid, 1x) |
| `path:trainType` | 2 of 2 | `#len-min`, `#len-above` | none | none | none |

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

Pooled: 100,00% (8 of 8) | Hierarchical: 100,00% | Optional fields only: n/a

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 5 | `status`, `msg`, `data`, `data.id`, `data.trainType`, `data.routeId`, `data.basicPriceRate`, `data.firstClassPriceRate` | none | none |

### Response Value Partition Coverage

Pooled: 100,00% (13 of 13) | Hierarchical: 100,00% | Spec constraints only: 100,00%

| Field | Covered | Covered partitions | Uncovered partitions | Returned outside the spec |
|-------|---------|--------------------|----------------------|---------------------------|
| `200:status` | 2 of 2 | `#enum=0`, `#enum=1` | none | none |
| `200:msg` | 2 of 2 | `#enum=Success`, `#enum=No that config` | none | none |
| `200:data` | 1 of 1 | `#none` | none | none |
| `200:data.id` | 1 of 1 | `#format` | none | none |
| `200:data.trainType` | 2 of 2 | `#len-min`, `#len-above` | none | none |
| `200:data.routeId` | 1 of 1 | `#format` | none | none |
| `200:data.basicPriceRate` | 2 of 2 | `#interior`, `#max` | none | none |
| `200:data.firstClassPriceRate` | 2 of 2 | `#interior`, `#max` | none | none |

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 5
- **Number of Unique Response Bodies:** 3

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"719287d6-d3e7-4b54-9a92-71d039748b22","trainType":"GaoTieOne","routeId":"a3f256c1-0e43-4f7d-9c21-121bf258101f","basicPriceRate":0.7,"firstClassPriceRate":1.0}}
```

```json
{"status":0,"msg":"No that config","data":null}
```

```json
{"status":1,"msg":"Success","data":{"id":"ab58f008-3da2-4d07-95b9-b1d036f366f9","trainType":"X","routeId":"7b1e4c52-2f0a-4c8e-9d3b-5a6f8e2c1d90","basicPriceRate":1.0,"firstClassPriceRate":0.8}}
```



---

