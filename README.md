# MySawit-backend-manage-pembayaran

When `XENDIT_API_KEY` is empty, top up returns a local mock payment URL:

```http
http://localhost:8085/api/pembayaran/wallet/topup/mock-pay/{externalId}
```

Open that URL and click `Mark as paid` to simulate the Xendit callback.

## Observability

Monitoring and profiling setup is documented in [docs/observability.md](docs/observability.md).

Run the local Prometheus + Grafana stack:

```powershell
docker compose -f docker-compose.monitoring.yml up --build
```
