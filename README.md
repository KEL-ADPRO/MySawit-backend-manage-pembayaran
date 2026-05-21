# MySawit-backend-manage-pembayaran

When `XENDIT_API_KEY` is empty, top up returns a local mock payment URL:

```http
http://localhost:8085/api/pembayaran/wallet/topup/mock-pay/{externalId}
```

Open that URL and click `Mark as paid` to simulate the Xendit callback.

## Observability

Monitoring and profiling setup is documented in [docs/observability.md](docs/observability.md).

Integration points with other modules are documented in [docs/integration.md](docs/integration.md).

Run the local Prometheus + Grafana stack:

```bash
docker compose -f docker-compose.monitoring.yml up --build -d
```

If your local app is already using ports `8085` and `9090`, run it with alternate
host ports:

```bash
APP_HOST_PORT=18085 GRPC_HOST_PORT=19090 docker compose -f docker-compose.monitoring.yml up --build -d
```
