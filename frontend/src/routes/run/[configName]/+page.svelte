<script>
    import { page } from "$app/stores";
    import { onMount, onDestroy } from "svelte";
    import { goto } from "$app/navigation";
    import {
        Spinner,
        Button,
        Card,
        Timeline,
        TimelineItem,
        P,
    } from "flowbite-svelte";
    import {
        CheckCircleSolid,
        ClockSolid,
        ClockOutline,
        ExclamationCircleSolid,
    } from "flowbite-svelte-icons";

    let configName = $page.params.configName;
    let progressLogs = $state([]);
    let currentStep = $state("Initializing...");
    let isRunning = $state(true);
    let hasError = $state(false);
    let storagePath = $state(null);
    let runMode = $state("full");
    let eventSource;

    onMount(() => {
        const urlParams = new URLSearchParams(window.location.search);
        const requestedMode = urlParams.get("mode") === "static" ? "static" : "full";
        runMode = requestedMode;

        // Connect to SSE endpoint for real-time progress
        eventSource = new EventSource(
            `/api/run-coverage/${encodeURIComponent(configName)}?mode=${encodeURIComponent(requestedMode)}`,
        );

        eventSource.onmessage = (event) => {
            const data = JSON.parse(event.data);

            if (data.type === "progress") {
                progressLogs = [
                    ...progressLogs,
                    {
                        type: "progress",
                        message: data.message,
                        timestamp: new Date(),
                    },
                ];
            } else if (data.type === "step") {
                currentStep = data.message;
                progressLogs = [
                    ...progressLogs,
                    {
                        type: "step",
                        message: data.message,
                        timestamp: new Date(),
                    },
                ];
            } else if (data.type === "runMode") {
                runMode = data.message === "static" ? "static" : "full";
            } else if (data.type === "info") {
            } else if (data.type === "error") {
                //hasError = true;
                progressLogs = [
                    ...progressLogs,
                    {
                        type: "error",
                        message: data.message,
                        timestamp: new Date(),
                    },
                ];
            } else if (data.type === "storagePath") {
                storagePath = data.message;
            } else if (data.type === "complete") {
                isRunning = false;
                runMode = data.runMode === "static" ? "static" : runMode;
                progressLogs = [
                    ...progressLogs,
                    {
                        type: "complete",
                        message: "Config executed",
                        timestamp: new Date(),
                    },
                ];
                // Close the connection after receiving complete message
                eventSource.close();
            }
        };

        eventSource.onerror = (error) => {
            console.error("EventSource error:", error);
            console.error("EventSource readyState:", eventSource.readyState);

            // If already finished successfully, don't show error
            if (!isRunning && !hasError) {
                console.log("Stream closed normally after completion");
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
        <h1 class="text-3xl font-bold mb-4">Running: {configName}</h1>

        <div class="mb-6 m-3 flex items-center gap-3">
            {#if isRunning}
                <Spinner size="8" />
                <span class="text-lg">{currentStep}</span>
            {:else if hasError}
                <ExclamationCircleSolid class="w-8 h-8 text-red-500" />
                <span class="text-lg text-red-600">Error occurred</span>
            {:else}
                <CheckCircleSolid class="w-8 h-8 text-green-500" />
                <span class="text-lg text-green-600"
                    >Completed successfully</span
                >
            {/if}
        </div>

        <div class="mb-6">
            <h2 class="text-xl font-semibold mb-3">Progress Log</h2>
            <div class="space-y-4">
                {#each progressLogs as log, index}
                    <div class="flex gap-4">
                        <!-- Timeline dot and line -->
                        <div class="flex flex-col items-center">
                            <div
                                class="w-8 h-8 rounded-full flex items-center justify-center ring-4 ring-white dark:ring-gray-900"
                                class:bg-blue-100={log.type !== "error" &&
                                    log.type !== "complete"}
                                class:dark:bg-blue-900={log.type !==
                                    "error" && log.type !== "complete"}
                                class:bg-red-200={log.type === "error"}
                                class:dark:bg-red-900={log.type === "error"}
                                class:bg-green-200={log.type === "complete"}
                                class:dark:bg-green-900={log.type ===
                                    "complete"}
                            >
                                {#if log.type === "error"}
                                    <ExclamationCircleSolid
                                        class="w-5 h-5 text-red-600 dark:text-red-400"
                                    />
                                {:else if log.type === "complete"}
                                    <CheckCircleSolid
                                        class="w-5 h-5 text-green-600 dark:text-green-400"
                                    />
                                {:else}
                                    <ClockSolid
                                        class="w-5 h-5 text-blue-600 dark:text-blue-400"
                                    />
                                {/if}
                            </div>
                            {#if index < progressLogs.length - 1}
                                <div
                                    class="w-1 h-8 bg-gray-300 dark:bg-gray-600 mt-2"
                                ></div>
                            {/if}
                        </div>

                        <!-- Log content -->
                        <div class="pt-1">
                            <p
                                class="font-medium text-gray-900 dark:text-white"
                            >
                                {log.message}
                            </p>
                            <p class="text-sm text-gray-500 dark:text-gray-400">
                                {log.timestamp.toLocaleTimeString()}
                            </p>
                        </div>
                    </div>
                {/each}
            </div>
        </div>

        {#if !isRunning}
            <div class="flex gap-3">
                <Button color="primary" onclick={() => goto("/")}
                    >Back to Home</Button
                >
                {#if !hasError && storagePath}
                    {#if runMode === "static"}
                        <Button
                            color="green"
                            onclick={() => {
                                const runTimestamp = storagePath || "static-analysis";
                                goto(
                                    `/static-analysis/${encodeURIComponent(configName)}/${encodeURIComponent(runTimestamp)}`,
                                );
                            }}>View Static Architecture</Button
                        >
                    {:else}
                        <Button
                            color="green"
                            onclick={() => {
                                const runTimestamp = storagePath;
                                goto(
                                    `/report/${encodeURIComponent(configName)}/${encodeURIComponent(runTimestamp)}`,
                                );
                            }}>View Report</Button
                        >
                    {/if}
                {/if}
            </div>
        {/if}
    </Card>
</div>
