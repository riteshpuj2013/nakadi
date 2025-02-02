package org.zalando.nakadi.service.publishing;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zalando.nakadi.domain.Feature;
import org.zalando.nakadi.security.UsernameHasher;
import org.zalando.nakadi.service.FeatureToggleService;

import java.util.function.Supplier;

@Component
public class NakadiKpiPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(NakadiKpiPublisher.class);

    private final FeatureToggleService featureToggleService;
    private final EventsProcessor eventsProcessor;
    private final UsernameHasher usernameHasher;
    private final EventMetadata eventMetadata;

    @Autowired
    protected NakadiKpiPublisher(final FeatureToggleService featureToggleService,
                                 final EventsProcessor eventsProcessor,
                                 final UsernameHasher usernameHasher,
                                 final EventMetadata eventMetadata) {
        this.featureToggleService = featureToggleService;
        this.eventsProcessor = eventsProcessor;
        this.usernameHasher = usernameHasher;
        this.eventMetadata = eventMetadata;
    }

    public void publish(final String etName, final Supplier<JSONObject> eventSupplier) {
        try {
            if (!featureToggleService.isFeatureEnabled(Feature.KPI_COLLECTION)) {
                return;
            }

            eventsProcessor.queueEvent(etName,
                    eventMetadata.addTo(eventSupplier.get()));
        } catch (final Exception e) {
            LOG.error("Error occurred when submitting KPI event for publishing", e);
        }
    }

    public String hash(final String value) {
        return usernameHasher.hash(value);
    }

}
