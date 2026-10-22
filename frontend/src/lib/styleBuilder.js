export function buildCytoscapeStyle(config, options = {}) {
    const neutralEdges = Boolean(options.neutralEdges);
    const coveredEdgeColor = neutralEdges ? "#94a3b8" : config.edge.coveredColor;
    const uncoveredEdgeColor = neutralEdges ? "#cbd5e1" : config.edge.uncoveredColor;
    const labelBackgroundColor = neutralEdges ? "#f8fafc" : config.label.backgroundColor;
    const coveredLabelBackgroundColor = neutralEdges
        ? "#f8fafc"
        : config.label.coveredBackgroundColor ?? config.label.backgroundColor;
    const uncoveredLabelBackgroundColor = neutralEdges
        ? "#f8fafc"
        : config.label.uncoveredBackgroundColor ?? config.label.backgroundColor;

    return [
        {
            selector: "node",
            style: {
                width: config.node.size,
                height: config.node.size,
                "background-color": config.node.color,
                label: "data(label)",
                color: config.node.textColor,
                "text-valign": "center",
                "text-halign": "center",
                "font-size": 3
            }
        },
        {
            selector: "node[?isTestService]",
            style: {
                "background-color": "#2060c7"
            }
        },
        {
            selector: "node[?isOrigin]",
            style: {
                "background-color": "#3b82f6"
            }
        },
        {
            selector: "node[?isRootService]",
            style: {
                "background-color": "#3b82f6",
                "border-width": 2,
                "border-color": "#1d4ed8",
                "font-weight": "700",
                "text-outline-color": "#1e3a8a",
                "text-outline-width": 0.25
            }
        },
        {
            selector: "node.selected-vertex",
            style: {
                "border-width": 2,
                "border-color": "#60a5fa",
                "overlay-color": "#60a5fa",
                "overlay-opacity": 0.08,
                "overlay-padding": 4
            }
        },
        {
            selector: "edge.selected-edge",
            style: {
                width: config.edge.width + 0.75,
                opacity: 0.9,
                "border-width": 2,
                "border-color": "#60a5fa",
                "overlay-color": "#60a5fa",
                "overlay-opacity": 0.15,
                "overlay-padding": 4,
                "line-color": "#2474ab", //neutralEdges ? "#64748b" : config.edge.coveredColor,
                "target-arrow-color": neutralEdges ? "#64748b" : config.edge.coveredColor
            }
        },
        {
            selector: "edge",
            style: {
                width: config.edge.width,
                "curve-style": config.edge.curveStyle,
                "control-point-distances": config.edge.controlPointDistance,
                "control-point-weights": 0.5,
                "target-arrow-shape": "triangle",
                label: "data(label)",
                "font-size": config.label.fontSize,
                "text-margin-y": -10,
                "text-background-color": labelBackgroundColor,
                "text-background-opacity": config.label.opacity,
                "text-background-padding": config.label.padding,
                "text-background-shape": "round-rectangle",
                'text-border-style': 'solid',
                'text-border-opacity': 1,
                'text-border-width': config.label.borderWidth ?? 1,
                "text-border-color": config.label.borderColor,
                "color": config.label.textColor
            }
        },
        {
            selector: "edge[?covered]",
            style: {
                "line-color": coveredEdgeColor,
                "target-arrow-color": coveredEdgeColor,
                "text-background-color": coveredLabelBackgroundColor
            }
        },
        {
            selector: "edge[!covered]",
            style: {
                "line-color": uncoveredEdgeColor,
                "target-arrow-color": uncoveredEdgeColor,
                "line-style": "dashed",
                "text-background-color": uncoveredLabelBackgroundColor
            }
        }
    ];
}

export const defaultStyleConfig = {
    node: {
        size: 30,
        color: "#787373",
        textColor: "#ffffff"
    },
    edge: {
        width: 1.5,
        coveredColor: "#16a34a",
        uncoveredColor: "#dc2626",
        curveStyle: "bezier",
        controlPointDistance: 60
    },
    label: {
        fontSize: 2,
        backgroundColor: "#ffffff",
        coveredBackgroundColor: "#dcfce7",
        uncoveredBackgroundColor: "#fee2e2",
        textColor: "#374151",
        opacity: 0.95,
        padding: 2,
        borderWidth: 0.2,
        borderColor: "#d1d5db"
    }
};
