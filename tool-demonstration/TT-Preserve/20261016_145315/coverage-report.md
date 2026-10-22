# Microservice Integration Test Coverage Report

## Connection Coverage Statistics

| Metric | Value |
|--------|-------|
| Total REST connections | 36 |
| Covered REST connections | 31 |
| Uncovered REST connections | 5 |
| **REST Connection Coverage** | **86,11%** |

### Uncovered REST Connections

- **ts-seat-service** → **ts-travel2-service** `GET http://ts-travel2-service:16346/api/v1/travel2service/routes/{trainNumber}`
- **ts-travel2-service** → **ts-route-service** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`
- **ts-seat-service** → **ts-travel2-service** `GET http://ts-travel2-service:16346/api/v1/travel2service/train_types/{trainNumber}`
- **ts-travel2-service** → **ts-train-service** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`
- **ts-seat-service** → **ts-order-other-service** `POST http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/tickets`

### Covered REST Connections

- **ts-preserve-service** → **ts-seat-service** `POST http://ts-seat-service:18898/api/v1/seatservice/seats`
- **ts-seat-service** → **ts-travel-service** `GET http://ts-travel-service:12346/api/v1/travelservice/routes/{trainNumber}`
- **ts-travel-service** → **ts-route-service** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`
- **ts-preserve-service** → **ts-travel-service** `POST http://ts-travel-service:12346/api/v1/travelservice/trip_detail`
- **ts-travel-service** → **ts-ticketinfo-service** `POST http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo`
- **ts-ticketinfo-service** → **ts-basic-service** `POST http://ts-basic-service:15680/api/v1/basicservice/basic/travel`
- **ts-travel-service** → **ts-order-service** `GET http://ts-order-service:12031/api/v1/orderservice/order/{departureTime}/{toString}`
- **ts-travel-service** → **ts-seat-service** `POST http://ts-seat-service:18898/api/v1/seatservice/seats/left_tickets`
- **ts-seat-service** → **ts-travel-service** `GET http://ts-travel-service:12346/api/v1/travelservice/train_types/{trainNumber}`
- **ts-seat-service** → **ts-order-service** `POST http://ts-order-service:12031/api/v1/orderservice/order/tickets`
- **ts-preserve-service** → **ts-ticketinfo-service** `POST http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo`
- **ts-preserve-service** → **ts-security-service** `GET http://ts-security-service:11188/api/v1/securityservice/securityConfigs/{accountId}`
- **ts-security-service** → **ts-order-other-service** `GET http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/security/{checkDate}/{accountId}`
- **ts-preserve-service** → **ts-consign-service** `POST http://ts-consign-service:16111/api/v1/consignservice/consigns`
- **ts-preserve-service** → **ts-notification-service** `POST http://ts-notification-service:17853/api/v1/notifyservice/notification/preserve_success`
- **ts-basic-service** → **ts-route-service** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`
- **ts-travel-service** → **ts-train-service** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`
- **ts-basic-service** → **ts-train-service** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`
- **ts-seat-service** → **ts-config-service** `GET http://ts-config-service:15679/api/v1/configservice/configs/DirectTicketAllocationProportion`
- **ts-basic-service** → **ts-station-service** `GET http://ts-station-service:12345/api/v1/stationservice/stations/id/{stationName}`
- **ts-consign-service** → **ts-consign-price-service** `GET http://ts-consign-price-service:16110/api/v1/consignpriceservice/consignprice/{weight}/{within}`
- **ts-preserve-service** → **ts-food-service** `POST http://ts-food-service:18856/api/v1/foodservice/orders`
- **ts-preserve-service** → **ts-assurance-service** `GET http://ts-assurance-service:18888/api/v1/assuranceservice/assurances/{assuranceType}/{orderId}`
- **ts-travel-service** → **ts-ticketinfo-service** `GET http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/{stationName}`
- **ts-ticketinfo-service** → **ts-basic-service** `GET http://ts-basic-service:15680/api/v1/basicservice/basic/{name}`
- **ts-preserve-service** → **ts-user-service** `GET http://ts-user-service:12342/api/v1/userservice/users/id/{accountId}`
- **ts-basic-service** → **ts-price-service** `GET http://ts-price-service:16579/api/v1/priceservice/prices/{routeId}/{trainType}`
- **ts-preserve-service** → **ts-order-service** `POST http://ts-order-service:12031/api/v1/orderservice/order`
- **ts-preserve-service** → **ts-station-service** `GET http://ts-station-service:12345/api/v1/stationservice/stations/id/{stationName}`
- **ts-preserve-service** → **ts-contacts-service** `GET http://ts-contacts-service:12347/api/v1/contactservice/contacts/{contactsId}`
- **ts-security-service** → **ts-order-service** `GET http://ts-order-service:12031/api/v1/orderservice/order/security/{checkDate}/{accountId}`

## Request Element Coverage Statistics

*Computed over the called connections only.*

| Coverage Metric | Value |
|-----------------|-------|
| **Request Element Coverage (pooled)** | **100,00%** |
| Mean over connections (pooled) | 100,00% |
| Mean over connections (hierarchical) | 100,00% |
| Optional elements only (pooled) | 100,00% |
| Lowest / median connection value (pooled) | 100,00% / 100,00% |
| Connections with request elements | 30 |
| Connections with request element coverage n/a | 1 (callee not traced (mock): 1) |
| Connections whose request body was not recorded | 1 |
| Connections with interface mismatches | 0 |


## Request Value Partition Coverage Statistics

*Computed over the executed request elements of the called connections only.*

| Coverage Metric | Value |
|-----------------|-------|
| **Request Value Partition Coverage (pooled)** | **38,46%** |
| Pooled, spec constraints only | 55,88% |
| Share of partitions from spec constraints | 11,89% |
| Mean over connections (pooled) | 71,41% |
| Mean over connections (hierarchical) | 72,70% |
| Lowest / median connection value (pooled) | 31,58% / 100,00% |
| Connections with value partitions | 30 |
| Connections with value partition coverage n/a | 1 (callee not traced (mock): 1) |
| Values sent outside the spec | 15 |


## Request Body Schema Validation Statistics

| Information | Value |
|-----------------|-------|
| Connections with body schema | 10 |
| Bodies checked against schema | 83 |
| Bodies matching schema | 83 |
| Bodies missing from traces | 0 |

## Status Code Class Coverage Statistics

| Coverage Metric | Value |
|-----------------|-------|
| **Status Code Class Coverage (all connections)** | **55,56%** |
| **Status Code Class Coverage (covered connections only)** | **64,52%** |
| Lowest / median connection value (called connections) | 0,00% / 50,00% |

### Connections Missing Expected Status Codes

