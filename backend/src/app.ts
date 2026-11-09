import Fastify, { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import cors from '@fastify/cors';
import jwt from '@fastify/jwt';
import crypto from 'crypto';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { DatabaseSync } from 'node:sqlite';
import { createDatabase } from './db.js';
import { processQueuedConversions } from './workers/conversionWorker.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export interface AppOptions {
    db?: DatabaseSync;
    jwtSecret?: string;
    storageDir?: string;
}

export function buildApp(opts: AppOptions = {}): FastifyInstance {
    const fastify = Fastify({
        logger: false
    });

    const db = opts.db || createDatabase();
    const jwtSecret = opts.jwtSecret || 'dev-secret-super-secure-key-12345';
    const storageDir = opts.storageDir || path.resolve(__dirname, '../uploads');

    // Global correlation ID middleware
    fastify.addHook('onRequest', async (req, reply) => {
        const correlationId = (req.headers['x-correlation-id'] as string) || crypto.randomUUID();
        reply.header('x-correlation-id', correlationId);
    });

    fastify.register(cors, { origin: true });
    fastify.register(jwt, { secret: jwtSecret });

    // Authentication Decorator
    fastify.decorate('authenticate', async (request: FastifyRequest, reply: FastifyReply) => {
        try {
            await request.jwtVerify();
        } catch (err) {
            reply.status(401).send({ error: 'Unauthorized', message: 'Authentication required' });
        }
    });

    // ==========================================
    // Auth Routes
    // ==========================================
    fastify.post('/v1/auth/register', async (req, reply) => {
        const { email, password } = req.body as any;
        if (!email || !password) {
            return reply.status(400).send({ error: 'Email and password required' });
        }

        const accountId = crypto.randomUUID();
        const hash = crypto.createHash('sha256').update(password).digest('hex');
        const now = Date.now();

        try {
            db.prepare('INSERT INTO accounts (id, email, password_hash, created_at) VALUES (?, ?, ?, ?)').run(
                accountId, email.toLowerCase(), hash, now
            );
        } catch (e: any) {
            return reply.status(409).send({ error: 'Account already exists' });
        }

        const token = fastify.jwt.sign({ accountId, email: email.toLowerCase() }, { expiresIn: '7d' });
        return reply.status(201).send({ accountId, token });
    });

    fastify.post('/v1/auth/login', async (req, reply) => {
        const { email, password } = req.body as any;
        if (!email || !password) {
            return reply.status(400).send({ error: 'Email and password required' });
        }

        const hash = crypto.createHash('sha256').update(password).digest('hex');
        const account = db.prepare('SELECT * FROM accounts WHERE email = ? AND password_hash = ? AND status = ?').get(
            email.toLowerCase(), hash, 'ACTIVE'
        ) as any;

        if (!account) {
            return reply.status(401).send({ error: 'Invalid email or password' });
        }

        const token = fastify.jwt.sign({ accountId: account.id, email: account.email }, { expiresIn: '7d' });
        return { accountId: account.id, token };
    });

    fastify.post('/v1/account/deletion', { onRequest: [(fastify as any).authenticate] }, async (req, reply) => {
        const user = (req as any).user;
        db.prepare('UPDATE accounts SET status = ? WHERE id = ?').run('DELETED', user.accountId);
        return { success: true, message: 'Account marked for deletion' };
    });

    // ==========================================
    // Upload Routes (Phase 10)
    // ==========================================
    fastify.post('/v1/uploads', { onRequest: [(fastify as any).authenticate] }, async (req, reply) => {
        const user = (req as any).user;
        const { byteSize, checksum, mediaType } = req.body as any;

        if (!byteSize || !checksum || !mediaType) {
            return reply.status(400).send({ error: 'byteSize, checksum, and mediaType are required' });
        }

        const uploadId = crypto.randomUUID();
        const uploadUrl = `https://storage.doclens.local/upload/${uploadId}`;
        const now = Date.now();
        const expiresAt = now + 3600_000; // 1 hour

        db.prepare(`
            INSERT INTO upload_sessions (id, account_id, byte_size, checksum, media_type, upload_url, created_at, expires_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        `).run(uploadId, user.accountId, byteSize, checksum, mediaType, uploadUrl, now, expiresAt);

        return reply.status(201).send({ uploadId, uploadUrl, expiresAt: new Date(expiresAt).toISOString() });
    });

    fastify.post('/v1/uploads/:id/complete', { onRequest: [(fastify as any).authenticate] }, async (req, reply) => {
        const user = (req as any).user;
        const { id } = req.params as any;

        const session = db.prepare('SELECT * FROM upload_sessions WHERE id = ? AND account_id = ?').get(id, user.accountId) as any;
        if (!session) {
            return reply.status(404).send({ error: 'Upload session not found' });
        }

        if (session.status === 'COMPLETED') {
            return { assetId: id, checksum: session.checksum, byteSize: session.byte_size };
        }

        const now = Date.now();
        db.prepare('UPDATE upload_sessions SET status = ? WHERE id = ?').run('COMPLETED', id);
        db.prepare(`
            INSERT INTO assets (id, account_id, checksum, byte_size, media_type, storage_path, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        `).run(id, user.accountId, session.checksum, session.byte_size, session.media_type, `assets/${id}/original.jpg`, now);

        return { assetId: id, checksum: session.checksum, byteSize: session.byte_size };
    });

    // ==========================================
    // Document Routes (Phase 10 & 11)
    // ==========================================
    fastify.post('/v1/documents', { onRequest: [(fastify as any).authenticate] }, async (req, reply) => {
        const user = (req as any).user;
        const idempotencyKey = req.headers['idempotency-key'] as string;
        const { title, folderId, pages } = req.body as any;

        if (!title || !Array.isArray(pages)) {
            return reply.status(400).send({ error: 'title and pages array are required' });
        }

        // Idempotency check
        if (idempotencyKey) {
            const existing = db.prepare('SELECT * FROM documents WHERE idempotency_key = ? AND account_id = ?').get(idempotencyKey, user.accountId) as any;
            if (existing) {
                const existingPages = db.prepare('SELECT * FROM pages WHERE document_id = ? ORDER BY position ASC').all(existing.id);
                return reply.status(200).send({ ...existing, pages: existingPages, idempotentReplay: true });
            }
        }

        const docId = crypto.randomUUID();
        const now = Date.now();

        db.exec('BEGIN');
        try {
            db.prepare(`
                INSERT INTO documents (id, account_id, title, folder_id, revision, created_at, updated_at, idempotency_key)
                VALUES (?, ?, ?, ?, 1, ?, ?, ?)
            `).run(docId, user.accountId, title, folderId || null, now, now, idempotencyKey || null);

            for (const p of pages) {
                const pageId = p.id || crypto.randomUUID();
                db.prepare(`
                    INSERT INTO pages (id, document_id, position, asset_id, recipe_json, revision, created_at)
                    VALUES (?, ?, ?, ?, ?, 1, ?)
                `).run(pageId, docId, p.position, p.assetId, JSON.stringify(p.recipe || {}), now);
            }

            db.prepare(`
                INSERT INTO sync_changes (account_id, document_id, revision, change_type, payload_json, timestamp)
                VALUES (?, ?, 1, 'CREATED', ?, ?)
            `).run(user.accountId, docId, JSON.stringify({ title, pageCount: pages.length }), now);

            db.exec('COMMIT');
        } catch (err) {
            db.exec('ROLLBACK');
            throw err;
        }

        const createdPages = db.prepare('SELECT * FROM pages WHERE document_id = ? ORDER BY position ASC').all(docId);
        return reply.status(201).send({
            id: docId,
            title,
            folderId: folderId || null,
            revision: 1,
            pageCount: pages.length,
            pages: createdPages,
            createdAt: now,
            updatedAt: now
        });
    });

    fastify.get('/v1/documents/:id', { onRequest: [(fastify as any).authenticate] }, async (req, reply) => {
        const user = (req as any).user;
        const { id } = req.params as any;

        const doc = db.prepare('SELECT * FROM documents WHERE id = ? AND account_id = ? AND is_deleted = 0').get(id, user.accountId) as any;
        if (!doc) {
            return reply.status(404).send({ error: 'Document not found or access denied' });
        }

        const pages = db.prepare('SELECT * FROM pages WHERE document_id = ? ORDER BY position ASC').all(id);
        return { ...doc, pages };
    });

    fastify.patch('/v1/documents/:id', { onRequest: [(fastify as any).authenticate] }, async (req, reply) => {
        const user = (req as any).user;
        const { id } = req.params as any;
        const { expectedBaseRevision, title, folderId } = req.body as any;

        const doc = db.prepare('SELECT * FROM documents WHERE id = ? AND account_id = ? AND is_deleted = 0').get(id, user.accountId) as any;
        if (!doc) {
            return reply.status(404).send({ error: 'Document not found' });
        }

        if (expectedBaseRevision !== undefined && doc.revision !== expectedBaseRevision) {
            return reply.status(409).send({
                error: 'Revision conflict',
                currentRevision: doc.revision,
                message: `Server revision is ${doc.revision}, but client expected ${expectedBaseRevision}`
            });
        }

        const newRevision = doc.revision + 1;
        const now = Date.now();
        const updatedTitle = title !== undefined ? title : doc.title;
        const updatedFolder = folderId !== undefined ? folderId : doc.folder_id;

        db.prepare(`
            UPDATE documents SET title = ?, folder_id = ?, revision = ?, updated_at = ? WHERE id = ?
        `).run(updatedTitle, updatedFolder, newRevision, now, id);

        db.prepare(`
            INSERT INTO sync_changes (account_id, document_id, revision, change_type, payload_json, timestamp)
            VALUES (?, ?, ?, 'UPDATED', ?, ?)
        `).run(user.accountId, id, newRevision, JSON.stringify({ title: updatedTitle }), now);

        return { id, title: updatedTitle, folderId: updatedFolder, revision: newRevision, updatedAt: now };
    });

    fastify.delete('/v1/documents/:id', { onRequest: [(fastify as any).authenticate] }, async (req, reply) => {
        const user = (req as any).user;
        const { id } = req.params as any;

        const doc = db.prepare('SELECT * FROM documents WHERE id = ? AND account_id = ?').get(id, user.accountId) as any;
        if (!doc) {
            return reply.status(404).send({ error: 'Document not found' });
        }

        const now = Date.now();
        db.prepare('UPDATE documents SET is_deleted = 1, updated_at = ? WHERE id = ?').run(now, id);

        db.prepare(`
            INSERT INTO sync_changes (account_id, document_id, revision, change_type, payload_json, timestamp)
            VALUES (?, ?, ?, 'DELETED', '{}', ?)
        `).run(user.accountId, id, doc.revision + 1, now);

        return { success: true, deletedAt: now };
    });

    // ==========================================
    // Sync Changes Cursor (Phase 11)
    // ==========================================
    fastify.get('/v1/changes', { onRequest: [(fastify as any).authenticate] }, async (req, reply) => {
        const user = (req as any).user;
        const cursor = parseInt((req.query as any).cursor || '0', 10);

        const changes = db.prepare(`
            SELECT * FROM sync_changes WHERE account_id = ? AND cursor > ? ORDER BY cursor ASC LIMIT 100
        `).all(user.accountId, cursor) as any[];

        const nextCursor = changes.length > 0 ? changes[changes.length - 1].cursor : cursor;
        return {
            changes: changes.map(c => ({
                cursor: c.cursor,
                documentId: c.document_id,
                revision: c.revision,
                changeType: c.change_type,
                payload: JSON.parse(c.payload_json),
                timestamp: c.timestamp
            })),
            nextCursor,
            hasMore: changes.length === 100
        };
    });

    // ==========================================
    // Shares & Web Access (Phase 12)
    // ==========================================
    fastify.post('/v1/documents/:id/shares', { onRequest: [(fastify as any).authenticate] }, async (req, reply) => {
        const user = (req as any).user;
        const { id } = req.params as any;
        const { role = 'VIEWER', expiresInHours = 72 } = req.body as any;

        const doc = db.prepare('SELECT * FROM documents WHERE id = ? AND account_id = ? AND is_deleted = 0').get(id, user.accountId) as any;
        if (!doc) {
            return reply.status(404).send({ error: 'Document not found' });
        }

        const shareId = crypto.randomUUID();
        const token = crypto.randomBytes(24).toString('base64url');
        const now = Date.now();
        const expiresAt = expiresInHours ? now + (expiresInHours * 3600_000) : null;

        db.prepare(`
            INSERT INTO shares (id, document_id, account_id, token, role, created_at, expires_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        `).run(shareId, id, user.accountId, token, role, now, expiresAt);

        return reply.status(201).send({
            shareId,
            token,
            role,
            shareUrl: `https://scanner.doclens.local/share/${token}`,
            expiresAt: expiresAt ? new Date(expiresAt).toISOString() : null
        });
    });

    fastify.delete('/v1/shares/:id', { onRequest: [(fastify as any).authenticate] }, async (req, reply) => {
        const user = (req as any).user;
        const { id } = req.params as any;

        const result = db.prepare('UPDATE shares SET revoked = 1 WHERE id = ? AND account_id = ?').run(id, user.accountId);
        if (result.changes === 0) {
            return reply.status(404).send({ error: 'Share link not found or already revoked' });
        }
        return { success: true };
    });

    // Public endpoint for Web Companion resolving share tokens
    fastify.get('/v1/shared/:token', async (req, reply) => {
        const { token } = req.params as any;

        const share = db.prepare('SELECT * FROM shares WHERE token = ? AND revoked = 0').get(token) as any;
        if (!share) {
            return reply.status(404).send({ error: 'Invalid or revoked share token' });
        }

        if (share.expires_at && Date.now() > share.expires_at) {
            return reply.status(410).send({ error: 'Share link has expired' });
        }

        const doc = db.prepare('SELECT id, title, revision, updated_at FROM documents WHERE id = ? AND is_deleted = 0').get(share.document_id) as any;
        if (!doc) {
            return reply.status(404).send({ error: 'Shared document was removed' });
        }

        const pages = db.prepare('SELECT id, position, asset_id, recipe_json FROM pages WHERE document_id = ? ORDER BY position ASC').all(doc.id);
        const comments = db.prepare('SELECT * FROM comments WHERE document_id = ? ORDER BY created_at ASC').all(doc.id);

        return {
            document: doc,
            role: share.role,
            pages,
            comments
        };
    });

    // ==========================================
    // Anchored Comments (Phase 12)
    // ==========================================
    fastify.post('/v1/documents/:id/comments', async (req, reply) => {
        const { id } = req.params as any;
        const { pageId, authorName, text, anchorBox } = req.body as any;

        if (!pageId || !authorName || !text) {
            return reply.status(400).send({ error: 'pageId, authorName, and text are required' });
        }

        const commentId = crypto.randomUUID();
        const now = Date.now();

        db.prepare(`
            INSERT INTO comments (id, document_id, page_id, author_name, text, anchor_box, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        `).run(commentId, id, pageId, authorName, text, anchorBox || null, now);

        return reply.status(201).send({
            id: commentId,
            documentId: id,
            pageId,
            authorName,
            text,
            anchorBox: anchorBox || null,
            createdAt: now
        });
    });

    // ==========================================
    // Conversion Jobs (Phase 13)
    // ==========================================
    fastify.post('/v1/conversions', { onRequest: [(fastify as any).authenticate] }, async (req, reply) => {
        const user = (req as any).user;
        const { inputType, outputType, sourceUrl } = req.body as any;

        if (!inputType || !outputType) {
            return reply.status(400).send({ error: 'inputType and outputType are required' });
        }

        const jobId = crypto.randomUUID();
        const now = Date.now();

        db.prepare(`
            INSERT INTO conversion_jobs (id, account_id, input_type, output_type, source_url, status, created_at)
            VALUES (?, ?, ?, ?, ?, 'QUEUED', ?)
        `).run(jobId, user.accountId, inputType, outputType, sourceUrl || null, now);

        // Process queued jobs in background worker
        processQueuedConversions(db, storageDir);

        return reply.status(202).send({
            jobId,
            status: 'QUEUED',
            estimatedSeconds: 3,
            message: 'Conversion job queued in isolated conversion worker'
        });
    });

    fastify.get('/v1/jobs/:id', { onRequest: [(fastify as any).authenticate] }, async (req, reply) => {
        const user = (req as any).user;
        const { id } = req.params as any;

        const job = db.prepare('SELECT * FROM conversion_jobs WHERE id = ? AND account_id = ?').get(id, user.accountId) as any;
        if (!job) {
            return reply.status(404).send({ error: 'Job not found' });
        }

        return job;
    });

    fastify.get('/v1/conversions/:id/download', { onRequest: [(fastify as any).authenticate] }, async (req, reply) => {
        const user = (req as any).user;
        const { id } = req.params as any;

        const job = db.prepare('SELECT * FROM conversion_jobs WHERE id = ? AND account_id = ?').get(id, user.accountId) as any;
        if (!job) {
            return reply.status(404).send({ error: 'Conversion job not found' });
        }

        if (job.status !== 'COMPLETED') {
            return reply.status(400).send({ error: `Conversion is not completed (current status: ${job.status})` });
        }

        const filePath = path.join(storageDir, 'conversions', `${job.id}.pdf`);
        if (!fs.existsSync(filePath)) {
            return reply.status(404).send({ error: 'Converted file not found on server' });
        }

        const fileStream = fs.createReadStream(filePath);
        reply.header('Content-Type', 'application/pdf');
        reply.header('Content-Disposition', `attachment; filename="converted_${job.id}.pdf"`);
        return reply.send(fileStream);
    });

    // ==========================================
    // Entitlements (Phase 14)
    // ==========================================
    fastify.get('/v1/entitlements', { onRequest: [(fastify as any).authenticate] }, async (req, reply) => {
        const user = (req as any).user;
        const account = db.prepare('SELECT plan FROM accounts WHERE id = ?').get(user.accountId) as any;

        const plan = account?.plan || 'FREE';
        return {
            plan,
            features: {
                ocrUnlimited: true,
                cloudStorageLimitBytes: plan === 'PRO' ? 50_000_000_000 : 2_000_000_000,
                serverConversionQuotaPerMonth: plan === 'PRO' ? 200 : 10,
                teamSharing: plan === 'PRO'
            },
            status: 'ACTIVE'
        };
    });

    // ==========================================
    // Web Companion Static Client (WEB-01)
    // ==========================================
    const webDir = path.resolve(__dirname, '../../web');
    fastify.get('/web', async (req, reply) => {
        const filePath = path.join(webDir, 'index.html');
        if (fs.existsSync(filePath)) {
            return reply.type('text/html').send(fs.readFileSync(filePath, 'utf-8'));
        }
        return reply.status(404).send('Web companion not found');
    });

    fastify.get('/web/styles.css', async (req, reply) => {
        const filePath = path.join(webDir, 'styles.css');
        if (fs.existsSync(filePath)) {
            return reply.type('text/css').send(fs.readFileSync(filePath, 'utf-8'));
        }
        return reply.status(404).send('Not found');
    });

    fastify.get('/web/app.js', async (req, reply) => {
        const filePath = path.join(webDir, 'app.js');
        if (fs.existsSync(filePath)) {
            return reply.type('application/javascript').send(fs.readFileSync(filePath, 'utf-8'));
        }
        return reply.status(404).send('Not found');
    });

    fastify.get('/share/:token', async (req, reply) => {
        const filePath = path.join(webDir, 'index.html');
        if (fs.existsSync(filePath)) {
            return reply.type('text/html').send(fs.readFileSync(filePath, 'utf-8'));
        }
        return reply.status(404).send('Web companion not found');
    });

    return fastify;
}
