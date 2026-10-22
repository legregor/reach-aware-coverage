export function buildConnectionIndex(report) {
    const map = new Map();

    report.connectionReports.forEach(r => {
        const key = `${r.connection.source}->${r.connection.destination}:${r.connection.call.method}:${r.connection.endpoint.path}`
        r.covered = true;
        map.set(key, r)


    });

    report.uncoveredConnections.forEach(conn => {
        const uncoveredReport = {
            connection: conn,
            covered: false
        }
        const key = `${conn.source}->${conn.destination}:${conn.call.method}:${conn.endpoint.path}`
        map.set(key, uncoveredReport)

    });

    return map;
}

function decodePathForDisplay(path) {
    if (typeof path !== "string" || path.length === 0) {
        return path;
    }
    try {
        return decodeURIComponent(path);
    } catch {
        return path;
    }
}


export function mapReportToElements(report, testServiceNames = []) {
    const nodes = new Map();
    const edges = [];

    function ensureNode(name) {
        if (!nodes.has(name)) {
            nodes.set(name, {
                data: {
                    id: name,
                    label: name,
                    isTestService: testServiceNames.includes(name)
                }
            });
        }
    }

    // Covered connections
    report.coveredConnections.forEach(conn => {
        ensureNode(conn.source);
        ensureNode(conn.destination);
        const displayPath = decodePathForDisplay(conn.endpoint.path);

        edges.push({
            data: {
                id: `${conn.source}->${conn.destination}:${conn.call.method}:${conn.endpoint.path}`,
                source: conn.source,
                target: conn.destination,
                covered: true,
                method: conn.call.method,
                path: displayPath,
                label: `${conn.call.method} ${displayPath}`
            }
        });
    });

    // Uncovered connections
    report.uncoveredConnections.forEach(conn => {
        ensureNode(conn.source);
        ensureNode(conn.destination);
        const displayPath = decodePathForDisplay(conn.endpoint.path);

        edges.push({
            data: {
                id: `${conn.source}->${conn.destination}:${conn.call.method}:${conn.endpoint.path}`,
                source: conn.source,
                target: conn.destination,
                covered: false,
                method: conn.call.method,
                path: displayPath,
                label: `${conn.call.method} ${displayPath}`
            }
        });
    });

    return {
        nodes: Array.from(nodes.values()),
        edges
    };
}

export function mapStaticAnalysisToElements(staticAnalysis) {
    const services = staticAnalysis?.services || [];
    const connections = staticAnalysis?.connections || [];
    const originService = staticAnalysis?.originService;
    const nodes = new Map();
    const edges = new Map();

    function ensureNode(name, isExternal = false) {
        if (!name) return;
        if (!nodes.has(name)) {
            nodes.set(name, {
                data: {
                    id: name,
                    label: name,
                    isExternal
                }
            });
        }
    }

    services.forEach((service) => {
        const name = service?.name;
        if (!name) return;
        ensureNode(name, Boolean(service.isUnresolved));
        const node = nodes.get(name);
        if (node) {
            node.data.isOrigin = Boolean(
                service.isOrigin || (originService && name === originService),
            );
        }
    });

    connections.forEach((conn, index) => {
        const source = conn.source || conn.sourceService;
        const target = conn.target || conn.destinationService;
        if (!source || !target) return;

        ensureNode(source);
        ensureNode(target);

        const method = conn.method || "CALL";
        const endpointPath = decodePathForDisplay(conn.path || "/");
        const edgeId = conn.id || `${source}->${target}:${method}:${endpointPath}:${index}`;

        if (!edges.has(edgeId)) {
            edges.set(edgeId, {
                data: {
                    id: edgeId,
                    source,
                    target,
                    method,
                    path: endpointPath,
                    label: `${method} ${endpointPath}`,
                    covered: true,
                    pathId: conn.pathId || null
                }
            });
        }
    });

    return {
        nodes: Array.from(nodes.values()),
        edges: Array.from(edges.values())
    };
}
