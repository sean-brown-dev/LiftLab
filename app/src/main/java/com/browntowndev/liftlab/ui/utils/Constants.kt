package com.browntowndev.liftlab.ui.utils

object RestTimerNotification {
    const val ALERT_CHANNEL_ID = "RestTimerAlerts"
    const val ALERT_NOTIFICATION_ID = 1002
    const val ALERT_CHANNEL_NAME = "Rest Timer Alerts"
    const val ALERT_TITLE = "Rest Complete!"
    const val ALERT_TEXT = "Time for the next set."
    const val RETURN_TO_WORKOUT_REQUEST_CODE = 2000
    const val SKIP_REST_TIMER_REQUEST_CODE = 2001
    const val DISMISS_NOTIFICATION_REQUEST_CODE = 2002
}

object ActiveWorkoutNotification {
    const val RETURN_TO_WORKOUT_REQUEST_CODE = 2003
}