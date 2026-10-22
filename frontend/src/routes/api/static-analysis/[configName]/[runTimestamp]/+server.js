import { json } from "@sveltejs/kit";
import { readFile, writeFile } from "fs/promises";
import { join } from "path";
import { TOOL_ROOT } from "$env/static/private";

async function readJsonIfExists(filePath) {
    try {
        const content = await readFile(filePath, "utf-8");
        return JSON.parse(content);
    } catch (error) {
        if (error.code === "ENOENT") {
            return null;
        }
        throw error;
    }
}

function normalizeGraphSelection(rawGraphSelection) {
    if (!rawGraphSelection || typeof rawGraphSelection !== "object") {
        return null;
    }

    if (
        !Array.isArray(rawGraphSelection.selectedServices) ||
        !Array.isArray(rawGraphSelection.selectedConnections)
    ) {
        return null;
    }

    const selectedServices = [
        ...new Set(
            rawGraphSelection.selectedServices
                .filter((value) => typeof value === "string")
                .map((value) => value.trim())
                .filter(Boolean),
        ),
    ];
    const selectedConnections = [
        ...new Set(
            rawGraphSelection.selectedConnections
                .filter((value) => typeof value === "string")
                .map((value) => value.trim())
                .filter(Boolean),
        ),
    ];

    return {
        selectedServices,
        selectedConnections,
        updatedAt:
            typeof rawGraphSelection.updatedAt === "string" &&
            rawGraphSelection.updatedAt.trim()
                ? rawGraphSelection.updatedAt
                : null,
    };
}

export async function GET({ params }) {
    const { configName, runTimestamp } = params;

    try {
        const configPath = join(TOOL_ROOT, "storage", "configs", `${configName}.json`);
        const config = await readJsonIfExists(configPath);
        const graphSelection = normalizeGraphSelection(config?.graphSelection);

        const configRunRoot = join(TOOL_ROOT, "storage", "runs", configName);
        const runDir = join(configRunRoot, runTimestamp);

        const staticAnalysis =
            (await readJsonIfExists(join(runDir, "static-analysis.json"))) ||
            (await readJsonIfExists(join(configRunRoot, "static-analysis.json")));

        if (!staticAnalysis) {
            return json({ error: "Static analysis run not found" }, { status: 404 });
        }

        return json({
            configName,
            runTimestamp,
            staticAnalysis,
            graphSelection: graphSelection ?? normalizeGraphSelection(staticAnalysis.graphSelection),
        });
    } catch (error) {
        console.error("Failed to load static analysis run:", error);
        return json({ error: "Static analysis run not found" }, { status: 404 });
    }
}

export async function POST({ params, request }) {
    const { configName } = params;

    try {
        const payload = await request.json();
        const graphSelection = normalizeGraphSelection(payload?.graphSelection);
        if (!graphSelection) {
            return json(
                {
                    success: false,
                    error: "Invalid graphSelection format",
                },
                { status: 400 },
            );
        }

        const configPath = join(TOOL_ROOT, "storage", "configs", `${configName}.json`);
        const existingConfig = await readJsonIfExists(configPath);
        if (!existingConfig) {
            return json(
                {
                    success: false,
                    error: "Configuration not found",
                },
                { status: 404 },
            );
        }

        const updatedConfig = {
            ...existingConfig,
            graphSelection: {
                selectedServices: graphSelection.selectedServices,
                selectedConnections: graphSelection.selectedConnections,
                updatedAt: new Date().toISOString(),
            },
        };

        await writeFile(configPath, JSON.stringify(updatedConfig, null, 2), "utf-8");

        return json({
            success: true,
            configName,
            graphSelection: updatedConfig.graphSelection,
        });
    } catch (error) {
        console.error("Failed to persist static graph selection:", error);
        return json(
            { success: false, error: "Failed to persist static graph selection" },
            { status: 500 },
        );
    }
}
