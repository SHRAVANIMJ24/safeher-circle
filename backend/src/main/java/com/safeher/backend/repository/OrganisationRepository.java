package com.safeher.backend.repository;

import com.safeher.backend.entity.Organisation;
import com.safeher.backend.entity.OrgType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OrganisationRepository extends JpaRepository<Organisation, UUID> {

    /**
     * National entries (city is null) always come back, whatever city was
     * asked for. A woman in Nashik still needs to see 112 and 181.
     */
    @Query("""
            SELECT o FROM Organisation o
            WHERE o.city IS NULL
               OR LOWER(o.city) = LOWER(:city)
            ORDER BY
                CASE WHEN o.city IS NULL THEN 0 ELSE 1 END,
                o.orgType,
                o.name
            """)
    List<Organisation> findForCity(String city);

    List<Organisation> findByOrgTypeOrderByName(OrgType orgType);

    @Query("SELECT o FROM Organisation o ORDER BY "
         + "CASE WHEN o.city IS NULL THEN 0 ELSE 1 END, o.city, o.orgType, o.name")
    List<Organisation> findAllOrdered();

    @Query("SELECT DISTINCT o.city FROM Organisation o "
         + "WHERE o.city IS NOT NULL ORDER BY o.city")
    List<String> findCities();
}
