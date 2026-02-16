package eventplanner.features.attendee.service;

import eventplanner.features.attendee.entity.AttendeeInvite;
import eventplanner.features.attendee.enums.AttendeeInviteStatus;
import eventplanner.features.attendee.repository.AttendeeInviteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendeeInviteExpirationService {

    private final AttendeeInviteRepository inviteRepository;
    private final Clock clock;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expirePendingInvites() {
        try {
            LocalDateTime now = LocalDateTime.now(clock);
            List<AttendeeInvite> expired = inviteRepository.findExpiredPendingInvites(now);
            if (expired.isEmpty()) {
                return;
            }
            for (AttendeeInvite invite : expired) {
                invite.setStatus(AttendeeInviteStatus.EXPIRED);
                invite.setRespondedAt(now);
            }
            inviteRepository.saveAll(expired);
        } catch (Exception e) {
            // Best-effort cleanup
        }
    }
}
