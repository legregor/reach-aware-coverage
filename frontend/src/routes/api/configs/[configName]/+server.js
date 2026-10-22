import { json } from "@sveltejs/kit";
import { TOOL_ROOT } from "$env/static/private";
import { readFile } from "fs/promises";
import path from "path";

export async function GET({ params }) {
    const { configName } = params;
    const configPath = path.join(
        TOOL_ROOT,
        "storage",
        "configs",
        `${configName}.json`,
    );

    try {
        const content = await readFile(configPath, "utf-8");
        return json({ success: true, config: JSON.parse(content) });
    } catch (error) {
        if (error?.code === "ENOENT") {
            return json(
                { success: false, error: "Configuration not found" },
                { status: 404 },
            );
        }
        return json(
            { success: false, error: "Failed to load configuration" },
            { status: 500 },
        );
    }
}
