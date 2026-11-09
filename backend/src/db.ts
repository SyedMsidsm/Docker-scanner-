import { DatabaseSync } from 'node:sqlite';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export function createDatabase(dbPath: string = ':memory:'): DatabaseSync {
    const db = new DatabaseSync(dbPath);
    db.exec('PRAGMA foreign_keys = ON;');

    const migrationFile = path.resolve(__dirname, '../migrations/001_initial_schema.sql');
    if (fs.existsSync(migrationFile)) {
        const schema = fs.readFileSync(migrationFile, 'utf-8');
        db.exec(schema);
    } else {
        const distMigration = path.resolve(__dirname, '../../migrations/001_initial_schema.sql');
        if (fs.existsSync(distMigration)) {
            const schema = fs.readFileSync(distMigration, 'utf-8');
            db.exec(schema);
        }
    }

    return db;
}
