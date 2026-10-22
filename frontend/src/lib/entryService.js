/**
 * Resolves the entry service of a configuration: the single service the static
 * analysis starts from and follows recursively.
 *
 * It is the service the tests belong to — the same service the config views
 * highlight among the others. A config saved without tests only has an entry
 * service if it holds exactly one service; with several there is nothing to
 * tell them apart, and the caller has to report that.
 *
 * @param {Object} config - The configuration object
 * @returns {string|null} The entry service name, or null if the config has none
 */
export function resolveEntryService(config) {
    const testService = config?.tests?.[0]?.serviceName;
    if (testService) {
        return testService;
    }

    const services = config?.services || [];
    return services.length === 1 ? services[0]?.serviceName || null : null;
}
