<script>
    import {
        Button,
        Checkbox,
        Heading,
        Hr,
        Input,
        Listgroup,
        Modal,
        P,
        Search,
        Tooltip,
    } from "flowbite-svelte";
    import { MultiSelect } from "flowbite-svelte";
    import {
        ArrowDownToBracketOutline,
        CheckCircleSolid,
        PlaySolid,
    } from "flowbite-svelte-icons";
    import { SplitPane, Pane } from "flowbite-svelte";
    import { onMount } from "svelte";
    import { goto } from "$app/navigation";
    import { SvelteMap } from "svelte/reactivity";

    let { open = $bindable(), systems, onConfigSaved = null } = $props();

    let serviceMap = new Map();
    let multiSelected = [];
    let searchTerm = $state("");
    let runOnSave = $state(false);
    let selectedService = $state(undefined);
    let newConfigName = $state("");
    let configSavedToast = $state(false);
    let newConfigServices = $state(new SvelteMap());
    let newConfigTests = $state(new SvelteMap());
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
    Array.from(systems).forEach((svc) => serviceMap.set(svc.serviceName, svc));

    async function handleSave() {
        const newConfig = {
            configName: newConfigName,
            services: Array.from(newConfigServices.keys()).map((name) => ({
                serviceName: name,
            })),
            tests: Array.from(newConfigTests.values()),
        };

        console.log("Sending config:", newConfig);
        try {
            const response = await fetch("/api/configs", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(newConfig),
            });

            const result = await response.json();

            if (result.success) {
                console.log("Config saved successfully:", result.path);

                // Navigate to run page if runOnSave is checked
                if (runOnSave) {
                    open = false; // Close modal
                    goto(`/run/${encodeURIComponent(newConfigName)}`);
                }

                // Notify parent component of successful save
                if (onConfigSaved) {
                    onConfigSaved(newConfig);
                }

                open = false;
            } else {
                console.error("Failed to save config:", result.error);
                return;
            }
        } catch (error) {
            console.error("Failed to save config:", error);
            return;
        }
    }

    function handleServiceCheckbox(evt, service) {
        evt.stopPropagation();
        if (newConfigServices.has(service.serviceName)) {
            newConfigServices.delete(service.serviceName);
        } else {
            newConfigServices.set(service.serviceName, service);
        }
        newConfigServices = new SvelteMap(newConfigServices);
        console.log(newConfigServices);
    }

    function handleTestCheckbox(test) {
        const key = `${selectedService.serviceName}:${test.path}`;
        if (newConfigTests.has(key)) {
            newConfigTests.delete(key);
        } else {
            newConfigTests.set(key, {
                ...test,
                serviceName: selectedService.serviceName,
            });
        }
        newConfigTests = new SvelteMap(newConfigTests);
        console.log(newConfigTests);
    }

    function handleReset() {
        newConfigName = "";
        newConfigServices = new SvelteMap();
        newConfigTests = new SvelteMap();
        selectedService = undefined;
        runOnSave = false;
    }

    function handleSelectAllServices() {
        const isAllSelected = newConfigServices.size === systems.length;
        if (isAllSelected) {
            newConfigServices = new SvelteMap();
            return;
        }

        const allServices = new SvelteMap();
        for (const svc of systems) {
            allServices.set(svc.serviceName, svc);
        }
        newConfigServices = allServices;
    }
</script>

<Modal title={"Create New Config"} size="xl" bind:open>
    <div
        class="mb-3 rounded-md border border-blue-200 bg-blue-50 p-3 text-sm text-blue-900"
    >
        <p class="font-semibold">Info</p>
        <p class="mt-1">
            This configuration combines selected services and tests into one
            runnable setup. When only selecting services without specifying
            testfiles, only static analysis of the services will be performed,
            with no coverage information being produced.
        </p>
        <ul class="mt-2 list-disc pl-5">
            <li>Provide a name for the Config</li>
            <li>Pick one or more services from the left list</li>
            <li>Select test files for the currently selected service</li>
            <li>
                Optionally enable <span class="font-medium">Run Config</span> to
                start immediately after saving.
            </li>
        </ul>
    </div>
    <div class="flex gap-2">
        <Input
            class="text-placeholder"
            placeholder="New Config Name"
            bind:value={newConfigName}
        />
        <Checkbox
            bind:value={runOnSave}
            onclick={() => (runOnSave = !runOnSave)}>Run Config</Checkbox
        >
        <Button
            size="sm"
            color="green"
            disabled={!newConfigName || newServicesSize == 0}
            onclick={handleSave}
            >Create&nbsp;
            {#if runOnSave}
                <PlaySolid class="shrink-0 h-6 w-6" />
            {:else}<ArrowDownToBracketOutline class="shrink-0 h-6 w-6" />
            {/if}
        </Button>
        <Button size="sm" color="alternative" onclick={handleReset}
            >Reset</Button
        >
    </div>
    <SplitPane direction="vertical" initialSizes={[80, 20]}>
        <Pane class="mx-2">
            <div class="mb-2 flex items-center gap-2">
                <Search
                    size="md"
                    class="h-10 flex-1 m-1"
                    bind:value={searchTerm}
                    placeholder={"Search services by name"}
                ></Search>
                <Button
                    size="md"
                    class="h-10 whitespace-nowrap"
                    onclick={handleSelectAllServices}
                    >{newConfigServices.size === systems.length
                        ? "Deselect all"
                        : "Select all"}</Button
                >
            </div>
            <Listgroup>
                {#each filteredServices as svc (svc.serviceName)}
                    <li
                        class:bg-primary-100={selectedService?.serviceName ===
                            svc.serviceName}
                        class="flex items-center p-3"
                    >
                        <Checkbox
                            onclick={(e) => handleServiceCheckbox(e, svc)}
                            classes={{ div: "inline-flex" }}
                            checked={newConfigServices.has(svc.serviceName)}
                        />
                        <span
                            class="ml-2 cursor-pointer flex-1"
                            onclick={() =>
                                (selectedService = serviceMap.get(
                                    svc.serviceName,
                                ))}
                        >
                            {svc.serviceName} ({svc?.tests?.length ?? 0})
                        </span>
                    </li>
                {/each}
            </Listgroup>
        </Pane>

        <Pane class="mx-2">
            {#if selectedService}
                {#each selectedService.tests as test (`${selectedService.serviceName}:${test.path}`)}
                    <Checkbox
                        id={`${selectedService.serviceName}:${test.path}`}
                        class="ml-2"
                        onclick={() => handleTestCheckbox(test)}
                        checked={newConfigTests.has(
                            `${selectedService.serviceName}:${test.path}`,
                        )}
                        disabled={newConfigTests.size > 0 &&
                            !newConfigTests.has(
                                `${selectedService.serviceName}:${test.path}`,
                            )}
                    >
                        <div
                            class="font-mono text-md bg-gray-100 p-2 rounded mb-1"
                        >
                            <div>{test.fileName}</div>
                            <Tooltip>{test.path}</Tooltip>
                        </div>
                    </Checkbox>
                {/each}
            {/if}
        </Pane>
    </SplitPane>
</Modal>
