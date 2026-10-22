import { readdir, readFile, writeFile } from 'fs/promises';
import { join } from 'path';
import { TOOL_ROOT } from '$env/static/private';

const RUNS_DIR = join(TOOL_ROOT, 'storage', 'runs');

/**
 * Parse timestamp from directory name format: yyyyMMdd_HHmmss
 * @param {string} dirName - Directory name in format yyyyMMdd_HHmmss
 * @returns {string} ISO timestamp
 */
function parseTimestampFromDirName(dirName) {
    // Format: yyyyMMdd_HHmmss (e.g., "20260201_192002")
    const year = dirName.substring(0, 4);
    const month = dirName.substring(4, 6);
    const day = dirName.substring(6, 8);
    const hour = dirName.substring(9, 11);
    const minute = dirName.substring(11, 13);
    const second = dirName.substring(13, 15);

    return new Date(`${year}-${month}-${day}T${hour}:${minute}:${second}`).toISOString();
}

/**
 * Save a run result to the filesystem
 * @param {string} configName - The name of the configuration
 * @param {object} runData - The run data to save (should include coverage report data)
 * @param {string} storagePath - The storage path from the coverage tool
 * @returns {Promise<string>} The run ID (timestamp)
 */
export async function saveRun(configName, runData, storagePath) {
    // Extract timestamp from storagePath (last part of the path)
    const timestamp = storagePath.split('/').pop();

    const runToSave = {
        id: timestamp,
        configName,
        timestamp: new Date().toISOString(),
        storagePath,
        ...runData
    };

    // Save metadata to the same directory as coverage-report.json
    const runMetadataPath = join(storagePath, 'run-metadata.json');
    await writeFile(runMetadataPath, JSON.stringify(runToSave, null, 2));
    return timestamp;
}

/**
 * Get all runs for a specific configuration
 * @param {string} configName - The name of the configuration
 * @param {boolean} metaDataOnly - If true, only return metadata without the full report (default: false)
 * @returns {Promise<Array>} Array of run objects with coverage data, sorted by timestamp (newest first)
 */
export async function getRunsForConfig(configName, metaDataOnly = false) {
    const configRunsDir = join(RUNS_DIR, configName);

    try {
        const entries = await readdir(configRunsDir, { withFileTypes: true });
        const directories = entries.filter(e => e.isDirectory());

        const runs = await Promise.all(
            directories.map(async (dir) => {
                const reportPath = join(configRunsDir, dir.name, 'coverage-report.json');
                const metaDataPath = join(configRunsDir, dir.name, 'run-metadata.json');
                const msSystemPath = join(configRunsDir, dir.name, 'microservice-system.json');
                const staticAnalysisPath = join(configRunsDir, dir.name, 'static-analysis.json');

                try {
                    let staticAnalysis = null;
                    try {
                        const staticAnalysisContent = await readFile(staticAnalysisPath, 'utf-8');
                        staticAnalysis = JSON.parse(staticAnalysisContent);
                    } catch (e) {
                        // static analysis json doesn't exist
                    }

                    let microserviceSystem = null;
                    try {
                        const microserviceSystemContent = await readFile(msSystemPath, 'utf-8');
                        microserviceSystem = JSON.parse(microserviceSystemContent);
                    } catch (e) {
                        // System json doesn't exist
                    }

                    if (metaDataOnly) {
                        return {
                            id: dir.name,
                            configName,
                            timestamp: parseTimestampFromDirName(dir.name),
                            storagePath: join(configRunsDir, dir.name),
                            staticAnalysis,
                            microserviceSystem,
                            hasStaticAnalysis: Boolean(staticAnalysis && Object.keys(staticAnalysis).length > 0),
                            hasMicroserviceSystem: Boolean(microserviceSystem && Object.keys(microserviceSystem).length > 0),
                        }
                    }

                    let report = null;
                    try {
                        const reportContent = await readFile(reportPath, 'utf-8');
                        report = JSON.parse(reportContent);
                    } catch (e) {
                        // report not available
                    }

                    let metaData = null;
                    try {
                        const metadataContent = await readFile(metaDataPath, 'utf-8');
                        metaData = JSON.parse(metadataContent);
                    } catch (e) {
                        // Metadata file doesn't exist yet
                    }

                    return {
                        id: dir.name,
                        configName,
                        timestamp: parseTimestampFromDirName(dir.name),
                        report: report,
                        metaData: metaData,
                        microserviceSystem: microserviceSystem,
                        staticAnalysis: staticAnalysis,
                        hasStaticAnalysis: Boolean(staticAnalysis && Object.keys(staticAnalysis).length > 0),
                        hasMicroserviceSystem: Boolean(microserviceSystem && Object.keys(microserviceSystem).length > 0),
                    };
                } catch (e) {
                    console.error(`Failed to read run from ${reportPath}:`, e);
                    return null;
                }
            })
        );

        return runs.filter(r => r !== null).sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
    } catch (error) {
        if (error.code === 'ENOENT') {
            return [];
        }
        throw error;
    }
}

/**
 * Get a specific run by ID
 * @param {string} configName - The name of the configuration
 * @param {string} runId - The run ID (timestamp)
 * @returns {Promise<object>} The run object with coverage data
 */
export async function getRun(configName, runId) {
    const reportPath = join(RUNS_DIR, configName, runId, 'coverage-report.json');
    const metadataPath = join(RUNS_DIR, configName, runId, 'run-metadata.json');
    const msSystemPath = join(RUNS_DIR, configName, runId, 'microservice-system.json');

    const reportContent = await readFile(reportPath, 'utf-8');
    const report = JSON.parse(reportContent);

    // Try to read metadata if it exists
    let metadata = null;
    let microserviceSystem = null;
    try {
        const metadataContent = await readFile(metadataPath, 'utf-8');
        metadata = JSON.parse(metadataContent);
    } catch (e) {
        // Metadata doesn't exist
    }

    try {
        const microserviceSystemContent = await readFile(msSystemPath, 'utf-8');
        microserviceSystem = JSON.parse(microserviceSystemContent);
    } catch (e) {
        // System json doesn't exist
    }

    return {
        id: runId,
        configName,
        timestamp: parseTimestampFromDirName(runId),
        metaData: metadata,
        report: report,
        microserviceSystem: microserviceSystem
    };
}

export async function getMarkdown(configName, runId) {
    const markdownPath = join(RUNS_DIR, configName, runId, 'coverage-report.md');
    return readFile(markdownPath, 'utf-8');
}
