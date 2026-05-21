# Observability

This service exposes Spring Boot Actuator metrics through Micrometer, Prometheus
scrapes those metrics, and Grafana visualizes them through a provisioned dashboard.

## What Is Included

- Spring Boot Actuator endpoint: `/actuator/prometheus`
- Micrometer Prometheus registry dependency
- Docker Compose stack for PostgreSQL, the app, Prometheus, and Grafana
- Prometheus scrape config and starter alert rules
- Grafana Prometheus data source provisioning
- Grafana `MySawit Pembayaran` dashboard provisioning

## Local Architecture

```text
Browser
  |-- http://localhost:3000  -> Grafana
  |-- http://localhost:9091  -> Prometheus UI
  |-- http://localhost:8085  -> MySawit pembayaran app

Prometheus container
  |-- scrapes http://app:8085/actuator/prometheus inside the compose network

Grafana container
  |-- reads Prometheus from http://prometheus:9090 inside the compose network
```

The host port can change, but the internal app target must stay `app:8085`
because Prometheus talks to the app through the Docker Compose network.

## Prerequisites

1. Docker Desktop is running.
2. These commands work from a terminal:

```bash
docker ps
docker compose version
```

3. Run all commands from the repository root:

```bash
cd /Users/daffasyafitra/Downloads/mysawit/MySawit-backend-manage-pembayaran
```

## Start The Stack

Run the full monitoring stack:

```bash
docker compose -f docker-compose.monitoring.yml up --build -d
```

Open:

- Application: http://localhost:8085
- Health: http://localhost:8085/actuator/health
- Metrics: http://localhost:8085/actuator/prometheus
- Prometheus: http://localhost:9091
- Grafana: http://localhost:3000

Grafana local login:

```text
username: admin
password: admin
```

## If Ports Are Busy

If the app is already running locally from the IDE or Gradle, ports `8085` and
`9090` may be busy. Do not kill it unless you really want to. Run the monitoring
stack with alternate host ports instead:

```bash
APP_HOST_PORT=18085 GRPC_HOST_PORT=19090 docker compose -f docker-compose.monitoring.yml up --build -d
```

Open these URLs for the alternate-port run:

- Application: http://localhost:18085
- Health: http://localhost:18085/actuator/health
- Metrics: http://localhost:18085/actuator/prometheus
- Prometheus: http://localhost:9091
- Grafana: http://localhost:3000

Prometheus still scrapes `app:8085` internally, so no Prometheus config change is
needed when only the host port changes.

You can also override all exposed host ports:

```bash
POSTGRES_HOST_PORT=15432 \
APP_HOST_PORT=18085 \
GRPC_HOST_PORT=19090 \
PROMETHEUS_HOST_PORT=19091 \
GRAFANA_HOST_PORT=13000 \
docker compose -f docker-compose.monitoring.yml up --build -d
```

## Verify Prometheus

Check the containers:

```bash
docker compose -f docker-compose.monitoring.yml ps
```

Check the app metrics endpoint:

```bash
curl -s http://localhost:8085/actuator/prometheus | head
```

If you started with `APP_HOST_PORT=18085`, use:

```bash
curl -s http://localhost:18085/actuator/prometheus | head
```

Check Prometheus target status:

1. Open http://localhost:9091/targets.
2. Find the `mysawit-pembayaran` job.
3. Confirm the target state is `UP`.

Useful Prometheus queries:

```promql
up{job="mysawit-pembayaran"}
sum(rate(http_server_requests_seconds_count{job="mysawit-pembayaran"}[5m]))
sum(rate(http_server_requests_seconds_count{job="mysawit-pembayaran", status=~"5.."}[5m]))
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job="mysawit-pembayaran"}[5m])))
sum by (area) (jvm_memory_used_bytes{job="mysawit-pembayaran"})
hikaricp_connections_active{job="mysawit-pembayaran"}
process_cpu_usage{job="mysawit-pembayaran"}
jvm_threads_live_threads{job="mysawit-pembayaran"}
```

## Verify Grafana

1. Open http://localhost:3000.
2. Login with `admin` / `admin`.
3. Go to `Dashboards`.
4. Open folder `MySawit`.
5. Open dashboard `MySawit Pembayaran`.

The data source is provisioned from:

```text
monitoring/grafana/provisioning/datasources/prometheus.yml
```

The dashboard is provisioned from:

```text
monitoring/grafana/dashboards/mysawit-pembayaran.json
```

## Generate Traffic

Dashboard panels are easier to read after the app receives a few requests:

```bash
curl -i http://localhost:8085/actuator/health
curl -i http://localhost:8085/api/pembayaran/wallet/topup/mock-pay/example
```

If using alternate host port:

```bash
curl -i http://localhost:18085/actuator/health
curl -i http://localhost:18085/api/pembayaran/wallet/topup/mock-pay/example
```

Some protected API routes intentionally return `401` without auth headers. Those
responses still appear in HTTP request metrics.

## Alerts

Local starter alert rules live in:

```text
monitoring/prometheus/alert-rules.yml
```

Included alerts:

- Service down
- High HTTP 5xx rate
- High HTTP p95 latency
- High JVM heap usage

Open local alert state at:

```text
http://localhost:9091/alerts
```

