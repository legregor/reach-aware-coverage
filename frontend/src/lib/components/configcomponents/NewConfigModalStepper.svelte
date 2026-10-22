<script>
    import {Button, Input, Modal, Pane, Search, SplitPane, Tooltip} from "flowbite-svelte";
    import {goto} from "$app/navigation";
    import {SvelteMap} from "svelte/reactivity";
    import {
        ArrowDownToBracketOutline,
        PlaySolid,
    } from "flowbite-svelte-icons";

    let {open = $bindable(), systems, onConfigSaved = null} = $props();

    let searchTerm = $state("");
    let newConfigServices = $state(new SvelteMap());
    let newConfigTests = $state(new SvelteMap());
    let selectedService = $state(undefined)
    let selectedTest = $state(undefined)
    let newConfigName = $state(undefined)
    let current = $state(1)
    let filteredServices = $derived(
        systems
            .filter((svc) =>
                svc.serviceName
                    .toLowerCase()
                    .includes(searchTerm.toLowerCase()),
            )
            .sort((svc1, svc2) => svc2.tests.length - svc1.tests.length),
    );

    let newServicesSize = $derived(newConfigServices.size);
    let hasValidConfigName = $derived(
        typeof newConfigName === "string" &&
            newConfigName.trim().length > 0 &&
            !/\s/.test(newConfigName),
    );
    let hasSingleSelectedService = $derived(
        newConfigServices.size === 1 && !!selectedService,
    );
    let hasSingleSelectedTest = $derived(
        newConfigTests.size === 1 && !!selectedTest,
    );
    let canAdvanceToStep2 = $derived(hasValidConfigName);
    let canAdvanceToStep3 = $derived(
        hasValidConfigName && hasSingleSelectedService && hasSingleSelectedTest,
    );

    function goToNextStep() {
        if (current === 1 && canAdvanceToStep2) {
            current = 2;
            return;
        }

        if (current === 2 && canAdvanceToStep3) {
            current = 3;
        }
    }

    async function handleSave(runStatic = false) {
        const services = Array.from(newConfigServices.keys()).map((name) => ({
            serviceName: name,
        }));
        const tests = Array.from(newConfigTests.values());
        const newConfig = {
            configName: newConfigName,
            services,
            tests,
        };

        console.log("Sending config:", newConfig);
        try {
            const response = await fetch("/api/configs", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(newConfig),
            });

            const result = await response.json();

            if (result.success) {
                console.log("Config saved successfully:", result.path);

                // Notify parent component of successful save
                if (onConfigSaved) {
                    onConfigSaved(newConfig);
                }

                // If requested, run static analysis first using the saved config name
                if (runStatic) {
                    goto(
                        `/static-analysis/setup/${encodeURIComponent(newConfigName)}`,
                    );
                }

                // Clear internal state so the component is reset for a new config
                newConfigName = undefined;
                newConfigServices.clear();
                newConfigServices = new SvelteMap();
                newConfigTests.clear();
                newConfigTests = new SvelteMap();
                selectedService = undefined;
                selectedTest = undefined;
                searchTerm = "";
                current = 1;

                open = false;
            } else {
                console.error("Failed to save config:", result.error);
            }
        } catch (error) {
            console.error("Failed to save config:", error);
        }
    }

    const steps = [
        {
            id: 1,
            label: "Configuration",
            description: "Name"
        },
        {
            id: 2,
            label: "Service & Test",
            description: "Selection"
        },
        {
            id: 3,
            label: "Review",
            description: "Save"
        }
    ];

    function handleStepClick({current: target, last}) {
        if (target < last) {
            current = target;
            return;
        }

        if (target === 2 && canAdvanceToStep2) {
            current = 2;
            return;
        }

        if (target === 3 && canAdvanceToStep3) {
            current = 3;
            return;
        }
    }

    function handleServiceSelect(service) {
        // Only one service can be selected at a time
        newConfigServices.clear();
        newConfigServices.set(service.serviceName, service);
        newConfigServices = new SvelteMap(newConfigServices);
        selectedService = service;
        selectedTest = undefined;
        newConfigTests.clear();
        newConfigTests = new SvelteMap(newConfigTests);
    }

    function handleTestSelect(test) {
        // Only one test can be selected at a time
        newConfigTests.clear();
        const key = `${selectedService.serviceName}:${test.path}`;
        newConfigTests.set(key, {
            ...test,
            serviceName: selectedService.serviceName,
        });
        newConfigTests = new SvelteMap(newConfigTests);
        selectedTest = test;
    }

</script>

