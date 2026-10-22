/**
 * @param {Array} runs - Array of run objects
 */
export function getOverviewInfo(runs) {
    if (runs.length == 0) return {}
    const lastRunReport = runs[0].report
    const highestConnectionCoveragePercentage = lastRunReport?.connectionCoveragePercentage ?? undefined

    const statusCodeClassCoveragePercentage = lastRunReport?.statusCodeClassCoveragePercentage ?? undefined
    const totalConnections = lastRunReport?.totalConnections ?? undefined
    const requestElementMeanHierarchicalCoveragePercentage = lastRunReport?.requestElementMeanHierarchicalCoveragePercentage ?? undefined
    const requestValuePartitionMeanHierarchicalCoveragePercentage = lastRunReport?.requestValuePartitionMeanHierarchicalCoveragePercentage ?? undefined
    const responseFieldMeanHierarchicalCoveragePercentage = lastRunReport?.responseFieldMeanHierarchicalCoveragePercentage ?? undefined
    const responseValuePartitionMeanHierarchicalCoveragePercentage = lastRunReport?.responseValuePartitionMeanHierarchicalCoveragePercentage ?? undefined

    const lastRunDuration = runs[0]?.metaData?.testDurationMillis ?? undefined;

    return {
        highestConnectionCoveragePercentage: highestConnectionCoveragePercentage,
        statusCodeClassCoveragePercentage: statusCodeClassCoveragePercentage,
        totalConnections: totalConnections,
        lastRunDuration: lastRunDuration,
        requestElementMeanHierarchicalCoveragePercentage: requestElementMeanHierarchicalCoveragePercentage,
        requestValuePartitionMeanHierarchicalCoveragePercentage: requestValuePartitionMeanHierarchicalCoveragePercentage,
        responseFieldMeanHierarchicalCoveragePercentage: responseFieldMeanHierarchicalCoveragePercentage,
        responseValuePartitionMeanHierarchicalCoveragePercentage: responseValuePartitionMeanHierarchicalCoveragePercentage
    }
}