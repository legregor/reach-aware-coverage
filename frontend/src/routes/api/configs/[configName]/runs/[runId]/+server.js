import { json } from '@sveltejs/kit';
import { getRun } from '$lib/server/runs.js';

export async function GET({ params }) {
    const { configName, runId } = params;

    try {
        const run = await getRun(configName, runId);
        return json(run);
    } catch (error) {
        console.error('Error fetching run:', error);
        return json({ error: 'Run not found' }, { status: 404 });
    }
}
