package com.leonardovictormuller.napstop.domain.model

/**
 * Represents the user's desired stop destination.
 * The name is sanitized on construction.
 * Coordinates are validated via [GeoPoint].
 */
data class StopDestination(
    val name: String,
    val location: GeoPoint,
) {
    init {
        require(name.isNotBlank()) { "Destination name must not be blank." }
    }

    companion object {
        /** Sanitizes a raw string input: trims whitespace and removes control characters. */
        fun sanitizeName(raw: String): String =
            raw.trim().replace(Regex("[\\p{Cntrl}]"), "")
    }
}