- **ts-preserve-service** → **ts-notification-service** `POST http://ts-notification-service:17853/api/v1/notifyservice/notification/preserve_success`: missing `status=1`, `status=0` (seen: `200`)
- **ts-preserve-service** → **ts-assurance-service** `GET http://ts-assurance-service:18888/api/v1/assuranceservice/assurances/{assuranceType}/{orderId}`: missing `status=1`, `status=0` (seen: `403`, `status=403`)
- **ts-preserve-service** → **ts-seat-service** `POST http://ts-seat-service:18898/api/v1/seatservice/seats`: missing `status=0` (seen: `200`, `status=1`)
- **ts-seat-service** → **ts-travel-service** `GET http://ts-travel-service:12346/api/v1/travelservice/routes/{trainNumber}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-travel-service** → **ts-route-service** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-preserve-service** → **ts-travel-service** `POST http://ts-travel-service:12346/api/v1/travelservice/trip_detail`: missing `status=0` (seen: `200`, `status=1`, `status=null`)
- **ts-travel-service** → **ts-order-service** `GET http://ts-order-service:12031/api/v1/orderservice/order/{departureTime}/{toString}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-travel-service** → **ts-seat-service** `POST http://ts-seat-service:18898/api/v1/seatservice/seats/left_tickets`: missing `status=0` (seen: `200`, `status=1`, `status=null`)
- **ts-preserve-service** → **ts-ticketinfo-service** `POST http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo`: missing `status=0` (seen: `200`, `status=1`)
- **ts-preserve-service** → **ts-security-service** `GET http://ts-security-service:11188/api/v1/securityservice/securityConfigs/{accountId}`: missing `status=0` (seen: `200`, `status=1`, `status=null`)
- **ts-security-service** → **ts-order-other-service** `GET http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/security/{checkDate}/{accountId}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-preserve-service** → **ts-consign-service** `POST http://ts-consign-service:16111/api/v1/consignservice/consigns`: missing `status=0` (seen: `200`, `status=1`)
- **ts-basic-service** → **ts-route-service** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-seat-service** → **ts-config-service** `GET http://ts-config-service:15679/api/v1/configservice/configs/DirectTicketAllocationProportion`: missing `status=0` (seen: `200`, `status=1`)
- **ts-consign-service** → **ts-consign-price-service** `GET http://ts-consign-price-service:16110/api/v1/consignpriceservice/consignprice/{weight}/{within}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-preserve-service** → **ts-food-service** `POST http://ts-food-service:18856/api/v1/foodservice/orders`: missing `status=0` (seen: `200`, `status=1`)
- **ts-basic-service** → **ts-price-service** `GET http://ts-price-service:16579/api/v1/priceservice/prices/{routeId}/{trainType}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-preserve-service** → **ts-order-service** `POST http://ts-order-service:12031/api/v1/orderservice/order`: missing `status=0` (seen: `200`, `status=1`)
- **ts-preserve-service** → **ts-station-service** `GET http://ts-station-service:12345/api/v1/stationservice/stations/id/{stationName}`: missing `status=0` (seen: `200`, `status=1`)
- **ts-security-service** → **ts-order-service** `GET http://ts-order-service:12031/api/v1/orderservice/order/security/{checkDate}/{accountId}`: missing `status=0` (seen: `200`, `500`, `status=1`, `status=500`)

**Note:** 2 connections encountered server errors (5xx) - not counted towards coverage


## Response Field Coverage Statistics

*Computed over the called connections and their returned, expected status codes only.*

| Coverage Metric | Value |
|-----------------|-------|
| **Response Field Coverage (pooled)** | **0,00%** |
| Mean over connections (pooled) | 0,00% |
| Mean over connections (hierarchical) | 0,00% |
| Optional fields only (pooled) | 0,00% |
| Lowest / median connection value (pooled) | 0,00% / 0,00% |
| Connections with response fields | 28 |
| Connections with response field coverage n/a | 3 (callee not traced (mock): 1; no response schema: 1; no response fields: 1) |
| Fields returned but not in the spec | 241 |


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
| Connections with response value partition coverage n/a | 31 (callee not traced (mock): 1; no response schema: 1; no response fields: 1; no covered response fields: 28) |
| Values returned outside the spec | 0 |





# Detailed Statistics for Covered REST Connections

## ts-preserve-service → ts-seat-service

**Endpoint:** `POST http://ts-seat-service:18898/api/v1/seatservice/seats`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 5 |

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

- **Bodies checked against schema:** 5
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
| 200 | 5 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 5 | none | `body` | `status`, `msg`, `data`, `data.seatNo`, `data.startStation`, `data.destStation` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 5
- **Number of Unique Request Bodies:** 2

**List of unique Request Bodies:**

```json
{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","startStation":"nanjing","destStation":"shanghai","seatType":2}
```

```json
{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","startStation":"nanjing","destStation":"shanghai","seatType":3}
```


#### Response Bodies

- **Number of Response Bodies:** 5
- **Number of Unique Response Bodies:** 5

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Use a new seat number!","data":{"seatNo":1984254044,"startStation":"nanjing","destStation":"shanghai"}}
```

```json
{"status":1,"msg":"Use a new seat number!","data":{"seatNo":2051808796,"startStation":"nanjing","destStation":"shanghai"}}
```

```json
{"status":1,"msg":"Use a new seat number!","data":{"seatNo":1362973994,"startStation":"nanjing","destStation":"shanghai"}}
```

```json
{"status":1,"msg":"Use a new seat number!","data":{"seatNo":629492054,"startStation":"nanjing","destStation":"shanghai"}}
```

```json
{"status":1,"msg":"Use a new seat number!","data":{"seatNo":571721009,"startStation":"nanjing","destStation":"shanghai"}}
```



---

## ts-seat-service → ts-travel-service

**Endpoint:** `GET http://ts-travel-service:12346/api/v1/travelservice/routes/{trainNumber}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 20 |

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
| 200 | 20 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 20 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.stations`, `data.distances`, `data.startStationId`, `data.terminalStationId` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 20
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"92708982-77af-4318-be25-57ccb0ff69ad","stations":["nanjing","zhenjiang","wuxi","suzhou","shanghai"],"distances":[0,100,150,200,250],"startStationId":"nanjing","terminalStationId":"shanghai"}}
```



---

## ts-travel-service → ts-route-service

**Endpoint:** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`

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
| 200 | 29 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 29 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.stations`, `data.distances`, `data.startStationId`, `data.terminalStationId` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 29
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"92708982-77af-4318-be25-57ccb0ff69ad","stations":["nanjing","zhenjiang","wuxi","suzhou","shanghai"],"distances":[0,100,150,200,250],"startStationId":"nanjing","terminalStationId":"shanghai"}}
```



---

## ts-preserve-service → ts-travel-service

**Endpoint:** `POST http://ts-travel-service:12346/api/v1/travelservice/trip_detail`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 10 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (4 of 4) |
| Hierarchical | 100,00% |
| Optional elements only | 100,00% |

