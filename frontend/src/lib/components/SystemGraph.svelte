<script>
    import { createEventDispatcher, onMount } from "svelte";
    import cytoscape from "cytoscape";
    import { buildCytoscapeStyle } from "$lib/styleBuilder";

    const {
        elements,
        styleConfig,
        layout,
        edgeFilter,
        selectedNodeId,
        selectedEdgeId,
        neutralEdges = false,
    } = $props();

    const cytoscapeStyle = $derived(
        buildCytoscapeStyle(styleConfig, { neutralEdges }),
    );

    const dispatch = createEventDispatcher();

    let container;
    let cy;
    let hasDoneInitialAutofit = false;

    function canAutofit() {
        return (
            cy &&
            container &&
            container.clientWidth > 0 &&
            container.clientHeight > 0 &&
            cy.nodes().length > 0
        );
    }

    function runInitialAutofitIfReady() {
        if (hasDoneInitialAutofit || !canAutofit()) return;
        cy.resize();
        cy.layout({ name: "cose", animate: true, fit: true, padding: 80 }).run();
        hasDoneInitialAutofit = true;
    }

    onMount(() => {
        const resizeObserver = new ResizeObserver(() => {
            if (cy) {
                cy.resize();
                runInitialAutofitIfReady();
            }
        });
        resizeObserver.observe(container);

        cy = cytoscape({
            container,
            elements: elements,
            layout: {
                name: "cose",
                animate: true,
            },
            style: cytoscapeStyle,
        });

        cy.on("tap", "edge", (evt) => {
            dispatch("edgeSelected", evt.target.data());
        });

        cy.on("tap", "node", (evt) => {
            dispatch("nodeSelected", evt.target.data());
        });

        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                runInitialAutofitIfReady();
            });
        });

        return () => {
            resizeObserver.disconnect();
            if (cy) {
                cy.destroy();
            }
        };
    });

    $effect(() => {
        if (cy) {
            cy.style(cytoscapeStyle);
            cy.resize();
        }
    });

    $effect(() => {
        if (!cy) return;
        const previousViewport = cy.viewport();
        const previousNodePositions = new Map();
        cy.nodes().forEach((node) => {
            previousNodePositions.set(node.id(), {
                x: node.position("x"),
                y: node.position("y"),
            });
        });

        cy.elements().remove();
        cy.add(elements);

        let hasNewNode = false;
        cy.nodes().forEach((node) => {
            const previousPosition = previousNodePositions.get(node.id());
            if (previousPosition) {
                node.position(previousPosition);
                return;
            }
            hasNewNode = true;
        });

        if (hasNewNode) {
            const shouldFit = !hasDoneInitialAutofit;
            cy.layout({
                name: "cose",
                animate: true,
                fit: shouldFit,
                padding: shouldFit ? 80 : undefined,
            }).run();
            if (shouldFit) {
                hasDoneInitialAutofit = true;
            }
        } else {
            cy.viewport({
                zoom: previousViewport.zoom,
                pan: previousViewport.pan,
            });
        }
        cy.resize();
    });

    $effect(() => {
        if (!cy) return;

        cy.nodes().removeClass("selected-vertex");
        if (selectedNodeId) {
            cy.getElementById(selectedNodeId).addClass("selected-vertex");
        }
    });

    $effect(() => {
        if (!cy) return;

        cy.edges().removeClass("selected-edge");
        if (selectedEdgeId) {
            cy.getElementById(selectedEdgeId).addClass("selected-edge");
        }
    });

    $effect(() => {
        if (!cy) return;

        const edges = cy.edges();

        if (edgeFilter === "all") {
            edges.show();
        } else if (edgeFilter === "covered") {
            edges.hide();
            edges.filter((e) => e.data("covered")).show();
        } else if (edgeFilter === "uncovered") {
            edges.hide();
            edges.filter((e) => !e.data("covered")).show();
        }
    });

    export function resizeGraph() {
        if (cy) {
            cy.resize();
            cy.fit();
        }
    }

    export function getCyInstance() {
        return cy;
    }

    export function restoreNodePositions(nodePositions) {
        if (!cy) return false;
        try {
            cy.nodes().forEach((node) => {
                const pos = nodePositions[node.id()];
                if (pos) {
                    node.position({ x: pos.x, y: pos.y });
                }
            });
            return true;
        } catch (error) {
            console.error("Error restoring node positions:", error);
            return false;
        }
    }

    export function restoreViewport(zoom, pan) {
        if (!cy) return false;
        try {
            cy.viewport({ zoom, pan });
            return true;
        } catch (error) {
            console.error("Error restoring viewport:", error);
            return false;
        }
    }
</script>

<div bind:this={container} class="graph"></div>

<style>
    .graph {
        width: 100%;
        height: 100%;
        min-height: 480px;
        background: #f8fafc;
    }
</style>
