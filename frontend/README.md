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
