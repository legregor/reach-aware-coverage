<script>
    import { page } from "$app/stores";
    import { onMount, onDestroy } from "svelte";
    import { goto } from "$app/navigation";
    import { Spinner, Button, Card } from "flowbite-svelte";
    import {
        CheckCircleSolid,
        ClockSolid,
        ExclamationCircleSolid,
    } from "flowbite-svelte-icons";

    let configName = $page.params.configName;
    // A rerun from the config modal re-analyses the services instead of using the cache
    let forceReanalyze = $page.url.searchParams.get("force") === "1";
    let progressLogs = $state([]);
    let currentStep = $state("Initializing static analysis...");
    let isRunning = $state(true);
    let hasError = $state(false);
    let runId = $state("static-analysis");
    let eventSource;

    onMount(() => {
        eventSource = new EventSource(
            `/api/run-static-analysis/${encodeURIComponent(configName)}${forceReanalyze ? "?force=1" : ""}`,
        );

        eventSource.onmessage = async (event) => {
            const data = JSON.parse(event.data);

            if (data.type === "progress" || data.type === "step") {
                if (data.type === "step") {
                    currentStep = data.message;
                }
                progressLogs = [
                    ...progressLogs,
                    { type: data.type, message: data.message, timestamp: new Date() },
                ];
            } else if (data.type === "error") {
                hasError = true;
                isRunning = false;
                progressLogs = [
                    ...progressLogs,
                    { type: "error", message: data.message, timestamp: new Date() },
                ];
                eventSource.close();
            } else if (data.type === "complete") {
                runId = data.runId || "static-analysis";
                isRunning = false;
                progressLogs = [
                    ...progressLogs,
                    {
                        type: "complete",
                        message: "Static analysis completed",
                        timestamp: new Date(),
                    },
                ];
                eventSource.close();
                await goto(
                    `/static-analysis/${encodeURIComponent(configName)}/${encodeURIComponent(runId)}`,
                );
            }
        };

        eventSource.onerror = () => {
            if (!isRunning && !hasError) {
                eventSource.close();
                return;
            }
            hasError = true;
            isRunning = false;
            progressLogs = [
                ...progressLogs,
                {
                    type: "error",
                    message: "Connection to server lost or failed to start",
                    timestamp: new Date(),
                },
            ];
            eventSource.close();
        };
    });

    onDestroy(() => {
        if (eventSource) {
            eventSource.close();
        }
    });
</script>

<div class="flex flex-col items-center justify-center bg-gray-50 p-8">
    <Card class="w-full max-w-4xl p-4">
        <h1 class="text-3xl font-bold mb-4">Static Analysis: {configName}</h1>

        <div class="mb-6 m-3 flex items-center gap-3">
            {#if isRunning}
                <Spinner size="8" />
                <span class="text-lg">{currentStep}</span>
            {:else if hasError}
                <ExclamationCircleSolid class="w-8 h-8 text-red-500" />
                <span class="text-lg text-red-600">Error occurred</span>
            {:else}
                <CheckCircleSolid class="w-8 h-8 text-green-500" />
                <span class="text-lg text-green-600">Completed successfully</span>
            {/if}
        </div>

        <div class="mb-6">
            <h2 class="text-xl font-semibold mb-3">Progress Log</h2>
            <div class="space-y-4">
                {#each progressLogs as log, index}
                    <div class="flex gap-4">
                        <div class="flex flex-col items-center">
                            <div
                                class="w-8 h-8 rounded-full flex items-center justify-center ring-4 ring-white"
                                class:bg-primary-200={log.type !== "error" &&
                                    log.type !== "complete"}
                                class:bg-red-200={log.type === "error"}
                                class:bg-green-200={log.type === "complete"}
                            >
                                {#if log.type === "error"}
                                    <ExclamationCircleSolid
                                        class="w-5 h-5 text-red-600"
                                    />
                                {:else if log.type === "complete"}
                                    <CheckCircleSolid
                                        class="w-5 h-5 text-green-600"
                                    />
                                {:else}
                                    <ClockSolid
                                        class="w-5 h-5 text-primary-600"
                                    />
                                {/if}
                            </div>
                            {#if index < progressLogs.length - 1}
                                <div class="w-1 h-8 bg-gray-300 mt-2"
                                ></div>
                            {/if}
                        </div>
                        <div class="pt-1">
                            <p class="font-medium text-gray-900">
                                {log.message}
                            </p>
                            <p class="text-sm text-gray-500">
                                {log.timestamp.toLocaleTimeString()}
                            </p>
                        </div>
                    </div>
                {/each}
            </div>
        </div>

        {#if !isRunning}
            <div class="flex gap-3">
                <Button color="primary" onclick={() => goto("/")}>Back to Home</Button>
                {#if !hasError}
                    <Button
                        color="green"
                        onclick={() =>
                            goto(
                                `/static-analysis/${encodeURIComponent(configName)}/${encodeURIComponent(runId)}`,
                            )}>View Static Architecture</Button
                    >
                {/if}
            </div>
        {/if}
    </Card>
</div>
