# Microservice Integration Test Coverage Report

## Connection Coverage Statistics

| Metric | Value |
|--------|-------|
| Total REST connections | 7 |
| Covered REST connections | 7 |
| Uncovered REST connections | 0 |
| **REST Connection Coverage** | **100,00%** |

### Uncovered REST Connections

*No uncovered connections*


### Covered REST Connections

- **ts-cancel-service** → **ts-order-service** `GET http://ts-order-service:12031/api/v1/orderservice/order/{orderId}`
- **ts-cancel-service** → **ts-order-service** `PUT http://ts-order-service:12031/api/v1/orderservice/order`
- **ts-cancel-service** → **ts-order-other-service** `GET http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/{orderId}`
- **ts-cancel-service** → **ts-inside-payment-service** `GET http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/{userId}/{money}`
- **ts-cancel-service** → **ts-notification-service** `POST http://ts-notification-service:17853/api/v1/notifyservice/notification/order_cancel_success`
- **ts-cancel-service** → **ts-order-other-service** `PUT http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther`
- **ts-cancel-service** → **ts-user-service** `GET http://ts-user-service:12342/api/v1/userservice/users/id/{orderId}`

## Request Element Coverage Statistics

*Computed over the called connections only.*

| Coverage Metric | Value |
|-----------------|-------|
| **Request Element Coverage (pooled)** | **100,00%** |
| Mean over connections (pooled) | 100,00% |
| Mean over connections (hierarchical) | 100,00% |
| Optional elements only (pooled) | 100,00% |
| Lowest / median connection value (pooled) | 100,00% / 100,00% |
| Connections with request elements | 6 |
| Connections with request element coverage n/a | 1 (callee not traced (mock): 1) |
| Connections whose request body was not recorded | 1 |
| Connections with interface mismatches | 0 |


## Request Value Partition Coverage Statistics

*Computed over the executed request elements of the called connections only.*

| Coverage Metric | Value |
|-----------------|-------|
| **Request Value Partition Coverage (pooled)** | **38,95%** |
| Pooled, spec constraints only | 100,00% |
| Share of partitions from spec constraints | 6,32% |
| Mean over connections (pooled) | 78,52% |
| Mean over connections (hierarchical) | 78,82% |
| Lowest / median connection value (pooled) | 35,56% / 100,00% |
| Connections with value partitions | 6 |
| Connections with value partition coverage n/a | 1 (callee not traced (mock): 1) |
| Values sent outside the spec | 0 |


## Request Body Schema Validation Statistics

| Information | Value |
|-----------------|-------|
| Connections with body schema | 2 |
| Bodies checked against schema | 3 |
| Bodies matching schema | 3 |
| Bodies missing from traces | 0 |

## Status Code Class Coverage Statistics

| Coverage Metric | Value |
|-----------------|-------|
| **Status Code Class Coverage (all connections)** | **50,00%** |
| **Status Code Class Coverage (covered connections only)** | **50,00%** |
| Lowest / median connection value (called connections) | 50,00% / 50,00% |

### Connections Missing Expected Status Codes

- **ts-cancel-service** → **ts-order-service** `GET http://ts-order-service:12031/api/v1/orderservice/order/{orderId}`: missing `4xx` (seen: `200`)
- **ts-cancel-service** → **ts-order-service** `PUT http://ts-order-service:12031/api/v1/orderservice/order`: missing `4xx` (seen: `200`)
- **ts-cancel-service** → **ts-order-other-service** `GET http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/{orderId}`: missing `4xx` (seen: `200`)
- **ts-cancel-service** → **ts-inside-payment-service** `GET http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/{userId}/{money}`: missing `4xx` (seen: `200`)
- **ts-cancel-service** → **ts-notification-service** `POST http://ts-notification-service:17853/api/v1/notifyservice/notification/order_cancel_success`: missing `4xx` (seen: `200`)
- **ts-cancel-service** → **ts-order-other-service** `PUT http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther`: missing `4xx` (seen: `200`)
- **ts-cancel-service** → **ts-user-service** `GET http://ts-user-service:12342/api/v1/userservice/users/id/{orderId}`: missing `4xx` (seen: `200`)


