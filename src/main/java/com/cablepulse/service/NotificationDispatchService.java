package com.cablepulse.service;

import com.cablepulse.util.PiiMaskingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Placeholder dispatch layer for WhatsApp / SMS broadcast workflows.
 * Persists intent via structured logs until an external messaging provider is wired.
 */
@Service
public class NotificationDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDispatchService.class);

    public String sendPendingReminder() {
        String trackingId = "brd-pending-" + UUID.randomUUID();
        logger.info("Broadcast queued: type=PENDING_REMINDER, trackingId={}", trackingId);
        return trackingId;
    }

    public String sendActiveReminder() {
        String trackingId = "brd-active-" + UUID.randomUUID();
        logger.info("Broadcast queued: type=ACTIVE_REMINDER, trackingId={}", trackingId);
        return trackingId;
    }

    public String createOutageBulletin(String title, String body, String territoryId) {
        String trackingId = "bulletin-" + UUID.randomUUID();
        logger.info(
                "Outage bulletin queued: trackingId={}, territoryId={}",
                trackingId,
                PiiMaskingUtil.redactIdentifier(territoryId));
        return trackingId;
    }

    public String broadcastOutage(String title, String body, String territoryId) {
        String trackingId = "ntf-outage-" + UUID.randomUUID();
        logger.info(
                "Outage notification queued: trackingId={}, territoryId={}",
                trackingId,
                PiiMaskingUtil.redactIdentifier(territoryId));
        return trackingId;
    }

    public String dispatchCustomerAlert(String customerId, List<Integer> months, String message) {
        String trackingId = "ntf-dispatch-" + UUID.randomUUID();
        logger.info(
                "Customer alert queued: trackingId={}, customerRef={}, monthCount={}",
                trackingId,
                PiiMaskingUtil.redactIdentifier(customerId),
                months != null ? months.size() : 0);
        return trackingId;
    }

    public String dispatchGenericAlert(Map<String, Object> payload) {
        String trackingId = "ntf-generic-" + UUID.randomUUID();
        logger.info("Generic alert queued: trackingId={}, payloadKeyCount={}", trackingId,
                payload != null ? payload.size() : 0);
        return trackingId;
    }
}
