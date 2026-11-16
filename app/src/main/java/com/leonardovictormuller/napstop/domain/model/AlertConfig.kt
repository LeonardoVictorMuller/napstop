package com.leonardovictormuller.napstop.domain.model

/**
 * Configuration for the two-stage alert system.
 *
 * @param vibrationLeadTimeMinutes Minutes before arrival to trigger vibration alert.
 * @param alarmLeadTimeMinutes     Minutes before arrival to trigger the audible alarm.
 *
 * Business rule: vibration must fire BEFORE the alarm, so vibrationLeadTimeMinutes
 * must be strictly greater than alarmLeadTimeMinutes.
 */
data class AlertConfig(
    val vibrationLeadTimeMinutes: Int,
    val alarmLeadTimeMinutes: Int,
) {
    init {
        require(vibrationLeadTimeMinutes > 0) {
            "vibrationLeadTimeMinutes must be positive, got $vibrationLeadTimeMinutes"
        }
        require(alarmLeadTimeMinutes > 0) {
            "alarmLeadTimeMinutes must be positive, got $alarmLeadTimeMinutes"
        }
        require(vibrationLeadTimeMinutes > alarmLeadTimeMinutes) {
            "vibrationLeadTimeMinutes ($vibrationLeadTimeMinutes) must be greater than " +
                "alarmLeadTimeMinutes ($alarmLeadTimeMinutes) so vibration fires first."
        }
    }
}
