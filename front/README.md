# Autostrada Auctions React Frontend

This folder contains the React application that replaces the former JSP UI.

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

The React app consumes backend DTO APIs under `/api/**`. Public marketplace,
auth, user dashboard, cart/order, comments, and admin/store-admin screens are
routed in React.
