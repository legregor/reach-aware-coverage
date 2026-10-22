import { runCoverageTool } from '$lib/server/runTool.js';
import { TOOL_ROOT } from '$env/static/private';
import { readFile } from 'fs/promises';
import path from 'path';

export async function GET({ params, url }) {
    const { configName } = params;
    const staticOnly = url.searchParams.get('mode') === 'static';

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
                    console.error('[SSE] Failed to send event:', error);
                    isClosed = true;
                }
            }

            try {
                const configPath = path.join(TOOL_ROOT, 'storage', 'configs', `${configName}.json`);
                console.log('[SSE] Loading config from:', configPath);

                const configData = await readFile(configPath, 'utf-8');
                const config = JSON.parse(configData);

                sendEvent('progress', 'Configuration loaded');
                sendEvent('runMode', staticOnly ? 'static' : 'full');
                sendEvent('info', `Config: ${config.configName}`);
                sendEvent('info', `Services: ${config.services.length}, Tests: ${config.tests.length}`);

                //run with callbacks
                const result = await runCoverageTool(config, { staticOnly }, (type, message) => {
                    sendEvent(type, message);
                });

                if (result.success) {
                    sendEvent('complete', 'Analysis finished successfully', {
                        runMode: staticOnly ? 'static' : 'full'
                    });
                } else {
                    sendEvent('error', `Analysis failed with exit code ${result.exitCode}`);
                }
            } catch (error) {
                console.error('[SSE] Error:', error);
                sendEvent('error', `Error: ${error.message}`);
            } finally {
                isClosed = true;
                try {
                    controller.close();
                } catch (e) {
                    // ignore
                }
            }
        }
    });

    return new Response(stream, {
        headers: {
            'Content-Type': 'text/event-stream',
            'Cache-Control': 'no-cache',
            'Connection': 'keep-alive'
        }
    });
}
