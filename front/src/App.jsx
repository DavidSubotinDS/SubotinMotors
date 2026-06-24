import { RouterProvider } from 'react-router-dom';

import { createAppRouter } from './router.jsx';

export default function App() {
  return <RouterProvider router={createAppRouter()} />;
}
