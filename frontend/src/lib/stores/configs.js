import { writable } from 'svelte/store';
import { browser } from '$app/environment';

const STORAGE_KEY = 'microservice_configs_v2';

const defaultConfigs = [
    {
        configName: 'Config Alpha',
        services: [
            { serviceName: 'ts-order-service' },
            { serviceName: 'ts-payment-service' },
            { serviceName: 'ts-user-service' }
        ],
        tests: [
            { fileName: 'OrderTests.java', path: 'src/test/java/order/OrderTests.java', serviceName: 'ts-order-service' },
            { fileName: 'PaymentTests.java', path: 'src/test/java/payment/PaymentTests.java', serviceName: 'ts-payment-service' }
        ]
    },
    {
        configName: 'Config Beta',
        services: [
            { serviceName: 'ts-travel-service' },
            { serviceName: 'ts-route-service' }
        ],
        tests: [
            { fileName: 'TravelTests.java', path: 'src/test/java/travel/TravelTests.java', serviceName: 'ts-travel-service' }
        ]
    },
    {
        configName: 'Config Gamma',
        services: [
            { serviceName: 'ts-seat-service' },
            { serviceName: 'ts-ticketinfo-service' }
        ],
        tests: [
            { fileName: 'SeatAllocationTests.java', path: 'src/test/java/seat/SeatAllocationTests.java', serviceName: 'ts-seat-service' },
            { fileName: 'TicketInfoTests.java', path: 'src/test/java/ticketinfo/TicketInfoTests.java', serviceName: 'ts-ticketinfo-service' }
        ]
    },
    {
        configName: 'Config Delta',
        services: [
            { serviceName: 'ts-food-service' },
            { serviceName: 'ts-news-service' }
        ],
        tests: [
            { fileName: 'FoodTests.java', path: 'src/test/java/food/FoodTests.java', serviceName: 'ts-food-service' },
            { fileName: 'NewsTests.java', path: 'src/test/java/news/NewsTests.java', serviceName: 'ts-news-service' }
        ]
    },
    {
        configName: 'Config Epsilon',
        services: [
            { serviceName: 'ts-voucher-service' },
            { serviceName: 'ts-verification-code-service' }
        ],
        tests: [
            { fileName: 'VoucherTests.java', path: 'src/test/java/voucher/VoucherTests.java', serviceName: 'ts-voucher-service' },
            { fileName: 'VerificationTests.java', path: 'src/test/java/verification/VerificationTests.java', serviceName: 'ts-verification-code-service' }
        ]
    },
    {
        configName: 'Config Zeta',
        services: [
            { serviceName: 'ts-admin-basic-info-service' },
            { serviceName: 'ts-config-service' }
        ],
        tests: [
            { fileName: 'AdminTests.java', path: 'src/test/java/admin/AdminTests.java', serviceName: 'ts-admin-basic-info-service' },
            { fileName: 'ConfigTests.java', path: 'src/test/java/config/ConfigTests.java', serviceName: 'ts-config-service' }
        ]
    }
];

function createConfigsStore() {
    const initial = browser ? JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null') || defaultConfigs : defaultConfigs;
    const { subscribe, set, update } = writable(initial);

    subscribe((value) => {
        if (!browser) return;
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(value));
        } catch (e) {
            // ignore storage errors
        }
    });

    return {
        subscribe,
        set,
        update,
        add: (cfg) => update((cs) => [cfg, ...cs]),
        removeAt: (idx) => update((cs) => cs.filter((_, i) => i !== idx))
    };
}

export const configs = createConfigsStore();
