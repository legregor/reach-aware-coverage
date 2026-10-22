# Microservice Integration Test Coverage Report

## Connection Coverage Statistics

| Metric | Value |
|--------|-------|
| Total REST connections | 29 |
| Covered REST connections | 29 |
| Uncovered REST connections | 0 |
| **REST Connection Coverage** | **100,00%** |

### Uncovered REST Connections

*No uncovered connections*


### Covered REST Connections

- **ts-route-plan-service** → **ts-travel2-service** `POST http://ts-travel2-service:16346/api/v1/travel2service/trips/left`
- **ts-travel2-service** → **ts-seat-service** `POST http://ts-seat-service:18898/api/v1/seatservice/seats/left_tickets`
- **ts-seat-service** → **ts-travel-service** `GET http://ts-travel-service:12346/api/v1/travelservice/train_types/{trainNumber}`
- **ts-travel-service** → **ts-train-service** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`
- **ts-route-plan-service** → **ts-travel-service** `POST http://ts-travel-service:12346/api/v1/travelservice/trips/left`
- **ts-travel-service** → **ts-route-service** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`
- **ts-travel-service** → **ts-seat-service** `POST http://ts-seat-service:18898/api/v1/seatservice/seats/left_tickets`
- **ts-seat-service** → **ts-config-service** `GET http://ts-config-service:15679/api/v1/configservice/configs/DirectTicketAllocationProportion`
- **ts-travel2-service** → **ts-ticketinfo-service** `POST http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo`
- **ts-ticketinfo-service** → **ts-basic-service** `POST http://ts-basic-service:15680/api/v1/basicservice/basic/travel`
- **ts-basic-service** → **ts-train-service** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`
- **ts-travel2-service** → **ts-ticketinfo-service** `GET http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/{stationName}`
- **ts-seat-service** → **ts-travel-service** `GET http://ts-travel-service:12346/api/v1/travelservice/routes/{trainNumber}`
- **ts-route-plan-service** → **ts-travel-service** `GET http://ts-travel-service:12346/api/v1/travelservice/routes/{tripId}`
- **ts-seat-service** → **ts-order-other-service** `POST http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/tickets`
- **ts-seat-service** → **ts-travel2-service** `GET http://ts-travel2-service:16346/api/v1/travel2service/routes/{trainNumber}`
- **ts-ticketinfo-service** → **ts-basic-service** `GET http://ts-basic-service:15680/api/v1/basicservice/basic/{name}`
- **ts-travel-service** → **ts-ticketinfo-service** `GET http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/{stationName}`
- **ts-basic-service** → **ts-price-service** `GET http://ts-price-service:16579/api/v1/priceservice/prices/{routeId}/{trainType}`
- **ts-travel2-service** → **ts-order-other-service** `GET http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/{departureTime}/{toString}`
- **ts-basic-service** → **ts-route-service** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`
- **ts-seat-service** → **ts-travel2-service** `GET http://ts-travel2-service:16346/api/v1/travel2service/train_types/{trainNumber}`
- **ts-travel-service** → **ts-ticketinfo-service** `POST http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo`
- **ts-basic-service** → **ts-station-service** `GET http://ts-station-service:12345/api/v1/stationservice/stations/id/{stationName}`
- **ts-travel-service** → **ts-order-service** `GET http://ts-order-service:12031/api/v1/orderservice/order/{departureTime}/{toString}`
- **ts-travel2-service** → **ts-train-service** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`
- **ts-travel2-service** → **ts-route-service** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`
- **ts-seat-service** → **ts-order-service** `POST http://ts-order-service:12031/api/v1/orderservice/order/tickets`
- **ts-route-plan-service** → **ts-travel2-service** `GET http://ts-travel2-service:16346/api/v1/travel2service/routes/{tripId}`

## Request Element Coverage Statistics

*Computed over the called connections only.*

| Coverage Metric | Value |
|-----------------|-------|
| **Request Element Coverage (pooled)** | **100,00%** |
| Mean over connections (pooled) | 100,00% |
| Mean over connections (hierarchical) | 100,00% |
| Optional elements only (pooled) | 100,00% |
| Lowest / median connection value (pooled) | 100,00% / 100,00% |
| Connections with request elements | 29 |
| Connections with request element coverage n/a | 0 |
| Connections whose request body was not recorded | 0 |
| Connections with interface mismatches | 0 |


## Request Value Partition Coverage Statistics

*Computed over the executed request elements of the called connections only.*

| Coverage Metric | Value |
|-----------------|-------|
| **Request Value Partition Coverage (pooled)** | **40,30%** |
| Pooled, spec constraints only | 58,06% |
| Share of partitions from spec constraints | 15,42% |
| Mean over connections (pooled) | 77,78% |
| Mean over connections (hierarchical) | 78,46% |
| Lowest / median connection value (pooled) | 31,58% / 100,00% |
| Connections with value partitions | 28 |
| Connections with value partition coverage n/a | 1 (no partitions: 1) |
| Values sent outside the spec | 2 |


## Request Body Schema Validation Statistics

| Information | Value |
|-----------------|-------|
| Connections with body schema | 8 |
| Bodies checked against schema | 15 |
| Bodies matching schema | 15 |
| Bodies missing from traces | 0 |

## Status Code Class Coverage Statistics

| Coverage Metric | Value |
|-----------------|-------|
| **Status Code Class Coverage (all connections)** | **56,90%** |
| **Status Code Class Coverage (covered connections only)** | **56,90%** |
| Lowest / median connection value (called connections) | 50,00% / 50,00% |

### Connections Missing Expected Status Codes