- **Covered:** `body:from`, `body:to`, `body:travelDate`, `body:tripId`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 36,36% (4 of 11) | Hierarchical: 37,50% | Spec constraints only: 100,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `body:from` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:to` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:travelDate` | 1 of 2 | `#format` | `#none` | none | none |
| `body:tripId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |

### Request Body Schema Validation

- **Bodies checked against schema:** 10
- **Bodies missing from traces:** 0

**Request body schema:**

```json
{
  "type" : "object",
  "properties" : {
    "from" : {
      "type" : "string"
    },
    "to" : {
      "type" : "string"
    },
    "travelDate" : {
      "type" : "string",
      "format" : "date-time"
    },
    "tripId" : {
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
| Body Outcomes Seen | `status=1`, `status=null` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 10 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 10 | none | `body` | `status`, `msg`, `data`, `data.tripResponse`, `data.tripResponse.tripId`, `data.tripResponse.tripId.type`, `data.tripResponse.tripId.number`, `data.tripResponse.trainTypeId`, `data.tripResponse.startingStation`, `data.tripResponse.terminalStation`, `data.tripResponse.startingTime`, `data.tripResponse.endTime`, `data.tripResponse.economyClass`, `data.tripResponse.confortClass`, `data.tripResponse.priceForEconomyClass`, `data.tripResponse.priceForConfortClass`, `data.trip`, `data.trip.tripId`, `data.trip.tripId.type`, `data.trip.tripId.number`, `data.trip.trainTypeId`, `data.trip.routeId`, `data.trip.startingTime`, `data.trip.startingStationId`, `data.trip.stationsId`, `data.trip.terminalStationId`, `data.trip.endTime` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 10
- **Number of Unique Request Bodies:** 6

**List of unique Request Bodies:**

```json
{"tripId":"G1432","travelDate":"2050-01-01T00:00:00.000+00:00","from":"Nan Jing","to":"Shang Hai"}
```

```json
{"tripId":"INVALID","travelDate":"2050-01-01T00:00:00.000+00:00","from":"Nan Jing","to":"Shang Hai"}
```

```json
{"tripId":"G1432","travelDate":"2000-01-01T00:00:00.000+00:00","from":"Nan Jing","to":"Shang Hai"}
```

```json
{"tripId":"G8134","travelDate":"2050-01-01T00:00:00.000+00:00","from":"Nan Jing","to":"Shang Hai"}
```

```json
{"tripId":"G1432","travelDate":"2050-01-01T00:00:00.000+00:00","from":"INVALID","to":"INVALID"}
```

```json
{"tripId":"G1432","travelDate":"2050-01-01T00:00:00.000+00:00","from":"Nan Jing","to":"Jia Xing Nan"}
```


#### Response Bodies

- **Number of Response Bodies:** 10
- **Number of Unique Response Bodies:** 7

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"tripResponse":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","startingStation":"Nan Jing","terminalStation":"Shang Hai","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T17:00:00.000+00:00","economyClass":1073741822,"confortClass":1073741822,"priceForEconomyClass":"95.0","priceForConfortClass":"250.0"},"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingTime":"3925-12-31T16:00:00.000+00:00","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","endTime":"3925-12-31T16:00:00.000+00:00"}}}
```

```json
{"status":1,"msg":"Success","data":{"tripResponse":null,"trip":null}}
```

```json
{"status":1,"msg":"Success","data":{"tripResponse":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","startingStation":"Nan Jing","terminalStation":"Shang Hai","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T17:00:00.000+00:00","economyClass":1073741821,"confortClass":1073741821,"priceForEconomyClass":"95.0","priceForConfortClass":"250.0"},"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingTime":"3925-12-31T16:00:00.000+00:00","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","endTime":"3925-12-31T16:00:00.000+00:00"}}}
```

```json
{"status":1,"msg":"Success","data":{"tripResponse":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","startingStation":"Nan Jing","terminalStation":"Shang Hai","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T17:00:00.000+00:00","economyClass":1073741819,"confortClass":1073741819,"priceForEconomyClass":"95.0","priceForConfortClass":"250.0"},"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingTime":"3925-12-31T16:00:00.000+00:00","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","endTime":"3925-12-31T16:00:00.000+00:00"}}}
```

```json
{"status":1,"msg":"Success","data":{"tripResponse":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","startingStation":"Nan Jing","terminalStation":"Shang Hai","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T17:00:00.000+00:00","economyClass":1073741820,"confortClass":1073741820,"priceForEconomyClass":"95.0","priceForConfortClass":"250.0"},"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingTime":"3925-12-31T16:00:00.000+00:00","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","endTime":"3925-12-31T16:00:00.000+00:00"}}}
```

```json
{"status":null,"msg":null,"data":null}
```

```json
{"status":1,"msg":"Success","data":{"tripResponse":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","startingStation":"Nan Jing","terminalStation":"Shang Hai","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T17:00:00.000+00:00","economyClass":1073741823,"confortClass":1073741823,"priceForEconomyClass":"95.0","priceForConfortClass":"250.0"},"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingTime":"3925-12-31T16:00:00.000+00:00","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","endTime":"3925-12-31T16:00:00.000+00:00"}}}
```



---

## ts-travel-service → ts-ticketinfo-service

**Endpoint:** `POST http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 8 |

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
| `body:trip.tripId.type` | 1 of 6 | `#enum=G` | `#none`, `#enum=D`, `#enum=Z`, `#enum=T`, `#enum=K` | none | none |

### Request Body Schema Validation

- **Bodies checked against schema:** 8
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
| Body Outcomes Seen | `status=1`, `status=null`, `status=0` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 200 | 8 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 8 | none | `body` | `status`, `msg`, `data`, `data.status`, `data.percent`, `data.trainType`, `data.trainType.id`, `data.trainType.economyClass`, `data.trainType.confortClass`, `data.trainType.averageSpeed`, `data.prices`, `data.prices.confortClass`, `data.prices.economyClass` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 8
- **Number of Unique Request Bodies:** 4

**List of unique Request Bodies:**

```json
{"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingTime":"3925-12-31T16:00:00.000+00:00","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","endTime":"3925-12-31T16:00:00.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Shang Hai","departureTime":"2050-01-01T00:00:00.000+00:00"}
```

```json
{"trip":{"tripId":{"type":"G","number":"8134"},"trainTypeId":"test","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingTime":"2013-05-04T01:00:00.000+00:00","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","endTime":"2013-05-04T07:51:52.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Shang Hai","departureTime":"2050-01-01T00:00:00.000+00:00"}
```

```json
{"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingTime":"3925-12-31T16:00:00.000+00:00","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","endTime":"3925-12-31T16:00:00.000+00:00"},"startingPlace":"INVALID","endPlace":"INVALID","departureTime":"2050-01-01T00:00:00.000+00:00"}
```

```json
{"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingTime":"3925-12-31T16:00:00.000+00:00","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","endTime":"3925-12-31T16:00:00.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Jia Xing Nan","departureTime":"2050-01-01T00:00:00.000+00:00"}
```


#### Response Bodies

- **Number of Response Bodies:** 8
- **Number of Unique Response Bodies:** 4

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"status":true,"percent":1.0,"trainType":{"id":"GaoTieOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":250},"prices":{"confortClass":"250.0","economyClass":"95.0"}}}
```

```json
{"status":null,"msg":null,"data":null}
```

```json
{"status":0,"msg":"Start place or end place not exist!","data":{"status":false,"percent":1.0,"trainType":{"id":"GaoTieOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":250},"prices":{"confortClass":"120.0","economyClass":"95.0"}}}
```

```json
{"status":1,"msg":"Success","data":{"status":true,"percent":1.0,"trainType":{"id":"GaoTieOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":250},"prices":{"confortClass":"120.0","economyClass":"95.0"}}}
```



---

## ts-ticketinfo-service → ts-basic-service

**Endpoint:** `POST http://ts-basic-service:15680/api/v1/basicservice/basic/travel`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 13 |

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
| `body:trip.tripId.type` | 1 of 6 | `#enum=G` | `#none`, `#enum=D`, `#enum=Z`, `#enum=T`, `#enum=K` | none | none |

### Request Body Schema Validation

- **Bodies checked against schema:** 13
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
| Body Outcomes Seen | `status=1`, `status=null`, `status=0` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 200 | 13 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 13 | none | `body` | `status`, `msg`, `data`, `data.status`, `data.percent`, `data.trainType`, `data.trainType.id`, `data.trainType.economyClass`, `data.trainType.confortClass`, `data.trainType.averageSpeed`, `data.prices`, `data.prices.confortClass`, `data.prices.economyClass` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 13
- **Number of Unique Request Bodies:** 9

**List of unique Request Bodies:**

```json
{"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T16:00:00.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Shang Hai","departureTime":"2026-10-16T12:57:01.313+00:00"}
```

```json
{"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T16:00:00.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Shang Hai","departureTime":"2050-01-01T00:00:00.000+00:00"}
```

```json
{"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T16:00:00.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Shang Hai","departureTime":"2026-10-16T12:57:06.502+00:00"}
```

```json
{"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T16:00:00.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Shang Hai","departureTime":"2026-10-16T12:57:09.834+00:00"}
```

```json
{"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T16:00:00.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Shang Hai","departureTime":"2026-10-16T12:57:07.676+00:00"}
```

```json
{"trip":{"tripId":{"type":"G","number":"8134"},"trainTypeId":"test","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","startingTime":"2013-05-04T01:00:00.000+00:00","endTime":"2013-05-04T07:51:52.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Shang Hai","departureTime":"2050-01-01T00:00:00.000+00:00"}
```

```json
{"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T16:00:00.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Shang Hai","departureTime":"2026-10-16T12:56:59.293+00:00"}
```

```json
{"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T16:00:00.000+00:00"},"startingPlace":"INVALID","endPlace":"INVALID","departureTime":"2050-01-01T00:00:00.000+00:00"}
```

```json
{"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T16:00:00.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Jia Xing Nan","departureTime":"2050-01-01T00:00:00.000+00:00"}
```


#### Response Bodies

- **Number of Response Bodies:** 13
- **Number of Unique Response Bodies:** 4

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"status":true,"percent":1.0,"trainType":{"id":"GaoTieOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":250},"prices":{"confortClass":"250.0","economyClass":"95.0"}}}
```

```json
{"status":null,"msg":null,"data":null}
```

```json
{"status":0,"msg":"Start place or end place not exist!","data":{"status":false,"percent":1.0,"trainType":{"id":"GaoTieOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":250},"prices":{"confortClass":"120.0","economyClass":"95.0"}}}
```

```json
{"status":1,"msg":"Success","data":{"status":true,"percent":1.0,"trainType":{"id":"GaoTieOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":250},"prices":{"confortClass":"120.0","economyClass":"95.0"}}}
```



---

## ts-travel-service → ts-order-service

**Endpoint:** `GET http://ts-order-service:12031/api/v1/orderservice/order/{departureTime}/{toString}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 8 |

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
| `path:travelDate` | 0 of 1 | none | `#format` | none | `Sat Jan 01 08:00:00 CST 2050` (not a valid date-time, 8x) |
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
| 200 | 8 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 8 | none | `body` | `status`, `msg`, `data`, `data.travelDate`, `data.trainNumber`, `data.noSeat`, `data.businessSeat`, `data.firstClassSeat`, `data.secondClassSeat`, `data.hardSeat`, `data.softSeat`, `data.hardBed`, `data.softBed`, `data.highSoftBed` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 8
- **Number of Unique Response Bodies:** 6

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","noSeat":0,"businessSeat":0,"firstClassSeat":1,"secondClassSeat":0,"hardSeat":0,"softSeat":0,"hardBed":0,"softBed":0,"highSoftBed":0}}
```

```json
{"status":1,"msg":"Success","data":{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","noSeat":0,"businessSeat":0,"firstClassSeat":2,"secondClassSeat":0,"hardSeat":0,"softSeat":0,"hardBed":0,"softBed":0,"highSoftBed":0}}
```

```json
{"status":1,"msg":"Success","data":{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","noSeat":0,"businessSeat":0,"firstClassSeat":3,"secondClassSeat":1,"hardSeat":0,"softSeat":0,"hardBed":0,"softBed":0,"highSoftBed":0}}
```

```json
{"status":1,"msg":"Success","data":{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","noSeat":0,"businessSeat":0,"firstClassSeat":3,"secondClassSeat":0,"hardSeat":0,"softSeat":0,"hardBed":0,"softBed":0,"highSoftBed":0}}
```

```json
{"status":1,"msg":"Success","data":{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G8134","noSeat":0,"businessSeat":0,"firstClassSeat":0,"secondClassSeat":0,"hardSeat":0,"softSeat":0,"hardBed":0,"softBed":0,"highSoftBed":0}}
```

```json
{"status":1,"msg":"Success","data":{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","noSeat":0,"businessSeat":0,"firstClassSeat":0,"secondClassSeat":0,"hardSeat":0,"softSeat":0,"hardBed":0,"softBed":0,"highSoftBed":0}}
```



---

## ts-travel-service → ts-seat-service

**Endpoint:** `POST http://ts-seat-service:18898/api/v1/seatservice/seats/left_tickets`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 15 |

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

- **Bodies checked against schema:** 15
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
| Body Outcomes Seen | `status=1`, `status=null` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 15 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 15 | none | `body` | `status`, `msg`, `data` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 15
- **Number of Unique Request Bodies:** 7

**List of unique Request Bodies:**

```json
{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","startStation":"nanjing","destStation":"shanghai","seatType":3}
```

```json
{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","startStation":"nanjing","destStation":"shanghai","seatType":2}
```

```json
{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G8134","startStation":"nanjing","destStation":"shanghai","seatType":2}
```

```json
{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","startStation":"INVALID","destStation":"INVALID","seatType":3}
```

```json
{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","startStation":"INVALID","destStation":"INVALID","seatType":2}
```

```json
{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","startStation":"nanjing","destStation":"jiaxingnan","seatType":3}
```

```json
{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","startStation":"nanjing","destStation":"jiaxingnan","seatType":2}
```


#### Response Bodies

- **Number of Response Bodies:** 15
- **Number of Unique Response Bodies:** 6

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Get Left Ticket of Internal Success","data":1073741822}
```

```json
{"status":1,"msg":"Get Left Ticket of Internal Success","data":1073741821}
```

```json
{"status":1,"msg":"Get Left Ticket of Internal Success","data":1073741819}
```

```json
{"status":1,"msg":"Get Left Ticket of Internal Success","data":1073741820}
```

```json
{"status":null,"msg":null,"data":null}
```

```json
{"status":1,"msg":"Get Left Ticket of Internal Success","data":1073741823}
```



---

## ts-seat-service → ts-travel-service

**Endpoint:** `GET http://ts-travel-service:12346/api/v1/travelservice/train_types/{trainNumber}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 20 |

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
| Body Outcomes Seen | `status=1`, `status=0` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 200 | 20 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 20 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.economyClass`, `data.confortClass`, `data.averageSpeed` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 20
- **Number of Unique Response Bodies:** 2

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"GaoTieOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":250}}
```

```json
{"status":0,"msg":"No Content","data":null}
```



---

## ts-seat-service → ts-order-service

**Endpoint:** `POST http://ts-order-service:12031/api/v1/orderservice/order/tickets`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 20 |

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

- **Bodies checked against schema:** 20
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
| Body Outcomes Seen | `status=1`, `status=0` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 200 | 20 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 20 | none | `body` | `status`, `msg`, `data`, `data.soldTickets`, `data.soldTickets[].seatNo`, `data.soldTickets[].startStation`, `data.soldTickets[].destStation` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 20
- **Number of Unique Request Bodies:** 7

**List of unique Request Bodies:**

```json
{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","startStation":"nanjing","destStation":"shanghai","seatType":2}
```

```json
{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","startStation":"nanjing","destStation":"shanghai","seatType":3}
```

```json
{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G8134","startStation":"nanjing","destStation":"shanghai","seatType":2}
```

```json
{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","startStation":"INVALID","destStation":"INVALID","seatType":2}
```

```json
{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","startStation":"INVALID","destStation":"INVALID","seatType":3}
```

```json
{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","startStation":"nanjing","destStation":"jiaxingnan","seatType":2}
```

```json
{"travelDate":"2050-01-01T00:00:00.000+00:00","trainNumber":"G1432","startStation":"nanjing","destStation":"jiaxingnan","seatType":3}
```


#### Response Bodies

- **Number of Response Bodies:** 20
- **Number of Unique Response Bodies:** 5

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"soldTickets":[{"seatNo":571721009,"startStation":"nanjing","destStation":"shanghai"}]}}
```

```json
{"status":1,"msg":"Success","data":{"soldTickets":[{"seatNo":1984254044,"startStation":"nanjing","destStation":"shanghai"},{"seatNo":571721009,"startStation":"nanjing","destStation":"shanghai"}]}}
```

```json
{"status":1,"msg":"Success","data":{"soldTickets":[{"seatNo":629492054,"startStation":"nanjing","destStation":"shanghai"},{"seatNo":1984254044,"startStation":"nanjing","destStation":"shanghai"},{"seatNo":571721009,"startStation":"nanjing","destStation":"shanghai"},{"seatNo":2051808796,"startStation":"nanjing","destStation":"shanghai"}]}}
```

```json
{"status":1,"msg":"Success","data":{"soldTickets":[{"seatNo":1984254044,"startStation":"nanjing","destStation":"shanghai"},{"seatNo":571721009,"startStation":"nanjing","destStation":"shanghai"},{"seatNo":2051808796,"startStation":"nanjing","destStation":"shanghai"}]}}
```

```json
{"status":0,"msg":"Order is Null.","data":null}
```



---

## ts-preserve-service → ts-ticketinfo-service

**Endpoint:** `POST http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 5 |

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
| `body:trip.tripId.type` | 1 of 6 | `#enum=G` | `#none`, `#enum=D`, `#enum=Z`, `#enum=T`, `#enum=K` | none | none |

### Request Body Schema Validation

- **Bodies checked against schema:** 5
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
| 200 | 5 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 5 | none | `body` | `status`, `msg`, `data`, `data.status`, `data.percent`, `data.trainType`, `data.trainType.id`, `data.trainType.economyClass`, `data.trainType.confortClass`, `data.trainType.averageSpeed`, `data.prices`, `data.prices.confortClass`, `data.prices.economyClass` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 5
- **Number of Unique Request Bodies:** 5

**List of unique Request Bodies:**

```json
{"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T16:00:00.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Shang Hai","departureTime":"2026-10-16T12:57:01.313+00:00"}
```

```json
{"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T16:00:00.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Shang Hai","departureTime":"2026-10-16T12:57:06.502+00:00"}
```

```json
{"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T16:00:00.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Shang Hai","departureTime":"2026-10-16T12:57:09.834+00:00"}
```

```json
{"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T16:00:00.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Shang Hai","departureTime":"2026-10-16T12:57:07.676+00:00"}
```

```json
{"trip":{"tripId":{"type":"G","number":"1432"},"trainTypeId":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","startingStationId":"shanghai","stationsId":"suzhou","terminalStationId":"taiyuan","startingTime":"3925-12-31T16:00:00.000+00:00","endTime":"3925-12-31T16:00:00.000+00:00"},"startingPlace":"Nan Jing","endPlace":"Shang Hai","departureTime":"2026-10-16T12:56:59.293+00:00"}
```


#### Response Bodies

- **Number of Response Bodies:** 5
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"status":true,"percent":1.0,"trainType":{"id":"GaoTieOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":250},"prices":{"confortClass":"250.0","economyClass":"95.0"}}}
```



---

## ts-preserve-service → ts-security-service

**Endpoint:** `GET http://ts-security-service:11188/api/v1/securityservice/securityConfigs/{accountId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 13 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (1 of 1) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:accountId`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:accountId` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1`, `status=null` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 13 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 13 | none | `body` | `status`, `msg`, `data` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 13
- **Number of Unique Response Bodies:** 7

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success.r","data":"4d2a46c7-71cb-4cf1-b5bb-b68406d90000"}
```

```json
{"status":1,"msg":"Success.r","data":"4d2a46c7-0000-4cf1-b5bb-b68406d90000"}
```

```json
{"status":null,"msg":null,"data":null}
```

```json
{"status":1,"msg":"Success.r","data":"4d2a46c7-71cb-4cf1-b5bb-b68400000000"}
```

```json
{"status":1,"msg":"Success.r","data":"4d2a46c7-71cb-4cf1-b5bb-b68406d00000"}
```

```json
{"status":1,"msg":"Success.r","data":"4d2a46c7-71cb-4cf1-b5bb-b68406d90001"}
```

```json
{"status":1,"msg":"Success.r","data":"4d2a46c7-71cb-4cf1-b5bb-b68406000000"}
```



---

## ts-security-service → ts-order-other-service

**Endpoint:** `GET http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/security/{checkDate}/{accountId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 12 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (2 of 2) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:checkDate`, `path:accountId`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 50,00% (1 of 2) | Hierarchical: 50,00% | Spec constraints only: 0,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:checkDate` | 0 of 1 | none | `#format` | none | `Fri Oct 16 20:57:00 CST 2026` (not a valid date-time, 1x); `Fri Oct 16 20:57:06 CST 2026` (not a valid date-time, 3x); `Fri Oct 16 20:57:05 CST 2026` (not a valid date-time, 1x); `Fri Oct 16 20:57:09 CST 2026` (not a valid date-time, 1x); `Fri Oct 16 20:57:07 CST 2026` (not a valid date-time, 2x); `Fri Oct 16 20:57:08 CST 2026` (not a valid date-time, 3x); `Fri Oct 16 20:56:48 CST 2026` (not a valid date-time, 1x) |
| `path:accountId` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 12 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 12 | none | `body` | `status`, `msg`, `data`, `data.orderNumInLastOneHour`, `data.orderNumOfValidOrder` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 12
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"orderNumInLastOneHour":0,"orderNumOfValidOrder":0}}
```



---

## ts-preserve-service → ts-consign-service

**Endpoint:** `POST http://ts-consign-service:16111/api/v1/consignservice/consigns`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 1 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (10 of 10) |
| Hierarchical | 100,00% |
| Optional elements only | 100,00% |

- **Covered:** `body:accountId`, `body:consignee`, `body:from`, `body:handleDate`, `body:orderId`, `body:phone`, `body:targetDate`, `body:to`, `body:weight`, `body:within`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** `body:id`

### Request Value Partition Coverage

Pooled: 34,48% (10 of 29) | Hierarchical: 35,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `body:accountId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:consignee` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:from` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:handleDate` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:orderId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:phone` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:targetDate` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:to` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:weight` | 1 of 3 | `#positive` | `#negative`, `#zero` | `#none` | none |
| `body:within` | 1 of 2 | `#false` | `#true` | `#none` | none |

### Request Body Schema Validation

- **Bodies checked against schema:** 1
- **Bodies missing from traces:** 0

**Request body schema:**

```json
{
  "type" : "object",
  "properties" : {
    "accountId" : {
      "type" : "string"
    },
    "consignee" : {
      "type" : "string"
    },
    "from" : {
      "type" : "string"
    },
    "handleDate" : {
      "type" : "string"
    },
    "id" : {
      "type" : "string"
    },
    "orderId" : {
      "type" : "string"
    },
    "phone" : {
      "type" : "string"
    },
    "targetDate" : {
      "type" : "string"
    },
    "to" : {
      "type" : "string"
    },
    "weight" : {
      "type" : "number",
      "format" : "double"
    },
    "within" : {
      "type" : "boolean"
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
| 200 | `200` | 1 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.orderId`, `data.accountId`, `data.handleDate`, `data.targetDate`, `data.from`, `data.to`, `data.consignee`, `data.phone`, `data.weight`, `data.price` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 1
- **Number of Unique Request Bodies:** 1

**List of unique Request Bodies:**

```json
{"id":null,"orderId":"6c276857-cb47-4791-a40a-bea40c0b0af2","accountId":"4d2a46c7-71cb-4cf1-b5bb-b68406d90000","handleDate":"date","targetDate":"Sat Jan 01 01:00:00 CET 2050","from":"nanjing","to":"shanghai","consignee":"name","phone":"911","weight":75.82,"within":false}
```


#### Response Bodies

- **Number of Response Bodies:** 1
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"You have consigned successfully! The price is 307.28","data":{"id":"c6fead50-5074-40c0-a24e-dcaf1cff47b6","orderId":"6c276857-cb47-4791-a40a-bea40c0b0af2","accountId":"4d2a46c7-71cb-4cf1-b5bb-b68406d90000","handleDate":"date","targetDate":"Sat Jan 01 01:00:00 CET 2050","from":"nanjing","to":"shanghai","consignee":"name","phone":"911","weight":75.82,"price":307.28}}
```



---

## ts-preserve-service → ts-notification-service

**Endpoint:** `POST http://ts-notification-service:17853/api/v1/notifyservice/notification/preserve_success`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 3 |

### Request Element Coverage

*n/a (called service not traced (no server span, e.g. a mock): request body not recorded)*

- **Not recorded:** `body:date`, `body:email`, `body:endPlace`, `body:orderNumber`, `body:price`, `body:seatClass`, `body:seatNumber`, `body:startingPlace`, `body:startingTime`, `body:username`

### Request Value Partition Coverage

*n/a (called service not traced (no server span, e.g. a mock): request body not recorded)*

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx |
| Status Code Class Coverage | 0% |

| Status Code | Count |
|-------------|-------|
| 200 | 3 |

### Response Field Coverage

*n/a (called service not traced (no server span, e.g. a mock): responses not recorded)*

### Response Value Partition Coverage

*n/a (called service not traced (no server span, e.g. a mock): responses not recorded)*


---

## ts-basic-service → ts-route-service

**Endpoint:** `GET http://ts-route-service:11178/api/v1/routeservice/routes/{routeId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 13 |

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
| 200 | 13 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 13 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.stations`, `data.distances`, `data.startStationId`, `data.terminalStationId` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 13
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"92708982-77af-4318-be25-57ccb0ff69ad","stations":["nanjing","zhenjiang","wuxi","suzhou","shanghai"],"distances":[0,100,150,200,250],"startStationId":"nanjing","terminalStationId":"shanghai"}}
```



---

## ts-travel-service → ts-train-service

**Endpoint:** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 25 |

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
| Body Outcomes Seen | `status=1`, `status=0` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 200 | 25 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 25 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.economyClass`, `data.confortClass`, `data.averageSpeed` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 25
- **Number of Unique Response Bodies:** 2

**List of unique Response Bodies:**

```json
{"status":1,"msg":"success","data":{"id":"GaoTieOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":250}}
```

```json
{"status":0,"msg":"here is no TrainType with the trainType id: test","data":null}
```



---

## ts-basic-service → ts-train-service

**Endpoint:** `GET http://ts-train-service:14567/api/v1/trainservice/trains/{trainTypeId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 13 |

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
| Body Outcomes Seen | `status=1`, `status=0` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 200 | 13 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 13 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.economyClass`, `data.confortClass`, `data.averageSpeed` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 13
- **Number of Unique Response Bodies:** 2

**List of unique Response Bodies:**

```json
{"status":1,"msg":"success","data":{"id":"GaoTieOne","economyClass":2147483647,"confortClass":2147483647,"averageSpeed":250}}
```

```json
{"status":0,"msg":"here is no TrainType with the trainType id: test","data":null}
```



---

## ts-seat-service → ts-config-service

**Endpoint:** `GET http://ts-config-service:15679/api/v1/configservice/configs/DirectTicketAllocationProportion`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 14 |

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
| 200 | 14 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 14 | none | `body` | `status`, `msg`, `data`, `data.name`, `data.value`, `data.description` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 14
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"name":"DirectTicketAllocationProportion","value":"0.5","description":"Allocation Proportion Of The Direct Ticket - From Start To End"}}
```



---

## ts-basic-service → ts-station-service

**Endpoint:** `GET http://ts-station-service:12345/api/v1/stationservice/stations/id/{stationName}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 98 |

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
| 200 | 98 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 98 | none | `body` | `status`, `msg`, `data` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 98
- **Number of Unique Response Bodies:** 4

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":"shanghai"}
```

```json
{"status":1,"msg":"Success","data":"nanjing"}
```

```json
{"status":0,"msg":"Not exists","data":"INVALID"}
```

```json
{"status":1,"msg":"Success","data":"jiaxingnan"}
```



---

## ts-consign-service → ts-consign-price-service

**Endpoint:** `GET http://ts-consign-price-service:16110/api/v1/consignpriceservice/consignprice/{weight}/{within}`

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

- **Covered:** `path:weight`, `path:isWithinRegion`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (2 of 2) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:weight` | 1 of 1 | `#non-empty` | none | `#empty` | none |
| `path:isWithinRegion` | 1 of 1 | `#non-empty` | none | `#empty` | none |

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
| 200 | `200` | 1 | none | `body` | `status`, `msg`, `data` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 1
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":307.28}
```



---

## ts-preserve-service → ts-food-service

**Endpoint:** `POST http://ts-food-service:18856/api/v1/foodservice/orders`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 1 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (6 of 6) |
| Hierarchical | 100,00% |
| Optional elements only | 100,00% |

- **Covered:** `body:foodName`, `body:foodType`, `body:orderId`, `body:price`, `body:stationName`, `body:storeName`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** `body:id`

### Request Value Partition Coverage

Pooled: 33,33% (6 of 18) | Hierarchical: 33,33% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `body:foodName` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:foodType` | 1 of 3 | `#positive` | `#negative`, `#zero` | `#none` | none |
| `body:orderId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:price` | 1 of 3 | `#positive` | `#negative`, `#zero` | `#none` | none |
| `body:stationName` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:storeName` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |

### Request Body Schema Validation

- **Bodies checked against schema:** 1
- **Bodies missing from traces:** 0

**Request body schema:**

```json
{
  "type" : "object",
  "properties" : {
    "foodName" : {
      "type" : "string"
    },
    "foodType" : {
      "type" : "integer",
      "format" : "int32"
    },
    "id" : {
      "type" : "string"
    },
    "orderId" : {
      "type" : "string"
    },
    "price" : {
      "type" : "number",
      "format" : "double"
    },
    "stationName" : {
      "type" : "string"
    },
    "storeName" : {
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
| 200 | 1 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 1 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.orderId`, `data.foodType`, `data.stationName`, `data.storeName`, `data.foodName`, `data.price` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 1
- **Number of Unique Request Bodies:** 1

**List of unique Request Bodies:**

```json
{"id":null,"orderId":"6c276857-cb47-4791-a40a-bea40c0b0af2","foodType":2,"stationName":"station","storeName":"store","foodName":"food","price":5.99}
```


#### Response Bodies

- **Number of Response Bodies:** 1
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success.","data":{"id":"0fe7e553-eeb8-4e22-a5a8-3a2656663455","orderId":"6c276857-cb47-4791-a40a-bea40c0b0af2","foodType":2,"stationName":"station","storeName":"store","foodName":"food","price":5.99}}
```



---

## ts-preserve-service → ts-assurance-service

**Endpoint:** `GET http://ts-assurance-service:18888/api/v1/assuranceservice/assurances/{assuranceType}/{orderId}`

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

- **Covered:** `path:typeIndex`, `path:orderId`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 50,00% (2 of 4) | Hierarchical: 66,67% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:typeIndex` | 1 of 3 | `#positive` | `#negative`, `#zero` | none | none |
| `path:orderId` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 4xx |
| Body Outcomes Seen | `status=403` |
| Status Code Class Coverage | 0% |

| Status Code | Count |
|-------------|-------|
| 403 | 1 |

### Response Field Coverage

*n/a (the documented responses have no fields)*

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 403 | `403` | 1 | none | none | `timestamp`, `status`, `error`, `message`, `path` |

### Response Value Partition Coverage

*n/a (the documented responses have no fields)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 1
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"timestamp":"2026-10-16T12:57:00.287+00:00","status":403,"error":"Forbidden","message":"","path":"/api/v1/assuranceservice/assurances/100/689f741e-9edc-4bb8-ba9c-7c84a9c39118"}
```



---

## ts-travel-service → ts-ticketinfo-service

**Endpoint:** `GET http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/{stationName}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 48 |

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
| 200 | 48 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 48 | none | `body` | `status`, `msg`, `data` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 48
- **Number of Unique Response Bodies:** 4

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":"shanghai"}
```

```json
{"status":1,"msg":"Success","data":"nanjing"}
```

```json
{"status":0,"msg":"Not exists","data":"INVALID"}
```

```json
{"status":1,"msg":"Success","data":"jiaxingnan"}
```



---

## ts-ticketinfo-service → ts-basic-service

**Endpoint:** `GET http://ts-basic-service:15680/api/v1/basicservice/basic/{name}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 48 |

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
| 200 | 48 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 48 | none | `body` | `status`, `msg`, `data` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 48
- **Number of Unique Response Bodies:** 4

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":"nanjing"}
```

```json
{"status":1,"msg":"Success","data":"shanghai"}
```

```json
{"status":0,"msg":"Not exists","data":"INVALID"}
```

```json
{"status":1,"msg":"Success","data":"jiaxingnan"}
```



---

## ts-preserve-service → ts-user-service

**Endpoint:** `GET http://ts-user-service:12342/api/v1/userservice/users/id/{accountId}`

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

- **Covered:** `path:userId`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:userId` | 1 of 1 | `#non-empty` | none | `#empty` | none |

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

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 4 | none | `body` | `status`, `msg`, `data`, `data.userId`, `data.userName`, `data.password`, `data.gender`, `data.documentType`, `data.documentNum`, `data.email` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 4
- **Number of Unique Response Bodies:** 4

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Find User Success","data":{"userId":"4d2a46c7-71cb-4cf1-b5bb-b68406d90000","userName":"user1","password":"111111","gender":1,"documentType":1,"documentNum":"2135488099312X","email":"fdse_microservice@163.com"}}
```

```json
{"status":0,"msg":"No User","data":null}
```

```json
{"status":1,"msg":"Find User Success","data":{"userId":"4d2a46c7-71cb-4cf1-b5bb-b68400000000","userName":"user4","password":"111111","gender":1,"documentType":1,"documentNum":"2135488099312X","email":"fdse_microservice@163.com"}}
```

```json
{"status":1,"msg":"Find User Success","data":{"userId":"4d2a46c7-71cb-4cf1-b5bb-b68406d00000","userName":"user2","password":"111111","gender":1,"documentType":1,"documentNum":"2135488099312X","email":"fdse_microservice@163.com"}}
```



---

## ts-basic-service → ts-price-service

**Endpoint:** `GET http://ts-price-service:16579/api/v1/priceservice/prices/{routeId}/{trainType}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 12 |

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
| 200 | 12 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 12 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.trainType`, `data.routeId`, `data.basicPriceRate`, `data.firstClassPriceRate` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 12
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"6d20b8cb-039c-474c-ae25-b6177ea41152","trainType":"GaoTieOne","routeId":"92708982-77af-4318-be25-57ccb0ff69ad","basicPriceRate":0.38,"firstClassPriceRate":1.0}}
```



---

## ts-preserve-service → ts-order-service

**Endpoint:** `POST http://ts-order-service:12031/api/v1/orderservice/order`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 5 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (16 of 16) |
| Hierarchical | 100,00% |
| Optional elements only | 100,00% |

- **Covered:** `body:accountId`, `body:boughtDate`, `body:coachNumber`, `body:contactsDocumentNumber`, `body:contactsName`, `body:documentType`, `body:from`, `body:id`, `body:price`, `body:seatClass`, `body:seatNumber`, `body:status`, `body:to`, `body:trainNumber`, `body:travelDate`, `body:travelTime`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 36,36% (16 of 44) | Hierarchical: 37,50% | Spec constraints only: 100,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `body:accountId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:boughtDate` | 1 of 2 | `#format` | `#none` | none | none |
| `body:coachNumber` | 1 of 2 | `#positive` | `#zero` | `#none`, `#negative` (caller only sends 0, 5) | none |
| `body:contactsDocumentNumber` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:contactsName` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:documentType` | 1 of 3 | `#positive` | `#negative`, `#zero` | `#none` | none |
| `body:from` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:id` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:price` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:seatClass` | 1 of 3 | `#positive` | `#negative`, `#zero` | `#none` | none |
| `body:seatNumber` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:status` | 1 of 3 | `#zero` | `#negative`, `#positive` | `#none` | none |
| `body:to` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trainNumber` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:travelDate` | 1 of 2 | `#format` | `#none` | none | none |
| `body:travelTime` | 1 of 2 | `#format` | `#none` | none | none |

### Request Body Schema Validation

- **Bodies checked against schema:** 5
- **Bodies missing from traces:** 0

**Request body schema:**

```json
{
  "type" : "object",
  "properties" : {
    "accountId" : {
      "type" : "string"
    },
    "boughtDate" : {
      "type" : "string",
      "format" : "date-time"
    },
    "coachNumber" : {
      "type" : "integer",
      "format" : "int32"
    },
    "contactsDocumentNumber" : {
      "type" : "string"
    },
    "contactsName" : {
      "type" : "string"
    },
    "documentType" : {
      "type" : "integer",
      "format" : "int32"
    },
    "from" : {
      "type" : "string"
    },
    "id" : {
      "type" : "string"
    },
    "price" : {
      "type" : "string"
    },
    "seatClass" : {
      "type" : "integer",
      "format" : "int32"
    },
    "seatNumber" : {
      "type" : "string"
    },
    "status" : {
      "type" : "integer",
      "format" : "int32"
    },
    "to" : {
      "type" : "string"
    },
    "trainNumber" : {
      "type" : "string"
    },
    "travelDate" : {
      "type" : "string",
      "format" : "date-time"
    },
    "travelTime" : {
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
| 200 | 5 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 5 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.boughtDate`, `data.travelDate`, `data.travelTime`, `data.accountId`, `data.contactsName`, `data.documentType`, `data.contactsDocumentNumber`, `data.trainNumber`, `data.coachNumber`, `data.seatClass`, `data.seatNumber`, `data.from`, `data.to`, `data.status`, `data.price` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 5
- **Number of Unique Request Bodies:** 5

**List of unique Request Bodies:**

```json
{"id":"c6eab12b-3dee-4915-82a2-b13d84de51c5","boughtDate":"2026-10-16T12:57:01.313+00:00","travelDate":"2050-01-01T00:00:00.000+00:00","travelTime":"3925-12-31T16:00:00.000+00:00","accountId":"4d2a46c7-71cb-4cf1-b5bb-b68406d90000","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1432","coachNumber":5,"seatClass":2,"seatNumber":"1984254044","from":"nanjing","to":"shanghai","status":0,"price":"250.0"}
```

```json
{"id":"e55bd86e-5872-4ebb-b7e0-3bc73ed6bf3e","boughtDate":"2026-10-16T12:57:06.502+00:00","travelDate":"2050-01-01T00:00:00.000+00:00","travelTime":"3925-12-31T16:00:00.000+00:00","accountId":"4d2a46c7-0000-4cf1-b5bb-b68406d90000","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1432","coachNumber":5,"seatClass":2,"seatNumber":"2051808796","from":"nanjing","to":"shanghai","status":0,"price":"250.0"}
```

```json
{"id":"79adb1b0-1173-44be-8a7b-497c89e6cf76","boughtDate":"2026-10-16T12:57:09.834+00:00","travelDate":"2050-01-01T00:00:00.000+00:00","travelTime":"3925-12-31T16:00:00.000+00:00","accountId":"4d2a46c7-71cb-4cf1-b5bb-b68400000000","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1432","coachNumber":5,"seatClass":2,"seatNumber":"1362973994","from":"nanjing","to":"shanghai","status":0,"price":"250.0"}
```

```json
{"id":"77da0403-0bd8-4001-b464-0605c284b76b","boughtDate":"2026-10-16T12:57:07.676+00:00","travelDate":"2050-01-01T00:00:00.000+00:00","travelTime":"3925-12-31T16:00:00.000+00:00","accountId":"4d2a46c7-71cb-4cf1-b5bb-b68406d00000","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1432","coachNumber":5,"seatClass":3,"seatNumber":"629492054","from":"nanjing","to":"shanghai","status":0,"price":"95.0"}
```

```json
{"id":"d66dcc16-5ec9-44fb-8ca4-d1e642a935b3","boughtDate":"2026-10-16T12:56:59.292+00:00","travelDate":"2050-01-01T00:00:00.000+00:00","travelTime":"3925-12-31T16:00:00.000+00:00","accountId":"4d2a46c7-71cb-4cf1-b5bb-b68406000000","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1432","coachNumber":5,"seatClass":2,"seatNumber":"571721009","from":"nanjing","to":"shanghai","status":0,"price":"250.0"}
```


#### Response Bodies

- **Number of Response Bodies:** 5
- **Number of Unique Response Bodies:** 5

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"6c276857-cb47-4791-a40a-bea40c0b0af2","boughtDate":"2026-10-16T12:57:01.313+00:00","travelDate":"2050-01-01T00:00:00.000+00:00","travelTime":"3925-12-31T16:00:00.000+00:00","accountId":"4d2a46c7-71cb-4cf1-b5bb-b68406d90000","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1432","coachNumber":5,"seatClass":2,"seatNumber":"1984254044","from":"nanjing","to":"shanghai","status":0,"price":"250.0"}}
```

```json
{"status":1,"msg":"Success","data":{"id":"197d44a1-f1e7-4484-9097-023fde36a435","boughtDate":"2026-10-16T12:57:06.502+00:00","travelDate":"2050-01-01T00:00:00.000+00:00","travelTime":"3925-12-31T16:00:00.000+00:00","accountId":"4d2a46c7-0000-4cf1-b5bb-b68406d90000","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1432","coachNumber":5,"seatClass":2,"seatNumber":"2051808796","from":"nanjing","to":"shanghai","status":0,"price":"250.0"}}
```

```json
{"status":1,"msg":"Success","data":{"id":"0d853dcd-ceea-481f-9d8b-35e4c3dfa635","boughtDate":"2026-10-16T12:57:09.834+00:00","travelDate":"2050-01-01T00:00:00.000+00:00","travelTime":"3925-12-31T16:00:00.000+00:00","accountId":"4d2a46c7-71cb-4cf1-b5bb-b68400000000","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1432","coachNumber":5,"seatClass":2,"seatNumber":"1362973994","from":"nanjing","to":"shanghai","status":0,"price":"250.0"}}
```

```json
{"status":1,"msg":"Success","data":{"id":"34839bc0-9884-4fa2-959c-1f570c34be89","boughtDate":"2026-10-16T12:57:07.676+00:00","travelDate":"2050-01-01T00:00:00.000+00:00","travelTime":"3925-12-31T16:00:00.000+00:00","accountId":"4d2a46c7-71cb-4cf1-b5bb-b68406d00000","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1432","coachNumber":5,"seatClass":3,"seatNumber":"629492054","from":"nanjing","to":"shanghai","status":0,"price":"95.0"}}
```

```json
{"status":1,"msg":"Success","data":{"id":"689f741e-9edc-4bb8-ba9c-7c84a9c39118","boughtDate":"2026-10-16T12:56:59.292+00:00","travelDate":"2050-01-01T00:00:00.000+00:00","travelTime":"3925-12-31T16:00:00.000+00:00","accountId":"4d2a46c7-71cb-4cf1-b5bb-b68406000000","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1432","coachNumber":5,"seatClass":2,"seatNumber":"571721009","from":"nanjing","to":"shanghai","status":0,"price":"250.0"}}
```



---

## ts-preserve-service → ts-station-service

**Endpoint:** `GET http://ts-station-service:12345/api/v1/stationservice/stations/id/{stationName}`

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
| Body Outcomes Seen | `status=1` |
| Status Code Class Coverage | 50% |

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
- **Number of Unique Response Bodies:** 2

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":"shanghai"}
```

```json
{"status":1,"msg":"Success","data":"nanjing"}
```



---

## ts-preserve-service → ts-contacts-service

**Endpoint:** `GET http://ts-contacts-service:12347/api/v1/contactservice/contacts/{contactsId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 12 |

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
| Status Code Classes Covered | 2xx, 5xx |
| Body Outcomes Seen | `status=1`, `status=0`, `status=500` |
| Status Code Class Coverage | 100% |

| Status Code | Count |
|-------------|-------|
| 500 | 1 |
| 200 | 11 |

### Response Field Coverage

*n/a (the spec documents no response for the returned status codes)*

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | *not documented* | 11 | none | none | none |

- **Ignored status codes (not expected):** [500]

### Response Value Partition Coverage

*n/a (the spec documents no response for the returned status codes)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 12
- **Number of Unique Response Bodies:** 3

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61","accountId":"4d2a46c7-71cb-4cf1-b5bb-b68406d9da61","name":"Contacts_One","documentType":1,"documentNumber":"DocumentNumber_One","phoneNumber":"ContactsPhoneNum_One"}}
```

```json
{"status":0,"msg":"No contacts according to contacts id","data":null}
```

```json
{"timestamp":"2026-10-16T12:57:06.841+00:00","status":500,"error":"Internal Server Error","message":"","path":"/api/v1/contactservice/contacts/INVALID"}
```



---

## ts-security-service → ts-order-service

**Endpoint:** `GET http://ts-order-service:12031/api/v1/orderservice/order/security/{checkDate}/{accountId}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 13 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (2 of 2) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:checkDate`, `path:accountId`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 50,00% (1 of 2) | Hierarchical: 50,00% | Spec constraints only: 0,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:checkDate` | 0 of 1 | none | `#format` | none | `Fri Oct 16 20:57:00 CST 2026` (not a valid date-time, 1x); `Fri Oct 16 20:57:06 CST 2026` (not a valid date-time, 3x); `Fri Oct 16 20:57:05 CST 2026` (not a valid date-time, 2x); `Fri Oct 16 20:57:09 CST 2026` (not a valid date-time, 1x); `Fri Oct 16 20:57:07 CST 2026` (not a valid date-time, 2x); `Fri Oct 16 20:57:08 CST 2026` (not a valid date-time, 3x); `Fri Oct 16 20:56:47 CST 2026` (not a valid date-time, 1x) |
| `path:accountId` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `status=1`, `status=0` |
| Status Code Classes Covered | 2xx, 5xx |
| Body Outcomes Seen | `status=1`, `status=500` |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 500 | 1 |
| 200 | 12 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 12 | none | `body` | `status`, `msg`, `data`, `data.orderNumInLastOneHour`, `data.orderNumOfValidOrder` |

- **Ignored status codes (not expected):** [500]

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 13
- **Number of Unique Response Bodies:** 3

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Check Security Success . ","data":{"orderNumInLastOneHour":0,"orderNumOfValidOrder":0}}
```

```json
{"status":1,"msg":"Check Security Success . ","data":{"orderNumInLastOneHour":1,"orderNumOfValidOrder":1}}
```

```json
{"timestamp":"2026-10-16T12:57:05.578+00:00","status":500,"error":"Internal Server Error","message":"","path":"/api/v1/orderservice/order/security/Fri%20Oct%2016%2020:57:05%20CST%202026/INVALID"}
```



---

