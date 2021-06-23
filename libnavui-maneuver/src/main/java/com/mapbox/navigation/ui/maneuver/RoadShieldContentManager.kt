package com.mapbox.navigation.ui.maneuver

import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.ui.maneuver.model.Component
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.ui.maneuver.model.RoadShieldError
import com.mapbox.navigation.ui.maneuver.model.RoadShieldResult
import com.mapbox.navigation.utils.internal.LoggerProvider
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class RoadShieldContentManager {
    private companion object {
        private val TAG = Tag("MbxRoadShieldContentManager")
    }

    private val maneuversToShieldsMap = hashMapOf<String, ByteArray?>()
    private val maneuversToFailuresMap = hashMapOf<String, RoadShieldError>()
    private val requestedShields = mutableListOf<String>()
    private val urlsToShieldsMap = hashMapOf<String, ByteArray?>()

    private val mainJob = ThreadController.getMainScopeAndRootJob()
    private val awaitingCallbacks = mutableListOf<() -> Boolean>()

    suspend fun getShields(
        startIndex: Int,
        endIndex: Int,
        maneuvers: List<Maneuver>
    ): RoadShieldResult {
        val range = startIndex..endIndex
        return getShields(
            maneuvers.filterIndexed { index, _ -> index in range }
        )
    }

    private suspend fun getShields(
        maneuvers: List<Maneuver>
    ): RoadShieldResult {
        val idToUrlMap = hashMapOf<String, String?>()
        maneuvers.forEach { maneuver ->
            maneuver.primary.let {
                idToUrlMap[it.id] = it.componentList.findShieldUrl()
            }
            maneuver.secondary?.let {
                idToUrlMap[it.id] = it.componentList.findShieldUrl()
            }
            maneuver.sub?.let {
                idToUrlMap[it.id] = it.componentList.findShieldUrl()
            }
        }

        mainJob.scope.launch {
            prepareShields(idToUrlMap)
        }

        return try {
            waitForShields(idToUrlMap)
        } catch (ex: CancellationException) {
            val errors: Map<String, RoadShieldError> =
                idToUrlMap
                    .filter { it.value != null }
                    .mapValues { RoadShieldError(it.value as String, "canceled") }
            return RoadShieldResult(emptyMap(), errors)
        }
    }

    fun cancel() {
        mainJob.job.children.forEach { it.cancel() }
    }

    private suspend fun prepareShields(idToUrlMap: Map<String, String?>) {
        idToUrlMap.forEach { entry ->
            val id = entry.key
            val url = entry.value
            if (!maneuversToShieldsMap.containsKey(id) && !requestedShields.contains(id)) {
                maneuversToFailuresMap.remove(id)
                if (url != null) {
                    val availableShield = urlsToShieldsMap[url]
                    if (availableShield != null) {
                        maneuversToShieldsMap[id] = availableShield
                    } else {
                        requestedShields.add(id)
                        RoadShieldDownloader.downloadImage(url).fold(
                            { error ->
                                LoggerProvider.logger.e(TAG, Message(error))
                                maneuversToFailuresMap[id] = RoadShieldError(url, error)
                            },
                            { value ->
                                maneuversToShieldsMap[id] = value
                            }
                        )
                        requestedShields.remove(id)
                    }
                } else {
                    maneuversToShieldsMap[id] = null
                }
            }
        }

        invalidate()
    }

    private suspend fun waitForShields(
        idToUrlMap: Map<String, String?>
    ): RoadShieldResult {
        return suspendCancellableCoroutine { continuation ->
            val callback = {
                if (
                    idToUrlMap.keys.all {
                        maneuversToShieldsMap.containsKey(it)
                            || maneuversToFailuresMap.containsKey(it)
                    }
                ) {
                    val shields = maneuversToShieldsMap
                        .filterKeys { idToUrlMap.keys.contains(it) }
                        .mapValues {
                            // TODO: cleanup force casts?
                            val value = it.value
                            if (value != null) {
                                RoadShield(idToUrlMap[it.key]!!, value)
                            } else {
                                null
                            }
                        }
                    val errors = maneuversToFailuresMap.filterKeys { idToUrlMap.keys.contains(it) }
                    continuation.resume(RoadShieldResult(shields, errors))
                    true
                } else {
                    false
                }
            }
            awaitingCallbacks.add(callback)
            continuation.invokeOnCancellation {
                awaitingCallbacks.remove(callback)
            }
        }
    }

    private fun invalidate() {
        val iterator = awaitingCallbacks.iterator()
        while (iterator.hasNext()) {
            val remove = iterator.next().invoke()
            if (remove) {
                iterator.remove()
            }
        }
    }

    private fun List<Component>.findShieldUrl(): String? {
        val node = this.find { it.node is RoadShieldComponentNode }?.node
        return if (node is RoadShieldComponentNode) {
            node.shieldUrl
        } else {
            null
        }
    }
}