- **ts-route-plan-service** → **ts-travel2-service** `POST http://ts-travel2-service:16346/api/v1/travel2service/trips/left`: missing `status=0` (seen: `200`, `status=1`)
- **ts-travel2-service** → **ts-seat-service** `POST http://ts-seat-service:18898/api/v1/seatservice/seats/left_tickets`: missing `status=0` (seen: `200`, `status=1`)
- **ts-seat-service** → **ts-travel-service** `GET http://ts-travel-service:12346/api/v1/travelservice/train_types/{trainNumber}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-travel-service** → **ts-train-service** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-route-plan-service** → **ts-travel-service** `POST http://ts-travel-service:12346/api/v1/travelservice/trips/left`: missing `status=0` (seen: `200`, `status=1`)
- **ts-travel-service** → **ts-route-service** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-travel-service** → **ts-seat-service** `POST http://ts-seat-service:18898/api/v1/seatservice/seats/left_tickets`: missing `status=0` (seen: `200`, `status=1`)
- **ts-seat-service** → **ts-config-service** `GET http://ts-config-service:15679/api/v1/configservice/configs/DirectTicketAllocationProportion`: missing `status=0` (seen: `200`, `status=1`)
- **ts-travel2-service** → **ts-ticketinfo-service** `POST http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo`: missing `status=0` (seen: `200`, `status=1`)
- **ts-ticketinfo-service** → **ts-basic-service** `POST http://ts-basic-service:15680/api/v1/basicservice/basic/travel`: missing `status=0` (seen: `200`, `status=1`)
- **ts-basic-service** → **ts-train-service** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-seat-service** → **ts-travel-service** `GET http://ts-travel-service:12346/api/v1/travelservice/routes/{trainNumber}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-route-plan-service** → **ts-travel-service** `GET http://ts-travel-service:12346/api/v1/travelservice/routes/{tripId}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-seat-service** → **ts-order-other-service** `POST http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/tickets`: missing `status=1` (seen: `200`, `status=0`)
- **ts-seat-service** → **ts-travel2-service** `GET http://ts-travel2-service:16346/api/v1/travel2service/routes/{trainNumber}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-basic-service** → **ts-price-service** `GET http://ts-price-service:16579/api/v1/priceservice/prices/{routeId}/{trainType}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-travel2-service** → **ts-order-other-service** `GET http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/{departureTime}/{toString}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-basic-service** → **ts-route-service** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-seat-service** → **ts-travel2-service** `GET http://ts-travel2-service:16346/api/v1/travel2service/train_types/{trainNumber}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-travel-service** → **ts-ticketinfo-service** `POST http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo`: missing `status=0` (seen: `200`, `status=1`)
- **ts-travel-service** → **ts-order-service** `GET http://ts-order-service:12031/api/v1/orderservice/order/{departureTime}/{toString}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-travel2-service** → **ts-train-service** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-travel2-service** → **ts-route-service** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-seat-service** → **ts-order-service** `POST http://ts-order-service:12031/api/v1/orderservice/order/tickets`: missing `status=1` (seen: `200`, `status=0`)
- **ts-route-plan-service** → **ts-travel2-service** `GET http://ts-travel2-service:16346/api/v1/travel2service/routes/{tripId}`: missing `status=0` (seen: `200`, `status=1`)


## Response Field Coverage Statistics

*Computed over the called connections and their returned, expected status codes only.*

| Coverage Metric | Value |
|-----------------|-------|
| **Response Field Coverage (pooled)** | **0,00%** |
| Mean over connections (pooled) | 0,00% |
| Mean over connections (hierarchical) | 0,00% |
| Optional fields only (pooled) | 0,00% |
| Lowest / median connection value (pooled) | 0,00% / 0,00% |
| Connections with response fields | 25 |
| Connections with response field coverage n/a | 4 (no response schema: 4) |
| Fields returned but not in the spec | 186 |


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
| Connections with response value partition coverage n/a | 29 (no response schema: 4; no covered response fields: 25) |
| Values returned outside the spec | 0 |





# Detailed Statistics for Covered REST Connections

## ts-route-plan-service → ts-travel2-service

**Endpoint:** `POST http://ts-travel2-service:16346/api/v1/travel2service/trips/left`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 3 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (3 of 3) |
| Hierarchical | 100,00% |
| Optional elements only | 100,00% |

- **Covered:** `body:startingPlace`, `body:endPlace`, `body:departureTime`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none
- *No body schema in the OpenAPI spec: body elements taken from the caller's DTO*

### Request Value Partition Coverage

*n/a (the executed request elements have no value partitions)*

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 3 |

### Response Field Coverage

*n/a (the spec documents no response for the returned status codes)*

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | *not documented* | 3 | none | none | none |

### Response Value Partition Coverage

*n/a (the spec documents no response for the returned status codes)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 3
- **Number of Unique Request Bodies:** 3

**List of unique Request Bodies:**

```json
{"startingPlace":"Shang Hai","endPlace":"Su Zhou","departureTime":"3025-07-28T16:00:00.000+00:00"}
```

```json
{"startingPlace":"Nan Jing","endPlace":"Bei Jing","departureTime":"3025-07-28T16:00:00.000+00:00"}
```

```json
{"startingPlace":"Station A","endPlace":"Station B","departureTime":"2025-05-04T01:00:00.000+00:00"}
```


#### Response Bodies

- **Number of Response Bodies:** 3
- **Number of Unique Response Bodies:** 2

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success Query","data":[]}
```

```json
{"status":1,"msg":"Success Query","data":[{"tripId":{"type":"Z","number":"1235"},"trainTypeId":"ZhiDa","startingStation":"Nan Jing","terminalStation":"Bei Jing","startingTime":"2013-05-04T03:31:52.000+00:00","endTime":"2013-05-04T13:31:52.000+00:00","economyClass":1073741823,"confortClass":1073741823,"priceForEconomyClass":"420.0","priceForConfortClass":"1200.0"}]}
```



---

## ts-travel2-service → ts-seat-service

**Endpoint:** `POST http://ts-seat-service:18898/api/v1/seatservice/seats/left_tickets`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 2 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (5 of 5) |
| Hierarchical | 100,00% |
| Optional elements only | 100,00% |

