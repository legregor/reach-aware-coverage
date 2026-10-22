<script>
    import { Modal, Badge, Accordion, AccordionItem } from "flowbite-svelte";

    let { report, open = $bindable(false) } = $props();

    const title = $derived(
        `${report?.connection?.source} → ${report?.connection?.destination}`,
    );

    const isCovered = $derived(report?.covered !== false);

    let showOnlyUniqueRequests = $state(false);
    let showOnlyUniqueResponses = $state(false);

    const displayedRequestBodies = $derived(
        showOnlyUniqueRequests
            ? report?.uniqueRequestBodies || []
            : report?.requestBodies || [],
    );

    const displayedResponseBodies = $derived(
        showOnlyUniqueResponses
            ? report?.uniqueResponseBodies || []
            : report?.responseBodies || [],
    );

    const requestBodySchemaChecks = $derived(
        report?.requestBodySchemaChecks || [],
    );

    const requestResponseMetricConfigs = [
        {
            key: "requestElementCoverage",
            label: "Request elements",
            description: "Request element coverage measures the extent to which the input elements of reachable connections are exercised.",
        },
        {
            key: "requestValuePartitionCoverage",
            label: "Request value partitions",
            description: "Request element value partition coverage measures how thoroughly different classes of input values are exercised.",
        },
        {
            key: "responseFieldCoverage",
            label: "Response fields",
            description: "The interface specification of an interaction point defines one or more response variants, each with its own response structure; response field coverage measures how thoroughly these response fields are exercised.",
        },
        {
            key: "responseValuePartitionCoverage",
            label: "Response value partitions",
            description: "For each response field, a set of value partitions is defined as for request elements; response field value partition coverage measures how thoroughly these partitions are exercised.",
        },
    ];

    const requestResponseMetrics = $derived(
        requestResponseMetricConfigs
            .map((metric) => ({
                ...metric,
                coverage: report?.[metric.key],
                value: report?.[metric.key]?.hierarchicalCoveragePercentage,
            }))
            .filter((metric) => metric.coverage != null),
    );

    function formatPercentage(value) {
        if (value == null || Number.isNaN(value)) return "—";
        return `${Number(value).toFixed(2)}%`;
    }

    const detailMetricStatus = (metric) => {
        const coverage = metric.coverage;
        if (!coverage) return "Unavailable";
        return coverage.notApplicable === true ? "Not applicable" : "Applicable";
    };

    function normalizeBody(value) {
        if (value == null) return "";
        if (typeof value !== "string") {
            return JSON.stringify(value);
        }
        try {
            return JSON.stringify(JSON.parse(value));
        } catch {
            return value;
        }
    }

    function findSchemaCheckForBody(body) {
        const normalizedBody = normalizeBody(body);
        return (
            requestBodySchemaChecks.find(
                (check) => normalizeBody(check.requestBody) === normalizedBody,
            ) || null
        );
    }

    const displayedRequestBodyItems = $derived(
        displayedRequestBodies.map((body) => ({
            body,
            schemaCheck: findSchemaCheckForBody(body),
        })),
    );

    function formatJson(value) {
        if (!value) return "";
        if (typeof value !== "string") {
            return JSON.stringify(value, null, 2);
        }
        try {
            return JSON.stringify(JSON.parse(value), null, 2);
        } catch {
            return value;
        }
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    }

    function parseBody(value) {
        if (value == null) return null;
        if (typeof value === "string") {
            try {
                return JSON.parse(value);
            } catch {
                return null;
            }
        }
        return value;
    }

    function renderJsonPrimitive(value) {
        if (typeof value === "string") {
            return `<span class="json-string">&quot;${escapeHtml(value)}&quot;</span>`;
        }
        if (typeof value === "number") {
            return `<span class="json-number">${value}</span>`;
        }
        if (typeof value === "boolean") {
            return `<span class="json-boolean">${value}</span>`;
        }
        if (value === null) {
            return `<span class="json-null">null</span>`;
        }
        return escapeHtml(String(value));
    }

    function renderJsonNode(node, depth, path, presentFields) {
        const indent = "  ".repeat(depth);
        const childIndent = "  ".repeat(depth + 1);

        if (Array.isArray(node)) {
            if (node.length === 0) {
                return `<span class="json-empty-array">[] (empty!)</span>`;
            }
            const renderedItems = node
                .map((item, index) => {
                    const comma = index < node.length - 1 ? "," : "";
                    return `${childIndent}${renderJsonNode(item, depth + 1, path, presentFields)}${comma}`;
                })
                .join("\n");
            return `[\n${renderedItems}\n${indent}]`;
        }

        if (node && typeof node === "object") {
            const entries = Object.entries(node);
            if (entries.length === 0) return "{}";
            const renderedEntries = entries
                .map(([key, value], index) => {
                    const keyPath = path ? `${path}.${key}` : key;
                    const isPresent = presentFields.has(keyPath);
                    const isPresentEmptyArray =
                        isPresent && Array.isArray(value) && value.length === 0;
                    const keyClass = isPresentEmptyArray
                        ? "json-key json-empty-array-key"
                        : isPresent
                          ? "json-key json-present-key"
                          : "json-key";
                    const comma = index < entries.length - 1 ? "," : "";
                    return `${childIndent}<span class="${keyClass}">&quot;${escapeHtml(key)}&quot;</span>: ${renderJsonNode(value, depth + 1, keyPath, presentFields)}${comma}`;
                })
                .join("\n");
            return `{\n${renderedEntries}\n${indent}}`;
        }

        return renderJsonPrimitive(node);
    }

    function renderRequestBodyWithHighlights(body, schemaCheck) {
        const parsed = parseBody(body);
        if (parsed == null || !schemaCheck) {
            return `<pre class="json-block">${escapeHtml(formatJson(body))}</pre>`;
        }
        const presentFields = new Set(schemaCheck.presentFields || []);
        const highlightedJson = renderJsonNode(parsed, 0, "", presentFields);
        return `<pre class="json-block">${highlightedJson}</pre>`;
    }
