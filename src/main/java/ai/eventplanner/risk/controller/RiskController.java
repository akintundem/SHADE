package ai.eventplanner.risk.controller;

import ai.eventplanner.risk.dto.response.RiskResponse;
import ai.eventplanner.risk.service.RiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/risks")
@Tag(name = "Risks")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Get risk alerts")
    public reactor.core.publisher.Mono<ResponseEntity<List<RiskResponse>>> getRisks(@PathVariable("eventId") String eventId,
                                                                                    @RequestParam(value = "lat", defaultValue = "0") String lat,
                                                                                    @RequestParam(value = "lon", defaultValue = "0") String lon) {
        return riskService.computeRisks(eventId, lat, lon)
                .map(riskMaps -> riskMaps.stream()
                        .map(this::convertToRiskResponse)
                        .toList())
                .map(ResponseEntity::ok);
    }

    private RiskResponse convertToRiskResponse(java.util.Map<String, Object> riskMap) {
        RiskResponse response = new RiskResponse();
        response.setRiskId((String) riskMap.get("id"));
        response.setRiskType((String) riskMap.get("type"));
        response.setRiskLevel((String) riskMap.get("level"));
        response.setTitle((String) riskMap.get("title"));
        response.setDescription((String) riskMap.get("description"));
        response.setProbability((Double) riskMap.get("probability"));
        response.setImpact((String) riskMap.get("impact"));
        response.setMitigation((String) riskMap.get("mitigation"));
        response.setMetadata(riskMap);
        return response;
    }
}