- **Covered:** `body:destStation`, `body:seatType`, `body:startStation`, `body:trainNumber`, `body:travelDate`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 35,71% (5 of 14) | Hierarchical: 36,67% | Spec constraints only: 100,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `body:destStation` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:seatType` | 1 of 3 | `#positive` | `#negative`, `#zero` | `#none` | none |
| `body:startStation` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trainNumber` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:travelDate` | 1 of 2 | `#format` | `#none` | none | none |

### Request Body Schema Validation

- **Bodies checked against schema:** 2
- **Bodies missing from traces:** 0

**Request body schema:**

```json
{
  "type" : "object",
  "properties" : {
    "destStation" : {
      "type" : "string"
    },
    "seatType" : {
      "type" : "integer",
      "format" : "int32"
    },
    "startStation" : {
      "type" : "string"
    },
    "trainNumber" : {
      "type" : "string"
    },
    "travelDate" : {
      "type" : "string",
      "format" : "date-time"
    }
  }
}
```

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 2 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 2 | none | `body` | `status`, `msg`, `data` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 2
- **Number of Unique Request Bodies:** 2

**List of unique Request Bodies:**

```json
{"travelDate":"3025-07-28T16:00:00.000+00:00","trainNumber":"Z1235","startStation":"nanjing","destStation":"beijing","seatType":3}
```

```json
{"travelDate":"3025-07-28T16:00:00.000+00:00","trainNumber":"Z1235","startStation":"nanjing","destStation":"beijing","seatType":2}
```


#### Response Bodies

- **Number of Response Bodies:** 2
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Get Left Ticket of Internal Success","data":1073741823}
```



---

## ts-seat-service → ts-travel-service

**Endpoint:** `GET http://ts-travel-service:12346/api/v1/travelservice/train_types/{trainNumber}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 2 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (1 of 1) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:tripId`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:tripId` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 2 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 2 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.economyClass`, `data.confortClass`, `data.averageSpeed` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 2
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"DongCheOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":180}}
```



---

## ts-travel-service → ts-train-service

**Endpoint:** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 3 |

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
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 3 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 3 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.economyClass`, `data.confortClass`, `data.averageSpeed` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 3
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"success","data":{"id":"DongCheOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":180}}
```



---

## ts-route-plan-service → ts-travel-service

**Endpoint:** `POST http://ts-travel-service:12346/api/v1/travelservice/trips/left`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 3 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (3 of 3) |
| Hierarchical | 100,00% |
| Optional elements only | 100,00% |

- **Covered:** `body:departureTime`, `body:endPlace`, `body:startingPlace`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 37,50% (3 of 8) | Hierarchical: 38,89% | Spec constraints only: 100,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `body:departureTime` | 1 of 2 | `#format` | `#none` | none | none |
| `body:endPlace` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:startingPlace` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |

### Request Body Schema Validation

- **Bodies checked against schema:** 3
- **Bodies missing from traces:** 0

**Request body schema:**

```json
{
  "type" : "object",
  "properties" : {
    "departureTime" : {
      "type" : "string",
      "format" : "date-time"
    },
    "endPlace" : {
      "type" : "string"
    },
    "startingPlace" : {
      "type" : "string"
    }
  }
}
```

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 3 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 3 | none | `body` | `status`, `msg`, `data`, `data[].tripId`, `data[].tripId.type`, `data[].tripId.number`, `data[].trainTypeId`, `data[].startingStation`, `data[].terminalStation`, `data[].startingTime`, `data[].endTime`, `data[].economyClass`, `data[].confortClass`, `data[].priceForEconomyClass`, `data[].priceForConfortClass` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 3
- **Number of Unique Request Bodies:** 3

**List of unique Request Bodies:**

```json
{"startingPlace":"Shang Hai","endPlace":"Su Zhou","departureTime":"3025-07-28T16:00:00.000+00:00"}
```

```json
{"startingPlace":"Nan Jing","endPlace":"Bei Jing","departureTime":"3025-07-28T16:00:00.000+00:00"}
```

```json
{"startingPlace":"Station A","endPlace":"Station B","departureTime":"2025-05-04T01:00:00.000+00:00"}
```


#### Response Bodies

- **Number of Response Bodies:** 3
- **Number of Unique Response Bodies:** 2

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":[{"tripId":{"type":"D","number":"1345"},"trainTypeId":"DongCheOne","startingStation":"Shang Hai","terminalStation":"Su Zhou","startingTime":"2013-05-03T23:00:00.000+00:00","endTime":"2013-05-03T23:16:00.000+00:00","economyClass":1073741823,"confortClass":1073741823,"priceForEconomyClass":"22.5","priceForConfortClass":"50.0"}]}
```

```json
{"status":1,"msg":"Success","data":[]}
```



---

## ts-travel-service → ts-route-service

**Endpoint:** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 24 |

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
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 24 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 24 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.stations`, `data.distances`, `data.startStationId`, `data.terminalStationId` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 24
- **Number of Unique Response Bodies:** 5

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"084837bb-53c8-4438-87c8-0321a4d09917","stations":["suzhou","shanghai"],"distances":[0,50],"startStationId":"suzhou","terminalStationId":"shanghai"}}
```

```json
{"status":1,"msg":"Success","data":{"id":"a3f256c1-0e43-4f7d-9c21-121bf258101f","stations":["nanjing","suzhou","shanghai"],"distances":[0,200,250],"startStationId":"nanjing","terminalStationId":"shanghai"}}
```

```json
{"status":1,"msg":"Success","data":{"id":"aefcef3f-3f42-46e8-afd7-6cb2a928bd3d","stations":["nanjing","shanghai"],"distances":[0,250],"startStationId":"nanjing","terminalStationId":"shanghai"}}
```

