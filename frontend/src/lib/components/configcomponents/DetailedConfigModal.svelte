<script>
    import {
        Modal,
        Button,
        Table,
        TableHead,
        TableHeadCell,
        TableBody,
        TableBodyRow,
        TableBodyCell,
        Badge,
    } from "flowbite-svelte";
    import { CheckCircleSolid, PlaySolid, RefreshOutline } from "flowbite-svelte-icons";
    import { goto } from "$app/navigation";
    import { resolveEntryService } from "$lib/entryService.js";

    let { config = $bindable(), showModal = $bindable(), runs } = $props();

    let entryService = $derived(resolveEntryService(config));

    let loading = $state(false);
    let error = $state(null);

    // Fetch runs when modal opens
    // $effect(() => {
    //     if (showModal && config?.configName) {
    //         fetchRuns();
    //     }
    // });

    async function fetchRuns() {
        loading = true;
        error = null;

        try {
            const response = await fetch(
                `/api/configs/${encodeURIComponent(config.configName)}/runs?metaDataOnly=true`,
            );
            if (!response.ok) {
                throw new Error("Failed to fetch runs");
            }
            runs = await response.json();
        } catch (err) {
            console.error("Error fetching runs:", err);
            error = err.message;
        } finally {
            loading = false;
        }
    }

    function formatDuration(ms) {
        if (!ms) return "N/A";
        const seconds = Math.floor(ms / 1000);
        const minutes = Math.floor(seconds / 60);
        const remainingSeconds = seconds % 60;
        if (minutes > 0) {
            return `${minutes}m ${remainingSeconds}s`;
        }
        return `${seconds}s`;
    }

    function formatTimestamp(timestamp) {
        const date = new Date(timestamp);
        return date.toLocaleString("en-US", {
            year: "numeric",
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        });
    }

    function viewReport(run) {
        showModal = false;
        goto(
            `/report/${encodeURIComponent(config.configName)}/${encodeURIComponent(run.id)}`,
            { state: { run } },
        );
    }

    function hasStaticAnalysis(run) {
        return Boolean(
            run?.staticAnalysis && Object.keys(run.staticAnalysis).length > 0,
        ) || Boolean(
            run?.microserviceSystem && Object.keys(run.microserviceSystem).length > 0,
        ) || Boolean(
            run?.report?.microserviceSystem &&
                Object.keys(run.report.microserviceSystem).length > 0,
        );
    }

    function getAdditionalServices() {
        const seen = new Set((config?.services || []).map((service) => service.serviceName));
        const additional = [];

        for (const runItem of runs || []) {
            const staticServices = runItem?.staticAnalysis?.services || [];
            const microserviceServices = runItem?.microserviceSystem?.microservices || [];

            for (const service of [...staticServices, ...microserviceServices]) {
                const name = service?.name || service?.serviceName;
                if (name && !seen.has(name)) {
                    seen.add(name);
                    additional.push(name);
                }
            }
        }

        return additional;
    }

    function getVisibleServices() {
        return [
            ...(config?.services || []),
            ...getAdditionalServices().map((name) => ({ serviceName: name })),
        ];
    }

    function viewStaticAnalysis(run) {
        showModal = false;
        goto(
            `/static-analysis/${encodeURIComponent(config.configName)}/${encodeURIComponent(run.id)}`,
            { state: { run } },
        );
    }

    function rerunStaticAnalysis() {
        showModal = false;
        goto(
            `/static-analysis/setup/${encodeURIComponent(config.configName)}?force=1`,
        );
    }

    function runConfig() {
        showModal = false;
        goto(`/run/${encodeURIComponent(config.configName)}`);
    }

</script>

