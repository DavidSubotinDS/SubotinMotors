import { createServer } from 'node:net';

for (const port of [18080, 15173, 18081]) {
  await new Promise((resolve, reject) => {
    const server = createServer();
    server.once('error', () => reject(new Error(`E2E port ${port} is still occupied`)));
    server.listen(port, '127.0.0.1', () => server.close(resolve));
  });
}
console.log('All three E2E ports are free.');