```json
{"status":1,"msg":"Success","data":{"id":"92708982-77af-4318-be25-57ccb0ff69ad","stations":["nanjing","zhenjiang","wuxi","suzhou","shanghai"],"distances":[0,100,150,200,250],"startStationId":"nanjing","terminalStationId":"shanghai"}}
```

```json
{"status":1,"msg":"Success","data":{"id":"f3d4d4ef-693b-4456-8eed-59c0d717dd08","stations":["shanghai","suzhou"],"distances":[0,50],"startStationId":"shanghai","terminalStationId":"suzhou"}}
```



---

## ts-travel-service → ts-seat-service

**Endpoint:** `POST http://ts-seat-service:18898/api/v1/seatservice/seats/left_tickets`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 2 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (5 of 5) |
| Hierarchical | 100,00% |
| Optional elements only | 100,00% |

- **Covered:** `body:destStation`, `body:seatType`, `body:startStation`, `body:trainNumber`, `body:travelDate`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 35,71% (5 of 14) | Hierarchical: 36,67% | Spec constraints only: 100,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `body:destStation` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:seatType` | 1 of 3 | `#positive` | `#negative`, `#zero` | `#none` | none |
| `body:startStation` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trainNumber` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:travelDate` | 1 of 2 | `#format` | `#none` | none | none |

### Request Body Schema Validation

- **Bodies checked against schema:** 2
- **Bodies missing from traces:** 0

**Request body schema:**

```json
{
  "type" : "object",
  "properties" : {
    "destStation" : {
      "type" : "string"
    },
    "seatType" : {
      "type" : "integer",
      "format" : "int32"
    },
    "startStation" : {
      "type" : "string"
    },
    "trainNumber" : {
      "type" : "string"
    },
    "travelDate" : {
      "type" : "string",
      "format" : "date-time"
    }
  }
}
```

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 2 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 2 | none | `body` | `status`, `msg`, `data` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 2
- **Number of Unique Request Bodies:** 2

**List of unique Request Bodies:**

```json
{"travelDate":"3025-07-28T16:00:00.000+00:00","trainNumber":"D1345","startStation":"shanghai","destStation":"suzhou","seatType":2}
```

```json
{"travelDate":"3025-07-28T16:00:00.000+00:00","trainNumber":"D1345","startStation":"shanghai","destStation":"suzhou","seatType":3}
```


#### Response Bodies

- **Number of Response Bodies:** 2
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Get Left Ticket of Internal Success","data":1073741823}
```



---

## ts-seat-service → ts-config-service

**Endpoint:** `GET http://ts-config-service:15679/api/v1/configservice/configs/DirectTicketAllocationProportion`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 4 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (1 of 1) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:configName`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:configName` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 4 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 4 | none | `body` | `status`, `msg`, `data`, `data.name`, `data.value`, `data.description` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 4
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"name":"DirectTicketAllocationProportion","value":"0.5","description":"Allocation Proportion Of The Direct Ticket - From Start To End"}}
```



---

## ts-travel2-service → ts-ticketinfo-service

**Endpoint:** `POST http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 1 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (14 of 14) |
| Hierarchical | 100,00% |
| Optional elements only | 100,00% |

- **Covered:** `body:departureTime`, `body:endPlace`, `body:startingPlace`, `body:trip`, `body:trip.endTime`, `body:trip.routeId`, `body:trip.startingStationId`, `body:trip.startingTime`, `body:trip.stationsId`, `body:trip.terminalStationId`, `body:trip.trainTypeId`, `body:trip.tripId`, `body:trip.tripId.number`, `body:trip.tripId.type`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 31,58% (12 of 38) | Hierarchical: 37,04% | Spec constraints only: 50,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `body:departureTime` | 1 of 2 | `#format` | `#none` | none | none |
| `body:endPlace` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:startingPlace` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip` | 0 of 1 | none | `#none` | none | none |
| `body:trip.endTime` | 1 of 2 | `#format` | `#none` | none | none |
| `body:trip.routeId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.startingStationId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.startingTime` | 1 of 2 | `#format` | `#none` | none | none |
| `body:trip.stationsId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.terminalStationId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.trainTypeId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.tripId` | 0 of 1 | none | `#none` | none | none |
| `body:trip.tripId.number` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.tripId.type` | 1 of 6 | `#enum=Z` | `#none`, `#enum=G`, `#enum=D`, `#enum=T`, `#enum=K` | none | none |

### Request Body Schema Validation

- **Bodies checked against schema:** 1
- **Bodies missing from traces:** 0

**Request body schema:**

```json
{
  "type" : "object",
  "properties" : {
    "departureTime" : {
      "type" : "string",
      "format" : "date-time"
    },
    "endPlace" : {
      "type" : "string"
    },
    "startingPlace" : {
      "type" : "string"
    },
    "trip" : {
      "$ref" : "#/definitions/Trip"
    }
  }
}
```

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 1 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 1 | none | `body` | `status`, `msg`, `data`, `data.status`, `data.percent`, `data.trainType`, `data.trainType.id`, `data.trainType.economyClass`, `data.trainType.confortClass`, `data.trainType.averageSpeed`, `data.prices`, `data.prices.confortClass`, `data.prices.economyClass` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 1
- **Number of Unique Request Bodies:** 1

**List of unique Request Bodies:**

```json
{"trip":{"tripId":{"type":"Z","number":"1235"},"trainTypeId":"ZhiDa","routeId":"9fc9c261-3263-4bfa-82f8-bb44e06b2f52","startingStationId":"shanghai","stationsId":"nanjing","terminalStationId":"beijing","startingTime":"2013-05-04T03:31:52.000+00:00","endTime":"2013-05-04T09:51:52.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Bei Jing","departureTime":"3025-07-28T16:00:00.000+00:00"}
```


#### Response Bodies

