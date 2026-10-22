/**
 * Utility functions for saving and loading graph state (positions, viewport, styling)
 * Persists state to server storage instead of browser localStorage
 */

export function saveGraphState(cy, styleConfig, edgeFilter) {
    if (!cy) return null;

    const nodePositions = {};
    cy.nodes().forEach((node) => {
        nodePositions[node.id()] = {
            x: node.position("x"),
            y: node.position("y"),
        };
    });

    const viewport = cy.viewport();

    return {
        timestamp: new Date().toISOString(),
        nodePositions,
        viewport: {
            zoom: viewport.zoom,
            pan: {
                x: viewport.pan.x,
                y: viewport.pan.y,
            },
        },
        styleConfig,
        edgeFilter,
    };
}

export function loadGraphState(cy, state) {
    if (!cy || !state) return false;

    try {
        // Restore node positions
        if (state.nodePositions) {
            cy.nodes().forEach((node) => {
                const pos = state.nodePositions[node.id()];
                if (pos) {
                    node.position({ x: pos.x, y: pos.y });
                }
            });
        }

        // Restore viewport
        if (state.viewport) {
            cy.viewport({
                zoom: state.viewport.zoom,
                pan: state.viewport.pan,
            });
        }

        return true;
    } catch (error) {
        console.error("Error loading graph state:", error);
        return false;
    }
}

export function exportGraphStateAsJSON(state, filename = "graph-state.json") {
    const json = JSON.stringify(state, null, 2);
    const blob = new Blob([json], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename || "graph-state.json";
    link.click();
    URL.revokeObjectURL(url);
}

export function importGraphStateFromFile() {
    return new Promise((resolve, reject) => {
        const input = document.createElement("input");
        input.type = "file";
        input.accept = ".json";

        input.onchange = (e) => {
            const file = e.target.files?.[0];
            if (!file) {
                reject(new Error("No file selected"));
                return;
            }

            const reader = new FileReader();
            reader.onload = (event) => {
                try {
                    const state = JSON.parse(event.target.result);
                    resolve(state);
                } catch (error) {
                    reject(new Error("Invalid JSON file"));
                }
            };
            reader.onerror = () => reject(new Error("Failed to read file"));
            reader.readAsText(file);
        };

        input.click();
    });
}

// Server-based storage functions
export async function saveToServerStorage(configName, runTimestamp, state) {
    try {
        const response = await fetch(
            `/api/configs/${encodeURIComponent(configName)}/runs/${encodeURIComponent(runTimestamp)}/graph-state`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(state),
            }
        );

        if (!response.ok) {
            throw new Error(`Failed to save: ${response.statusText}`);
        }

        const result = await response.json();
        return result.success;
    } catch (error) {
        console.error("Error saving to server storage:", error);
        return false;
    }
}

export async function loadFromServerStorage(configName, runTimestamp) {
    try {
        const response = await fetch(
            `/api/configs/${encodeURIComponent(configName)}/runs/${encodeURIComponent(runTimestamp)}/graph-state`
        );

        if (!response.ok) {
            if (response.status === 404) {
                return null;
            }
            throw new Error(`Failed to load: ${response.statusText}`);
        }

        const result = await response.json();
        return result.data || result;
    } catch (error) {
        console.error("Error loading from server storage:", error);
        return null;
    }
}
