<script>
    import {
        Search,
        Button,
        Table,
        TableHead,
        TableHeadCell,
        TableBody,
        TableBodyRow,
        TableBodyCell,
        Badge,
        Dropdown,
    } from "flowbite-svelte";
    import {
        CirclePlusOutline,
        PlaySolid,
        ChevronLeftOutline,
        ChevronRightOutline,
        ChevronDownOutline,
        CaretUpSolid,
        CaretDownSolid,
    } from "flowbite-svelte-icons";
    import DetailedConfigModal from "$lib/components/configcomponents/DetailedConfigModal.svelte";
    import NewConfigModal from "$lib/components/configcomponents/NewConfigModal.svelte";
    import { CheckCircleSolid } from "flowbite-svelte-icons";
    import { goto } from "$app/navigation";
    import { onMount } from "svelte";
    import { SvelteMap } from "svelte/reactivity";
    import { getOverviewInfo } from "$lib/reportOverViewUtils.js";
    import NewConfigModalStepper from "$lib/components/configcomponents/NewConfigModalStepper.svelte";

    let { data } = $props();

    let systems = data.systems;
    let configs = $state(data.configs || []);
    let configStats = new SvelteMap();

    let showNewConfigModal = $state(false);
    let searchTerm = $state("");
    let showDetailedModal = $state(false);
    let selectedConfig = $state(null);

    const ITEMS_PER_PAGE = 5;
    let currentPage = $state(1);
    let sortBy = $state("latest-run");
    let sortDirection = $state("desc");

    const sortOptions = [
        { value: "test-files", label: "Amount of Test Files" },
        { value: "services", label: "Amount of Services" },
        { value: "latest-run", label: "Latest Run" },
        { value: "duration", label: "Last Run Duration" },
        { value: "connection-coverage", label: "Connection Coverage" },
        { value: "status-coverage", label: "Status Coverage" },
        { value: "request-element-coverage", label: "Request Element Coverage" },
        { value: "request-value-partition-coverage", label: "Request Value Partition Coverage" },
        { value: "response-field-coverage", label: "Response Field Coverage" },
        { value: "response-value-partition-coverage", label: "Response Value Partition Coverage" },
        { value: "total-connections", label: "Total Connections" },
    ];

    function getSortLabel(value) {
        return sortOptions.find((option) => option.value === value)?.label;
    }

    let selectedRuns = $derived(
        selectedConfig?.configName
            ? (configStats.get(selectedConfig.configName).runs ?? [])
            : [],
    );

    // Load stats for all configs on mount
    onMount(() => {
        configs.forEach((cfg) => {
            if (!configStats.has(cfg.configName)) {
                loadConfigStats(cfg.configName);
            }
        });
    });

    let filteredConfigs = $derived(
        (() => {
            let filtered = configs.filter((c) => {
                const searchLower = searchTerm.toLowerCase();

                // Search in config name
                if (c.configName.toLowerCase().includes(searchLower)) {
                    return true;
                }

                // Search in test file names
                if (
                    c.tests?.some((t) =>
                        t.fileName.toLowerCase().includes(searchLower),
                    )
                ) {
                    return true;
                }

                // Search in service names
                if (
                    c.services?.some((s) =>
                        s.serviceName.toLowerCase().includes(searchLower),
                    )
                ) {
                    return true;
                }

                return false;
            });

            // Apply sorting
            filtered.sort((a, b) => {
                const statsA = configStats.get(a.configName);
                const statsB = configStats.get(b.configName);

                let result = 0;

                switch (sortBy) {
                    case "test-files":
                        result =
                            (b.tests?.length || 0) - (a.tests?.length || 0);
                        break;
                    case "services":
                        result =
                            (b.services?.length || 0) -
                            (a.services?.length || 0);
                        break;
                    case "latest-run":
                        const dateA = statsA?.runs?.[0]?.timestamp
                            ? new Date(statsA.runs[0].timestamp).getTime()
                            : 0;
                        const dateB = statsB?.runs?.[0]?.timestamp
                            ? new Date(statsB.runs[0].timestamp).getTime()
                            : 0;
                        result = dateB - dateA;
                        break;
                    case "duration":
                        const durationA = statsA?.overview?.lastRunDuration;
                        const durationB = statsB?.overview?.lastRunDuration;
                        const hasDurationA =
                            durationA !== null && durationA !== undefined;
                        const hasDurationB =
                            durationB !== null && durationB !== undefined;

                        if (!hasDurationA && !hasDurationB) {
                            return 0;
                        }

                        if (!hasDurationA) {
                            return 1;
                        }

                        if (!hasDurationB) {
                            return -1;
                        }

                        return sortDirection === "asc"
                            ? durationA - durationB
                            : durationB - durationA;
                    case "connection-coverage":
                        const coverageA =
                            statsA?.overview
                                ?.highestConnectionCoveragePercentage || 0;
                        const coverageB =
                            statsB?.overview
                                ?.highestConnectionCoveragePercentage || 0;
                        result = coverageB - coverageA;
                        break;
                    case "status-coverage":
                        const statusCoverageA =
                            statsA?.overview
                                ?.statusCodeClassCoveragePercentage || 0;
                        const statusCoverageB =
                            statsB?.overview
                                ?.statusCodeClassCoveragePercentage || 0;
                        result = statusCoverageB - statusCoverageA;
                        break;
                    case "request-element-coverage":
                        const requestElementCoverageA =
                            statsA?.overview
                                ?.requestElementMeanHierarchicalCoveragePercentage || 0;
                        const requestElementCoverageB =
                            statsB?.overview
                                ?.requestElementMeanHierarchicalCoveragePercentage || 0;
                        result = requestElementCoverageB - requestElementCoverageA;
                        break;
                    case "request-value-partition-coverage":
                        const requestValuePartitionCoverageA =
                            statsA?.overview
                                ?.requestValuePartitionMeanHierarchicalCoveragePercentage || 0;
                        const requestValuePartitionCoverageB =
                            statsB?.overview
                                ?.requestValuePartitionMeanHierarchicalCoveragePercentage || 0;
                        result = requestValuePartitionCoverageB - requestValuePartitionCoverageA;
                        break;
                    case "response-field-coverage":
                        const responseFieldCoverageA =
                            statsA?.overview
                                ?.responseFieldMeanHierarchicalCoveragePercentage || 0;
                        const responseFieldCoverageB =
                            statsB?.overview
                                ?.responseFieldMeanHierarchicalCoveragePercentage || 0;
                        result = responseFieldCoverageB - responseFieldCoverageA;
                        break;
                    case "response-value-partition-coverage":
                        const responseValuePartitionCoverageA =
                            statsA?.overview
                                ?.responseValuePartitionMeanHierarchicalCoveragePercentage || 0;
                        const responseValuePartitionCoverageB =
                            statsB?.overview
                                ?.responseValuePartitionMeanHierarchicalCoveragePercentage || 0;
                        result = responseValuePartitionCoverageB - responseValuePartitionCoverageA;
                        break;
                    case "total-connections":
                        const connectionsA =
                            statsA?.overview?.totalConnections || 0;
                        const connectionsB =
                            statsB?.overview?.totalConnections || 0;
                        result = connectionsB - connectionsA;
                        break;
                    default:
                        result = 0;
                }

                return sortDirection === "asc" ? -result : result;
            });

            return filtered;
        })(),
    );

    let paginatedConfigs = $derived(
        (() => {
            const start = (currentPage - 1) * ITEMS_PER_PAGE;
            return filteredConfigs.slice(start, start + ITEMS_PER_PAGE);
        })(),
    );

    let totalPages = $derived(
        Math.ceil(filteredConfigs.length / ITEMS_PER_PAGE),
    );

    async function loadConfigStats(configName) {
        try {
            const response = await fetch(
                `/api/configs/${encodeURIComponent(configName)}/runs`,
            );
            if (!response.ok) throw new Error("Failed to fetch runs");
            const runs = await response.json();

            let lastRun = null;
            if (runs.length > 0) {
                lastRun = runs[0]; // Already sorted newest first
            }

            const overview = getOverviewInfo(runs);

            configStats.set(configName, {
                runs: runs,
                overview: overview,
            });
        } catch (error) {
            console.error(`Error loading stats for ${configName}:`, error);
            configStats.set(configName, { runs: [], overview: {} });
        }
    }

    function handleCardClick(cfg) {
        selectedConfig = cfg;
        showDetailedModal = true;
    }

    function handleCreate(event) {
        const cfg = event.detail;
    }

    function runCoverage(cfg, event) {
        event.stopPropagation();
        goto(`/run/${encodeURIComponent(cfg.configName)}`);
    }

    function handleConfigSaved(newConfig) {
        // Add the new config to the configs array
        configs = [...configs, newConfig];
        // Load stats for the new config
        loadConfigStats(newConfig.configName);
    }

    function formatTimestamp(timestamp) {
        if (!timestamp) return "N/A";
        const date = new Date(timestamp);
        return date.toLocaleString("en-US", {
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        });
    }

    function formatDuration(durationMs) {
        if (durationMs === null || durationMs === undefined) return "N/A";

        const totalMs = Math.max(0, Math.round(durationMs));
        const totalSeconds = Math.floor(totalMs / 1000);
        const hours = Math.floor(totalSeconds / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const seconds = totalSeconds % 60;

        if (hours > 0) {
            return `${hours}h ${minutes}m ${seconds}s`;
        }

        if (minutes > 0) {
            return `${minutes}m ${seconds}s`;
        }

        if (seconds > 0) {
            return `${seconds}s`;
        }

        return `${totalMs}ms`;
    }

    function previousPage() {
        if (currentPage > 1) {
            currentPage--;
        }
    }

    function nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
        }
    }
