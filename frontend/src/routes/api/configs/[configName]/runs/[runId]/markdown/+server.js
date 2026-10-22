import { json } from '@sveltejs/kit';
import { getMarkdown } from '$lib/server/runs.js';

export async function GET({ params }) {
    const { configName, runId } = params;

    try {
        const markdown = await getMarkdown(configName, runId);
        return new Response(markdown, {
            headers: {
                'content-type': 'text/markdown; charset=utf-8',
            },
        });
    } catch (error) {
        console.error('Error fetching markdown report:', error);
        return json({ error: 'Markdown report not found' }, { status: 404 });
    }
}
