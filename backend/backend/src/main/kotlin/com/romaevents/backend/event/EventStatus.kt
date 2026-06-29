package com.romaevents.backend.event

//EventStatus is an enumeration that defines the possible statuses of an event. It includes two values: ACTIVE_NOW, which indicates that the event is currently active and happening, and UPCOMING, which indicates that the event is scheduled for a future date and time. This enum can be used to categorize events based on their timing and to filter events accordingly in the application.
enum class EventStatus {
    ACTIVE_NOW,
    UPCOMING
}