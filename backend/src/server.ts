import { buildApp } from './app.js';
import { createDatabase } from './db.js';
import path from 'path';

const PORT = parseInt(process.env.PORT || '8080', 10);
const HOST = process.env.HOST || '0.0.0.0';
const DB_PATH = process.env.DB_PATH || path.resolve(process.cwd(), 'doclens_server.db');

const db = createDatabase(DB_PATH);
const app = buildApp({ db });

async function start() {
    try {
        await app.listen({ port: PORT, host: HOST });
        console.log(`DocLens Cloud API running on http://${HOST}:${PORT}`);
    } catch (err) {
        app.log.error(err);
        process.exit(1);
    }
}

start();