<Modal bind:open={showModal} size="xl" class="w-full max-w-6xl">
    {#if config}
        <div class="flex items-center justify-between mb-4">
            <h2 class="text-2xl font-bold">{config.configName}</h2>
            <!-- Run Config comes first so the modal opens with the focus on it;
                 the rerun button is only shown to its left -->
            <div class="flex items-center gap-3 mr-10">
                <Button color="green" class="order-2" onclick={runConfig}>
                    <PlaySolid class="w-4 h-4 mr-2" />
                    Run Config
                </Button>
                <Button
                    color="sky"
                    class="order-1"
                    onclick={rerunStaticAnalysis}
                    title={entryService
                        ? `Analyses the architecture again, starting from ${entryService}`
                        : "Add a test to this config so the analysis knows which service to start from"}
                    disabled={!entryService}
                >
                    <RefreshOutline class="w-4 h-4 mr-2" />
                    Rerun Static Analysis
                </Button>
            </div>
        </div>

        <!-- Config Details Section -->
        <div class="mb-6">
            <h3 class="text-lg font-semibold mb-2">Configuration Details</h3>
            <div class="grid grid-cols-2 gap-4">
                <div>
                    <h4 class="font-medium text-sm text-gray-500 mb-1">
                        Services ({config.services?.length || 0})
                    </h4>
                    <div
                        class="bg-gray-50 p-3 rounded-lg max-h-32 overflow-y-auto"
                    >
                        {#if getVisibleServices().length > 0}
                            <div class="flex flex-wrap gap-2">
                                {#each getVisibleServices() as service}
                                    <Badge
                                        color={config.tests?.some(
                                            (t) =>
                                                t.serviceName ===
                                                service.serviceName,
                                        )
                                            ? "indigo"
                                            : "sky"}
                                    >
                                        {service.serviceName}
                                    </Badge>
                                {/each}
                            </div>
                        {:else}
                            <p class="text-sm text-gray-400">
                                No services selected
                            </p>
                        {/if}
                    </div>
                </div>
                <div>
                    <h4 class="font-medium text-sm text-gray-500 mb-1">
                        Tests ({config.tests?.length || 0})
                    </h4>
                    <div
                        class="bg-gray-50 p-3 rounded-lg max-h-32 overflow-y-auto"
                    >
                        {#if config.tests && config.tests.length > 0}
                            <div class="flex flex-wrap gap-2">
                                {#each config.tests as test}
                                    <Badge color="purple">
                                        {test.fileName}
                                    </Badge>
                                {/each}
                            </div>
                        {:else}
                            <p class="text-sm text-gray-400">
                                No tests selected
                            </p>
                        {/if}
                    </div>
                </div>
            </div>
        </div>

        <!-- Run History Section -->
        <div>
            <h3 class="text-lg font-semibold mb-2">Run History</h3>

            {#if loading}
                <div class="text-center py-8">
                    <p class="text-gray-500">Loading runs...</p>
                </div>
            {:else if error}
                <div class="bg-red-50 text-red-800 p-4 rounded-lg">
                    <p>Error: {error}</p>
                </div>
            {:else if runs.length === 0}
                <div class="text-center py-8 bg-gray-50 rounded-lg">
                    <p class="text-gray-500">
                        No runs yet for this configuration
                    </p>
                </div>
            {:else}
                <div class="overflow-x-auto">
                    <Table>
                        <TableHead>
                            <TableHeadCell>Status</TableHeadCell>
                            <TableHeadCell>Timestamp</TableHeadCell>
                            <TableHeadCell>Test Duration</TableHeadCell>
                            <TableHeadCell>Action</TableHeadCell>
                        </TableHead>
                        <TableBody>
                            {#each runs as run}
                                <TableBodyRow>
                                    <TableBodyCell>
                                        {#if run.report != null && run.report != undefined}
                                            <Badge
                                                color="green"
                                                class="flex items-center gap-1 w-fit"
                                            >
                                                <CheckCircleSolid size="sm" />
                                                Full Analysis
                                            </Badge>
                                        {:else if hasStaticAnalysis(run)}
                                            <Badge
                                                color="blue"
                                                class="flex items-center gap-1 w-fit"
                                            >
                                                <CheckCircleSolid size="sm" />
                                                Static Analysis
                                            </Badge>
                                        {:else}
                                            <Badge
                                                color="red"
                                                class="flex items-center gap-1 w-fit"
                                            >
                                                <CheckCircleSolid size="sm" />
                                                Failed
                                            </Badge>
                                        {/if}
                                    </TableBodyCell>
                                    <TableBodyCell
                                        >{formatTimestamp(
                                            run.timestamp,
                                        )}</TableBodyCell
                                    >
                                    <TableBodyCell
                                        >{formatDuration(
                                            run.metaData?.testDurationMillis,
                                        )}</TableBodyCell
                                    >
                                    <TableBodyCell>
                                        <div class="flex items-center gap-2">
                                            {#if run.report != null && run.report !== undefined}
                                                <Button color="blue"
                                                        size="xs"
                                                        onclick={() => viewReport(run)}>
                                                    View Report
                                                </Button>
                                            {/if}

                                            {#if hasStaticAnalysis(run)}
                                                <Button
                                                    size="xs"
                                                    color="alternative"
                                                    onclick={() =>
                                                        viewStaticAnalysis(run)}
                                                >
                                                    View Static Analysis
                                                </Button>
                                            {/if}
                                        </div>
                                    </TableBodyCell>
                                </TableBodyRow>
                            {/each}
                        </TableBody>
                    </Table>
                </div>
            {/if}
        </div>
    {/if}
</Modal>