- **Number of Response Bodies:** 1
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"status":true,"percent":1.0,"trainType":{"id":"ZhiDa","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":120},"prices":{"confortClass":"1200.0","economyClass":"420.0"}}}
```



---

## ts-ticketinfo-service → ts-basic-service

**Endpoint:** `POST http://ts-basic-service:15680/api/v1/basicservice/basic/travel`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 2 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (14 of 14) |
| Hierarchical | 100,00% |
| Optional elements only | 100,00% |

- **Covered:** `body:departureTime`, `body:endPlace`, `body:startingPlace`, `body:trip`, `body:trip.endTime`, `body:trip.routeId`, `body:trip.startingStationId`, `body:trip.startingTime`, `body:trip.stationsId`, `body:trip.terminalStationId`, `body:trip.trainTypeId`, `body:trip.tripId`, `body:trip.tripId.number`, `body:trip.tripId.type`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 34,21% (13 of 38) | Hierarchical: 37,19% | Spec constraints only: 62,50%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `body:departureTime` | 1 of 2 | `#format` | `#none` | none | none |
| `body:endPlace` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:startingPlace` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip` | 0 of 1 | none | `#none` | none | none |
| `body:trip.endTime` | 1 of 2 | `#format` | `#none` | none | none |
| `body:trip.routeId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.startingStationId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.startingTime` | 1 of 2 | `#format` | `#none` | none | none |
| `body:trip.stationsId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.terminalStationId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.trainTypeId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.tripId` | 0 of 1 | none | `#none` | none | none |
| `body:trip.tripId.number` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.tripId.type` | 2 of 6 | `#enum=D`, `#enum=Z` | `#none`, `#enum=G`, `#enum=T`, `#enum=K` | none | none |

### Request Body Schema Validation

- **Bodies checked against schema:** 2
- **Bodies missing from traces:** 0

**Request body schema:**

```json
{
  "type" : "object",
  "properties" : {
    "departureTime" : {
      "type" : "string",
      "format" : "date-time"
    },
    "endPlace" : {
      "type" : "string"
    },
    "startingPlace" : {
      "type" : "string"
    },
    "trip" : {
      "$ref" : "#/definitions/Trip"
    }
  }
}
```

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 2 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 2 | none | `body` | `status`, `msg`, `data`, `data.status`, `data.percent`, `data.trainType`, `data.trainType.id`, `data.trainType.economyClass`, `data.trainType.confortClass`, `data.trainType.averageSpeed`, `data.prices`, `data.prices.confortClass`, `data.prices.economyClass` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 2
- **Number of Unique Request Bodies:** 2

**List of unique Request Bodies:**

```json
{"trip":{"tripId":{"type":"D","number":"1345"},"trainTypeId":"DongCheOne","routeId":"f3d4d4ef-693b-4456-8eed-59c0d717dd08","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","startingTime":"2013-05-03T23:00:00.000+00:00","endTime":"2013-05-04T11:59:52.000+00:00"},"startingPlace":"Shang Hai","endPlace":"Su Zhou","departureTime":"3025-07-28T16:00:00.000+00:00"}
```

```json
{"trip":{"tripId":{"type":"Z","number":"1235"},"trainTypeId":"ZhiDa","routeId":"9fc9c261-3263-4bfa-82f8-bb44e06b2f52","startingStationId":"shanghai","stationsId":"nanjing","terminalStationId":"beijing","startingTime":"2013-05-04T03:31:52.000+00:00","endTime":"2013-05-04T09:51:52.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Bei Jing","departureTime":"3025-07-28T16:00:00.000+00:00"}
```


#### Response Bodies

- **Number of Response Bodies:** 2
- **Number of Unique Response Bodies:** 2

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"status":true,"percent":1.0,"trainType":{"id":"DongCheOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":180},"prices":{"confortClass":"50.0","economyClass":"22.5"}}}
```

```json
{"status":1,"msg":"Success","data":{"status":true,"percent":1.0,"trainType":{"id":"ZhiDa","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":120},"prices":{"confortClass":"1200.0","economyClass":"420.0"}}}
```



---

## ts-basic-service → ts-train-service

**Endpoint:** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 2 |

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
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 2 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 2 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.economyClass`, `data.confortClass`, `data.averageSpeed` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 2
- **Number of Unique Response Bodies:** 2

**List of unique Response Bodies:**

```json
{"status":1,"msg":"success","data":{"id":"DongCheOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":180}}
```

```json
{"status":1,"msg":"success","data":{"id":"ZhiDa","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":120}}
```



---

## ts-travel2-service → ts-ticketinfo-service

**Endpoint:** `GET http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/{stationName}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 11 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (1 of 1) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:name`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:name` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1`, `status=0` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 200 | 11 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 11 | none | `body` | `status`, `msg`, `data` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 11
- **Number of Unique Response Bodies:** 6

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":"suzhou"}
```

```json
{"status":1,"msg":"Success","data":"shanghai"}
```

```json
{"status":1,"msg":"Success","data":"nanjing"}
```

```json
{"status":1,"msg":"Success","data":"beijing"}
```

```json
{"status":0,"msg":"Not exists","data":"Station A"}
```

```json
{"status":0,"msg":"Not exists","data":"Station B"}
```



---

## ts-seat-service → ts-travel-service

**Endpoint:** `GET http://ts-travel-service:12346/api/v1/travelservice/routes/{trainNumber}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 2 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (1 of 1) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:tripId`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:tripId` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 2 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 2 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.stations`, `data.distances`, `data.startStationId`, `data.terminalStationId` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 2
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"f3d4d4ef-693b-4456-8eed-59c0d717dd08","stations":["shanghai","suzhou"],"distances":[0,50],"startStationId":"shanghai","terminalStationId":"suzhou"}}
```



---

## ts-route-plan-service → ts-travel-service

**Endpoint:** `GET http://ts-travel-service:12346/api/v1/travelservice/routes/{tripId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 1 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (1 of 1) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:tripId`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:tripId` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 1 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 1 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.stations`, `data.distances`, `data.startStationId`, `data.terminalStationId` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 1
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"f3d4d4ef-693b-4456-8eed-59c0d717dd08","stations":["shanghai","suzhou"],"distances":[0,50],"startStationId":"shanghai","terminalStationId":"suzhou"}}
```



---

## ts-seat-service → ts-order-other-service

**Endpoint:** `POST http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/tickets`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 2 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (5 of 5) |
| Hierarchical | 100,00% |
| Optional elements only | 100,00% |

- **Covered:** `body:destStation`, `body:seatType`, `body:startStation`, `body:trainNumber`, `body:travelDate`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 35,71% (5 of 14) | Hierarchical: 36,67% | Spec constraints only: 100,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `body:destStation` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:seatType` | 1 of 3 | `#positive` | `#negative`, `#zero` | `#none` | none |
| `body:startStation` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trainNumber` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:travelDate` | 1 of 2 | `#format` | `#none` | none | none |

