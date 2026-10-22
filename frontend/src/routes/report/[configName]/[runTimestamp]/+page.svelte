<script>
    import { page } from "$app/state";
    import {
        SplitPane,
        Pane,
        Label,
        Radio,
        Tabs,
        TabItem,
    } from "flowbite-svelte";
    import {
        ChartPieSolid,
        ClipboardListSolid,
        PaletteSolid,
    } from "flowbite-svelte-icons";
    import { onMount } from "svelte";
    import SystemGraph from "$lib/components/SystemGraph.svelte";
    import {
        buildConnectionIndex,
        mapReportToElements,
    } from "$lib/graphMapper";
    import { defaultStyleConfig } from "$lib/styleBuilder";
    import ConnectionReportDetailViewModal from "$lib/components/ConnectionReportDetailViewModal.svelte";
    import NodeReportDetailViewModal from "$lib/components/NodeReportDetailViewModal.svelte";
    import GraphStylePanel from "$lib/components/GraphStyle/GraphStylePanel.svelte";
    import CoverageReportView from "$lib/components/CoverageReportView.svelte";
    import { getContext } from "svelte";
    import { getOverviewInfo } from "$lib/reportOverViewUtils";
    import {
        saveGraphState,
        loadGraphState,
        exportGraphStateAsJSON,
        importGraphStateFromFile,
        saveToServerStorage,
        loadFromServerStorage,
    } from "$lib/graphStateManager";

    const { params } = page;

    let report = $state(null);
    let microserviceSystem = $state(null);
    let metaData = $state(null);
    let loading = $state(false);
    let error = $state(null);
    let graphElements = $state([]);
    let connectionIndex = $state(null);
    let styleConfig = $state(defaultStyleConfig);
    let selectedEdgeReport = $state(null);
    let detailedModalOpen = $state(false);
    let selectedNode = $state(null);
    let nodeModalOpen = $state(false);
    let edgeFilter = $state("all");
    let graphRef = $state(null);
    let overviewInfo = $state(null);

    onMount(async () => {
        // Try to get report from passed state first
        if (page.state?.run?.report) {
            report = page.state.run.report;
            metaData = page.state.run.metaData;
            microserviceSystem = page.state.run.microserviceSystem;
            initializeGraph();
        } else {
            // Fetch the report if not in state
            await fetchReport();
        }
    });
    async function fetchReport() {
        loading = true;
        error = null;

        try {
            const response = await fetch(
                `/api/configs/${encodeURIComponent(params.configName)}/runs/${encodeURIComponent(params.runTimestamp)}`,
            );
            if (!response.ok) throw new Error("Failed to fetch report");
            const data = await response.json();
            report = data.report;
            metaData = data.metaData;
            microserviceSystem = data.microserviceSystem;
            initializeGraph();
        } catch (err) {
            console.error("Error fetching report:", err);
            error = err.message;
        } finally {
            loading = false;
        }
    }

    function initializeGraph() {
        if (report) {
            const testServiceNames = metaData?.testServiceNames || [];
            const { nodes, edges } = mapReportToElements(
                report,
                testServiceNames,
            );
            graphElements = [...nodes, ...edges];
            connectionIndex = buildConnectionIndex(report);
            overviewInfo = getOverviewInfo([{ report }]);
        }
    }

    // $effect(() => {
    //     if (drawer.open !== undefined) {
    //         graphRef?.resizeGraph();
    //     }
    // });

    const handleEdgeSelectedEvent = (edge) => {
        if (connectionIndex) {
            selectedEdgeReport = connectionIndex.get(edge.detail.id);
            detailedModalOpen = true;
        }
    };

    const handleNodeSelectedEvent = (node) => {
        selectedNode = node?.detail?.id || null;
        nodeModalOpen = true;
    };

    function formatTimestamp(timestamp) {
        // Parse format: YYYYMMDD_HHMMSS
        const match = timestamp.match(
            /(\d{4})(\d{2})(\d{2})_(\d{2})(\d{2})(\d{2})/,
        );
        if (!match) return timestamp;

        const [, year, month, day, hour, minute, second] = match;
        const date = new Date(
            year,
            parseInt(month) - 1,
            day,
            hour,
            minute,
            second,
        );

        return new Intl.DateTimeFormat("en-US", {
            year: "numeric",
            month: "long",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
            hour12: true,
        }).format(date);
    }

    function handleSaveGraphState() {
        const cy = graphRef?.getCyInstance?.();
        if (!cy) {
            alert("Graph not ready");
            return;
        }

        const state = saveGraphState(cy, styleConfig, edgeFilter);
        if (state) {
            saveToServerStorage(
                params.configName,
                params.runTimestamp,
                state,
            ).then((success) => {
                if (success) {
                    alert("Graph state saved to server storage!");
                } else {
                    alert("Failed to save graph state");
                }
            });
        }
    }

    function handleExportGraphState() {
        const cy = graphRef?.getCyInstance?.();
        if (!cy) {
            alert("Graph not ready");
            return;
        }

        const state = saveGraphState(cy, styleConfig, edgeFilter);
        if (state) {
            const filename = `graph-state-${params.configName}-${params.runTimestamp}.json`;
            exportGraphStateAsJSON(state, filename);
        }
    }

    async function handleLoadGraphState() {
        const savedState = await loadFromServerStorage(
            params.configName,
            params.runTimestamp,
        );
        if (!savedState) {
            alert("No saved state found for this run");
            return;
        }

        const cy = graphRef?.getCyInstance?.();
        if (!cy) {
            alert("Graph not ready");
            return;
        }

        loadGraphState(cy, savedState);
        styleConfig = savedState.styleConfig || styleConfig;
        edgeFilter = savedState.edgeFilter || edgeFilter;
        alert("Graph state restored!");
    }

    async function handleImportGraphState() {
        try {
            const state = await importGraphStateFromFile();
            if (!state) return;

            const cy = graphRef?.getCyInstance?.();
            if (!cy) {
                alert("Graph not ready");
                return;
            }

            loadGraphState(cy, state);
            styleConfig = state.styleConfig || styleConfig;
            edgeFilter = state.edgeFilter || edgeFilter;
            alert("Graph state imported successfully!");
        } catch (error) {
            alert(`Error importing state: ${error.message}`);
        }
    }
