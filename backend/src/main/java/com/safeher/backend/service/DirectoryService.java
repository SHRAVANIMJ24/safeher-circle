package com.safeher.backend.service;

import com.safeher.backend.dto.OrganisationResponse;
import com.safeher.backend.entity.OrgType;
import com.safeher.backend.exception.ApiException;
import com.safeher.backend.repository.OrganisationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectoryService {

    private final OrganisationRepository organisationRepository;

    @Transactional(readOnly = true)
    public List<OrganisationResponse> list(String city, String type) {
        var results = (city == null || city.isBlank())
                ? organisationRepository.findAllOrdered()
                : organisationRepository.findForCity(city.trim());

        var stream = results.stream();

        if (type != null && !type.isBlank()) {
            OrgType parsed = parseType(type);
            stream = stream.filter(o -> o.getOrgType() == parsed);
        }

        return stream.map(OrganisationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<String> cities() {
        return organisationRepository.findCities();
    }

    private OrgType parseType(String type) {
        try {
            return OrgType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Unknown type. Use HELPLINE, NGO, SHELTER, LEGAL_AID or POLICE.");
        }
    }
}
