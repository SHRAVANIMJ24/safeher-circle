package com.safeher.backend.controller;

import com.safeher.backend.dto.OrganisationResponse;
import com.safeher.backend.service.DirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Open to everyone, signed in or not.
 *
 * This is the one part of the app someone might reach in an emergency, and
 * asking her to create an account before showing her a phone number would be
 * indefensible.
 */
@RestController
@RequestMapping("/api/directory")
@RequiredArgsConstructor
public class DirectoryController {

    private final DirectoryService directoryService;

    @GetMapping
    public ResponseEntity<List<OrganisationResponse>> list(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String type) {

        return ResponseEntity.ok(directoryService.list(city, type));
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> cities() {
        return ResponseEntity.ok(directoryService.cities());
    }
}
