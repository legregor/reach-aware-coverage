jaeger-run:
	docker compose up -d jaeger

coverage-build:
	cd coverage && mvn clean install -DskipTests

# usage: make coverage-run ARGS="arg1 arg2 arg3"
coverage-run:
	cd coverage && java -jar target/coverage-1.0.0.jar $(ARGS)

hypertrace-image-build:
	cd tracing/hypertrace && docker build -f train-ticket-hypertrace-base.Dockerfile -t train-ticket-hypertrace-base .