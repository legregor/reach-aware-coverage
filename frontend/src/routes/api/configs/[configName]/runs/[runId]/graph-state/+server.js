import { json } from '@sveltejs/kit';
import { writeFile, mkdir, readFile } from 'fs/promises';
import path from 'path';
import { TOOL_ROOT } from '$env/static/private';

export async function POST({ params, request }) {
    try {
        const { configName, runId } = params;
        const state = await request.json();

        const graphStateDir = path.join(TOOL_ROOT, 'storage', 'graph-states', configName);
        const stateFile = path.join(graphStateDir, `${runId}.json`);

        await mkdir(graphStateDir, { recursive: true });
        await writeFile(stateFile, JSON.stringify(state, null, 2), 'utf-8');

        return json({
            success: true,
            message: 'Graph state saved successfully',
            path: stateFile
        });
    } catch (error) {
        console.error('Error saving graph state:', error);
        return json({
            success: false,
            error: error.message
        }, { status: 500 });
    }
}

export async function GET({ params }) {
    try {
        const { configName, runId } = params;

        const graphStateDir = path.join(TOOL_ROOT, 'storage', 'graph-states', configName);
        const stateFile = path.join(graphStateDir, `${runId}.json`);

        const data = await readFile(stateFile, 'utf-8');
        const state = JSON.parse(data);

        return json({
            success: true,
            data: state
        });
    } catch (error) {
        if (error.code === 'ENOENT') {
            return json({
                success: false,
                error: 'No saved state found'
            }, { status: 404 });
        }

        console.error('Error loading graph state:', error);
        return json({
            success: false,
            error: error.message
        }, { status: 500 });
    }
}