## Response Field Coverage Statistics

*Computed over the called connections and their returned, expected status codes only.*

| Coverage Metric | Value |
|-----------------|-------|
| **Response Field Coverage (pooled)** | **0,00%** |
| Mean over connections (pooled) | 0,00% |
| Mean over connections (hierarchical) | 0,00% |
| Optional fields only (pooled) | 0,00% |
| Lowest / median connection value (pooled) | 0,00% / 0,00% |
| Connections with response fields | 6 |
| Connections with response field coverage n/a | 1 (callee not traced (mock): 1) |
| Fields returned but not in the spec | 88 |


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
| Connections with response value partition coverage n/a | 7 (callee not traced (mock): 1; no covered response fields: 6) |
| Values returned outside the spec | 0 |





# Detailed Statistics for Covered REST Connections

## ts-cancel-service → ts-order-service

**Endpoint:** `GET http://ts-order-service:12031/api/v1/orderservice/order/{orderId}`

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

- **Covered:** `path:orderId`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:orderId` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `2xx`, `4xx` |
| Status Code Classes Covered | 2xx |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 6 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 5 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.boughtDate`, `data.travelDate`, `data.travelTime`, `data.accountId`, `data.contactsName`, `data.documentType`, `data.contactsDocumentNumber`, `data.trainNumber`, `data.coachNumber`, `data.seatClass`, `data.seatNumber`, `data.from`, `data.to`, `data.status`, `data.price` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 5
- **Number of Unique Response Bodies:** 4

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success.","data":{"id":"e9cb042e-54d3-4b56-b5fa-67ff1c1c992b","boughtDate":"2026-10-22T08:02:47.222+00:00","travelDate":"1970-01-03T23:24:49.287+00:00","travelTime":"1970-01-02T10:17:36.799+00:00","accountId":"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1235","coachNumber":5,"seatClass":0,"seatNumber":"FirstClass-30","from":"shanghai","to":"beijing","status":0,"price":"100.0"}}
```

```json
{"status":1,"msg":"Success.","data":{"id":"f8e2dc60-bd59-4af9-bf15-507f3b6572d7","boughtDate":"2026-10-22T08:02:47.218+00:00","travelDate":"1970-01-03T23:24:49.287+00:00","travelTime":"1970-01-02T10:17:36.799+00:00","accountId":"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1235","coachNumber":5,"seatClass":2,"seatNumber":"FirstClass-30","from":"shanghai","to":"beijing","status":2,"price":"100.0"}}
```

```json
{"status":0,"msg":"Order Not Found","data":null}
```

```json
{"status":1,"msg":"Success.","data":{"id":"e9cb042e-54d3-4b56-b5fa-67ff1c1c992c","boughtDate":"2026-10-22T08:02:47.226+00:00","travelDate":"1970-01-03T23:24:49.287+00:00","travelTime":"1970-01-02T10:17:36.799+00:00","accountId":"0ccf8330-4f74-488c-8572-9da813727e12","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1235","coachNumber":5,"seatClass":0,"seatNumber":"FirstClass-30","from":"shanghai","to":"beijing","status":0,"price":"100.0"}}
```



---

## ts-cancel-service → ts-order-service

**Endpoint:** `PUT http://ts-order-service:12031/api/v1/orderservice/order`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 2 |

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

Pooled: 35,56% (16 of 45) | Hierarchical: 36,46% | Spec constraints only: 100,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `body:accountId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:boughtDate` | 1 of 2 | `#format` | `#none` | none | none |
| `body:coachNumber` | 1 of 3 | `#positive` | `#negative`, `#zero` | `#none` | none |
| `body:contactsDocumentNumber` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:contactsName` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:documentType` | 1 of 3 | `#positive` | `#negative`, `#zero` | `#none` | none |
| `body:from` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:id` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:price` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:seatClass` | 1 of 3 | `#zero` | `#negative`, `#positive` | `#none` | none |
| `body:seatNumber` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:status` | 1 of 3 | `#positive` | `#negative`, `#zero` | `#none` | none |
| `body:to` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trainNumber` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:travelDate` | 1 of 2 | `#format` | `#none` | none | none |
| `body:travelTime` | 1 of 2 | `#format` | `#none` | none | none |

