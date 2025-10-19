package ai.eventplanner.payments.controller;

import ai.eventplanner.payments.service.PaymentsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments")
public class PaymentsController {

	private final PaymentsService paymentsService;

	public PaymentsController(PaymentsService paymentsService) {
		this.paymentsService = paymentsService;
	}

	@PostMapping("/intent")
	@Operation(summary = "Create payment intent (planning fees)")
	public ResponseEntity<Map<String, Object>> createIntent(@Valid @RequestBody Map<String, Object> payload) throws Exception {
		Long amount = Long.parseLong(payload.getOrDefault("amountCents", 0).toString());
		String currency = payload.getOrDefault("currency", "usd").toString();
		String userId = payload.getOrDefault("userId", "").toString();
		return ResponseEntity.ok(paymentsService.createPlatformFeeIntent(amount, currency, userId));
	}

	@PostMapping("/confirm")
	@Operation(summary = "Confirm payment")
	public ResponseEntity<Map<String, Object>> confirm(@Valid @RequestBody Map<String, Object> payload) {
		return ResponseEntity.ok(Map.of("status", "client_confirm"));
	}
}


