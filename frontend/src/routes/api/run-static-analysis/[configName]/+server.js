import { runCoverageTool } from '$lib/server/runTool.js';
import { resolveEntryService } from '$lib/entryService.js';
import { TOOL_ROOT } from '$env/static/private';
import { readFile } from 'fs/promises';
import path from 'path';

export async function GET({ params, url }) {
    const { configName } = params;
    // A rerun ignores the static analysis cache so changed services are picked up
    const forceReanalyze = url.searchParams.get('force') === '1';

    const stream = new ReadableStream({
        async start(controller) {
            const encoder = new TextEncoder();
            let isClosed = false;

            function sendEvent(type, message, extra = {}) {
                if (isClosed) return;
                try {
                    const data = JSON.stringify({ type, message, ...extra });
                    controller.enqueue(encoder.encode(`data: ${data}\n\n`));
                } catch (error) {
                    console.error('[Static SSE] Failed to send event:', error);
                    isClosed = true;
                }
            }

            try {
                const configPath = path.join(
                    TOOL_ROOT,
                    'storage',
                    'configs',
                    `${configName}.json`,
                );
                const configData = await readFile(configPath, 'utf-8');
                const config = JSON.parse(configData);

                sendEvent('step', 'Starting static analysis');
                sendEvent('progress', 'Configuration loaded');
                sendEvent('info', `Config: ${config.configName}`);

                const entryService = resolveEntryService(config);
                if (!entryService) {
                    sendEvent(
                        'error',
                        `Config "${configName}" names no entry service to start the static analysis from`,
                    );
                    return;
                }
                sendEvent('info', `Entry service: ${entryService}`);
                if (forceReanalyze) {
                    sendEvent('info', 'Ignoring the static analysis cache');
                }

                const result = await runCoverageTool(
                    config,
                    { staticOnly: true, forceReanalyze },
                    (type, message) => {
                        sendEvent(type, message);
                    },
                );

                if (result.success) {
                    const runId =
                        result.storagePath?.split('/').pop() || 'static-analysis';
                    sendEvent('complete', 'Static analysis finished successfully', {
                        runId,
                        storagePath: result.storagePath,
                    });
                } else {
                    sendEvent(
                        'error',
                        `Static analysis failed with exit code ${result.exitCode}`,
                    );
                }
            } catch (error) {
                console.error('[Static SSE] Error:', error);
                sendEvent('error', `Error: ${error.message}`);
            } finally {
                isClosed = true;
                try {
                    controller.close();
                } catch {
                    // ignore
                }
            }
        },
    });

    return new Response(stream, {
        headers: {
            'Content-Type': 'text/event-stream',
            'Cache-Control': 'no-cache',
            Connection: 'keep-alive',
        },
    });
}