### Request Body Schema Validation

- **Bodies checked against schema:** 2
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
| Expected | `2xx`, `4xx` |
| Status Code Classes Covered | 2xx |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 2 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 2 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.boughtDate`, `data.travelDate`, `data.travelTime`, `data.accountId`, `data.contactsName`, `data.documentType`, `data.contactsDocumentNumber`, `data.trainNumber`, `data.coachNumber`, `data.seatClass`, `data.seatNumber`, `data.from`, `data.to`, `data.status`, `data.price` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 2
- **Number of Unique Request Bodies:** 2

**List of unique Request Bodies:**

```json
{"id":"e9cb042e-54d3-4b56-b5fa-67ff1c1c992b","boughtDate":"2026-10-22T08:02:47.222+00:00","travelDate":"1970-01-03T23:24:49.287+00:00","travelTime":"1970-01-02T10:17:36.799+00:00","accountId":"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1235","coachNumber":5,"seatClass":0,"seatNumber":"FirstClass-30","from":"shanghai","to":"beijing","status":4,"price":"100.0"}
```

```json
{"id":"e9cb042e-54d3-4b56-b5fa-67ff1c1c992c","boughtDate":"2026-10-22T08:02:47.226+00:00","travelDate":"1970-01-03T23:24:49.287+00:00","travelTime":"1970-01-02T10:17:36.799+00:00","accountId":"0ccf8330-4f74-488c-8572-9da813727e12","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1235","coachNumber":5,"seatClass":0,"seatNumber":"FirstClass-30","from":"shanghai","to":"beijing","status":4,"price":"100.0"}
```


#### Response Bodies

- **Number of Response Bodies:** 2
- **Number of Unique Response Bodies:** 2

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"e9cb042e-54d3-4b56-b5fa-67ff1c1c992b","boughtDate":"2026-10-22T08:02:47.222+00:00","travelDate":"1970-01-03T23:24:49.287+00:00","travelTime":"1970-01-02T10:17:36.799+00:00","accountId":"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1235","coachNumber":5,"seatClass":0,"seatNumber":"FirstClass-30","from":"shanghai","to":"beijing","status":4,"price":"100.0"}}
```

```json
{"status":1,"msg":"Success","data":{"id":"e9cb042e-54d3-4b56-b5fa-67ff1c1c992c","boughtDate":"2026-10-22T08:02:47.226+00:00","travelDate":"1970-01-03T23:24:49.287+00:00","travelTime":"1970-01-02T10:17:36.799+00:00","accountId":"0ccf8330-4f74-488c-8572-9da813727e12","contactsName":"Contacts_One","documentType":1,"contactsDocumentNumber":"DocumentNumber_One","trainNumber":"G1235","coachNumber":5,"seatClass":0,"seatNumber":"FirstClass-30","from":"shanghai","to":"beijing","status":4,"price":"100.0"}}
```



---

## ts-cancel-service → ts-order-other-service

**Endpoint:** `GET http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/{orderId}`

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