This local setup does not include Alertmanager. For production notifications,
add Alertmanager and route alerts to Slack, email, PagerDuty, or the team's
preferred incident channel.

## Load Test With JMeter

JMeter is useful for generating traffic while Prometheus and Grafana show how the
service behaves under load.

Install JMeter:

```bash
brew install jmeter
```

Run the reusable load test plan:

```bash
mkdir -p profiling/jmeter/baseline
jmeter -n \
  -t monitoring/jmeter/mysawit-load-test.jmx \
  -Jhost=localhost \
  -Jport=18085 \
  -Jthreads=20 \
  -Jramp=20 \
  -Jduration=60 \
  -Jthink_ms=100 \
  -l profiling/jmeter/baseline/results.jtl \
  -e \
  -o profiling/jmeter/baseline/report
```

Open the report:

```bash
open profiling/jmeter/baseline/report/index.html
```

While the test is running, watch:

- Grafana: http://localhost:3000
- Prometheus target status: http://localhost:9091/targets
- Prometheus alerts: http://localhost:9091/alerts

Useful Prometheus queries during a JMeter run:

```promql
sum(rate(http_server_requests_seconds_count{job="mysawit-pembayaran"}[1m]))
sum(rate(http_server_requests_seconds_count{job="mysawit-pembayaran", status=~"5.."}[1m]))
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job="mysawit-pembayaran"}[1m])))
process_cpu_usage{job="mysawit-pembayaran"}
hikaricp_connections_active{job="mysawit-pembayaran"}
```

Start small, then raise `threads` and `duration`. For example:

```bash
jmeter -n -t monitoring/jmeter/mysawit-load-test.jmx -Jport=18085 -Jthreads=50 -Jramp=30 -Jduration=180 -l profiling/jmeter/50-users.jtl -e -o profiling/jmeter/50-users-report
```

## Stop The Stack

Stop containers but keep volumes:

```bash
docker compose -f docker-compose.monitoring.yml down
```

Stop containers and delete local monitoring/PostgreSQL volumes:

```bash
docker compose -f docker-compose.monitoring.yml down -v
```

## Troubleshooting

Port already in use:

```bash
lsof -nP -iTCP:8085 -sTCP:LISTEN
lsof -nP -iTCP:9090 -sTCP:LISTEN
```

Use alternate ports instead of stopping local Java:

```bash
APP_HOST_PORT=18085 GRPC_HOST_PORT=19090 docker compose -f docker-compose.monitoring.yml up --build -d
```

Prometheus target is `DOWN`:

```bash
docker compose -f docker-compose.monitoring.yml logs app
docker compose -f docker-compose.monitoring.yml logs prometheus
```

Then confirm the app exposes metrics from inside the compose network:

```bash
docker compose -f docker-compose.monitoring.yml exec prometheus wget -qO- http://app:8085/actuator/prometheus | head
```

Grafana dashboard is missing:

```bash
docker compose -f docker-compose.monitoring.yml logs grafana
```

Then check these files exist:

```bash
ls monitoring/grafana/provisioning/datasources/prometheus.yml
ls monitoring/grafana/provisioning/dashboards/dashboards.yml
ls monitoring/grafana/dashboards/mysawit-pembayaran.json
```

Docker build fails on gRPC/protobuf tooling:

- The Docker build stage must use a Debian/glibc-based JDK image.
- Alpine can fail because `protoc-gen-grpc-java` native binaries may not run
  correctly there on ARM machines.

## Production Notes

Do not expose `/actuator/prometheus` broadly on the public internet. Prefer one
of these patterns:

- Prometheus runs in the same private network and scrapes the service privately.
- A load balancer, firewall, or security group allows the endpoint only from
  Prometheus.
- The endpoint is protected by infrastructure-level authentication.

Use pinned image versions in production instead of `latest`, for example:

```yaml
prom/prometheus:v3.x.x
grafana/grafana:12.x.x
postgres:16-alpine
```

Recommended production next steps:

- Add Alertmanager for notifications.
- Add persistent volume backup policy for Grafana if dashboards are edited in UI.
- Add service-level dashboards for business metrics such as top-up count, failed
  payment callbacks, approved payroll count, and Xendit API failure rate.
- Consider separating management endpoints onto a private management port if the
  service is exposed publicly.

## Profiling

Monitoring tells you that something is wrong. Profiling helps find where CPU,
memory, latency, or blocking time is spent.

Java Flight Recorder is the safest first profiling tool for this Spring Boot
service. With a local JDK installed, start the app, find its process id, and
record a profile:

```bash
mkdir -p profiling
jcmd
jcmd <pid> JFR.start name=mysawit settings=profile filename=profiling/mysawit-pembayaran.jfr duration=120s
```

For a longer recording that you stop manually:

```bash
jcmd <pid> JFR.start name=mysawit settings=profile
jcmd <pid> JFR.dump name=mysawit filename=profiling/mysawit-pembayaran.jfr
jcmd <pid> JFR.stop name=mysawit
```

Open the `.jfr` file with Java Mission Control.

If the app is started by Docker or Elastic Beanstalk, you can also use JVM
options:

```text
-XX:StartFlightRecording=filename=/tmp/mysawit-pembayaran.jfr,dumponexit=true,settings=profile,duration=120s
```

Use JFR first unless you already know you need flame graphs or allocation hot
spots from async-profiler.
