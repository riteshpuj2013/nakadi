package org.zalando.nakadi.service;

import org.json.JSONObject;
import org.zalando.nakadi.security.Client;
import org.zalando.nakadi.service.publishing.NakadiKpiPublisher;

import java.util.HashMap;
import java.util.Map;

public abstract class ConsumptionKpiCollector {
    private final String clientId;
    private final String clientRealm;
    private final String appNameHashed;
    private final NakadiKpiPublisher kpiPublisher;
    private final String kpiEventType;
    private final long kpiFlushIntervalMs;

    private final Map<String, StreamKpiData> kpiDataPerEventType = new HashMap<>();
    private long lastKpiEventSent = System.currentTimeMillis();

    public ConsumptionKpiCollector(
            final Client client,
            final NakadiKpiPublisher kpiPublisher,
            final String kpiEventType,
            final long kpiFlushIntervalMs) {
        this.clientId = client.getClientId();
        this.clientRealm = client.getRealm();
        this.appNameHashed = kpiPublisher.hash(clientId);
        this.kpiPublisher = kpiPublisher;
        this.kpiEventType = kpiEventType;
        this.kpiFlushIntervalMs = kpiFlushIntervalMs;
    }

    public void sendKpi() {
        kpiDataPerEventType.forEach(this::publishKpi);
        kpiDataPerEventType.clear();
    }

    public void checkAndSendKpi() {
        if ((System.currentTimeMillis() - lastKpiEventSent) > kpiFlushIntervalMs) {
            sendKpi();
            lastKpiEventSent = System.currentTimeMillis();
        }
    }

    public void recordBatchSent(final String eventType, final int bytesCount, final int eventsCount) {
        final StreamKpiData kpiData = kpiDataPerEventType.computeIfAbsent(eventType, (x) -> new StreamKpiData());
        kpiData.bytesSent += bytesCount;
        kpiData.numberOfEventsSent += eventsCount;
        kpiData.batchesCount += 1;
    }

    protected abstract JSONObject enrich(JSONObject o);

    private JSONObject convertKpiData(final String eventType, final StreamKpiData data) {
        return enrich(new JSONObject()
                .put("event_type", eventType)
                .put("app", clientId)
                .put("app_hashed", appNameHashed)
                .put("token_realm", clientRealm)
                .put("number_of_events", data.numberOfEventsSent)
                .put("bytes_streamed", data.bytesSent)
                .put("batches_streamed", data.batchesCount));
    }

    private void publishKpi(final String eventType, final StreamKpiData data) {
        kpiPublisher.publish(
                kpiEventType,
                () -> convertKpiData(eventType, data));
    }

    private static class StreamKpiData {
        private long bytesSent = 0;
        private long numberOfEventsSent = 0;
        private int batchesCount = 0;
    }
}
