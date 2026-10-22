# Tool Demonstration

## TrainTicket
For TrainTicket, we ran four different integration scenarios from the [TrainTicket-with-test-suites](https://github.com/tum-i4/train-ticket-with-test-suites) repository.

## Setup
All services that you want to use the coverage tool on, need to be built on our Hypertrace+OpenTelemetry base image. 
For this, just add the following line to the `Dockerfile` of the service:
```
FROM train-ticket-hypertrace-base:latest
```

## Demonstration Scenarios
Before running any scenarios, you need to set the environment variables to TrainTicket and load the `.env` file. You can do this by running the following command:
```
set -a; source .env; set +a
```

### 1. Search Cheapest Result
The existing test suite
```
./run-coverage.sh $MICROSERVICES_ROOT/ts-route-plan-service \
    $MICROSERVICES_ROOT/ts-route-plan-service/src/test/java/plan/integration/post/SearchCheapestResultTest.java \
    --config TT-CheapestResult \
    --openapi-dir ./tool-demonstration/springfox_open_api \
    --config-file tool-demonstration/configs/TT-CheapestResult.json \
    --force-reanalyze
```

### 2. Search Cheapest Result 2
The new test suite with improved coverage
```
./run-coverage.sh $MICROSERVICES_ROOT/ts-route-plan-service \
    $MICROSERVICES_ROOT/ts-route-plan-service/src/test/java/plan/integration/post/SearchCheapestResultTest2.java \
    --config TT-CheapestResult2 \
    --openapi-dir ./tool-demonstration/springfox_open_api \
    --config-file tool-demonstration/configs/TT-CheapestResult2.json \
    --force-reanalyze
```
### 3. Query for Travel
Query for Travel with enriched API specs with original test suite
```
./run-coverage.sh $MICROSERVICES_ROOT/ts-basic-service \
    $MICROSERVICES_ROOT/ts-basic-service/src/test/java/fdse/microservice/integration/post/QueryForTravelTest.java \
    --config TT-QueryForTravel \
    --openapi-dir ./tool-demonstration/enriched_open_api \
    --config-file tool-demonstration/configs/TT-QueryForTravel.json \
    --force-reanalyze
```

### 4. Query for Travel 
Query for Travel with enriched API specs and new test suite with improved coverage
```
 ./run-coverage.sh $MICROSERVICES_ROOT/ts-basic-service \
    $MICROSERVICES_ROOT/ts-basic-service/src/test/java/fdse/microservice/integration/post/QueryForTravelTest2.java \
    --config TT-QueryForTravel \
    --openapi-dir ./tool-demonstration/enriched_open_api \
    --config-file tool-demonstration/configs/TT-QueryForTravel2.json \
    --force-reanalyze
```

### 5. Preserve
Preserve scenario with unreachable connections
```
./run-coverage.sh $MICROSERVICES_ROOT/ts-preserve-service \
    $MICROSERVICES_ROOT/ts-preserve-service/src/test/java/preserve/integration/post/PreserveTest.java \
    --config TT-Preserve \
    --openapi-dir ./tool-demonstration/springfox_open_api \
    --config-file tool-demonstration/configs/TT-Preserve.json \
    --force-reanalyze
```

### 6. Preserve 2
Preserve scenario with unreachable connections configured to be excluded from analysis (through config-file)
```
./run-coverage.sh $MICROSERVICES_ROOT/ts-preserve-service \
    $MICROSERVICES_ROOT/ts-preserve-service/src/test/java/preserve/integration/post/PreserveTest.java \
    --config TT-Preserve2 \
    --openapi-dir ./tool-demonstration/springfox_open_api \
    --config-file tool-demonstration/configs/TT-Preserve2.json \
    --force-reanalyze
```
