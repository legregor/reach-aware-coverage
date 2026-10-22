<script>
    import { Button, Dropdown, DropdownItem } from "flowbite-svelte";
    import { page } from "$app/state";
    import { ChevronDownOutline } from "flowbite-svelte-icons";
    let { report, configName, runId } = $props();

    const { params } = page;

    function formatJson(value) {
        if (value === null || value === undefined) return "";

        if (typeof value === "string") {
            try {
                return JSON.stringify(JSON.parse(value), null, 2);
            } catch {
                return value;
            }
        }

        return JSON.stringify(value, null, 2);
    }

    function getStatusCodeCounts(statusCodes) {
        return (statusCodes ?? []).reduce((counts, code) => {
            counts[code] = (counts[code] ?? 0) + 1;
            return counts;
        }, {});
    }

    function getMethodBadgeClass(method) {
        const normalized = (method ?? "").toUpperCase();

        switch (normalized) {
            case "GET":
                return "bg-emerald-100 text-emerald-800";
            case "POST":
                return "bg-sky-100 text-sky-800";
            case "PUT":
                return "bg-amber-100 text-amber-800";
            case "PATCH":
                return "bg-cyan-100 text-cyan-800";
            case "DELETE":
                return "bg-rose-100 text-rose-800";
            case "OPTIONS":
                return "bg-violet-100 text-violet-800";
            case "HEAD":
                return "bg-indigo-100 text-indigo-800";
            default:
                return "bg-gray-200 text-gray-700";
        }
    }

    async function fetchMarkdownReport() {
        const response = await fetch(
            `/api/configs/${encodeURIComponent(params.configName)}/runs/${encodeURIComponent(params.runTimestamp)}/markdown`,
        );

        if (!response.ok) {
            throw new Error("Failed to fetch markdown report");
        }

        return response.text();
    }

    async function downloadReportAsMarkdown() {
        if (!report) return;

        const markdown = await fetchMarkdownReport();
        const blob = new Blob([markdown], { type: "text/markdown" });
        const url = URL.createObjectURL(blob);

        const link = document.createElement("a");
        link.href = url;
        link.download = "coverage-report.md";
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        URL.revokeObjectURL(url);
    }

    function downloadReportAsJson() {
        if (!report) return;

        const json = JSON.stringify(report, null, 2);
        const blob = new Blob([json], { type: "application/json" });
        const url = URL.createObjectURL(blob);

        const link = document.createElement("a");
        link.href = url;
        link.download = "coverage-report.json";
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        URL.revokeObjectURL(url);
    }
</script>

<div
    class="p-4 pb-8 overflow-y-auto space-y-6 bg-gray-50"
    style="max-height: calc(100vh - 200px);"
