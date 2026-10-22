<div class="min-h-screen bg-gray-50 px-4 py-8">
    <div class="mx-auto max-w-5xl space-y-8">
        <header
            class="rounded-xl border border-gray-200 bg-white p-6 shadow-sm"
        >
            <h1 class="text-3xl font-bold text-gray-900">
                Microservice Coverage Tool Documentation
            </h1>
            <p class="mt-3 text-gray-700">
                This page explains the full workflow of the tool, including how
                to create and run configurations, how to inspect coverage
                results, and how key metrics are calculated.
            </p>
        </header>

        <section
            class="rounded-xl border border-gray-200 bg-white p-6 shadow-sm"
        >
            <h2 class="text-xl font-bold text-gray-900">Quick Start</h2>
            <ol class="mt-4 list-decimal space-y-2 pl-5 text-gray-800">
                <li>
                    Open the main page <code>/</code> and click
                    <strong>New Config</strong>.
                </li>
                <li>
                    Choose one or more services, optionally select tests, and
                    run it directly or save it.
                </li>
                <li>
                    Run the configuration from the main page or from the config
                    details modal.
                </li>
                <li>
                    After execution, open either <strong>View Report</strong>
                    (full coverage analysis) or
                    <strong>View Static Architecture</strong> (if no tests were run).
                </li>
                <li>
                    In the report page, inspect graph details, coverage tables,
                    and download report artifacts (JSON or Markdown).
                </li>
            </ol>
        </section>

        <section
            class="rounded-xl border border-gray-200 bg-white p-6 shadow-sm"
        >
            <h2 class="text-xl font-bold text-gray-900">
                Main Overview (<code>/</code>)
            </h2>
            <p class="mt-3 text-gray-700">
                The overview is your entry point for configuration management,
                browsing historical runs, and launching new executions.
            </p>

            <h3 class="mt-5 text-lg font-semibold text-gray-900">
                What You Can Do
            </h3>
            <ul class="mt-3 list-disc space-y-2 pl-5 text-gray-800">
                <li>
                    Search configurations by config name, service name, or test
                    file name.
                </li>
                <li>
                    Sort configs by test count, service count, latest run,
                    duration, coverage metrics, etc.
                </li>
                <li>
                    Toggle ascending/descending sorting and paginate through
                    results.
                </li>
                <li>
                    Inspect each config card with last run stats and high-level
                    coverage numbers.
                </li>
                <li>Open a detailed modal by clicking a config card.</li>
                <li>
                    Create a new config with selected services and test files.
                </li>
                <li>Run a config directly from card details.</li>
            </ul>

            <h3 class="mt-5 text-lg font-semibold text-gray-900">
                Config Detail Modal
            </h3>
            <ul class="mt-3 list-disc space-y-2 pl-5 text-gray-800">
                <li>Shows selected services and tests for the config.</li>
                <li>
                    Shows run history with status labels: full analysis, static
                    analysis, or failed.
                </li>
                <li>
                    Lets you open historical runs in either report or
                    static-analysis view.
                </li>
                <li>
                    Lets you trigger a new run via <strong>Run Config</strong>.
                </li>
            </ul>

            <h3 class="mt-5 text-lg font-semibold text-gray-900">
                New Config Modal
            </h3>
            <ul class="mt-3 list-disc space-y-2 pl-5 text-gray-800">
                <li>Set a config name.</li>
                <li>
                    Select one or more services (with optional
                    select-all/deselect-all).
                </li>
                <li>Select test files for selected services.</li>
                <li>
                    Optionally enable <strong>Run Config</strong> to execute immediately
                    after save.
                </li>
                <li>
                    Important behavior: if you save with services but without
                    tests, the run performs static architecture analysis only
                    (no runtime coverage metrics).
                </li>
            </ul>
        </section>

        <section
            class="rounded-xl border border-gray-200 bg-white p-6 shadow-sm"
        >
            <h2 class="text-xl font-bold text-gray-900">
                Run Execution View (<code>/run/[configName]</code>)
            </h2>
            <p class="mt-3 text-gray-700">
                The run page streams progress in real time via Server-Sent
                Events and records all run steps in a timeline.
            </p>
            <ul class="mt-3 list-disc space-y-2 pl-5 text-gray-800">
                <li>Displays the current execution step.</li>
                <li>Shows chronological progress logs with timestamps.</li>
                <li>Handles completion and error states.</li>
                <li>
                    After completion, navigates to the correct output page: <br
                    />
                    Full run (with tests):
                    <code>/report/[configName]/[runTimestamp]</code>.
                    <br />Static-only run (no tests):<code
                        >/static-analysis/[configName]/[runTimestamp]</code
                    >.
                </li>
            </ul>
        </section>

        <section
            class="rounded-xl border border-gray-200 bg-white p-6 shadow-sm"
        >
            <h2 class="text-xl font-bold text-gray-900">
                Report View (<code>/report/[configName]/[runTimestamp]</code>)
            </h2>
            <p class="mt-3 text-gray-700">
                The report page combines a graph-based system view with tabbed
                analytical panels for overview, detailed coverage, and styling.
            </p>

            <h3 class="mt-5 text-lg font-semibold text-gray-900">
                Graph + Interaction
            </h3>
            <ul class="mt-3 list-disc space-y-2 pl-5 text-gray-800">
                <li>
                    Visualizes nodes (services) and edges (REST connections).
                </li>
                <li>Edge click opens connection details modal.</li>
                <li>Node click opens service details modal.</li>
                <li>Supports edge filters: all, covered, uncovered.</li>
            </ul>

            <h3 class="mt-5 text-lg font-semibold text-gray-900">Tabs</h3>
            <ul class="mt-3 list-disc space-y-2 pl-5 text-gray-800">
                <li>
                    <strong>Overview:</strong> run metadata and key metrics.
                </li>
                <li>
                    <strong>Coverage Report:</strong> detailed tables/lists for covered
                    and uncovered connections, status codes, request/response payload
                    snippets, and schema checks.
                </li>
                <li>
                    <strong>Styling:</strong> node, edge, and label style controls
                    for the graph.
                </li>
            </ul>

            <h3 class="mt-5 text-lg font-semibold text-gray-900">
                Report Downloads
            </h3>
            <p class="mt-3 text-gray-700">
                In the Coverage Report tab, the download dropdown supports:
            </p>
            <ul class="mt-2 list-disc space-y-2 pl-5 text-gray-800">
                <li>
                    <strong>JSON</strong> download (<code
                        >coverage-report.json</code
                    >)
                </li>
                <li>
                    <strong>Markdown</strong> download (<code
                        >coverage-report.md</code
                    >)
                </li>
            </ul>
        </section>

        <section
            class="rounded-xl border border-gray-200 bg-white p-6 shadow-sm"
        >
            <h2 class="text-xl font-bold text-gray-900">Metric Definitions</h2>
            <p class="mt-3 text-gray-700">
                These definitions reflect the current backend implementation.
            </p>

            <div class="mt-5 rounded-lg border border-gray-200 bg-gray-50 p-4">
                <h3 class="text-lg font-semibold text-gray-900">
                    Connection Coverage
                </h3>
                <p class="mt-2 text-gray-800">
                    Measures how many expected REST connections were actually
                    observed during execution.
                </p>
                <p class="mt-3 text-sm text-gray-900">
                    <strong>Formula:</strong>
                    <code>
                        connectionCoveragePercentage = (coveredConnections /
                        totalConnections) * 100
                    </code>
                </p>
                <p class="mt-2 text-sm text-gray-700">
                    If there are no total connections, this metric is
                    <code>0.0</code>.
                </p>
            </div>

            <div class="mt-4 rounded-lg border border-gray-200 bg-gray-50 p-4">
                <h3 class="text-lg font-semibold text-gray-900">
                    Status Code Class Coverage
                </h3>
                <p class="mt-2 text-gray-800">
                    Evaluates whether each connection exercised expected success
                    and error classes at runtime.
                </p>
                <ul class="mt-3 list-disc space-y-2 pl-5 text-gray-800">
                    <li><code>0%</code>: neither 2xx nor 4xx seen.</li>
                    <li><code>50%</code>: either 2xx or 4xx seen.</li>
                    <li><code>100%</code>: both 2xx and 4xx seen.</li>
                    <li>
                        5xx is tracked but does not increase this percentage.
                    </li>
                </ul>
                <p class="mt-3 text-sm text-gray-900">
                    <strong>All-connections aggregate:</strong>
                    average of per-connection status-class coverage across all expected
                    connections, where uncovered connections implicitly contribute
                    <code>0%</code>.
                </p>
                <p class="mt-2 text-sm text-gray-900">
                    <strong>Covered-connections only:</strong>
                    average of per-connection status-class coverage only for observed
                    connections.
                </p>
            </div>
        </section>

        <section
            class="rounded-xl border border-amber-200 bg-amber-50 p-6 shadow-sm"
        >
            <h2 class="text-xl font-bold text-amber-900">
                Static Analysis
            </h2>
            <p class="mt-3 text-amber-900">
                Static architecture visualization is available from the setup
                flow and can also be opened directly. Current pages:
                <code>/static-analysis/setup/[configName]</code>,
                <code>/static-analysis/[configName]/[runTimestamp]</code> and
                <code>/static-analysis/[runTimestamp]</code>.
            </p>
            <ul class="mt-3 list-disc space-y-2 pl-5 text-amber-900">
                <li>
                    Displays architecture graph from
                    <code>microservice-system.json</code>, based on
                    <code>communicationPathResult.staticTraceResults</code>.
                </li>
                <li>
                    Supports graph styling and navigation similar to report
                    graph views.
                </li>
                <li>Does not provide runtime coverage metrics by itself.</li>
                <li>Feature set and UX are expected to evolve.</li>
            </ul>
        </section>

        <section
            class="rounded-xl border border-gray-200 bg-white p-6 shadow-sm"
        >
            <h2 class="text-xl font-bold text-gray-900">Data and Artifacts</h2>
            <ul class="mt-3 list-disc space-y-2 pl-5 text-gray-800">
                <li>
                    Each run stores artifacts under
                    <code>storage/runs/[configName]/[runTimestamp]</code>.
                </li>
                <li>
                    Common artifacts include:
                    <code>coverage-report.json</code>,
                    <code>coverage-report.md</code>,
                    <code>run-metadata.json</code>, and
                    <code>microservice-system.json</code>.
                </li>
                <li>
                    API endpoints are used by the UI for run lists, run details,
                    and markdown export retrieval.
                </li>
            </ul>
        </section>
    </div>
</div>