### Request Body Schema Validation

- **Bodies checked against schema:** 2
- **Bodies missing from traces:** 0

**Request body schema:**

```json
{
  "type" : "object",
  "properties" : {
    "destStation" : {
      "type" : "string"
    },
    "seatType" : {
      "type" : "integer",
      "format" : "int32"
    },
    "startStation" : {
      "type" : "string"
    },
    "trainNumber" : {
      "type" : "string"
    },
    "travelDate" : {
      "type" : "string",
      "format" : "date-time"
    }
  }
}
```

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=0` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 2 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 2 | none | `body` | `status`, `msg` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 2
- **Number of Unique Request Bodies:** 2

**List of unique Request Bodies:**

```json
{"travelDate":"3025-07-28T16:00:00.000+00:00","trainNumber":"Z1235","startStation":"nanjing","destStation":"beijing","seatType":3}
```

```json
{"travelDate":"3025-07-28T16:00:00.000+00:00","trainNumber":"Z1235","startStation":"nanjing","destStation":"beijing","seatType":2}
```


#### Response Bodies

- **Number of Response Bodies:** 2
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":0,"msg":"Seat is Null.","data":null}
```



---

## ts-seat-service → ts-travel2-service

**Endpoint:** `GET http://ts-travel2-service:16346/api/v1/travel2service/routes/{trainNumber}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 2 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (1 of 1) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:tripId`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:tripId` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 2 |

### Response Field Coverage

*n/a (the spec documents no response for the returned status codes)*

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | *not documented* | 2 | none | none | none |

### Response Value Partition Coverage

*n/a (the spec documents no response for the returned status codes)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 2
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"[Get Route By Trip ID] Success","data":{"id":"9fc9c261-3263-4bfa-82f8-bb44e06b2f52","stations":["nanjing","xuzhou","jinan","beijing"],"distances":[0,500,700,1200],"startStationId":"nanjing","terminalStationId":"beijing"}}
```



---

## ts-ticketinfo-service → ts-basic-service

**Endpoint:** `GET http://ts-basic-service:15680/api/v1/basicservice/basic/{name}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 21 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (1 of 1) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:stationName`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:stationName` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1`, `status=0` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 200 | 21 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 21 | none | `body` | `status`, `msg`, `data` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 21
- **Number of Unique Response Bodies:** 6

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":"suzhou"}
```

```json
{"status":1,"msg":"Success","data":"shanghai"}
```

```json
{"status":1,"msg":"Success","data":"beijing"}
```

```json
{"status":1,"msg":"Success","data":"nanjing"}
```

```json
{"status":0,"msg":"Not exists","data":"Station A"}
```

```json
{"status":0,"msg":"Not exists","data":"Station B"}
```



---

## ts-travel-service → ts-ticketinfo-service

**Endpoint:** `GET http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/{stationName}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 10 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (1 of 1) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:name`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:name` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1`, `status=0` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 200 | 10 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 10 | none | `body` | `status`, `msg`, `data` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 10
- **Number of Unique Response Bodies:** 6

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":"shanghai"}
```

```json
{"status":1,"msg":"Success","data":"suzhou"}
```

```json
{"status":1,"msg":"Success","data":"beijing"}
```

```json
{"status":1,"msg":"Success","data":"nanjing"}
```

```json
{"status":0,"msg":"Not exists","data":"Station B"}
```

```json
{"status":0,"msg":"Not exists","data":"Station A"}
```



---

## ts-basic-service → ts-price-service

