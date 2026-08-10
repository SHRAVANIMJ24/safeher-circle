package com.safeher.backend.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Blurs a coordinate before it is stored against a post.
 *
 * Why this exists: someone posting about a domestic situation from home, with
 * an exact coordinate and a timestamp, has effectively published their address.
 * Rounding to two decimal places puts the point somewhere inside roughly a
 * 1.1km square, which is enough to answer "is this near me" and not enough to
 * find a building.
 *
 * SOS alerts deliberately skip this — there, precision is the whole point.
 */
@Component
public class LocationCoarsener {

    /** Two decimal places ≈ 1.1km at the equator, less nearer the poles. */
    private static final int PRECISION = 2;

    public BigDecimal coarsen(BigDecimal coordinate) {
        if (coordinate == null) {
            return null;
        }
        return coordinate.setScale(PRECISION, RoundingMode.HALF_UP);
    }

    public boolean isValidLatitude(BigDecimal lat) {
        return lat != null
                && lat.compareTo(BigDecimal.valueOf(-90)) >= 0
                && lat.compareTo(BigDecimal.valueOf(90)) <= 0;
    }

    public boolean isValidLongitude(BigDecimal lng) {
        return lng != null
                && lng.compareTo(BigDecimal.valueOf(-180)) >= 0
                && lng.compareTo(BigDecimal.valueOf(180)) <= 0;
    }
}
