# Observability

This service uses Spring Boot Actuator, Micrometer, Prometheus, and Grafana.

## Monitoring

The application exposes Prometheus metrics at:

```http
GET /actuator/prometheus
```

Run the local monitoring stack:

```powershell
docker compose -f docker-compose.monitoring.yml up --build
```

Open:

- Application: http://localhost:8085
- Health: http://localhost:8085/actuator/health
- Prometheus: http://localhost:9091
- Grafana: http://localhost:3000

Grafana credentials for local development:

```text
username: admin
password: admin
```

Prometheus is provisioned as the default Grafana data source. The `MySawit Pembayaran`
dashboard is loaded automatically from `monitoring/grafana/dashboards`.

Useful Prometheus queries:

```promql
up{job="mysawit-pembayaran"}
sum(rate(http_server_requests_seconds_count{job="mysawit-pembayaran"}[5m]))
sum(rate(http_server_requests_seconds_count{job="mysawit-pembayaran", status=~"5.."}[5m]))
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job="mysawit-pembayaran"}[5m])))
sum by (area) (jvm_memory_used_bytes{job="mysawit-pembayaran"})
hikaricp_connections_active{job="mysawit-pembayaran"}
```

The local Prometheus config also includes starter alerts for service down, high
HTTP 5xx rate, high p95 latency, and high JVM heap usage.

## Production Notes

Do not expose `/actuator/prometheus` broadly on the public internet. Prefer one of:

- Prometheus runs in the same private network and scrapes the service privately.
- A load balancer or security group allows the endpoint only from Prometheus.
- The endpoint is protected by infrastructure-level auth.

For production Docker image tags, pin explicit Prometheus and Grafana versions after
choosing the versions used by the deployment environment.

## Profiling

Monitoring tells you that something is wrong. Profiling helps find where the time,
CPU, memory, or blocking is spent.

Java Flight Recorder is the safest first profiling tool for this Spring Boot service.
With a local JDK installed, start the app, find its process id, and record a profile:

```powershell
New-Item -ItemType Directory -Force profiling | Out-Null
jcmd
jcmd <pid> JFR.start name=mysawit settings=profile filename=profiling\mysawit-pembayaran.jfr duration=120s
```

For a longer recording that you stop manually:

```powershell
jcmd <pid> JFR.start name=mysawit settings=profile
jcmd <pid> JFR.dump name=mysawit filename=profiling\mysawit-pembayaran.jfr
jcmd <pid> JFR.stop name=mysawit
```

Open the `.jfr` file with Java Mission Control.

If the app is started by Docker or Elastic Beanstalk, you can also use JVM options:

```text
-XX:StartFlightRecording=filename=/tmp/mysawit-pembayaran.jfr,dumponexit=true,settings=profile,duration=120s
```

For deeper CPU investigations, use async-profiler from a controlled environment. Use
JFR first unless you already know you need flame graphs or allocation hot spots.
