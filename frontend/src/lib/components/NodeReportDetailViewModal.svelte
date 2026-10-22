<script>
    import { Modal, Badge, Accordion, AccordionItem } from "flowbite-svelte";

    let {
        selectedNode = null,
        microserviceSystem = null,
        open = $bindable(false),
    } = $props();

    const service = $derived(
        microserviceSystem?.microservices?.find(
            (entry) => entry.name === selectedNode,
        ) || null,
    );

    const title = $derived(
        selectedNode ? `Service Details: ${selectedNode}` : "Service Details",
    );

    const methodSortOrder = {
        GET: 0,
        POST: 1,
        PUT: 2,
        DELETE: 3,
    };

    const sortedEndpoints = $derived(
        (service?.restEndpoints || []).slice().sort((left, right) => {
            const leftMethod = left?.method?.toUpperCase?.() || "";
            const rightMethod = right?.method?.toUpperCase?.() || "";

            const leftOrder =
                methodSortOrder[leftMethod] ?? Number.MAX_SAFE_INTEGER;
            const rightOrder =
                methodSortOrder[rightMethod] ?? Number.MAX_SAFE_INTEGER;

            if (leftOrder !== rightOrder) {
                return leftOrder - rightOrder;
            }

            return (left?.path || "").localeCompare(right?.path || "");
        }),
    );

    function getMethodBadgeColor(method) {
        switch ((method || "").toUpperCase()) {
            case "GET":
                return "blue";
            case "POST":
                return "green";
            case "DELETE":
                return "red";
            default:
                return "gray";
        }
    }

    function getMethodBadgeClass(method) {
        if ((method || "").toUpperCase() === "PUT") {
            return "bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-300";
        }

        return "";
    }
</script>

<Modal {title} form bind:open>
    {#if !selectedNode}
        <p class="text-sm text-gray-500">No service selected.</p>
    {:else if !service}
        <p class="text-sm text-gray-500">
            No endpoint information found for this service.
        </p>
    {:else}
        <div class="space-y-4 max-h-[70vh] overflow-y-auto">
            <div class="border-b pb-4">
                <h3 class="font-semibold text-lg mb-2">Service</h3>
                <Badge color="blue">{service.name}</Badge>
            </div>

            <div>
                <h3 class="font-semibold text-lg mb-2">
                    Endpoints ({sortedEndpoints.length || 0})
                </h3>

                {#if sortedEndpoints.length > 0}
                    <Accordion multiple={true}>
                        {#each sortedEndpoints as endpoint, index}
                            <AccordionItem open={index === 0}>
                                {#snippet header()}
                                    <div class="flex items-center gap-2">
                                        <Badge
                                            color={getMethodBadgeColor(
                                                endpoint.method,
                                            )}
                                            class={getMethodBadgeClass(
                                                endpoint.method,
                                            )}>{endpoint.method}</Badge
                                        >
                                        <span class="font-mono text-xs"
                                            >{endpoint.path}</span
                                        >
                                    </div>
                                {/snippet}

                                <div class="space-y-3 text-sm">
                                    <div>
                                        <p class="text-xs text-gray-600 mb-1">
                                            Return Type
                                        </p>
                                        <div
                                            class="font-mono text-xs bg-gray-100 p-2 rounded"
                                        >
                                            {endpoint.returnType}
                                        </div>
                                    </div>

                                    <div>
                                        <p class="text-xs text-gray-600 mb-1">
                                            Arguments
                                        </p>
                                        {#if endpoint.arguments && endpoint.arguments.length > 0}
                                            <div class="space-y-1">
                                                {#each endpoint.arguments as arg}
                                                    <div
                                                        class="font-mono text-xs bg-gray-100 p-2 rounded"
                                                    >
                                                        {arg}
                                                    </div>
                                                {/each}
                                            </div>
                                        {:else}
                                            <p class="text-xs text-gray-500">
                                                No arguments
                                            </p>
                                        {/if}
                                    </div>
                                </div>
                            </AccordionItem>
                        {/each}
                    </Accordion>
                {:else}
                    <p class="text-sm text-gray-500">No endpoints available.</p>
                {/if}
            </div>
        </div>
    {/if}
</Modal>
