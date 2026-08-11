package com.safeher.backend.controller;

import com.safeher.backend.dto.CreateReportRequest;
import com.safeher.backend.entity.User;
import com.safeher.backend.exception.ApiException;
import com.safeher.backend.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Reporting requires an account.
     *
     * Anonymous reporting sounds kinder but it removes the only brake on mass
     * false reporting, and a brigading campaign is a far more likely threat
     * here than someone being unable to sign up.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> report(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateReportRequest request) {

        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Sign in to report something.");
        }

        reportService.report(user, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Thanks. A moderator will look at this."));
    }
}
