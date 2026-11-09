import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { buildApp } from '../src/app.js';
import { createDatabase } from '../src/db.js';

describe('DocLens Cloud API & Sync Test Suite', () => {
    let app: any;
    let aliceToken: string;
    let aliceId: string;
    let bobToken: string;
    let bobId: string;

    before(async () => {
        const db = createDatabase(':memory:');
        app = buildApp({ db });
        await app.ready();
    });

    after(async () => {
        await app.close();
    });

    test('1. Register and Login Alice', async () => {
        const res = await app.inject({
            method: 'POST',
            url: '/v1/auth/register',
            payload: { email: 'alice@example.com', password: 'Password123!' }
        });

        assert.equal(res.statusCode, 201);
        const body = JSON.parse(res.body);
        assert.ok(body.token);
        assert.ok(body.accountId);
        aliceToken = body.token;
        aliceId = body.accountId;

        // Login
        const loginRes = await app.inject({
            method: 'POST',
            url: '/v1/auth/login',
            payload: { email: 'alice@example.com', password: 'Password123!' }
        });
        assert.equal(loginRes.statusCode, 200);
        assert.ok(JSON.parse(loginRes.body).token);

        // Reject Duplicate
        const dupRes = await app.inject({
            method: 'POST',
            url: '/v1/auth/register',
            payload: { email: 'alice@example.com', password: 'Password123!' }
        });
        assert.equal(dupRes.statusCode, 409);
    });

    test('2. Register Bob for cross-account isolation test', async () => {
        const res = await app.inject({
            method: 'POST',
            url: '/v1/auth/register',
            payload: { email: 'bob@example.com', password: 'BobPassword999!' }
        });
        assert.equal(res.statusCode, 201);
        const body = JSON.parse(res.body);
        bobToken = body.token;
        bobId = body.accountId;
    });

    let uploadedAssetId: string;

    test('3. Upload Session and Asset Finalization', async () => {
        const sessionRes = await app.inject({
            method: 'POST',
            url: '/v1/uploads',
            headers: { authorization: `Bearer ${aliceToken}` },
            payload: {
                byteSize: 1048576,
                checksum: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
                mediaType: 'image/jpeg'
            }
        });

        assert.equal(sessionRes.statusCode, 201);
        const sessionBody = JSON.parse(sessionRes.body);
        assert.ok(sessionBody.uploadId);
        assert.ok(sessionBody.uploadUrl);
        uploadedAssetId = sessionBody.uploadId;

        // Complete upload
        const completeRes = await app.inject({
            method: 'POST',
            url: `/v1/uploads/${uploadedAssetId}/complete`,
            headers: { authorization: `Bearer ${aliceToken}` }
        });
        assert.equal(completeRes.statusCode, 200);
        assert.equal(JSON.parse(completeRes.body).assetId, uploadedAssetId);
    });

    let documentId: string;

    test('4. Idempotent Document Creation', async () => {
        const idempotencyKey = 'req-key-alice-doc-001';
        const docPayload = {
            title: 'Alice Tax Return 2026',
            pages: [
                { id: 'page-1', position: 0, assetId: uploadedAssetId, recipe: { rotation: 0 } },
                { id: 'page-2', position: 1, assetId: uploadedAssetId, recipe: { rotation: 90 } }
            ]
        };

        // First creation
        const res1 = await app.inject({
            method: 'POST',
            url: '/v1/documents',
            headers: {
                authorization: `Bearer ${aliceToken}`,
                'idempotency-key': idempotencyKey
            },
            payload: docPayload
        });

        assert.equal(res1.statusCode, 201);
        const body1 = JSON.parse(res1.body);
        assert.ok(body1.id);
        assert.equal(body1.title, 'Alice Tax Return 2026');
        assert.equal(body1.revision, 1);
        assert.equal(body1.pages.length, 2);
        documentId = body1.id;

        // Second creation with identical idempotency key -> Must return same document
        const res2 = await app.inject({
            method: 'POST',
            url: '/v1/documents',
            headers: {
                authorization: `Bearer ${aliceToken}`,
                'idempotency-key': idempotencyKey
            },
            payload: docPayload
        });

        assert.equal(res2.statusCode, 200);
        const body2 = JSON.parse(res2.body);
        assert.equal(body2.id, documentId);
        assert.equal(body2.idempotentReplay, true);
    });

    test('5. Cross-Account Access Denial (Multi-Tenant Isolation)', async () => {
        // Bob attempts to read Alice's document -> must be denied (404)
        const getRes = await app.inject({
            method: 'GET',
            url: `/v1/documents/${documentId}`,
            headers: { authorization: `Bearer ${bobToken}` }
        });
        assert.equal(getRes.statusCode, 404);

        // Bob attempts to update Alice's document -> must be denied (404)
        const patchRes = await app.inject({
            method: 'PATCH',
            url: `/v1/documents/${documentId}`,
            headers: { authorization: `Bearer ${bobToken}` },
            payload: { title: 'Hacked by Bob' }
        });
        assert.equal(patchRes.statusCode, 404);

        // Bob attempts to delete Alice's document -> must be denied (404)
        const delRes = await app.inject({
            method: 'DELETE',
            url: `/v1/documents/${documentId}`,
            headers: { authorization: `Bearer ${bobToken}` }
        });
        assert.equal(delRes.statusCode, 404);
    });

    test('6. Revision Optimistic Locking and Sync Changes', async () => {
        // Alice updates with expected base revision 1 -> should succeed, revision becomes 2
        const patchRes = await app.inject({
            method: 'PATCH',
            url: `/v1/documents/${documentId}`,
            headers: { authorization: `Bearer ${aliceToken}` },
            payload: {
                expectedBaseRevision: 1,
                title: 'Alice Tax Return 2026 - Revised'
            }
        });
        assert.equal(patchRes.statusCode, 200);
        assert.equal(JSON.parse(patchRes.body).revision, 2);

        // Alice updates with stale base revision 1 -> must return 409 Conflict
        const conflictRes = await app.inject({
            method: 'PATCH',
            url: `/v1/documents/${documentId}`,
            headers: { authorization: `Bearer ${aliceToken}` },
            payload: {
                expectedBaseRevision: 1,
                title: 'Alice Tax Return 2026 - Conflict'
            }
        });
        assert.equal(conflictRes.statusCode, 409);
        assert.equal(JSON.parse(conflictRes.body).currentRevision, 2);

        // Check incremental change cursor
        const syncRes = await app.inject({
            method: 'GET',
            url: '/v1/changes?cursor=0',
            headers: { authorization: `Bearer ${aliceToken}` }
        });
        assert.equal(syncRes.statusCode, 200);
        const syncBody = JSON.parse(syncRes.body);
        assert.ok(syncBody.changes.length >= 2);
        assert.equal(syncBody.changes[0].changeType, 'CREATED');
        assert.equal(syncBody.changes[1].changeType, 'UPDATED');
    });

    let shareToken: string;

    test('7. Scoped Shares and Web Companion Resolution', async () => {
        // Alice creates a share link
        const shareRes = await app.inject({
            method: 'POST',
            url: `/v1/documents/${documentId}/shares`,
            headers: { authorization: `Bearer ${aliceToken}` },
            payload: { role: 'VIEWER', expiresInHours: 24 }
        });
        assert.equal(shareRes.statusCode, 201);
        const shareBody = JSON.parse(shareRes.body);
        assert.ok(shareBody.token);
        assert.ok(shareBody.shareUrl);
        shareToken = shareBody.token;

        // Public resolution for web companion (No auth header)
        const resolveRes = await app.inject({
            method: 'GET',
            url: `/v1/shared/${shareToken}`
        });
        assert.equal(resolveRes.statusCode, 200);
        const resolveBody = JSON.parse(resolveRes.body);
        assert.equal(resolveBody.document.id, documentId);
        assert.equal(resolveBody.pages.length, 2);
        assert.equal(resolveBody.role, 'VIEWER');

        // Revoke share
        const revokeRes = await app.inject({
            method: 'DELETE',
            url: `/v1/shares/${shareBody.shareId}`,
            headers: { authorization: `Bearer ${aliceToken}` }
        });
        assert.equal(revokeRes.statusCode, 200);

        // Re-resolution after revocation -> must fail
        const postRevokeRes = await app.inject({
            method: 'GET',
            url: `/v1/shared/${shareToken}`
        });
        assert.equal(postRevokeRes.statusCode, 404);
    });

    test('8. Anchored Comments', async () => {
        const commentRes = await app.inject({
            method: 'POST',
            url: `/v1/documents/${documentId}/comments`,
            payload: {
                pageId: 'page-1',
                authorName: 'Auditor Dave',
                text: 'Please confirm this signature matches the original form.',
                anchorBox: '0.1,0.2,0.3,0.15'
            }
        });
        assert.equal(commentRes.statusCode, 201);
        const commentBody = JSON.parse(commentRes.body);
        assert.ok(commentBody.id);
        assert.equal(commentBody.authorName, 'Auditor Dave');
    });

    test('9. Entitlements Verification', async () => {
        const entRes = await app.inject({
            method: 'GET',
            url: '/v1/entitlements',
            headers: { authorization: `Bearer ${aliceToken}` }
        });
        assert.equal(entRes.statusCode, 200);
        const entBody = JSON.parse(entRes.body);
        assert.equal(entBody.plan, 'FREE');
        assert.equal(entBody.features.ocrUnlimited, true);
    });

    test('10. Document Conversion Pipeline & SSRF Protection (CNV-01..04, Phase 13)', async () => {
        // 10a. Valid DOCX to PDF conversion
        const convRes = await app.inject({
            method: 'POST',
            url: '/v1/conversions',
            headers: { authorization: `Bearer ${aliceToken}` },
            payload: {
                inputType: 'DOCX',
                outputType: 'PDF',
                sourceUrl: 'https://storage.doclens.local/sample.docx'
            }
        });
        assert.equal(convRes.statusCode, 202);
        const convBody = JSON.parse(convRes.body);
        assert.ok(convBody.jobId);

        // Check Job status transitioned to COMPLETED
        const jobRes = await app.inject({
            method: 'GET',
            url: `/v1/jobs/${convBody.jobId}`,
            headers: { authorization: `Bearer ${aliceToken}` }
        });
        assert.equal(jobRes.statusCode, 200);
        const jobBody = JSON.parse(jobRes.body);
        assert.equal(jobBody.status, 'COMPLETED');
        assert.ok(jobBody.result_url);

        // Download Converted PDF
        const dlRes = await app.inject({
            method: 'GET',
            url: `/v1/conversions/${convBody.jobId}/download`,
            headers: { authorization: `Bearer ${aliceToken}` }
        });
        assert.equal(dlRes.statusCode, 200);
        assert.equal(dlRes.headers['content-type'], 'application/pdf');
        assert.ok(dlRes.rawPayload.subarray(0, 5).toString('ascii').startsWith('%PDF-'));

        // 10b. Website Conversion SSRF Protection (reject localhost/private IP)
        const ssrfRes = await app.inject({
            method: 'POST',
            url: '/v1/conversions',
            headers: { authorization: `Bearer ${aliceToken}` },
            payload: {
                inputType: 'WEBSITE',
                outputType: 'PDF',
                sourceUrl: 'http://127.0.0.1:8080/internal-secrets'
            }
        });
        assert.equal(ssrfRes.statusCode, 202);
        const ssrfJobId = JSON.parse(ssrfRes.body).jobId;

        const ssrfJobRes = await app.inject({
            method: 'GET',
            url: `/v1/jobs/${ssrfJobId}`,
            headers: { authorization: `Bearer ${aliceToken}` }
        });
        assert.equal(ssrfJobRes.statusCode, 200);
        const ssrfJobBody = JSON.parse(ssrfJobRes.body);
        assert.equal(ssrfJobBody.status, 'FAILED');
        assert.ok(ssrfJobBody.error_message?.includes('blocked'));
    });

    test('11. Web Companion Client Access (WEB-01, Phase 12)', async () => {
        const webRes = await app.inject({
            method: 'GET',
            url: '/web'
        });
        assert.equal(webRes.statusCode, 200);
        assert.ok(webRes.body.includes('DocLens Web Companion'));

        const cssRes = await app.inject({
            method: 'GET',
            url: '/web/styles.css'
        });
        assert.equal(cssRes.statusCode, 200);
        assert.ok(cssRes.body.includes('--gold-primary'));
    });

    test('12. Account Deletion Workflow (CLD-01, Phase 10)', async () => {
        const delRes = await app.inject({
            method: 'POST',
            url: '/v1/account/deletion',
            headers: { authorization: `Bearer ${aliceToken}` }
        });
        assert.equal(delRes.statusCode, 200);
        assert.equal(JSON.parse(delRes.body).success, true);
    });
});
