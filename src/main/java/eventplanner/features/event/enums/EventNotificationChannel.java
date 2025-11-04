package eventplanner.features.event.enums;

import eventplanner.common.domain.enums.CommunicationType;

public enum EventNotificationChannel {
    EMAIL(CommunicationType.EMAIL),
    SMS(CommunicationType.SMS),
    PUSH(CommunicationType.PUSH_NOTIFICATION);

    private final CommunicationType communicationType;

    EventNotificationChannel(CommunicationType communicationType) {
        this.communicationType = communicationType;
    }

    public CommunicationType toCommunicationType() {
        return communicationType;
    }
}
