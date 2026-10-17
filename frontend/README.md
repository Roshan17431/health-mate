# Health Mate Frontend

React + Vite frontend for the Health Mate Spring Boot API.

## Run Locally

1. Install dependencies:

   ```bash
   npm install
   ```

2. Configure the backend URL:

   ```bash
   cp .env.example .env.local
   ```

   Set `VITE_API_BASE_URL` to the Spring Boot API URL. For local backend development, keep `http://localhost:8080`.

3. Start the frontend:

   ```bash
   npm run dev
   ```

## Checks

```bash
npm run lint
npm run build
```

## Deployment

On Render, this frontend is deployed as a static site from the repository root `render.yaml`. Set `VITE_API_BASE_URL` to the deployed backend API URL.

## Production deployment

Use the [deployment plan](../docs/deployment/production-plan.md), [runbook](../docs/deployment/runbook.md), and [readiness report](../docs/deployment/production-readiness-report.md). Production validates the explicitly migrated schema, disables SQL seeds, and requires private configuration. Local staging uses synthetic records and separate services; it does not authorize changes to production.
