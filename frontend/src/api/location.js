/**
 * Asks the browser where it is.
 *
 * Resolves with null rather than rejecting when location is unavailable. An
 * alarm must never be blocked because a GPS fix failed — an alert with no
 * coordinates is far better than no alert at all.
 */
export function currentPosition(timeoutMs = 8000) {
    return new Promise((resolve) => {
        if (!navigator.geolocation) {
            resolve(null);
            return;
        }

        navigator.geolocation.getCurrentPosition(
            (position) => {
                resolve({
                    latitude: Number(position.coords.latitude.toFixed(6)),
                    longitude: Number(position.coords.longitude.toFixed(6)),
                    accuracyMeters: position.coords.accuracy,
                });
            },
            () => resolve(null),
            { enableHighAccuracy: true, timeout: timeoutMs, maximumAge: 0 }
        );
    });
}
