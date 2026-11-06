package com.dujiajun.courseblock

import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.dujiajun.courseblock.helper.CourseManager

internal class CourseSyncViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    enum class Outcome { SYNCED, NEEDS_LOGIN, STILL_UNAUTHENTICATED, FAILED }

    private val pendingOutcome = MutableLiveData<Outcome?>()
    val outcome: LiveData<Outcome?> = pendingOutcome
    private var downloading = false

    var waitingForLogin: Boolean
        get() = savedState["waiting_for_sync_login"] ?: false
        set(value) { savedState["waiting_for_sync_login"] = value }

    val isBusy: Boolean
        get() = downloading || waitingForLogin || pendingOutcome.value != null

    fun start(courseManager: CourseManager, allowLogin: Boolean) {
        if (isBusy) return
        downloading = true
        courseManager.updateCourses(object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                downloading = false
                pendingOutcome.value = when (msg.what) {
                    CourseManager.SUCCEEDED -> Outcome.SYNCED
                    CourseManager.UNLOGIN ->
                        if (allowLogin) Outcome.NEEDS_LOGIN else Outcome.STILL_UNAUTHENTICATED
                    else -> Outcome.FAILED
                }
            }
        })
    }

    fun clearOutcome() {
        pendingOutcome.value = null
    }
}
