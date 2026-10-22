<script>
    import { page } from "$app/state";
    import { goto } from "$app/navigation";
    import { onMount } from "svelte";
    import { SplitPane, Pane, Tabs, TabItem, Button } from "flowbite-svelte";
    import { ChartPieSolid, ClipboardListSolid } from "flowbite-svelte-icons";
    import SystemGraph from "$lib/components/SystemGraph.svelte";
    import { mapStaticAnalysisToElements } from "$lib/graphMapper";
    import { defaultStyleConfig } from "$lib/styleBuilder";

    const { params } = page;
    const DEFAULT_EXPECTED_STATUS_CODES = ["2xx", "4xx"];
    const statusTokenPattern = /^([1-5]xx|[1-5]\d\d)$/i;

    let loading = $state(true);
    let saving = $state(false);
    let runningDynamic = $state(false);
    let error = $state(null);
    let saveMessage = $state("");

    let graphRef = $state(null);
    let styleConfig = $state(defaultStyleConfig);
    let graphElements = $state([]);
    let selectedNodeId = $state(null);
    let selectedEdgeId = $state(null);
    let hoveredConnectionId = $state(null);
    let originService = $state("");

    let allGraphElements = [];
    let persistedGraphSelection = null;
    let loadedConfig = $state({
        configName: params.configName,
        services: [],
        tests: [],
        globalExpectedStatusCodes: [...DEFAULT_EXPECTED_STATUS_CODES],
        connectionOverrides: [],
        graphSelection: null,
    });

    let serviceSelection = $state({});
    let connectionEditors = $state([]);
    let globalStatusInput = $state(DEFAULT_EXPECTED_STATUS_CODES.join(", "));

    onMount(async () => {
        loading = true;
        error = null;
        saveMessage = "";

        try {
            await fetchConfig();
            const { graphSourceData, graphSelection } = await fetchStaticGraphSource();
            persistedGraphSelection = graphSelection;
            mapGraphElements(graphSourceData);
            initializeEditorState();
            applySelectionsToGraph();
        } catch (err) {
            console.error("Error loading static architecture editor:", err);
            error = err?.message || "Failed to load static architecture";
        } finally {
            loading = false;
        }
    });

    async function fetchConfig() {
        const response = await fetch(
            `/api/configs/${encodeURIComponent(params.configName)}`,
        );
        if (!response.ok) {
            loadedConfig = {
                configName: params.configName,
                services: [],
                tests: [],
                globalExpectedStatusCodes: [...DEFAULT_EXPECTED_STATUS_CODES],
                connectionOverrides: [],
                graphSelection: null,
            };
            return;
        }

        const payload = await response.json();
        const config = payload?.config || {};
        loadedConfig = {
            configName: config.configName || params.configName,
            services: Array.isArray(config.services) ? config.services : [],
            tests: Array.isArray(config.tests) ? config.tests : [],
            globalExpectedStatusCodes:
                Array.isArray(config.globalExpectedStatusCodes) &&
                config.globalExpectedStatusCodes.length > 0
                    ? config.globalExpectedStatusCodes
                    : [...DEFAULT_EXPECTED_STATUS_CODES],
            connectionOverrides: Array.isArray(config.connectionOverrides)
                ? config.connectionOverrides
                : [],
            graphSelection: normalizeGraphSelection(config.graphSelection),
        };
        persistedGraphSelection = loadedConfig.graphSelection;
    }

    function normalizeGraphSelection(rawSelection) {
        if (
            !rawSelection ||
            !Array.isArray(rawSelection.selectedServices) ||
            !Array.isArray(rawSelection.selectedConnections)
        ) {
            return null;
        }
        return {
            selectedServices: [
                ...new Set(
                    rawSelection.selectedServices
                        .filter((value) => typeof value === "string")
                        .map((value) => value.trim())
                        .filter(Boolean),
                ),
            ],
            selectedConnections: [
                ...new Set(
                    rawSelection.selectedConnections
                        .filter((value) => typeof value === "string")
                        .map((value) => value.trim())
                        .filter(Boolean),
                ),
            ],
        };
    }

    async function fetchStaticGraphSource() {
        const stateRun = page.state?.run;
        const stateStaticAnalysis = stateRun?.staticAnalysis;

        if (stateStaticAnalysis && Object.keys(stateStaticAnalysis).length > 0) {
            return {
                graphSourceData: stateStaticAnalysis,
                graphSelection: normalizeGraphSelection(
                    stateStaticAnalysis.graphSelection,
                ),
            };
        }

        const response = await fetch(
            `/api/static-analysis/${encodeURIComponent(params.configName)}/${encodeURIComponent(params.runTimestamp)}`,
        );
        if (!response.ok) throw new Error("Failed to fetch static architecture");

        const data = await response.json();
        return {
            graphSourceData: data.staticAnalysis,
            graphSelection: normalizeGraphSelection(data.graphSelection),
        };
    }

    function mapGraphElements(data) {
        const { nodes, edges } = mapStaticAnalysisToElements(data);
        allGraphElements = [...nodes, ...edges];
        originService =
            nodes.find((node) => node?.data?.isOrigin || node?.data?.isRootService)
                ?.data?.id || "";
    }

    function isNodeElement(element) {
        return (
            element &&
            element.data &&
            !Object.prototype.hasOwnProperty.call(element.data, "source")
        );
    }

    function normalizeStatuses(rawValue) {
        return [
            ...new Set(
                String(rawValue || "")
                    .split(",")
                    .map((token) => token.trim())
                    .filter(Boolean)
                    .map((token) => token.toLowerCase()),
            ),
        ];
    }

    function getInvalidStatuses(rawValue) {
        return normalizeStatuses(rawValue).filter(
            (token) => !statusTokenPattern.test(token),
        );
    }

    function statusesEqual(a, b) {
        return [...a].sort().join("|") === [...b].sort().join("|");
    }

    function findMatchingOverride(edgeData) {
        const normalizePath = (path) => {
            const value = path || "/";
            try {
                return decodeURIComponent(value);
            } catch {
                return value;
            }
        };
        return loadedConfig.connectionOverrides.find((override) => {
            if (!override) return false;
            return (
                (override.sourceService || "") === edgeData.source &&
                (override.destinationService || "") === edgeData.target &&
                (override.method || "").toUpperCase() ===
                    (edgeData.method || "CALL").toUpperCase() &&
                normalizePath(override.path) === normalizePath(edgeData.path)
            );
        });
    }

    function initializeEditorState() {
        const nodes = allGraphElements.filter((element) => isNodeElement(element));
        const edges = allGraphElements.filter((element) => !isNodeElement(element));

        const persistedSelectedServices = new Set(
            persistedGraphSelection?.selectedServices || [],
        );
        const selectedConnectionsFromPersistence = new Set(
            persistedGraphSelection?.selectedConnections || [],
        );
        const hasPersistedSelection =
            persistedSelectedServices.size > 0 ||
            selectedConnectionsFromPersistence.size > 0;

        const nextSelection = {};
        for (const node of nodes) {
            const serviceName = node.data.id;
            const serviceSelected = hasPersistedSelection
                ? persistedSelectedServices.has(serviceName)
                : true;
            nextSelection[serviceName] =
                serviceName === originService ? true : serviceSelected;
        }
        serviceSelection = nextSelection;

        const effectiveGlobalStatuses =
            loadedConfig.globalExpectedStatusCodes?.length > 0
                ? loadedConfig.globalExpectedStatusCodes
                : DEFAULT_EXPECTED_STATUS_CODES;
        globalStatusInput = effectiveGlobalStatuses.join(", ");

        connectionEditors = edges.map((edge) => {
            const override = findMatchingOverride(edge.data);
            const expectedStatuses =
                override?.expectedStatusCodes?.length > 0
                    ? override.expectedStatusCodes
                    : effectiveGlobalStatuses;
            return {
                id: edge.data.id,
                source: edge.data.source,
                target: edge.data.target,
                method: edge.data.method || "CALL",
                path: edge.data.path || "/",
                selected: hasPersistedSelection
                    ? selectedConnectionsFromPersistence.has(edge.data.id)
                    : true,
                statusInput: expectedStatuses.join(", "),
            };
        });
    }

    function applySelectionsToGraph() {
        const selectedServices = new Set(
            Object.entries(serviceSelection)
                .filter(([, selected]) => selected)
                .map(([serviceName]) => serviceName),
        );
        const selectedConnections = new Set(
            connectionEditors
                .filter(
                    (connection) =>
                        connection.selected &&
                        selectedServices.has(connection.source) &&
                        selectedServices.has(connection.target),
                )
                .map((connection) => connection.id),
        );

        graphElements = allGraphElements.filter((element) => {
            if (isNodeElement(element)) {
                return selectedServices.has(element.data.id);
            }
            return (
                selectedConnections.has(element.data.id) &&
                selectedServices.has(element.data.source) &&
                selectedServices.has(element.data.target)
            );
        });

        if (selectedNodeId && !selectedServices.has(selectedNodeId)) {
            selectedNodeId = null;
        }
        if (selectedEdgeId && !selectedConnections.has(selectedEdgeId)) {
            selectedEdgeId = null;
        }
    }

    function handleServiceToggle(serviceName, checked) {
        serviceSelection = { ...serviceSelection, [serviceName]: checked };
        applySelectionsToGraph();
    }

    function handleConnectionToggle(connectionId, checked) {
        connectionEditors = connectionEditors.map((connection) =>
            connection.id === connectionId
                ? { ...connection, selected: checked }
                : connection,
        );
        applySelectionsToGraph();
    }

    function handleConnectionStatusChange(connectionId, value) {
        connectionEditors = connectionEditors.map((connection) =>
            connection.id === connectionId
                ? { ...connection, statusInput: value }
                : connection,
        );
    }

    function handleNodeSelected(event) {
        selectedNodeId = event.detail?.id || null;
        selectedEdgeId = null;
    }

    function handleEdgeSelected(event) {
        selectedEdgeId = event.detail?.id || null;
        selectedNodeId = null;
    }

    function handleConnectionHoverStart(connectionId) {
        hoveredConnectionId = connectionId;
    }

    function handleConnectionHoverEnd(connectionId) {
        if (hoveredConnectionId === connectionId) {
            hoveredConnectionId = null;
        }
    }

    function fullConnectionLabel(connection) {
        return `${connection.source} -> ${connection.target} | ${connection.method} ${connection.path}`;
    }

    function buildConfigPayload() {
        const invalidGlobalStatuses = getInvalidStatuses(globalStatusInput);
        if (invalidGlobalStatuses.length > 0) {
            error =
                `Invalid global status token(s): ${invalidGlobalStatuses.join(", ")}. ` +
                "Use classes like 2xx or exact codes like 404.";
            return null;
        }

        const invalidConnection = connectionEditors.find(
            (connection) => getInvalidStatuses(connection.statusInput).length > 0,
        );
        if (invalidConnection) {
            const invalidTokens = getInvalidStatuses(invalidConnection.statusInput);
            error =
                `Invalid status token(s) for ${invalidConnection.source} -> ${invalidConnection.target} ` +
                `${invalidConnection.method} ${invalidConnection.path}: ${invalidTokens.join(", ")}`;
            return null;
        }

        const normalizedGlobalStatuses = normalizeStatuses(globalStatusInput);
        const effectiveGlobalStatuses =
            normalizedGlobalStatuses.length > 0
                ? normalizedGlobalStatuses
                : [...DEFAULT_EXPECTED_STATUS_CODES];

        const selectedServices = Object.entries(serviceSelection)
            .filter(([, selected]) => selected)
            .map(([serviceName]) => ({ serviceName }));
        const selectedServiceSet = new Set(
            selectedServices.map((service) => service.serviceName),
        );
        const selectedConnectionIds = connectionEditors
            .filter(
                (connection) =>
                    connection.selected &&
                    selectedServiceSet.has(connection.source) &&
                    selectedServiceSet.has(connection.target),
            )
            .map((connection) => connection.id);

        const connectionOverrides = connectionEditors
            .filter(
                (connection) =>
                    connection.selected &&
                    selectedServiceSet.has(connection.source) &&
                    selectedServiceSet.has(connection.target),
            )
            .map((connection) => {
                const normalizedStatuses = normalizeStatuses(connection.statusInput);
                return {
                    sourceService: connection.source,
                    destinationService: connection.target,
                    method: connection.method,
                    path: connection.path,
                    expectedStatusCodes:
                        normalizedStatuses.length > 0
                            ? normalizedStatuses
                            : effectiveGlobalStatuses,
                };
            })
            .filter(
                (override) =>
                    !statusesEqual(
                        override.expectedStatusCodes,
                        effectiveGlobalStatuses,
                    ),
            );

        return {
            configName: params.configName,
            services: selectedServices,
            tests: loadedConfig.tests || [],
            globalExpectedStatusCodes: effectiveGlobalStatuses,
            connectionOverrides,
            graphSelection: {
                selectedServices: selectedServices.map((service) => service.serviceName),
                selectedConnections: selectedConnectionIds,
            },
        };
    }

    async function persistConfig(payload, successMessage) {
        saving = true;
        try {
            const response = await fetch("/api/configs", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload),
            });
            const result = await response.json();
            if (!response.ok || !result.success) {
                throw new Error(result?.error || "Failed to save config");
            }
            saveMessage = successMessage;
            loadedConfig = {
                ...payload,
                graphSelection: normalizeGraphSelection(payload.graphSelection),
            };
            persistedGraphSelection = loadedConfig.graphSelection;
            return true;
        } catch (err) {
            error = err?.message || "Failed to save config";
            return false;
        } finally {
            saving = false;
        }
    }

    async function saveConfig() {
        saveMessage = "";
        error = null;
        const payload = buildConfigPayload();
        if (!payload) {
            return;
        }
        await persistConfig(payload, "Configuration saved.");
    }

    async function saveConfigAndRunDynamicAnalysis() {
        saveMessage = "";
        error = null;
        const payload = buildConfigPayload();
        if (!payload) {
            return;
        }
        runningDynamic = true;
        const saveSucceeded = await persistConfig(
            payload,
            "Configuration saved. Starting dynamic analysis...",
        );
        if (saveSucceeded) {
            await goto(
                `/run/${encodeURIComponent(params.configName)}?mode=full`,
            );
        }
        runningDynamic = false;
    }

    function serviceEntries() {
        return Object.keys(serviceSelection).sort((a, b) => a.localeCompare(b));
    }

    function selectedServicesCount() {
        return Object.values(serviceSelection).filter(Boolean).length;
    }

    function selectedConnectionsCount() {
        return connectionEditors.filter(
            (connection) =>
                connection.selected &&
                serviceSelection[connection.source] &&
                serviceSelection[connection.target],
        ).length;
    }
