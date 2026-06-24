# Autostrada Auctions React Frontend

This folder contains the React application that will replace the JSP UI over time.

## Local development

```powershell
npm.cmd install
npm.cmd run dev
```

The Vite dev server runs on <http://localhost:5173>.

Set the backend URL with:

```powershell
$env:VITE_API_BASE_URL = "http://localhost:8080"
```

## Build and test

```powershell
npm.cmd run build
npm.cmd run test
```

The current React app consumes the backend API under `/api/public/*` and keeps links to legacy JSP flows where full React replacements have not been implemented yet.
