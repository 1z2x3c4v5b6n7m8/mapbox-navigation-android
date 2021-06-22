package com.mapbox.navigation.ui.maneuver

import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.ui.maneuver.model.Component
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.utils.internal.LoggerProvider
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.monitorChannelWithException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class RoadShieldContentManager {
    private companion object {
        private val TAG = Tag("MbxRoadShieldContentManager")
    }

    private val maneuversToShieldsMap = hashMapOf<String, ByteArray?>()
    private val requestedShields = mutableListOf<String>()
    private val urlsToShieldsMap = hashMapOf<String, ByteArray?>()

    private val job = ThreadController.getMainScopeAndRootJob()
    private val invalidationChannel = Channel<Unit>()
    private val awaitingCallbacks = mutableListOf<() -> Boolean>()

    init {
        job.scope.monitorChannelWithException(
            invalidationChannel,
            {
                val iterator = awaitingCallbacks.iterator()
                while (iterator.hasNext()) {
                    val remove = iterator.next().invoke()
                    if (remove) {
                        iterator.remove()
                    }
                }
            }
        )
    }

    suspend fun getShields(
        maneuvers: List<Maneuver>
    ): Map<String, RoadShield?> {
        val idToUrlMap = hashMapOf<String, String?>()
        maneuvers.forEach { maneuver ->
            maneuver.primary.let {
                idToUrlMap[it.id] = findShieldUrl(it.componentList)
            }
            maneuver.secondary?.let {
                idToUrlMap[it.id] = findShieldUrl(it.componentList)
            }
            maneuver.sub?.let {
                idToUrlMap[it.id] = findShieldUrl(it.componentList)
            }
        }

        job.scope.launch {
            prepareShields(idToUrlMap)
        }

        return waitForShields(idToUrlMap)
    }

    fun cancel() {
        // TODO: cancel downloads and coroutines
    }

    private suspend fun waitForShields(
        idToUrlMap: Map<String, String?>
    ): Map<String, RoadShield?> {
        return suspendCoroutine { continuation ->
            awaitingCallbacks.add {
                if (idToUrlMap.keys.all { maneuversToShieldsMap.containsKey(it) }) {
                    continuation.resume(
                        maneuversToShieldsMap
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
                    )
                    return@add true
                } else {
                    return@add false
                }
            }
        }
    }

    private suspend fun prepareShields(idToUrlMap: Map<String, String?>) {
        idToUrlMap.forEach { entry ->
            val id = entry.key
            val url = entry.value
            if (!maneuversToShieldsMap.containsKey(id) && !requestedShields.contains(id)) {
                if (url != null) {
                    val availableShield = urlsToShieldsMap[url]
                    if (availableShield != null) {
                        maneuversToShieldsMap[id] = availableShield
                    } else {
                        requestedShields.add(id)
                        RoadShieldDownloader.downloadImage(url).fold(
                            { error ->
                                LoggerProvider.logger.e(TAG, Message(error))
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

        invalidationChannel.send(Unit)
    }

    private fun findShieldUrl(components: List<Component>): String? {
        val node = components.find { it.node is RoadShieldComponentNode }?.node
        return if (node is RoadShieldComponentNode) {
            node.shieldUrl
        } else {
            null
        }
    }
}
