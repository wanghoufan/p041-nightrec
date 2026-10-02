package com.nightrec.app.recovery

/** 标准化采集失败原因（T071）。 */
enum class CaptureFailureKind {
    MIC_BUSY,
    PERMISSION_LOST,
    DEVICE,
    ENCODER,
    STORAGE_LOW,
}

object CaptureFailure {
    fun of(reason: String): CaptureFailureKind = when (reason.uppercase()) {
        "MIC_BUSY", "AUDIORECORD_INIT", "STATE_INITIALIZED" -> CaptureFailureKind.MIC_BUSY
        "PERMISSION_LOST", "SECURITY_EXCEPTION" -> CaptureFailureKind.PERMISSION_LOST
        "STORAGE_LOW" -> CaptureFailureKind.STORAGE_LOW
        "ENCODER", "ILLEGALSTATE", "MEDIACODEC" -> CaptureFailureKind.ENCODER
        else -> CaptureFailureKind.DEVICE
    }

    /** 是否值得短退避后自动恢复。 */
    fun recoverable(kind: CaptureFailureKind): Boolean = when (kind) {
        CaptureFailureKind.MIC_BUSY, CaptureFailureKind.DEVICE -> true
        CaptureFailureKind.PERMISSION_LOST, CaptureFailureKind.ENCODER, CaptureFailureKind.STORAGE_LOW -> false
    }
}