import { json } from '@sveltejs/kit';
import { runCoverageTool } from '$lib/server/runTool.js';

export async function POST({ request }) {
    try {
        const config = await request.json();

        // Validate config
        if (!config.configName || !config.services || !config.tests) {
            return json({
                success: false,
                error: 'Invalid config format'
            }, { status: 400 });
        }

        const result = await runCoverageTool(config);

        return json(result);
    } catch (error) {
        return json({
            success: false,
            error: error.message
        }, { status: 500 });
    }
}
