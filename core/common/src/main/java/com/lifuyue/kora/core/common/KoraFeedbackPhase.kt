package com.lifuyue.kora.core.common

enum class KoraFeedbackPhase {
    Idle,
    Validating,
    SuccessTransient,
    SuccessStable,
    ErrorRecoverable,
    InFlightFirstByte,
    InFlightStreaming,
    Stopped,
}