**Endpoint:** `GET http://ts-price-service:16579/api/v1/priceservice/prices/{routeId}/{trainType}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 2 |

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
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 2 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 2 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.trainType`, `data.routeId`, `data.basicPriceRate`, `data.firstClassPriceRate` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 2
- **Number of Unique Response Bodies:** 2

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"d5c4523a-827c-468c-95be-e9024a40572e","trainType":"DongCheOne","routeId":"f3d4d4ef-693b-4456-8eed-59c0d717dd08","basicPriceRate":0.45,"firstClassPriceRate":1.0}}
```

```json
{"status":1,"msg":"Success","data":{"id":"8fb01829-393f-4af4-9e96-f72866f94d14","trainType":"ZhiDa","routeId":"9fc9c261-3263-4bfa-82f8-bb44e06b2f52","basicPriceRate":0.35,"firstClassPriceRate":1.0}}
```



---

## ts-travel2-service → ts-order-other-service

**Endpoint:** `GET http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/{departureTime}/{toString}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 1 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (2 of 2) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:travelDate`, `path:trainNumber`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 50,00% (1 of 2) | Hierarchical: 50,00% | Spec constraints only: 0,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:travelDate` | 0 of 1 | none | `#format` | none | `Fri Jul 29 00:00:00 CST 3025` (not a valid date-time, 1x) |
| `path:trainNumber` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 1 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 1 | none | `body` | `status`, `msg`, `data`, `data.travelDate`, `data.trainNumber`, `data.noSeat`, `data.businessSeat`, `data.firstClassSeat`, `data.secondClassSeat`, `data.hardSeat`, `data.softSeat`, `data.hardBed`, `data.softBed`, `data.highSoftBed` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 1
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"travelDate":"3025-07-28T16:00:00.000+00:00","trainNumber":"Z1235","noSeat":0,"businessSeat":0,"firstClassSeat":0,"secondClassSeat":0,"hardSeat":0,"softSeat":0,"hardBed":0,"softBed":0,"highSoftBed":0}}
```



---

## ts-basic-service → ts-route-service

**Endpoint:** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 2 |

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
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 2 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 2 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.stations`, `data.distances`, `data.startStationId`, `data.terminalStationId` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 2
- **Number of Unique Response Bodies:** 2

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"f3d4d4ef-693b-4456-8eed-59c0d717dd08","stations":["shanghai","suzhou"],"distances":[0,50],"startStationId":"shanghai","terminalStationId":"suzhou"}}
```

```json
{"status":1,"msg":"Success","data":{"id":"9fc9c261-3263-4bfa-82f8-bb44e06b2f52","stations":["nanjing","xuzhou","jinan","beijing"],"distances":[0,500,700,1200],"startStationId":"nanjing","terminalStationId":"beijing"}}
```



---

## ts-seat-service → ts-travel2-service

**Endpoint:** `GET http://ts-travel2-service:16346/api/v1/travel2service/train_types/{trainNumber}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 2 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (1 of 1) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:tripId`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:tripId` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 2 |

### Response Field Coverage

*n/a (the spec documents no response for the returned status codes)*

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | *not documented* | 2 | none | none | none |

### Response Value Partition Coverage

*n/a (the spec documents no response for the returned status codes)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 2
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success query Train by trip id","data":{"id":"ZhiDa","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":120}}
```



---

## ts-travel-service → ts-ticketinfo-service

**Endpoint:** `POST http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 1 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (14 of 14) |
| Hierarchical | 100,00% |
| Optional elements only | 100,00% |

- **Covered:** `body:departureTime`, `body:endPlace`, `body:startingPlace`, `body:trip`, `body:trip.endTime`, `body:trip.routeId`, `body:trip.startingStationId`, `body:trip.startingTime`, `body:trip.stationsId`, `body:trip.terminalStationId`, `body:trip.trainTypeId`, `body:trip.tripId`, `body:trip.tripId.number`, `body:trip.tripId.type`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 31,58% (12 of 38) | Hierarchical: 37,04% | Spec constraints only: 50,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `body:departureTime` | 1 of 2 | `#format` | `#none` | none | none |
| `body:endPlace` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:startingPlace` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip` | 0 of 1 | none | `#none` | none | none |
| `body:trip.endTime` | 1 of 2 | `#format` | `#none` | none | none |
| `body:trip.routeId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.startingStationId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.startingTime` | 1 of 2 | `#format` | `#none` | none | none |
| `body:trip.stationsId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.terminalStationId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.trainTypeId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.tripId` | 0 of 1 | none | `#none` | none | none |
| `body:trip.tripId.number` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trip.tripId.type` | 1 of 6 | `#enum=D` | `#none`, `#enum=G`, `#enum=Z`, `#enum=T`, `#enum=K` | none | none |

### Request Body Schema Validation

- **Bodies checked against schema:** 1
- **Bodies missing from traces:** 0

**Request body schema:**

```json
{
  "type" : "object",
  "properties" : {
    "departureTime" : {
      "type" : "string",
      "format" : "date-time"
    },
    "endPlace" : {
      "type" : "string"
    },
    "startingPlace" : {
      "type" : "string"
    },
    "trip" : {
      "$ref" : "#/definitions/Trip"
    }
  }
}
```

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 1 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 1 | none | `body` | `status`, `msg`, `data`, `data.status`, `data.percent`, `data.trainType`, `data.trainType.id`, `data.trainType.economyClass`, `data.trainType.confortClass`, `data.trainType.averageSpeed`, `data.prices`, `data.prices.confortClass`, `data.prices.economyClass` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 1
- **Number of Unique Request Bodies:** 1

**List of unique Request Bodies:**

```json
{"trip":{"tripId":{"type":"D","number":"1345"},"trainTypeId":"DongCheOne","routeId":"f3d4d4ef-693b-4456-8eed-59c0d717dd08","startingTime":"2013-05-03T23:00:00.000+00:00","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","endTime":"2013-05-04T11:59:52.000+00:00"},"startingPlace":"Shang Hai","endPlace":"Su Zhou","departureTime":"3025-07-28T16:00:00.000+00:00"}
```


#### Response Bodies