>
    <div class="flex items-center justify-between gap-4">
        <h1 class="text-2xl font-bold">Test Coverage Report</h1>
        <Button
            class="text-sm font-medium text-white bg-blue-600 rounded hover:bg-blue-700"
            disabled={!report}
            >Download Report<ChevronDownOutline
                class="ms-2 h-6 w-6 text-white dark:text-white"
            /></Button
        >
        <Dropdown simple>
            <DropdownItem onclick={downloadReportAsJson}>JSON</DropdownItem>
            <DropdownItem onclick={downloadReportAsMarkdown}
                >Markdown</DropdownItem
            >
        </Dropdown>
    </div>

    <!-- REST Connection Coverage Statistics -->
    <section class="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <h2 class="text-lg font-bold mb-3">
            REST Connection Coverage Statistics
        </h2>
        <div class="overflow-x-auto">
            <table class="w-full border border-gray-300 text-sm">
                <tbody>
                    <tr class="border-b border-gray-300">
                        <td class="border-r border-gray-300 p-2 font-medium"
                            >Total REST connections</td
                        >
                        <td class="p-2">{report?.totalConnections ?? "-"}</td>
                    </tr>
                    <tr class="border-b border-gray-300">
                        <td class="border-r border-gray-300 p-2 font-medium"
                            >Covered REST connections</td
                        >
                        <td class="p-2"
                            >{report?.coveredConnections?.length ?? "-"}</td
                        >
                    </tr>
                    <tr class="border-b border-gray-300">
                        <td class="border-r border-gray-300 p-2 font-medium"
                            >Uncovered REST connections</td
                        >
                        <td class="p-2"
                            >{report?.uncoveredConnections?.length ?? "-"}</td
                        >
                    </tr>
                    <tr class="bg-blue-50">
                        <td class="border-r border-gray-300 p-2 font-bold"
                            >REST Connection Coverage</td
                        >
                        <td class="p-2 font-bold text-blue-600"
                            >{report?.connectionCoveragePercentage?.toFixed(
                                2,
                            ) ?? "-"}%</td
                        >
                    </tr>
                </tbody>
            </table>
        </div>
    </section>

    <!-- Uncovered REST Connections -->
    <section class="rounded-lg border border-gray-200 bg-red-50 p-4 shadow-sm">
        <h3 class="text-base font-bold mb-2">Uncovered REST Connections</h3>
        {#if report?.uncoveredConnections && report.uncoveredConnections.length > 0}
            <ul class="list-disc list-inside space-y-1 text-sm">
                {#each report.uncoveredConnections as connection}
                    <li>
                        <strong>{connection.source}</strong>
                        →
                        <strong>{connection.destination}</strong>
                        <span
                            class={`inline-block rounded px-1 py-0.5 mr-1 text-xs font-semibold ${getMethodBadgeClass(connection.call.method)}`}
                        >
                            {connection.call.method}
                        </span>
                        <code class="text-xs bg-gray-100 px-1 py-0.5 rounded"
                            >{connection.call.url}</code
                        >
                    </li>
                {/each}
            </ul>
        {:else}
            <p class="text-sm text-gray-500 italic">No uncovered connections</p>
        {/if}
    </section>

    <!-- Covered REST Connections -->
    <section
        class="rounded-lg border border-gray-200 bg-green-50 p-4 shadow-sm"
    >
        <h3 class="text-base font-bold mb-2">Covered REST Connections</h3>
        {#if report?.connectionReports && report.connectionReports.length > 0}
            <ul class="list-disc list-inside space-y-1 text-sm">
                {#each report.connectionReports as item}
                    <li>
                        <strong>{item.connection.source}</strong>
                        →
                        <strong>{item.connection.destination}</strong>
                        <span
                            class={`inline-block rounded px-1 py-0.5 mr-1 text-xs font-semibold ${getMethodBadgeClass(item.connection.call.method)}`}
                        >
                            {item.connection.call.method}
                        </span>
                        <code class="text-xs bg-gray-100 px-1 py-0.5 rounded"
                            >{item.connection.call.url}</code
                        >
                        <span class="text-xs text-gray-600 ml-2"
                            >({item.count} calls, {item.uniqueStatusCodes
                                .length} status code(s))</span
                        >
                    </li>
                {/each}
            </ul>
        {:else}
            <p class="text-sm text-gray-500 italic">No covered connections</p>
        {/if}
    </section>

    <!-- Status Code Class Coverage Statistics -->
    <section class="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <h2 class="text-lg font-bold mb-3">
            Status Code Class Coverage Statistics
        </h2>
        <div class="overflow-x-auto">
            <table class="w-full border border-gray-300 text-sm">
                <tbody>
                    <tr class="border-b border-gray-300">
                        <td class="border-r border-gray-300 p-2 font-bold"
                            >Status Code Class Coverage (all connections)</td
                        >
                        <td class="p-2 font-bold text-blue-600"
                            >{report?.statusCodeClassCoveragePercentage?.toFixed(
                                2,
                            ) ?? "-"}%</td
                        >
                    </tr>
                    <tr class="border-b border-gray-300">
                        <td class="border-r border-gray-300 p-2 font-bold"
                            >Status Code Class Coverage (covered connections
                            only)</td
                        >
                        <td class="p-2 font-bold text-blue-600"
                            >{report?.coveredConnectionsStatusCodeClassCoveragePercentage?.toFixed(
                                2,
                            ) ?? "-"}%</td
                        >
                    </tr>
                    <tr class="border-b border-gray-300">
                        <td class="border-r border-gray-300 p-2 font-medium"
                            >Connections with correct status codes (2xx)</td
                        >
                        <td class="p-2"
                            >{report?.connectionsWithCorrectStatusCodeClass ??
                                "-"}</td
                        >
                    </tr>
                    <tr>
                        <td class="border-r border-gray-300 p-2 font-medium"
                            >Connections with client error status codes (4xx)</td
                        >
                        <td class="p-2"
                            >{report?.connectionsWithClientErrorStatusCodeClasses ??
                                "-"}</td
                        >
                    </tr>
                </tbody>
            </table>
        </div>
    </section>

    <!-- Request value and response field coverage -->
    <section class="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <h2 class="text-lg font-bold mb-3">Hierarchical Coverage Highlights</h2>
        <div class="grid gap-3 md:grid-cols-2">
            <div class="rounded border border-amber-200 bg-amber-50 p-3">
                <p class="text-xs font-semibold uppercase tracking-wide text-amber-700">
                    Request Value Partitions
                </p>
                <p class="mt-2 text-2xl font-bold text-amber-700">
                    {report?.requestValuePartitionMeanHierarchicalCoveragePercentage != null
                        ? `${report.requestValuePartitionMeanHierarchicalCoveragePercentage.toFixed(2)}%`
                        : "-"}
                </p>
            </div>
            <div class="rounded border border-cyan-200 bg-cyan-50 p-3">
                <p class="text-xs font-semibold uppercase tracking-wide text-cyan-700">
                    Response Field Coverage
                </p>
                <p class="mt-2 text-2xl font-bold text-cyan-700">
                    {report?.responseFieldMeanHierarchicalCoveragePercentage != null
                        ? `${report.responseFieldMeanHierarchicalCoveragePercentage.toFixed(2)}%`
                        : "-"}
                </p>
            </div>
        </div>
    </section>

    <details class="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <summary class="flex cursor-pointer list-none items-center justify-between gap-2 text-base font-bold text-gray-900">
            <span>Request/Response Level Metrics </span>
            <span class="text-xs text-gray-500">▾</span>
        </summary>
        <div class="mt-4 space-y-4">
            <div class="rounded border border-gray-200 bg-gray-50 p-3">
                <h3 class="text-sm font-bold text-gray-900 mb-2">
                    Request elements
                </h3>
                <div class="grid gap-2 text-sm sm:grid-cols-3">
                    <div>
                        <span class="text-gray-600">Hierarchical</span>
                        <div class="font-semibold text-blue-600">
                            {report?.requestElementMeanHierarchicalCoveragePercentage != null
                                ? `${report.requestElementMeanHierarchicalCoveragePercentage.toFixed(2)}%`
                                : "-"}
                        </div>
                    </div>
                    <div>
                        <span class="text-gray-600">Pooled</span>
                        <div class="font-semibold text-blue-600">
                            {report?.requestElementPooledCoveragePercentage != null
                                ? `${report.requestElementPooledCoveragePercentage.toFixed(2)}%`
                                : "-"}
                        </div>
                    </div>
                    <div>
                        <span class="text-gray-600">Applicable connections</span>
                        <div class="font-semibold">
                            {report?.connectionsWithRequestElements ?? "-"}
                        </div>
                    </div>
                </div>
            </div>

            <div class="rounded border border-gray-200 bg-gray-50 p-3">
                <h3 class="text-sm font-bold text-gray-900 mb-2">
                    Request value partitions
                </h3>
                <div class="grid gap-2 text-sm sm:grid-cols-3">
                    <div>
                        <span class="text-gray-600">Hierarchical</span>
                        <div class="font-semibold text-amber-600">
                            {report?.requestValuePartitionMeanHierarchicalCoveragePercentage != null
                                ? `${report.requestValuePartitionMeanHierarchicalCoveragePercentage.toFixed(2)}%`
                                : "-"}
                        </div>
                    </div>
                    <div>
                        <span class="text-gray-600">Pooled</span>
                        <div class="font-semibold text-amber-600">
                            {report?.requestValuePartitionPooledCoveragePercentage != null
                                ? `${report.requestValuePartitionPooledCoveragePercentage.toFixed(2)}%`
                                : "-"}
                        </div>
                    </div>
                    <div>
                        <span class="text-gray-600">Applicable connections</span>
                        <div class="font-semibold">
                            {report?.connectionsWithRequestValuePartitions ?? "-"}
                        </div>
                    </div>
                </div>
            </div>

            <div class="rounded border border-gray-200 bg-gray-50 p-3">
                <h3 class="text-sm font-bold text-gray-900 mb-2">
                    Response fields
                </h3>
                <div class="grid gap-2 text-sm sm:grid-cols-3">
                    <div>
                        <span class="text-gray-600">Hierarchical</span>
                        <div class="font-semibold text-cyan-600">
                            {report?.responseFieldMeanHierarchicalCoveragePercentage != null
                                ? `${report.responseFieldMeanHierarchicalCoveragePercentage.toFixed(2)}%`
                                : "-"}
                        </div>
                    </div>
                    <div>
                        <span class="text-gray-600">Pooled</span>
                        <div class="font-semibold text-cyan-600">
                            {report?.responseFieldPooledCoveragePercentage != null
                                ? `${report.responseFieldPooledCoveragePercentage.toFixed(2)}%`
                                : "-"}
                        </div>
                    </div>
                    <div>
                        <span class="text-gray-600">Applicable connections</span>
                        <div class="font-semibold">
                            {report?.connectionsWithResponseFields ?? "-"}
                        </div>
                    </div>
                </div>
            </div>

            <div class="rounded border border-gray-200 bg-gray-50 p-3">
                <h3 class="text-sm font-bold text-gray-900 mb-2">
                    Response value partitions
                </h3>
                <div class="grid gap-2 text-sm sm:grid-cols-3">
                    <div>
                        <span class="text-gray-600">Hierarchical</span>
                        <div class="font-semibold text-violet-600">
                            {report?.responseValuePartitionMeanHierarchicalCoveragePercentage != null
                                ? `${report.responseValuePartitionMeanHierarchicalCoveragePercentage.toFixed(2)}%`
                                : "-"}
                        </div>
                    </div>
                    <div>
                        <span class="text-gray-600">Pooled</span>
                        <div class="font-semibold text-violet-600">
                            {report?.responseValuePartitionPooledCoveragePercentage != null
                                ? `${report.responseValuePartitionPooledCoveragePercentage.toFixed(2)}%`
                                : "-"}
                        </div>
                    </div>
                    <div>
                        <span class="text-gray-600">Applicable connections</span>
                        <div class="font-semibold">
                            {report?.connectionsWithResponseValuePartitions ?? "-"}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </details>

    <!-- Detailed Statistics for Covered REST Connections -->
    <section class="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <h2 class="text-lg font-bold mb-3">
            Detailed Statistics for Covered REST Connections
        </h2>

        {#if report?.connectionReports && report.connectionReports.length > 0}
            <div class="space-y-4">
                {#each report.connectionReports as item}
                    <div class="border border-gray-300 rounded p-3 space-y-3">
                        <div>
                            <h3 class="text-base font-bold">
                                {item.connection.source}
                                →
                                {item.connection.destination}
                            </h3>
                            <p class="text-sm text-gray-700">
                                <strong>Endpoint:</strong>
                                <span
                                    class={`inline-block rounded px-1 py-0.5 mr-1 text-xs font-semibold ${getMethodBadgeClass(item.connection.call.method)}`}
                                >
                                    {item.connection.call.method}
                                </span>
                                <code
                                    class="text-xs bg-gray-100 px-1 py-0.5 rounded"
                                    >{item.connection.call.url}</code
                                >
                            </p>
                        </div>

                        <div class="overflow-x-auto">
                            <table
                                class="w-full border border-gray-300 text-sm"
                            >
                                <tbody>
                                    <tr class="border-b border-gray-300">
                                        <td
                                            class="border-r border-gray-300 p-2 font-medium"
                                            >Usage Count</td
                                        >
                                        <td class="p-2">{item.count ?? "-"}</td>
                                    </tr>
                                    <tr class="border-b border-gray-300">
                                        <td
                                            class="border-r border-gray-300 p-2 font-medium"
                                            >Status Code Classes Covered</td
                                        >
                                        <td class="p-2"
                                            >{item.uniqueStatusCodeClasses
                                                ?.length
                                                ? item.uniqueStatusCodeClasses.join(
                                                      ", ",
                                                  )
                                                : "-"}</td
                                        >
                                    </tr>
                                    <tr class="border-b border-gray-300">
                                        <td
                                            class="border-r border-gray-300 p-2 font-medium"
                                            >Status Code Class Coverage</td
                                        >
                                        <td class="p-2">
                                            {item.statusCodeClassCoveragePercentage?.toFixed(
                                                0,
                                            ) ?? "-"}%
                                        </td>
                                    </tr>
                                    {#if item.bodySchemaApplicable}
                                        <tr class="border-b border-gray-300">
                                            <td
                                                class="border-r border-gray-300 p-2 font-medium"
                                                >Bodies checked against schema</td
                                            >
                                            <td class="p-2"
                                                >{item.bodySchemaCheckedCount ??
                                                    "-"}</td
                                            >
                                        </tr>
                                        <tr>
                                            <td
                                                class="border-r border-gray-300 p-2 font-medium"
                                                >Bodies missing from traces</td
                                            >
                                            <td class="p-2"
                                                >{item.bodySchemaMissingCount ??
                                                    "-"}</td
                                            >
                                        </tr>
                                    {/if}
                                </tbody>
                            </table>
                        </div>

                        <div>
                            <h4 class="text-sm font-bold mb-2">Status Codes</h4>
                            <div class="overflow-x-auto">
                                <table
                                    class="w-full border border-gray-300 text-sm"
                                >
                                    <tbody>
                                        {#each Object.entries(getStatusCodeCounts(item.statusCodes)) as [code, count]}
                                            <tr
                                                class="border-b border-gray-300 last:border-b-0"
                                            >
                                                <td
                                                    class="border-r border-gray-300 p-2 font-medium"
                                                    >{code}</td
                                                >
                                                <td class="p-2">{count}</td>
                                            </tr>
                                        {/each}
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        {#if item.bodySchemaApplicable}
                            <details>
                                <summary
                                    class="text-sm font-bold cursor-pointer"
                                >
                                    Request Body Schema
                                </summary>
                                <pre
                                    class="mt-2 text-xs bg-gray-100 p-2 rounded overflow-x-auto">{formatJson(
                                        item.bodySchema,
                                    )}</pre>
                            </details>
                        {/if}

                        {#if item.requestBodies?.length > 0}
                            <details>
                                <summary
                                    class="text-sm font-bold cursor-pointer"
                                >
                                    Request Bodies ({item.requestBodies.length} total,
                                    {item.uniqueRequestBodies?.length ?? 0} unique)
                                </summary>
                                <div class="mt-2 space-y-2">
                                    {#each item.uniqueRequestBodies ?? [] as body}
                                        <pre
                                            class="text-xs bg-gray-100 p-2 rounded overflow-x-auto">{formatJson(
                                                body,
                                            )}</pre>
                                    {/each}
                                </div>
                            </details>
                        {/if}

                        {#if item.responseBodies?.length > 0}
                            <details>
                                <summary
                                    class="text-sm font-bold cursor-pointer"
                                >
                                    Response Bodies ({item.responseBodies
                                        .length} total,
                                    {item.uniqueResponseBodies?.length ?? 0} unique)
                                </summary>
                                <div class="mt-2 space-y-2">
                                    {#each item.uniqueResponseBodies ?? [] as body}
                                        <pre
                                            class="text-xs bg-gray-100 p-2 rounded overflow-x-auto">{formatJson(
                                                body,
                                            )}</pre>
                                    {/each}
                                </div>
                            </details>
                        {/if}
                    </div>
                {/each}
            </div>
        {:else}
            <p class="text-sm text-gray-500 italic">
                No detailed statistics available
            </p>
        {/if}
    </section>
</div>