</script>

<div
    class="flex flex-col w-full items-center justify-self-center min-h-screen bg-gray-50 pt-8 px-4"
>
    <div class="w-full max-w-7xl mb-6 flex gap-3 items-center">
        <Search
            size="lg"
            clearable
            bind:value={searchTerm}
            placeholder="e.g. Config Name, Service Name, Testfile"
        />
        <div class="flex items-center gap-2">
            <span class="text-sm font-medium text-gray-700 whitespace-nowrap"
                >Sort by:</span
            >
            <Button
                id="sort-btn"
                class="w-52 overflow-hidden"
                size="md"
                color="light"
                outline
            >
                <span class="flex w-full items-center justify-between gap-2">
                    <span class="min-w-0 truncate text-left"
                        >{getSortLabel(sortBy)}</span
                    >
                    <ChevronDownOutline class="h-4 w-4 shrink-0" />
                </span>
            </Button>
            <Dropdown simple triggeredBy="#sort-btn" class="w-56 p-1 text-sm">
                {#each sortOptions as option}
                    <li>
                        <button
                            type="button"
                            class="w-full flex items-center gap-2 rounded px-2 py-1.5 text-left hover:bg-gray-100"
                            onclick={() => {
                                sortBy = option.value;
                                currentPage = 1;
                            }}
                        >
                            {#if sortBy === option.value}
                                <CheckCircleSolid
                                    class="h-4 w-4 text-blue-600"
                                />
                            {:else}
                                <span class="h-4 w-4"></span>
                            {/if}
                            <span>{option.label}</span>
                        </button>
                    </li>
                {/each}
            </Dropdown>
            <Button
                color="light"
                outline
                size="md"
                class="px-2"
                title={sortDirection === "asc"
                    ? "Currently sorted ascending (click to sort descending)"
                    : "Currently sorted descending (click to sort ascending)"}
                aria-label={sortDirection === "asc"
                    ? "Sort ascending"
                    : "Sort descending"}
                onclick={() => {
                    sortDirection = sortDirection === "asc" ? "desc" : "asc";
                    currentPage = 1;
                }}
            >
                {#if sortDirection === "asc"}
                    <CaretUpSolid class="h-4 w-4" />
                {:else}
                    <CaretDownSolid class="h-4 w-4" />
                {/if}
            </Button>
        </div>

        <Button
            color="blue"
            size="md"
            onclick={() => (showNewConfigModal = true)}
            class="whitespace-nowrap"
        >
            New Config&nbsp;
            <CirclePlusOutline class="shrink-0 h-6 w-6" />
        </Button>
    </div>

    <div class="w-full max-w-7xl">
        <div class="overflow-x-auto">
            <div class="space-y-3">
                {#each paginatedConfigs as cfg}
                    {@const stats = configStats.get(cfg.configName)}
                    <button
                        type="button"
                        onclick={() => handleCardClick(cfg)}
                        class="w-full text-left bg-white border border-gray-200 rounded-lg p-4 cursor-pointer hover:bg-gray-50 transition"
                    >
                        <div class="flex gap-4 items-start">
                            <!-- Left Column: Name, Services, and Test Files -->
                           <div class="w-[34%] min-w-[220px] max-w-[380px] shrink-0">
                                <h3
                                    class="text-lg font-semibold text-gray-900 mb-3"
                                >
                                    {cfg.configName}
                                </h3>

                                <!-- Services -->
                                <div class="mb-3">
                                    <p
                                        class="text-xs font-semibold text-gray-600 mb-2"
                                    >
                                        SERVICES ({cfg.services?.length || 0})
                                    </p>
                                   <div class="flex flex-wrap gap-1.5">
                                        {#if cfg.services && cfg.services.length > 0}
                                            {#each cfg.services as service}
                                                <Badge
                                                   class="text-[10px] px-2 py-1"
                                                   color={cfg.tests?.some(
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
                                       {:else}
                                           <span class="text-sm text-gray-400">
                                               No services
                                           </span>
                                       {/if}
                                   </div>
                               </div>

                               <!-- Test files -->
                               <div>
                                   <p
                                       class="text-xs font-semibold text-gray-600 mb-2"
                                   >
                                       TEST FILES ({cfg.tests?.length || 0})
                                   </p>
                                   <div class="flex flex-wrap gap-1.5">
                                       {#if cfg.tests && cfg.tests.length > 0}
                                           {#each cfg.tests as test}
                                               <Badge
                                                   class="text-[10px] px-2 py-1"
                                                   color="purple"
                                               >
                                                   {test.fileName}
                                               </Badge>
                                           {/each}
                                       {:else}
                                           <span class="text-sm text-gray-400">
                                               No tests
                                           </span>
                                       {/if}
                                   </div>
                               </div>
                           </div>

                           <!-- Middle Column: General Information -->
                           <div class="border-l pl-4 min-w-[160px] w-[20%] shrink-0">
                               <p
                                   class="text-xs font-semibold text-gray-600 mb-3"
                               >
                                   GENERAL INFORMATION
                               </p>
                               <div class="space-y-3">
                                   <div>
                                       <p class="text-xs text-gray-500">
                                           Total Runs
                                       </p>
                                       <p
                                           class="text-lg font-bold text-gray-900"
                                       >
                                           {stats?.runs?.length ?? 0}
                                       </p>
                                   </div>
                                   <div>
                                       <p class="text-xs text-gray-500">
                                           Total Connections
                                       </p>
                                       {#if stats?.overview?.totalConnections !== undefined}
                                           <p
                                               class="text-lg font-bold text-gray-900"
                                           >
                                               {stats.overview
                                                   .totalConnections}
                                            </p>
                                       {:else}
                                           <p class="text-sm text-gray-400">
                                               -
                                           </p>
                                       {/if}
                                   </div>
                               </div>
                           </div>

                           <!-- Right Column: Last Run Information -->
                           <div class="border-l pl-4 flex-1 min-w-[300px]">
                               <p
                                   class="text-xs font-semibold text-gray-600 mb-3"
                               >
                                   LAST RUN
                               </p>
                               <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                                   <div>
                                       <p class="text-xs text-gray-500">
                                           Timestamp
                                       </p>
                                       {#if stats?.runs?.length > 0}
                                           <p
                                               class="text-sm font-medium text-gray-900"
                                           >
                                               {formatTimestamp(
                                                   stats?.runs[0]?.timestamp,
                                               )}
                                            </p>
                                       {:else}
                                           <p class="text-sm text-gray-400">
                                               Never
                                           </p>
                                       {/if}
                                   </div>
                                   <div>
                                       <p class="text-xs text-gray-500">
                                           Last Run Duration
                                       </p>
                                       <p
                                           class="text-sm font-medium text-gray-900"
                                       >
                                           {formatDuration(
                                               stats?.overview
                                                   ?.lastRunDuration,
                                           )}
                                       </p>
                                   </div>
                                   <div>
                                       <p class="text-xs text-gray-500">
                                           Connection Coverage
                                       </p>
                                       {#if stats?.overview?.highestConnectionCoveragePercentage != null}
                                           <p
                                               class="text-lg font-bold text-blue-600"
                                           >
                                               {stats.overview.highestConnectionCoveragePercentage.toFixed(
                                                   2,
                                               )}%
                                            </p>
                                       {:else}
                                           <p class="text-sm text-gray-400">
                                               -
                                           </p>
                                       {/if}
                                   </div>
                                   <div>
                                       <p class="text-xs text-gray-500">
                                           Status Coverage
                                       </p>
                                       {#if stats?.overview?.statusCodeClassCoveragePercentage != null}
                                           <p
                                               class="text-lg font-bold text-blue-600"
                                           >
                                               {stats.overview.statusCodeClassCoveragePercentage.toFixed(
                                                   2,
                                               )}%
                                            </p>
                                       {:else}
                                           <p class="text-sm text-gray-400">
                                               -
                                           </p>
                                       {/if}
                                   </div>
                                   <div>
                                       <p class="text-xs text-gray-500">
                                           Req. Elements
                                       </p>
                                       {#if stats?.overview?.requestElementMeanHierarchicalCoveragePercentage != null}
                                           <p
                                               class="text-base font-bold text-blue-600"
                                           >
                                               {stats.overview.requestElementMeanHierarchicalCoveragePercentage.toFixed(
                                                   2,
                                               )}%
                                           </p>
                                       {:else}
                                           <p class="text-sm text-gray-400">-</p>
                                       {/if}
                                   </div>
                                   <div>
                                       <p class="text-xs text-gray-500">
                                           Req. Values
                                       </p>
                                       {#if stats?.overview?.requestValuePartitionMeanHierarchicalCoveragePercentage != null}
                                           <p
                                               class="text-base font-bold text-amber-600"
                                           >
                                               {stats.overview.requestValuePartitionMeanHierarchicalCoveragePercentage.toFixed(
                                                   2,
                                               )}%
                                           </p>
                                       {:else}
                                           <p class="text-sm text-gray-400">-</p>
                                       {/if}
                                   </div>
                                   <div>
                                       <p class="text-xs text-gray-500">
                                           Resp. Fields
                                       </p>
                                       {#if stats?.overview?.responseFieldMeanHierarchicalCoveragePercentage != null}
                                           <p
                                               class="text-base font-bold text-cyan-600"
                                           >
                                               {stats.overview.responseFieldMeanHierarchicalCoveragePercentage.toFixed(
                                                   2,
                                               )}%
                                           </p>
                                       {:else}
                                           <p class="text-sm text-gray-400">-</p>
                                       {/if}
                                   </div>
                                   <div>
                                       <p class="text-xs text-gray-500">
                                           Resp. Values
                                       </p>
                                       {#if stats?.overview?.responseValuePartitionMeanHierarchicalCoveragePercentage != null}
                                           <p
                                               class="text-base font-bold text-violet-600"
                                           >
                                               {stats.overview.responseValuePartitionMeanHierarchicalCoveragePercentage.toFixed(
                                                   2,
                                               )}%
                                           </p>
                                       {:else}
                                           <p class="text-sm text-gray-400">-</p>
                                       {/if}
                                   </div>
                               </div>
                           </div>
                        </div>
                    </button>
                {/each}
            </div>
        </div>

        {#if filteredConfigs.length === 0}
            <div class="text-center py-12">
                <p class="text-gray-500">No configurations found</p>
            </div>
        {/if}

        <!-- Pagination -->
        {#if totalPages > 1}
            <div class="flex items-center justify-between mt-6">
                <div class="text-sm text-gray-600">
                    Page {currentPage} of {totalPages} ({filteredConfigs.length}
                    total)
                </div>
                <div class="flex gap-2">
                    <Button
                        size="sm"
                        disabled={currentPage === 1}
                        onclick={previousPage}
                    >
                        <ChevronLeftOutline size="sm" />
                    </Button>
                    <Button
                        size="sm"
                        disabled={currentPage === totalPages}
                        onclick={nextPage}
                    >
                        <ChevronRightOutline size="sm" />
                    </Button>
                </div>
            </div>
        {/if}
    </div>
</div>

<NewConfigModalStepper
    bind:open={showNewConfigModal}
    {systems}
    onConfigSaved={handleConfigSaved}
/>
<DetailedConfigModal
    bind:showModal={showDetailedModal}
    config={selectedConfig}
    runs={selectedRuns}
/>
