import { listSystemsWithTests } from '$lib/server/overview.js';
import { TOOL_ROOT } from '$env/static/private';
import { readdir, readFile } from 'fs/promises';
import path from 'path';

/**
 * Server load for the main page — returns systems with tests and saved configs.
 */
export async function load() {
    try {
        const systems = await listSystemsWithTests();

        // Load saved configs from storage
        const configsDir = path.join(TOOL_ROOT, 'storage', 'configs');
        let configs = [];

        try {
            const files = await readdir(configsDir);
            const jsonFiles = files.filter(f => f.endsWith('.json'));

            configs = await Promise.all(
                jsonFiles.map(async (file) => {
                    const filePath = path.join(configsDir, file);
                    const content = await readFile(filePath, 'utf-8');
                    return JSON.parse(content);
                })
            );
        } catch (err) {
            console.error('Error loading configs:', err);
            // Directory might not exist yet, which is okay
        }

        return { systems, configs };
    } catch (err) {
        console.error('load / +page.server error', err);
        return { systems: [], configs: [] };
    }
}