</script>

{#if report}
    <Modal {title} size="xl" form bind:open>
        <div class="space-y-4 max-h-[70vh] overflow-y-auto">
            <!-- Connection Details -->
            <div class="border-b pb-4">
                <h3 class="font-semibold text-lg mb-2">Connection</h3>
                <div class="grid grid-cols-2 gap-2 text-sm">
                    <div>
                        <span class="text-gray-600">Method:</span>
                        <span class="font-mono ml-2"
                            >{report.connection.call.method}</span
                        >
                    </div>
                    <div>
                        <span class="text-gray-600">Status:</span>
                        <Badge color={isCovered ? "green" : "red"} class="ml-2">
                            {isCovered ? "Covered" : "Uncovered"}
                        </Badge>
                    </div>
                    <div class="col-span-2">
                        <span class="text-gray-600">Path:</span>
                        <div
                            class="font-mono text-xs mt-1 bg-gray-100 p-2 rounded"
                        >
                            {report.connection.endpoint.path}
                        </div>
                    </div>
                </div>
            </div>

            <!-- Coverage Stats (only for covered conns) -->
            {#if isCovered}
                <div class="border-b pb-4">
                    <h3 class="font-semibold text-lg mb-2">
                        Coverage Statistics
                    </h3>
                    <div class="space-y-2 text-sm">
                        <div class="flex justify-between">
                            <span class="text-gray-600">Invocations:</span>
                            <span class="font-semibold">{report.count}</span>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-gray-600"
                                >Status Code Coverage:</span
                            >
                            <span class="font-semibold"
                                >{report.statusCodeClassCoveragePercentage?.toFixed(
                                    1,
                                )}%</span
                            >
                        </div>
                        <div class="flex justify-between">
                            <span class="text-gray-600"
                                >Unique Status Codes:</span
                            >
                            <div class="flex gap-1">
                                {#each report.uniqueStatusCodes as code}
                                    <Badge color="blue">{code}</Badge>
                                {/each}
                            </div>
                        </div>
                        {#if report.bodySchemaApplicable}
                            <div class="flex justify-between">
                                <span class="text-gray-600"
                                    >Bodies Checked:</span
                                >
                                <span class="font-semibold"
                                    >{report.bodySchemaCheckedCount} / {report.bodySchemaCheckedCount +
                                        report.bodySchemaMissingCount}</span
                                >
                            </div>
                        {/if}
                    </div>
                </div>

                {#if requestResponseMetrics.length > 0}
                    <div class="border-b pb-4">
                        <h3 class="font-semibold text-lg mb-2">
                            Request & Response Metrics
                        </h3>
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
                            {#each requestResponseMetrics as metric}
                                <div class="rounded-lg border border-gray-200 bg-gray-50 p-3">
                                    <div class="flex items-center justify-between gap-2">
                                        <div class="text-gray-600 text-xs uppercase tracking-wide">
                                            {metric.label}
                                        </div>
                                        <span
                                            class="inline-flex h-4 w-4 items-center justify-center rounded-full border border-gray-400 text-[10px] text-gray-600 cursor-help"
                                            title={metric.description}
                                            aria-label={`More information about ${metric.label}`}
                                        >
                                            ?
                                        </span>
                                    </div>
                                    <div class="mt-2 font-semibold text-base">
                                        {metric.value != null
                                            ? formatPercentage(metric.value)
                                            : "—"}
                                    </div>
                                    {#if metric.coverage != null}
                                        <div class="mt-3 space-y-1 text-xs">
                                            <div class="flex justify-between gap-2">
                                                <span class="text-gray-500">Status</span>
                                                <span class="font-medium">
                                                    {detailMetricStatus(metric)}
                                                </span>
                                            </div>
                                            {#if metric.coverage.notApplicable === true}
                                                <div class="rounded bg-amber-50 border border-amber-200 p-2 text-amber-800">
                                                    {metric.coverage.notApplicableReason ||
                                                        "This metric is not applicable for this call."}
                                                </div>
                                            {:else}
                                                <div class="flex justify-between gap-2">
                                                    <span class="text-gray-500">Hierarchical</span>
                                                    <span class="font-medium">
                                                        {formatPercentage(
                                                            metric.coverage.hierarchicalCoveragePercentage,
                                                        )}
                                                    </span>
                                                </div>
                                                <div class="flex justify-between gap-2">
                                                    <span class="text-gray-500">Pooled</span>
                                                    <span class="font-medium">
                                                        {formatPercentage(
                                                            metric.coverage.pooledCoveragePercentage,
                                                        )}
                                                    </span>
                                                </div>
                                                {#if metric.coverage.constraintPooledCoveragePercentage != null}
                                                    <div class="flex justify-between gap-2">
                                                        <span class="text-gray-500">Constraint pooled</span>
                                                        <span class="font-medium">
                                                            {formatPercentage(
                                                                metric.coverage.constraintPooledCoveragePercentage,
                                                            )}
                                                        </span>
                                                    </div>
                                                {/if}
                                            {/if}
                                        </div>
                                    {/if}
                                </div>
                            {/each}
                        </div>
                    </div>
                {/if}

                <!-- Request Body Schema -->
                {#if report.bodySchemaApplicable && report.bodySchema}
                    <div class="border-b pb-4">
                        <h3 class="font-semibold text-lg mb-2">
                            Request Body Schema
                        </h3>
                        {#if report.bodySchemaMismatchReason}
                            <div
                                class="mb-2 p-2 bg-yellow-50 border border-yellow-200 rounded text-sm text-yellow-800"
                            >
                                <span class="font-semibold"
                                    >Validation Issue:</span
                                >
                                {report.bodySchemaMismatchReason}
                            </div>
                        {/if}
                        <div
                            class="bg-gray-100 p-3 rounded text-xs font-mono overflow-auto max-h-48"
                        >
                            {JSON.stringify(report.bodySchema, null, 2)}
                        </div>
                    </div>
                {/if}

                <!-- Request Bodies -->
                {#if report.requestBodies && report.requestBodies.length > 0}
                    <div class="border-b pb-4">
                        <div class="flex justify-between items-center mb-2">
                            <h3 class="font-semibold text-lg">
                                Request Bodies ({displayedRequestBodies.length}{#if showOnlyUniqueRequests}
                                    &nbspunique{/if})
                            </h3>
                            <label
                                class="flex items-center gap-2 text-sm cursor-pointer"
                            >
                                <input
                                    type="checkbox"
                                    bind:checked={showOnlyUniqueRequests}
                                    class="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500"
                                />
                                <span>Unique only</span>
                            </label>
                        </div>
                        <Accordion multiple={true}>
                            {#each displayedRequestBodyItems as item, index}
                                <AccordionItem open={index === 0}>
                                    {#snippet header()}Request {index +
                                            1}{/snippet}
                                    {#if item.schemaCheck}
                                        <div
                                            class="grid grid-cols-1 md:grid-cols-2 gap-3 mb-3"
                                        >
                                            <div>
                                                <p
                                                    class="text-xs font-semibold uppercase text-green-700 mb-1"
                                                >
                                                    Present Fields
                                                </p>
                                                {#if item.schemaCheck.missingFields && item.schemaCheck.missingFields.length === 0}
                                                    <div
                                                        class="text-xs text-green-700 bg-green-50 border border-green-200 rounded p-2"
                                                    >
                                                        All schema fields are
                                                        present
                                                    </div>
                                                {:else if item.schemaCheck.presentFields && item.schemaCheck.presentFields.length > 0}
                                                    <div
                                                        class="max-h-32 overflow-auto bg-green-50 border border-green-200 rounded p-2"
                                                    >
                                                        {#each item.schemaCheck.presentFields as field}
                                                            <div
                                                                class="font-mono text-xs text-green-800"
                                                            >
                                                                {field}
                                                            </div>
                                                        {/each}
                                                    </div>
                                                {:else}
                                                    <div
                                                        class="text-xs text-gray-500"
                                                    >
                                                        No schema fields present
                                                    </div>
                                                {/if}
                                            </div>

                                            <div>
                                                <p
                                                    class="text-xs font-semibold uppercase text-red-700 mb-1"
                                                >
                                                    Missing Fields
                                                </p>
                                                {#if item.schemaCheck.missingFields && item.schemaCheck.missingFields.length > 0}
                                                    <div
                                                        class="max-h-32 overflow-auto bg-red-50 border border-red-200 rounded p-2"
                                                    >
                                                        {#each item.schemaCheck.missingFields as field}
                                                            <div
                                                                class="font-mono text-xs text-red-800"
                                                            >
                                                                {field}
                                                            </div>
                                                        {/each}
                                                    </div>
                                                {:else}
                                                    <div
                                                        class="text-xs text-gray-500"
                                                    >
                                                        No schema fields missing
                                                    </div>
                                                {/if}
                                            </div>
                                        </div>
                                    {/if}
                                    <div
                                        class="bg-gray-100 p-3 rounded text-xs font-mono overflow-auto max-h-48"
                                    >
                                        {@html renderRequestBodyWithHighlights(
                                            item.body,
                                            item.schemaCheck,
                                        )}
                                    </div>
                                </AccordionItem>
                            {/each}
                        </Accordion>
                    </div>
                {/if}

                <!-- Response Bodies -->
                {#if report.responseBodies && report.responseBodies.length > 0}
                    <div class="border-b pb-4">
                        <div class="flex justify-between items-center mb-2">
                            <h3 class="font-semibold text-lg">
                                Response Bodies ({displayedResponseBodies.length}{#if showOnlyUniqueResponses}
                                    &nbspunique{/if})
                            </h3>
                            <label
                                class="flex items-center gap-2 text-sm cursor-pointer"
                            >
                                <input
                                    type="checkbox"
                                    bind:checked={showOnlyUniqueResponses}
                                    class="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500"
                                />
                                <span>Unique only</span>
                            </label>
                        </div>
                        <Accordion multiple={true}>
                            {#each displayedResponseBodies as responseBody, index}
                                <AccordionItem open={index === 0}>
                                    {#snippet header()}Response {index +
                                            1}{/snippet}
                                    <div
                                        class="bg-gray-100 p-3 rounded text-xs font-mono overflow-auto max-h-48"
                                    >
                                        {formatJson(responseBody)}
                                    </div>
                                </AccordionItem>
                            {/each}
                        </Accordion>
                    </div>
                {/if}
            {/if}

            <!-- Endpoint Details -->
            <div>
                <h3 class="font-semibold text-lg mb-2">Endpoint Details</h3>
                <div class="space-y-2 text-sm">
                    <div>
                        <span class="text-gray-600">Method:</span>
                        <span class="font-mono ml-2"
                            >{report.connection.endpoint.method}</span
                        >
                    </div>
                    <div>
                        <span class="text-gray-600">Return Type:</span>
                        <span class="font-mono text-xs ml-2"
                            >{report.connection.endpoint.returnType}</span
                        >
                    </div>
                    {#if report.connection.endpoint.arguments.length > 0}
                        <div>
                            <span class="text-gray-600 block mb-1"
                                >Arguments:</span
                            >
                            <div class="space-y-1">
                                {#each report.connection.endpoint.arguments as arg}
                                    <div
                                        class="font-mono text-xs bg-gray-100 p-2 rounded"
                                    >
                                        {arg}
                                    </div>
                                {/each}
                            </div>
                        </div>
                    {/if}
                </div>
            </div>
        </div>
    </Modal>
{/if}

<style>
    :global(.json-block) {
        margin: 0;
        white-space: pre-wrap;
        word-break: break-word;
    }

    :global(.json-key) {
        color: #1f2937;
    }

    :global(.json-present-key) {
        color: #15803d;
        font-weight: 700;
    }

    :global(.json-empty-array-key) {
        color: #d97706;
        font-weight: 700;
    }

    :global(.json-empty-array) {
        color: #d97706;
        font-weight: 700;
    }

    :global(.json-string) {
        color: #1d4ed8;
    }

    :global(.json-number) {
        color: #7c3aed;
    }

    :global(.json-boolean),
    :global(.json-null) {
        color: #dc2626;
    }
</style>
