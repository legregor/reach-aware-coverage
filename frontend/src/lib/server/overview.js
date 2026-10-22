import fs from 'fs/promises';
import path from 'path';
import { MICROSERVICES_ROOT, MICROSERVICES_PREFIX } from '$env/static/private';

/**
 * List directories under `root` that start with `prefix`.
 * Simple server-only helper (JavaScript) — expects `MICROSERVICES_ROOT` to be an absolute path.
 *
 * @param {string} [root=MICROSERVICES_ROOT]
 * @param {string} [prefix=MICROSERVICES_PREFIX||'ts-']
 * @returns {Promise<Array<{serviceName: string}>>}
 */
export async function listSystems(root = MICROSERVICES_ROOT, prefix = MICROSERVICES_PREFIX || 'ts-') {
    if (!root) {
        throw new Error('MICROSERVICES_ROOT environment variable is not set');
    }

    try {
        const dirents = await fs.readdir(root, { withFileTypes: true });

        return dirents
            .filter((d) => d.isDirectory() && d.name.startsWith(prefix))
            .map((d) => ({ serviceName: d.name }));
    } catch (err) {
        console.error('listSystems error', err);
        return [];
    }
}

/**
 * Similar to `listSystems` but also returns test file paths found under
 * each service's `src/test/java` directory. It recursively searches for
 * files that are in a path segment named `integration` (case-sensitive).
 * 
 */
export async function listSystemsWithTests(root = MICROSERVICES_ROOT, prefix = MICROSERVICES_PREFIX || 'ts-') {
    if (!root) {
        throw new Error('MICROSERVICES_ROOT environment variable is not set');
    }

    try {
        const dirents = await fs.readdir(root, { withFileTypes: true });

        const services = dirents
            .filter((d) => d.isDirectory() && d.name.startsWith(prefix))
            .map((d) => d.name);

        const results = [];

        for (const svc of services) {
            const svcRoot = path.join(root, svc);
            const testRoot = path.join(svcRoot, 'src', 'test', 'java');

            const svcEntry = { serviceName: svc, tests: [] };

            try {
                // check testRoot exists
                const st = await fs.stat(testRoot);
                if (!st.isDirectory()) {
                    results.push(svcEntry);
                    continue;
                }
            } catch (e) {
                // no tests folder
                results.push(svcEntry);
                continue;
            }

            // recursively walk testRoot and collect files whose path contains '/integration/' segment
            async function walk(dir) {
                const entries = await fs.readdir(dir, { withFileTypes: true });
                for (const ent of entries) {
                    const full = path.join(dir, ent.name);
                    if (ent.isDirectory()) {
                        await walk(full);
                    } else if (ent.isFile()) {
                        // check if the path contains an 'integration' directory segment
                        const rel = path.relative(testRoot, full);
                        const segments = rel.split(path.sep);
                        if (segments.includes('integration')) {
                            svcEntry.tests.push({ path: rel, fileName: ent.name });
                        }
                    }
                }
            }

            await walk(testRoot);

            results.push(svcEntry);
        }

        return results;
    } catch (err) {
        console.error('listSystemsWithTests error', err);
        return [];
    }
}
