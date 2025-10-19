package ai.eventplanner.comms.controller;

import ai.eventplanner.comms.model.CommunicationLogEntity;
import ai.eventplanner.comms.service.CommsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/comms")
@Tag(name = "Comms")
public class CommsController {

    private final CommsService commsService;

    public CommsController(CommsService commsService) {
        this.commsService = commsService;
    }

    @PostMapping("/email")
    @Operation(summary = "Send email")
    public ResponseEntity<Map<String, Object>> sendEmail(@Valid @RequestBody Map<String, Object> payload) {
        CommunicationLogEntity log = commsService.queueEmail(payload);
        return ResponseEntity.ok(buildResponse(log));
    }

    @PostMapping("/sms")
    @Operation(summary = "Send SMS")
    public ResponseEntity<Map<String, Object>> sendSms(@Valid @RequestBody Map<String, Object> payload) {
        CommunicationLogEntity log = commsService.queueSms(payload);
        return ResponseEntity.ok(buildResponse(log));
    }

    private Map<String, Object> buildResponse(CommunicationLogEntity log) {
        return Map.of(
                "id", log.getId().toString(),
                "channel", log.getChannel(),
                "recipient", log.getRecipient(),
                "status", log.getStatus(),
                "queuedAt", log.getCreatedAt().toString()
        );
    }
}