</script>

<div class="layout">
    {#if loading}
        <div class="center">
            <p class="text-gray-500">Loading static architecture...</p>
        </div>
    {:else if error}
        <div class="center">
            <p class="text-red-600">Error: {error}</p>
        </div>
    {:else}
        <div class="container-wrapper">
            <div class="editor-container">
                <SplitPane direction="horizontal" initialSizes={[65, 35]}>
                    <Pane>
                        <div class="graph-side h-full">
                            <div class="graph-header">
                                <div>
                                    <h1 class="text-lg font-bold text-gray-900">
                                        Static Architecture
                                    </h1>
                                    <p class="text-xs text-gray-600">
                                        {params.configName} · {params.runTimestamp}
                                    </p>
                                </div>
                                {#if originService}
                                    <span
                                        class="rounded-full bg-blue-100 text-blue-900 px-2 py-1 text-xs font-semibold"
                                    >
                                        Origin: {originService}
                                    </span>
                                {/if}
                            </div>
                            <div class="graph-content">
                                <SystemGraph
                                    bind:this={graphRef}
                                    elements={graphElements}
                                    {styleConfig}
                                    edgeFilter="all"
                                    {selectedNodeId}
                                    selectedEdgeId={hoveredConnectionId || selectedEdgeId}
                                    neutralEdges={true}
                                    on:nodeSelected={handleNodeSelected}
                                    on:edgeSelected={handleEdgeSelected}
                                />
                            </div>
                        </div>
                    </Pane>
                    <Pane>
                        <div class="h-full flex flex-col overflow-hidden bg-white">
                            <Tabs tabStyle="underline" classes={{
                                    active: "p-4 text-blue-600 border-blue-600 dark:text-blue-500 dark:border-blue-500",
                                }}>
                                <TabItem open class="flex-1" classes={{ button: "w-full" }}>
                                    {#snippet titleSlot()}
                                        <div class="flex items-center gap-2">
                                            <ChartPieSolid size="md" />
                                            Overview
                                        </div>
                                    {/snippet}
                                    <div class="pane-scroll">
                                        <div class="space-y-4">
                                            <div class="metric-grid">
                                                <div class="metric-card">
                                                    <p class="metric-label">Selected services</p>
                                                    <p class="metric-value">{selectedServicesCount()}</p>
                                                </div>
                                                <div class="metric-card">
                                                    <p class="metric-label">Selected connections</p>
                                                    <p class="metric-value">{selectedConnectionsCount()}</p>
                                                </div>
                                            </div>

                                            <div class="space-y-2">
                                                <h3 class="text-sm font-semibold text-gray-800">
                                                    Global expected statuses
                                                </h3>
                                                <input
                                                    type="text"
                                                    class={`w-full rounded border px-3 py-2 text-sm ${
                                                        getInvalidStatuses(globalStatusInput).length > 0
                                                            ? "border-red-300 bg-red-50"
                                                            : "border-gray-300 bg-white"
                                                    }`}
                                                    placeholder="2xx, 4xx"
                                                    bind:value={globalStatusInput}
                                                />
                                                <p class="text-xs text-gray-500">
                                                    Supports classes and exact codes: 2xx, 4xx, 404, 500.
                                                </p>
                                            </div>

                                            <div class="space-y-2">
                                                <div class="flex gap-2">
                                                    <Button
                                                        color="blue"
                                                        size="sm"
                                                        onclick={saveConfig}
                                                        disabled={saving || runningDynamic}
                                                    >
                                                        {saving ? "Saving..." : "Save Config"}
                                                    </Button>
                                                    <Button
                                                        color="green"
                                                        size="sm"
                                                        onclick={saveConfigAndRunDynamicAnalysis}
                                                        disabled={saving || runningDynamic}
                                                    >
                                                        {runningDynamic
                                                            ? "Starting..."
                                                            : "Save Config & Run Dynamic Analysis"}
                                                    </Button>
                                                </div>
                                                {#if saveMessage}
                                                    <p class="rounded border border-blue-200 bg-blue-50 px-2 py-1 text-sm text-blue-700">
                                                        {saveMessage}
                                                    </p>
                                                {/if}
                                            </div>
                                        </div>
                                    </div>
                                </TabItem>

                                <TabItem class="flex-1" classes={{ button: "w-full" }}>
                                    {#snippet titleSlot()}
                                        <div class="flex items-center gap-2">
                                            <ClipboardListSolid size="md" />
                                            Selection
                                        </div>
                                    {/snippet}
                                    <div class="pane-scroll">
                                        <section class="mb-5">
                                            <h3 class="text-sm font-semibold text-gray-800 mb-2">
                                                Services
                                            </h3>
                                            <div class="rounded border border-gray-200 bg-gray-50 max-h-52 overflow-y-auto">
                                                {#each serviceEntries() as serviceName}
                                                    {@const isOrigin = serviceName === originService}
                                                    <label
                                                        class={`flex items-center gap-2 px-3 py-2 border-b border-gray-200 last:border-b-0 ${
                                                            isOrigin
                                                                ? "bg-gray-100 text-gray-500"
                                                                : "bg-white"
                                                        }`}
                                                    >
                                                        <input
                                                            type="checkbox"
                                                            checked={serviceSelection[serviceName]}
                                                            onchange={(event) =>
                                                                handleServiceToggle(
                                                                    serviceName,
                                                                    event.currentTarget.checked,
                                                                )}
                                                            disabled={isOrigin}
                                                        />
                                                        <span class="text-sm text-gray-800">{serviceName}</span>
                                                        {#if isOrigin}
                                                            <span
                                                                class="ml-auto rounded bg-blue-100 text-blue-900 px-2 py-0.5 text-xs"
                                                            >
                                                                origin
                                                            </span>
                                                        {/if}
                                                    </label>
                                                {/each}
                                            </div>
                                        </section>

                                        <section>
                                            <h3 class="text-sm font-semibold text-gray-800 mb-2">
                                                Connections and overrides
                                            </h3>
                                            <div class="rounded border border-gray-200 overflow-hidden">
                                                <div class="relative max-h-[440px] overflow-y-auto overflow-x-hidden">
                                                    <table class="w-full table-fixed text-sm border-separate border-spacing-0">
                                                        <colgroup>
                                                            <col style="width: 56px;" />
                                                            <col style="width: 44%;" />
                                                            <col style="width: calc(56% - 56px);" />
                                                        </colgroup>
                                                        <thead class="bg-gray-100">
                                                            <tr class="text-left text-gray-700">
                                                                <th class="sticky top-0 z-20 bg-gray-100 px-3 py-2 shadow-[0_1px_0_0_#e5e7eb]">Use</th>
                                                                <th class="sticky top-0 z-20 bg-gray-100 px-3 py-2 shadow-[0_1px_0_0_#e5e7eb]">Connection</th>
                                                                <th class="sticky top-0 z-20 bg-gray-100 px-3 py-2 shadow-[0_1px_0_0_#e5e7eb]">Expected statuses</th>
                                                            </tr>
                                                        </thead>
                                                        <tbody>
                                                            {#each connectionEditors as connection}
                                                                {@const invalidStatuses = getInvalidStatuses(connection.statusInput)}
                                                                {@const serviceDisabled = !serviceSelection[connection.source] || !serviceSelection[connection.target]}
                                                                <tr
                                                                    class={`border-t border-gray-200 ${
                                                                        serviceDisabled
                                                                            ? "bg-gray-100 text-gray-500"
                                                                            : hoveredConnectionId === connection.id ||
                                                                                selectedEdgeId === connection.id
                                                                              ? "bg-blue-50"
                                                                              : "bg-white"
                                                                    }`}
                                                                    onmouseenter={() =>
                                                                        handleConnectionHoverStart(
                                                                            connection.id,
                                                                        )}
                                                                    onmouseleave={() =>
                                                                        handleConnectionHoverEnd(
                                                                            connection.id,
                                                                        )}
                                                                >
                                                                    <td class="px-3 py-2 align-top text-center">
                                                                        <input
                                                                            type="checkbox"
                                                                            checked={connection.selected}
                                                                            disabled={serviceDisabled}
                                                                            onchange={(event) =>
                                                                                handleConnectionToggle(
                                                                                    connection.id,
                                                                                    event.currentTarget.checked,
                                                                                )}
                                                                        />
                                                                    </td>
                                                                    <td class="px-3 py-2 pl-2 align-top">
                                                                        <div
                                                                            class="font-medium text-gray-900 truncate"
                                                                            title={fullConnectionLabel(connection)}
                                                                        >
                                                                            {connection.source} → {connection.target}
                                                                        </div>
                                                                        <div
                                                                            class="text-xs text-gray-600 break-all"
                                                                            title={fullConnectionLabel(connection)}
                                                                        >
                                                                            {connection.method} {connection.path}
                                                                        </div>
                                                                        {#if hoveredConnectionId === connection.id}
                                                                            <div class="mt-1 rounded border border-blue-200 bg-blue-50 px-2 py-1 text-xs text-blue-900 break-all">
                                                                                {fullConnectionLabel(connection)}
                                                                            </div>
                                                                        {/if}
                                                                    </td>
                                                                    <td class="px-3 py-2">
                                                                        <input
                                                                            type="text"
                                                                            value={connection.statusInput}
                                                                            disabled={!connection.selected || serviceDisabled}
                                                                            class={`w-full rounded border px-2 py-1.5 text-sm ${
                                                                                invalidStatuses.length > 0
                                                                                    ? "border-red-300 bg-red-50"
                                                                                    : "border-gray-300 bg-white"
                                                                            } disabled:bg-gray-100 disabled:text-gray-500`}
                                                                            oninput={(event) =>
                                                                                handleConnectionStatusChange(
                                                                                    connection.id,
                                                                                    event.currentTarget.value,
                                                                                )}
                                                                        />
                                                                    </td>
                                                                </tr>
                                                            {/each}
                                                        </tbody>
                                                    </table>
                                                </div>
                                            </div>
                                        </section>
                                    </div>
                                </TabItem>
                            </Tabs>
                        </div>
                    </Pane>
                </SplitPane>
            </div>
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

    .center {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 100%;
        height: 100%;
    }

    .container-wrapper {
        flex: 1;
        display: flex;
        min-width: 0;
    }

    .editor-container {
        flex: 1;
        background-color: #ffffff;
        border-radius: 0.5rem;
        border: 1px solid #e5e7eb;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        display: flex;
        overflow: hidden;
    }

    .graph-side {
        display: flex;
        flex-direction: column;
        height: 100%;
        min-width: 0;
    }

    .graph-header {
        border-bottom: 1px solid #e5e7eb;
        padding: 0.75rem 1rem;
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 0.5rem;
    }

    .graph-content {
        flex: 1;
        min-height: 0;
    }

    .pane-scroll {
        padding: 1rem;
        overflow-y: auto;
        max-height: calc(100vh - 180px);
    }

    .metric-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 0.75rem;
    }

    .metric-card {
        background: #f9fafb;
        border: 1px solid #e5e7eb;
        border-radius: 0.5rem;
        padding: 0.75rem;
    }

    .metric-label {
        font-size: 0.75rem;
        color: #6b7280;
        margin-bottom: 0.25rem;
    }

    .metric-value {
        font-size: 1.25rem;
        font-weight: 700;
        color: #111827;
    }
</style>
