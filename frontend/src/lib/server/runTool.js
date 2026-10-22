import { spawn } from 'child_process';
import { TOOL_ROOT, MICROSERVICES_ROOT } from '$env/static/private';
import path from 'path';
import { resolveEntryService } from '$lib/entryService.js';

/**
 * Runs the coverage tool with the given configuration
 * @param {Object} config - The configuration object
 * @param {string} config.configName - Name of the configuration
 * @param {Array<{serviceName: string}>} config.services - Array of service objects
 * @param {Array<{fileName: string, path: string, serviceName: string}>} config.tests - Array of test objects
 * @param {Object} options - Optional execution options
 * @param {boolean} options.staticOnly - Run static-only analysis (--static)
 * @param {boolean} options.forceReanalyze - Ignore the static analysis cache (--force-reanalyze)
 * @param {Function} progressCallback - Optional callback for progress updates (type, message)
 * @returns {Promise<{success: boolean, stdout: string, stderr: string, exitCode: number, storagePath: string}>}
 */
/**
 * The last few non-empty lines of a tool output stream, or null if it is empty.
 * @param {string} output
 * @param {number} count
 * @returns {string[]|null}
 */
function lastLines(output, count = 12) {
    const lines = output
        .split('\n')
        .map((line) => line.trimEnd())
        .filter((line) => line.trim());

    return lines.length ? lines.slice(-count) : null;
}

export async function runCoverageTool(config, options = {}, progressCallback = null) {
    if (typeof options === "function") {
        progressCallback = options;
        options = {};
    }

    const { staticOnly = false, forceReanalyze = false } = options;

    return new Promise((resolve, reject) => {
        // Build absolute paths for test files
        const testPaths = config.tests.map(test =>
            path.join(MICROSERVICES_ROOT, test.serviceName, 'src/test/java', test.path)
        );

        // Build absolute paths for service directories
        const servicePaths = config.services.map(service =>
            path.join(MICROSERVICES_ROOT, service.serviceName)
        );

        // The static analysis takes exactly one root service folder and no tests;
        // a coverage run takes the test files first, then every service directory.
        let args;
        if (staticOnly) {
            const entryService = resolveEntryService(config);
            if (!entryService) {
                reject({
                    success: false,
                    error: `Config "${config.configName}" has no entry service for the static analysis`,
                    stdout: '',
                    stderr: ''
                });
                return;
            }
            args = [
                path.join(MICROSERVICES_ROOT, entryService),
                '--config', config.configName,
                '--openapi-dir', './slim_open_api',
                '--static'
            ];
        } else {
            args = [...testPaths, ...servicePaths, '--config', config.configName,' --openapi-dir ./slim_open_api'];
        }

        if (forceReanalyze) {
            args.push('--force-reanalyze');
        }


        const childProcess = spawn('./run-coverage.sh', args, {
            cwd: TOOL_ROOT,
            shell: true,
            env: { ...process.env }
        });

        let stdout = '';
        let stderr = '';
        let storagePath = null;
        // Set once the tool has named a reason itself, so the fallback below stays quiet
        let reportedError = false;

        childProcess.stdout.on('data', (data) => {
            const output = data.toString();
            stdout += output;

            // Parse structured progress markers
            const lines = output.split('\n');
            lines.forEach(line => {
                if (line.startsWith('[STORAGE PATH]')) {
                    storagePath = line.replace('[STORAGE PATH]', '').trim();
                    console.log(`[Storage Path] ${storagePath}`);
                    if (progressCallback) progressCallback('storagePath', storagePath.split("/").pop());
                } else if (line.startsWith('[PROGRESS]')) {
                    const message = line.replace('[PROGRESS]', '').trim();
                    console.log(`[Progress] ${message}`);
                    if (progressCallback) progressCallback('progress', message);
                } else if (line.startsWith('[STEP]')) {
                    const message = line.replace('[STEP]', '').trim();
                    console.log(`[Step] ${message}`);
                    if (progressCallback) progressCallback('step', message);
                } else if (line.startsWith('[ERROR]')) {
                    // Maven reports its failures on stdout, not stderr
                    const message = line.replace('[ERROR]', '').trim();
                    console.error(`[Coverage Tool] ${line}`);
                    if (progressCallback && message) {
                        progressCallback('error', message);
                        reportedError = true;
                    }
                } else if (line.startsWith('[INFO]')) {
                    const message = line.replace('[INFO]', '').trim();
                    console.log(`[Info] ${message}`);
                    if (progressCallback) progressCallback('info', message);
                } else if (line.trim()) {
                    // Unmarked output is the tool's own log; keep it on the server
                    // console so a failed run can be traced back
                    console.log(`[Coverage Tool] ${line}`);
                }
            });
        });

        childProcess.stderr.on('data', (data) => {
            const error = data.toString();
            stderr += error;
            console.error(`[Coverage Tool] ${error.trimEnd()}`);
            if (!progressCallback) return;

            for (const line of error.split('\n')) {
                if (line.includes('[ERROR]')) {
                    progressCallback('error', line.replace('[ERROR]', '').trim());
                    reportedError = true;
                }
            }
        });

        childProcess.on('close', (exitCode) => {
            console.log(`[Coverage Tool] Process exited with code ${exitCode}`);
            if (exitCode !== 0 && progressCallback && !reportedError) {
                // The tool named no reason, so the end of its output is the only clue
                for (const line of lastLines(stderr) || lastLines(stdout) || []) {
                    progressCallback('error', line);
                }
            }
            if (exitCode === 0) {
                resolve({
                    success: true,
                    stdout,
                    stderr,
                    exitCode,
                    storagePath
                });
            } else {
                resolve({
                    success: false,
                    stdout,
                    stderr,
                    exitCode,
                    storagePath
                });
            }
        });

        childProcess.on('error', (error) => {
            console.error(`[Coverage Tool] Process error:`, error);
            reject({
                success: false,
                error: error.message,
                stdout,
                stderr
            });
        });
    });
}