- **Number of Response Bodies:** 1
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"status":true,"percent":1.0,"trainType":{"id":"DongCheOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":180},"prices":{"confortClass":"50.0","economyClass":"22.5"}}}
```



---

## ts-basic-service → ts-station-service

**Endpoint:** `GET http://ts-station-service:12345/api/v1/stationservice/stations/id/{stationName}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 29 |

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
| 200 | 29 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 29 | none | `body` | `status`, `msg`, `data` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 29
- **Number of Unique Response Bodies:** 6

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":"suzhou"}
```

```json
{"status":1,"msg":"Success","data":"shanghai"}
```

```json
{"status":1,"msg":"Success","data":"nanjing"}
```

```json
{"status":1,"msg":"Success","data":"beijing"}
```

```json
{"status":0,"msg":"Not exists","data":"Station A"}
```

```json
{"status":0,"msg":"Not exists","data":"Station B"}
```



---

## ts-travel-service → ts-order-service

**Endpoint:** `GET http://ts-order-service:12031/api/v1/orderservice/order/{departureTime}/{toString}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 1 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (2 of 2) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:travelDate`, `path:trainNumber`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 50,00% (1 of 2) | Hierarchical: 50,00% | Spec constraints only: 0,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:travelDate` | 0 of 1 | none | `#format` | none | `Fri Jul 29 00:00:00 CST 3025` (not a valid date-time, 1x) |
| `path:trainNumber` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 1 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 1 | none | `body` | `status`, `msg`, `data`, `data.travelDate`, `data.trainNumber`, `data.noSeat`, `data.businessSeat`, `data.firstClassSeat`, `data.secondClassSeat`, `data.hardSeat`, `data.softSeat`, `data.hardBed`, `data.softBed`, `data.highSoftBed` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 1
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"travelDate":"3025-07-28T16:00:00.000+00:00","trainNumber":"D1345","noSeat":0,"businessSeat":0,"firstClassSeat":0,"secondClassSeat":0,"hardSeat":0,"softSeat":0,"hardBed":0,"softBed":0,"highSoftBed":0}}
```



---

## ts-travel2-service → ts-train-service

**Endpoint:** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 3 |

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
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 3 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 3 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.economyClass`, `data.confortClass`, `data.averageSpeed` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 3
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"success","data":{"id":"ZhiDa","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":120}}
```



---

## ts-travel2-service → ts-route-service

**Endpoint:** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 24 |

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
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 24 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 24 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.stations`, `data.distances`, `data.startStationId`, `data.terminalStationId` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 24
- **Number of Unique Response Bodies:** 6

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"92708982-77af-4318-be25-57ccb0ff69ad","stations":["nanjing","zhenjiang","wuxi","suzhou","shanghai"],"distances":[0,100,150,200,250],"startStationId":"nanjing","terminalStationId":"shanghai"}}
```

```json
{"status":1,"msg":"Success","data":{"id":"9fc9c261-3263-4bfa-82f8-bb44e06b2f52","stations":["nanjing","xuzhou","jinan","beijing"],"distances":[0,500,700,1200],"startStationId":"nanjing","terminalStationId":"beijing"}}
```

```json
{"status":1,"msg":"Success","data":{"id":"1367db1f-461e-4ab7-87ad-2bcc05fd9cb7","stations":["shanghaihongqiao","jiaxingnan","hangzhou"],"distances":[0,150,300],"startStationId":"shanghaihongqiao","terminalStationId":"hangzhou"}}
```

```json
{"status":1,"msg":"Success","data":{"id":"d693a2c5-ef87-4a3c-bef8-600b43f62c68","stations":["taiyuan","shijiazhuang","nanjing","shanghai"],"distances":[0,300,950,1300],"startStationId":"taiyuan","terminalStationId":"shanghai"}}
```

```json
{"status":1,"msg":"Success","data":{"id":"20eb7122-3a11-423f-b10a-be0dc5bce7db","stations":["shanghai","taiyuan"],"distances":[0,1300],"startStationId":"shanghai","terminalStationId":"taiyuan"}}
```

```json
{"status":1,"msg":"Success","data":{"id":"0b23bd3e-876a-4af3-b920-c50a90c90b04","stations":["shanghai","nanjing","shijiazhuang","taiyuan"],"distances":[0,350,1000,1300],"startStationId":"shanghai","terminalStationId":"taiyuan"}}
```



---

## ts-seat-service → ts-order-service

**Endpoint:** `POST http://ts-order-service:12031/api/v1/orderservice/order/tickets`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 2 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (5 of 5) |
| Hierarchical | 100,00% |
| Optional elements only | 100,00% |

- **Covered:** `body:destStation`, `body:seatType`, `body:startStation`, `body:trainNumber`, `body:travelDate`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 35,71% (5 of 14) | Hierarchical: 36,67% | Spec constraints only: 100,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `body:destStation` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:seatType` | 1 of 3 | `#positive` | `#negative`, `#zero` | `#none` | none |
| `body:startStation` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trainNumber` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:travelDate` | 1 of 2 | `#format` | `#none` | none | none |

### Request Body Schema Validation

- **Bodies checked against schema:** 2
- **Bodies missing from traces:** 0

**Request body schema:**

```json
{
  "type" : "object",
  "properties" : {
    "destStation" : {
      "type" : "string"
    },
    "seatType" : {
      "type" : "integer",
      "format" : "int32"
    },
    "startStation" : {
      "type" : "string"
    },
    "trainNumber" : {
      "type" : "string"
    },
    "travelDate" : {
      "type" : "string",
      "format" : "date-time"
    }
  }
}
```

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=0` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 2 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 2 | none | `body` | `status`, `msg` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 2
- **Number of Unique Request Bodies:** 2

**List of unique Request Bodies:**

```json
{"travelDate":"3025-07-28T16:00:00.000+00:00","trainNumber":"D1345","startStation":"shanghai","destStation":"suzhou","seatType":2}
```

```json
{"travelDate":"3025-07-28T16:00:00.000+00:00","trainNumber":"D1345","startStation":"shanghai","destStation":"suzhou","seatType":3}
```


#### Response Bodies

- **Number of Response Bodies:** 2
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":0,"msg":"Order is Null.","data":null}
```



---

## ts-route-plan-service → ts-travel2-service

**Endpoint:** `GET http://ts-travel2-service:16346/api/v1/travel2service/routes/{tripId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 1 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (1 of 1) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:tripId`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:tripId` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 1 |

### Response Field Coverage

*n/a (the spec documents no response for the returned status codes)*

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | *not documented* | 1 | none | none | none |

### Response Value Partition Coverage

*n/a (the spec documents no response for the returned status codes)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 1
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"[Get Route By Trip ID] Success","data":{"id":"9fc9c261-3263-4bfa-82f8-bb44e06b2f52","stations":["nanjing","xuzhou","jinan","beijing"],"distances":[0,500,700,1200],"startStationId":"nanjing","terminalStationId":"beijing"}}
```



---