<Modal title={"Create New Config"} size="2xl" class="w-[95vw] max-w-7xl" bind:open>
    <div class="flex items-center gap-3">
        <ol class="flex w-full items-center text-sm font-medium text-gray-500 sm:text-base">
            {#each steps as step}
                {@const isCompleted = step.id < current}
                {@const isCurrent = step.id === current}
                <li class="flex flex-1 items-center">
                    <div class="flex items-center gap-2">
                        <span class={[
                            'flex h-8 w-8 items-center justify-center rounded-full border text-xs font-semibold',
                            isCompleted ? 'border-blue-600 bg-blue-600 text-white' : '',
                            isCurrent ? 'border-blue-200 bg-blue-50 text-blue-600' : '',
                            !isCompleted && !isCurrent ? 'border-gray-300 bg-white text-gray-500' : ''
                        ].join(' ')}>
                            {step.id}
                        </span>
                        <span class={isCompleted || isCurrent ? 'text-blue-600' : 'text-gray-500'}>
                            {step.label}
                        </span>
                    </div>
                    {#if step.id !== steps.length}
                        <span class="mx-3 h-px flex-1 bg-gray-200"></span>
                    {/if}
                </li>
            {/each}
        </ol>
        <Button
                color="light"
                onclick={() => (current = Math.max(1, current - 1))}
                disabled={current === 1}
                class="rounded px-4 py-2 disabled:opacity-50"
        >Previous</Button>

        <Button
                color="blue"
                onclick={goToNextStep}
                disabled={current === steps.length || (current === 1 && !canAdvanceToStep2) || (current === 2 && !canAdvanceToStep3)}
                class="rounded px-4 py-2 text-white disabled:opacity-50"
        >Next</Button>
    </div>

    {#if current === 1}
        <div>
            Give your new config a name. This will be used to identify the config in the future.
        </div>
        <Input
                class="text-placeholder"
                placeholder="New Config Name"
                bind:value={newConfigName}
        />
        {#if newConfigName !== undefined && !hasValidConfigName}
            <p class="mt-2 text-sm text-red-600">Config name must be non-empty and contain no whitespace.</p>
        {/if}
    {/if}
    {#if current === 2}
        <div>
            Select a service and the corresponding test file you want to include in the config. Select one test file for
            one selected service.
        </div>
        <div class="mb-2 items-center gap-2">
            <Search
                    size="md"
                    class="h-10 flex-1 m-1"
                    bind:value={searchTerm}
                    placeholder={"Search services by name"}
            ></Search>
        </div>
        <SplitPane direction="vertical">
            <Pane class="mx-2">
                {#each filteredServices as svc}
                    <button
                            type="button"
                            class:bg-blue-100={selectedService?.serviceName === svc.serviceName}
                            class:border-blue-500={selectedService?.serviceName === svc.serviceName}
                            class:border={selectedService?.serviceName === svc.serviceName}
                            class="w-full rounded-md border border-transparent p-3 text-left transition hover:bg-blue-50"
                            onclick={() => handleServiceSelect(svc)}
                    >
                        {svc.serviceName}
                        <span class="text-gray-500">
                ({svc.tests.length ?? 0})</span>
                    </button>
                {/each}
            </Pane>
            <Pane class="mx-2">
                {#if selectedService}
                    {#each selectedService?.tests as test}
                        <button
                                type="button"
                                class:bg-blue-100={selectedTest?.path === test.path}
                                class:border-blue-500={selectedTest?.path === test.path}
                                class:border={selectedTest?.path === test.path}
                                class="mb-1 w-full rounded border border-gray-200 bg-gray-100 p-2 text-left font-mono text-md transition hover:bg-blue-50"
                                onclick={() => handleTestSelect(test)}
                        >
                            <div>{test.fileName}</div>
                            <div class="text-sm text-gray-600"><Tooltip>{test.path}</Tooltip></div>
                        </button>
                    {/each}
                {/if}
            </Pane>
        </SplitPane>
    {/if}
    {#if current === 3}
        <div>
            <h3 class="text-lg font-medium leading-6 text-gray-900">
                Review Config
            </h3>
            <div class="mt-2">
                <p class="text-sm text-gray-500">
                    Please review the configuration details below before saving.
                </p>
            </div>
            <div class="mt-4">
                <p><strong>Config Name:</strong> {newConfigName}</p>
                <p><strong>Selected Service:</strong> {selectedService?.serviceName}</p>
                <p><strong>Selected Test:</strong> {selectedTest?.fileName}</p>
            </div>
            <div class="flex gap-2 mt-4">
                <Button
                        size="sm"
                        color="blue"
                        disabled={!newConfigName || newServicesSize == 0}
                        onclick={() => handleSave(false)}>
                    Save
                    <ArrowDownToBracketOutline class="shrink-0 h-6 w-6" />
                </Button>

                <Button
                        size="sm"
                        color="blue"
                        disabled={!newConfigName || newServicesSize == 0}
                        onclick={() => handleSave(true)}>
                    <PlaySolid class="shrink-0 h-6 w-6" />
                    Save & Run Static Analysis
                </Button>

            </div>

        </div>
    {/if}
</Modal>
