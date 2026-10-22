import { json } from '@sveltejs/kit';
import { listSystems, listSystemsWithTests } from '$lib/server/overview.js';

/**
 * GET /api/systems
 * Returns JSON array of { serviceName }
 */
export async function GET() {
    try {
        const systems = await listSystemsWithTests();
        return json(systems);
    } catch (err) {
        console.error('GET /api/systems error', err);
        return json([], { status: 500 });
    }
}
