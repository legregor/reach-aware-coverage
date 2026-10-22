import { json } from '@sveltejs/kit';
import { getRunsForConfig } from '$lib/server/runs.js';

export async function GET({ params, url }) {
    const { configName } = params;
    const metaDataOnly = url.searchParams.get('metaDataOnly') === 'true';

    try {
        const runs = await getRunsForConfig(configName, metaDataOnly);
        return json(runs);
    } catch (error) {
        console.error('Error fetching runs:', error);
        return json({ error: 'Failed to fetch runs' }, { status: 500 });
    }
}
