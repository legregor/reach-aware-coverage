import { json } from '@sveltejs/kit';
import { runCoverageTool } from '$lib/server/runTool.js';
import { TOOL_ROOT } from '$env/static/private';
import { writeFile, mkdir } from 'fs/promises';
import path from 'path';

export async function POST({ request }) {
    try {
        const config = await request.json();

        if (!config.configName || !config.services || !config.tests) {
            return json({
                success: false,
                error: 'Invalid config format'
            }, { status: 400 });
        }

        if (config.graphSelection) {
            const selectedServices = Array.isArray(config.graphSelection.selectedServices)
                ? config.graphSelection.selectedServices
                : [];
            const selectedConnections = Array.isArray(config.graphSelection.selectedConnections)
                ? config.graphSelection.selectedConnections
                : [];
            config.graphSelection = {
                selectedServices,
                selectedConnections,
                updatedAt: config.graphSelection.updatedAt || new Date().toISOString(),
            };
        }

        const configsDir = path.join(TOOL_ROOT, 'storage', 'configs');
        const configPath = path.join(configsDir, `${config.configName}.json`);

        await mkdir(configsDir, { recursive: true });

        await writeFile(configPath, JSON.stringify(config, null, 2), 'utf-8');

        return json({
            success: true,
            message: 'Config saved successfully',
            path: configPath
        });

    } catch (error) {
        return json({
            success: false,
            error: error.message
        }, { status: 500 });
    }
}