</script>

<ConnectionReportDetailViewModal
    report={selectedEdgeReport}
    bind:open={detailedModalOpen}
/>
<NodeReportDetailViewModal
    {selectedNode}
    {microserviceSystem}
    bind:open={nodeModalOpen}
/>
<div class="layout">
    {#if loading}
        <div class="flex items-center justify-center w-full h-screen">
            <p class="text-gray-500">Loading report...</p>
        </div>
    {:else if error}
        <div class="flex items-center justify-center w-full h-screen">
            <p class="text-red-500">Error: {error}</p>
        </div>
    {:else if report}
        <div class="container-wrapper">
            <div class="report-container">
                <SplitPane direction="horizontal" initialSizes={[70, 30]}>
                    <Pane>
                        <div class="graph-wrapper h-full">
                            <SystemGraph
                                bind:this={graphRef}
                                elements={graphElements}
                                {styleConfig}
                                {edgeFilter}
                                on:edgeSelected={handleEdgeSelectedEvent}
                                on:nodeSelected={handleNodeSelectedEvent}
                            />
                        </div>
                    </Pane>
                    <Pane>
                        <!-- Tabs for different views -->
                        <div class="h-full flex flex-col overflow-hidden">
                            <Tabs
                                tabStyle="underline"
                                classes={{
                                    active: "p-4 text-blue-600 border-blue-600 dark:text-blue-500 dark:border-blue-500",
                                }}
                            >
                                <TabItem
                                    open
                                    class="flex-1"
                                    classes={{ button: "w-full" }}
                                >
                                    {#snippet titleSlot()}
                                        <div class="flex items-center gap-2">
                                            <ChartPieSolid size="md" />
                                            Overview
                                        </div>
                                    {/snippet}
                                    <div
                                        class="p-4 pb-8 overflow-y-auto"
                                        style="max-height: calc(100vh - 200px);"
                                    >
                                        <h1 class="text-2xl font-bold mb-6">
                                            Run Overview
                                        </h1>

                                        <!-- Run Information -->
                                        <div class="space-y-4 mb-6">
                                            <div
                                                class="border-l-4 border-blue-500 pl-4"
                                            >
                                                <p
                                                    class="text-xs text-gray-500 font-semibold uppercase"
                                                >
                                                    Config Name
                                                </p>
                                                <p
                                                    class="text-lg font-semibold text-gray-900"
                                                >
                                                    {params.configName}
                                                </p>
                                            </div>
                                            <div
                                                class="border-l-4 border-green-500 pl-4"
                                            >
                                                <p
                                                    class="text-xs text-gray-500 font-semibold uppercase"
                                                >
                                                    Run Timestamp
                                                </p>
                                                <p
                                                    class="text-lg font-semibold text-gray-900"
                                                >
                                                    {formatTimestamp(
                                                        params.runTimestamp,
                                                    )}
                                                </p>
                                            </div>
                                        </div>

                                        <!-- Key Metrics Summary -->
                                        <div
                                            class="bg-gray-50 rounded-lg p-4 mb-6"
                                        >
                                            <h3
                                                class="text-sm font-bold text-gray-900 mb-4"
                                            >
                                                Key Metrics
                                            </h3>
                                            <div class="grid grid-cols-2 gap-4">
                                                <div>
                                                    <p
                                                        class="text-xs text-gray-600 mb-1"
                                                    >
                                                        Total Connections
                                                    </p>
                                                    <p
                                                        class="text-2xl font-bold text-gray-900"
                                                    >
                                                        {report?.totalConnections ??
                                                            "-"}
                                                    </p>
                                                </div>
                                                <div>
                                                    <p
                                                        class="text-xs text-gray-600 mb-1"
                                                    >
                                                        Covered
                                                    </p>
                                                    <p
                                                        class="text-2xl font-bold text-green-600"
                                                    >
                                                        {report
                                                            ?.coveredConnections
                                                            ?.length ?? 0}
                                                    </p>
                                                </div>
                                                <div>
                                                    <p
                                                        class="text-xs text-gray-600 mb-1"
                                                    >
                                                        Uncovered
                                                    </p>
                                                    <p
                                                        class="text-2xl font-bold text-red-600"
                                                    >
                                                        {report
                                                            ?.uncoveredConnections
                                                            ?.length ?? 0}
                                                    </p>
                                                </div>
                                                <div>
                                                    <p
                                                        class="text-xs text-gray-600 mb-1"
                                                    >
                                                        Conn. Coverage %
                                                    </p>
                                                    <p
                                                        class="text-2xl font-bold text-blue-600"
                                                    >
                                                        {report?.connectionCoveragePercentage?.toFixed(
                                                            2,
                                                        ) ?? "-"}%
                                                    </p>
                                                </div>
                                                <div>
                                                    <p
                                                        class="text-xs text-gray-600 mb-1"
                                                    >
                                                        Status Code Coverage
                                                    </p>
                                                    <p
                                                        class="text-2xl font-bold text-violet-600"
                                                    >
                                                        {report?.statusCodeClassCoveragePercentage?.toFixed(
                                                            2,
                                                        ) ?? "-"}%
                                                    </p>
                                                </div>
                                                <div>
                                                    <p
                                                        class="text-xs text-gray-600 mb-1"
                                                    >
                                                        Req. Value Partitions
                                                    </p>
                                                    <p
                                                        class="text-2xl font-bold text-amber-600"
                                                    >
                                                        {report?.requestValuePartitionMeanHierarchicalCoveragePercentage != null
                                                            ? `${report.requestValuePartitionMeanHierarchicalCoveragePercentage.toFixed(2)}%`
                                                            : "-"}
                                                    </p>
                                                </div>
                                                <div>
                                                    <p
                                                        class="text-xs text-gray-600 mb-1"
                                                    >
                                                        Response Fields
                                                    </p>
                                                    <p
                                                        class="text-2xl font-bold text-cyan-600"
                                                    >
                                                        {report?.responseFieldMeanHierarchicalCoveragePercentage != null
                                                            ? `${report.responseFieldMeanHierarchicalCoveragePercentage.toFixed(2)}%`
                                                            : "-"}
                                                    </p>
                                                </div>
                                            </div>
                                        </div>

                                        <!-- Status Code Coverage -->
                                        <div
                                            class="bg-gray-50 rounded-lg p-4 mb-6"
                                        >
                                            <h3
                                                class="text-sm font-bold text-gray-900 mb-3"
                                            >
                                                Status Code Class Coverage
                                            </h3>
                                            <div class="space-y-2">
                                                <div
                                                    class="flex justify-between items-center"
                                                >
                                                    <span
                                                        class="text-sm text-gray-600"
                                                        >All Connections</span
                                                    >
                                                    <span class="font-semibold"
                                                        >{report?.statusCodeClassCoveragePercentage?.toFixed(
                                                            2,
                                                        ) ?? "-"}%</span
                                                    >
                                                </div>
                                                <div
                                                    class="flex justify-between items-center"
                                                >
                                                    <span
                                                        class="text-sm text-gray-600"
                                                        >Covered Connections
                                                        Only</span
                                                    >
                                                    <span class="font-semibold"
                                                        >{report?.coveredConnectionsStatusCodeClassCoveragePercentage?.toFixed(
                                                            2,
                                                        ) ?? "-"}%</span
                                                    >
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </TabItem>
                                <TabItem
                                    class="flex-1"
                                    classes={{ button: "w-full" }}
                                >
                                    {#snippet titleSlot()}
                                        <div class="flex items-center gap-2">
                                            <ClipboardListSolid size="md" />
                                            Coverage Report
                                        </div>
                                    {/snippet}
                                    <CoverageReportView {report} />
                                </TabItem>
                                <TabItem
                                    class="flex-1"
                                    classes={{ button: "w-full" }}
                                >
                                    {#snippet titleSlot()}
                                        <div class="flex items-center gap-2">
                                            <PaletteSolid size="md" />
                                            Styling
                                        </div>
                                    {/snippet}
                                    <div
                                        class="p-4 pb-8 overflow-y-auto space-y-6"
                                        style="max-height: calc(100vh - 200px);"
                                    >
                                        <GraphStylePanel
                                            bind:styleConfig
                                            bind:edgeFilter
                                        />

                                        <div class="border-t pt-4">
                                            <h3
                                                class="text-sm font-semibold text-gray-900 mb-3"
                                            >
                                                Edge Filter
                                            </h3>
                                            <div class="space-y-2">
                                                <div class="flex items-center">
                                                    <Radio
                                                        name="edge-filter"
                                                        value="all"
                                                        bind:group={edgeFilter}
                                                    />
                                                    <Label class="ml-2"
                                                        >All Edges</Label
                                                    >
                                                </div>
                                                <div class="flex items-center">
                                                    <Radio
                                                        name="edge-filter"
                                                        value="covered"
                                                        bind:group={edgeFilter}
                                                    />
                                                    <Label class="ml-2"
                                                        >Covered Only</Label
                                                    >
                                                </div>
                                                <div class="flex items-center">
                                                    <Radio
                                                        name="edge-filter"
                                                        value="uncovered"
                                                        bind:group={edgeFilter}
                                                    />
                                                    <Label class="ml-2"
                                                        >Uncovered Only</Label
                                                    >
                                                </div>
                                            </div>
                                        </div>

                                        <div class="border-t pt-4">
                                            <h3
                                                class="text-sm font-semibold text-gray-900 mb-3"
                                            >
                                                Graph State
                                            </h3>
                                            <div class="space-y-2">
                                                <button
                                                    onclick={handleSaveGraphState}
                                                    class="w-full px-3 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 transition"
                                                >
                                                    Save State
                                                </button>
                                                <button
                                                    onclick={handleLoadGraphState}
                                                    class="w-full px-3 py-2 bg-green-600 text-white text-sm font-medium rounded-md hover:bg-green-700 transition"
                                                >
                                                    Load Saved State
                                                </button>
                                                <button
                                                    onclick={handleExportGraphState}
                                                    class="w-full px-3 py-2 bg-purple-600 text-white text-sm font-medium rounded-md hover:bg-purple-700 transition"
                                                >
                                                    Export as JSON
                                                </button>
                                                <button
                                                    onclick={handleImportGraphState}
                                                    class="w-full px-3 py-2 bg-purple-600 text-white text-sm font-medium rounded-md hover:bg-purple-700 transition"
                                                >
                                                    Import from JSON
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </TabItem>
                            </Tabs>
                        </div>
                    </Pane>
                </SplitPane>
            </div>
        </div>
    {:else}
        <div class="flex items-center justify-center w-full h-screen">
            <p class="text-gray-500">No report available</p>
        </div>
    {/if}
</div>

<style>
    .layout {
        display: flex;
        height: 100vh;
        overflow: hidden;
        padding: 1rem;
        background-color: #f3f4f6;
    }

    .container-wrapper {
        flex: 1;
        display: flex;
        min-width: 0;
    }

    .report-container {
        flex: 1;
        background-color: white;
        border-radius: 0.5rem;
        border: 1px solid #e5e7eb;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        display: flex;
        overflow: hidden;
    }

    .graph-wrapper {
        width: 100%;
        height: 100%;
        min-width: 0;
    }
</style>