- **Covered:** `path:orderId`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (1 of 1) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:orderId` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `2xx`, `4xx` |
| Status Code Classes Covered | 2xx |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 3 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 3 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.boughtDate`, `data.travelDate`, `data.travelTime`, `data.accountId`, `data.contactsName`, `data.documentType`, `data.contactsDocumentNumber`, `data.trainNumber`, `data.coachNumber`, `data.seatClass`, `data.seatNumber`, `data.from`, `data.to`, `data.status`, `data.price` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 3
- **Number of Unique Response Bodies:** 3

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"f8e2dc60-bd59-4af9-bf15-507f3b6572d4","boughtDate":"2026-10-22T08:02:34.818+00:00","travelDate":"1970-01-02T10:17:36.799+00:00","travelTime":"1970-01-02T10:17:36.799+00:00","accountId":"f8e2dc60-bd59-4af9-bf15-507f3b6572d4","contactsName":"Test","documentType":1,"contactsDocumentNumber":"Test","trainNumber":"Z1234","coachNumber":5,"seatClass":2,"seatNumber":"6A","from":"shanghai","to":"taiyuan","status":2,"price":"100"}}
```

```json
{"status":1,"msg":"Success","data":{"id":"f8e2dc60-bd59-4af9-bf15-507f3b6572d6","boughtDate":"2026-10-22T08:02:34.806+00:00","travelDate":"1970-01-02T10:17:36.799+00:00","travelTime":"1970-01-02T10:17:36.799+00:00","accountId":"f8e2dc60-bd59-4af9-bf15-507f3b6572d6","contactsName":"Test","documentType":1,"contactsDocumentNumber":"Test","trainNumber":"K1235","coachNumber":5,"seatClass":2,"seatNumber":"6A","from":"shanghai","to":"taiyuan","status":0,"price":"100"}}
```

```json
{"status":0,"msg":"Order Not Found","data":null}
```



---

## ts-cancel-service → ts-inside-payment-service

**Endpoint:** `GET http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/{userId}/{money}`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 3 |

### Request Element Coverage

| Metric | Value |
|--------|-------|
| Pooled | 100,00% (2 of 2) |
| Hierarchical | 100,00% |
| Optional elements only | n/a |

- **Covered:** `path:userId`, `path:money`
- **Uncovered:** none
- **Not reachable (never sent by the caller):** none

### Request Value Partition Coverage

Pooled: 100,00% (2 of 2) | Hierarchical: 100,00% | Spec constraints only: n/a

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `path:userId` | 1 of 1 | `#non-empty` | none | `#empty` | none |
| `path:money` | 1 of 1 | `#non-empty` | none | `#empty` | none |

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `2xx`, `4xx` |
| Status Code Classes Covered | 2xx |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 3 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 3 | none | `body` | `status`, `msg` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 3
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Draw Back Money Success","data":null}
```



---

## ts-cancel-service → ts-notification-service

**Endpoint:** `POST http://ts-notification-service:17853/api/v1/notifyservice/notification/order_cancel_success`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 1 |

### Request Element Coverage

*n/a (called service not traced (no server span, e.g. a mock): request body not recorded)*

- **Not recorded:** `body:date`, `body:email`, `body:endPlace`, `body:orderNumber`, `body:price`, `body:seatClass`, `body:seatNumber`, `body:startingPlace`, `body:startingTime`, `body:username`

### Request Value Partition Coverage

*n/a (called service not traced (no server span, e.g. a mock): request body not recorded)*

### Status Code Class Coverage

| Metric | Value |
|--------|-------|
| Expected | `2xx`, `4xx` |
| Status Code Classes Covered | 2xx |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 1 |

### Response Field Coverage

*n/a (called service not traced (no server span, e.g. a mock): responses not recorded)*

### Response Value Partition Coverage

*n/a (called service not traced (no server span, e.g. a mock): responses not recorded)*


---

## ts-cancel-service → ts-order-other-service

**Endpoint:** `PUT http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther`

### Statistics

| Metric | Value |
|--------|-------|
| Usage Count | 1 |

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

Pooled: 35,56% (16 of 45) | Hierarchical: 36,46% | Spec constraints only: 100,00%

| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |
|---------|---------|--------------------|----------------------|---------------|------------------|
| `body:accountId` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:boughtDate` | 1 of 2 | `#format` | `#none` | none | none |
| `body:coachNumber` | 1 of 3 | `#positive` | `#negative`, `#zero` | `#none` | none |
| `body:contactsDocumentNumber` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:contactsName` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:documentType` | 1 of 3 | `#positive` | `#negative`, `#zero` | `#none` | none |
| `body:from` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:id` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:price` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:seatClass` | 1 of 3 | `#positive` | `#negative`, `#zero` | `#none` | none |
| `body:seatNumber` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:status` | 1 of 3 | `#positive` | `#negative`, `#zero` | `#none` | none |
| `body:to` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:trainNumber` | 1 of 3 | `#non-empty` | `#none`, `#empty` | none | none |
| `body:travelDate` | 1 of 2 | `#format` | `#none` | none | none |
| `body:travelTime` | 1 of 2 | `#format` | `#none` | none | none |

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
| Expected | `2xx`, `4xx` |
| Status Code Classes Covered | 2xx |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 1 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 1 | none | `body` | `status`, `msg`, `data`, `data.id`, `data.boughtDate`, `data.travelDate`, `data.travelTime`, `data.accountId`, `data.contactsName`, `data.documentType`, `data.contactsDocumentNumber`, `data.trainNumber`, `data.coachNumber`, `data.seatClass`, `data.seatNumber`, `data.from`, `data.to`, `data.status`, `data.price` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Request Bodies

- **Number of Request Bodies:** 1
- **Number of Unique Request Bodies:** 1

**List of unique Request Bodies:**

```json
{"id":"f8e2dc60-bd59-4af9-bf15-507f3b6572d6","boughtDate":"2026-10-22T08:02:34.806+00:00","travelDate":"1970-01-02T10:17:36.799+00:00","travelTime":"1970-01-02T10:17:36.799+00:00","accountId":"f8e2dc60-bd59-4af9-bf15-507f3b6572d6","contactsName":"Test","documentType":1,"contactsDocumentNumber":"Test","trainNumber":"K1235","coachNumber":5,"seatClass":2,"seatNumber":"6A","from":"shanghai","to":"taiyuan","status":4,"price":"100"}
```


#### Response Bodies

- **Number of Response Bodies:** 1
- **Number of Unique Response Bodies:** 1

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Success","data":{"id":"f8e2dc60-bd59-4af9-bf15-507f3b6572d6","boughtDate":"2026-10-22T08:02:34.806+00:00","travelDate":"1970-01-02T10:17:36.799+00:00","travelTime":"1970-01-02T10:17:36.799+00:00","accountId":"f8e2dc60-bd59-4af9-bf15-507f3b6572d6","contactsName":"Test","documentType":1,"contactsDocumentNumber":"Test","trainNumber":"K1235","coachNumber":5,"seatClass":2,"seatNumber":"6A","from":"shanghai","to":"taiyuan","status":4,"price":"100"}}
```



---

## ts-cancel-service → ts-user-service

**Endpoint:** `GET http://ts-user-service:12342/api/v1/userservice/users/id/{orderId}`

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
| Expected | `2xx`, `4xx` |
| Status Code Classes Covered | 2xx |
| Status Code Class Coverage | 50% |

| Status Code | Count |
|-------------|-------|
| 200 | 2 |

### Response Field Coverage

Pooled: 0,00% (0 of 1) | Hierarchical: 0,00% | Optional fields only: 0,00%

| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |
|--------|------|-----------|----------------|------------------|------------------------------|
| 200 | `200` | 2 | none | `body` | `status`, `msg`, `data`, `data.userId`, `data.userName`, `data.password`, `data.gender`, `data.documentType`, `data.documentNum`, `data.email` |

### Response Value Partition Coverage

*n/a (no response field was returned with a value)*

### Recorded Bodies

#### Response Bodies

- **Number of Response Bodies:** 2
- **Number of Unique Response Bodies:** 2

**List of unique Response Bodies:**

```json
{"status":1,"msg":"Find User Success","data":{"userId":"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f","userName":"fdse_microservice","password":"111111","gender":1,"documentType":1,"documentNum":"2135488099312X","email":"fdse_microservice@163.com"}}
```

```json
{"status":0,"msg":"No User","data":null}
```



---

