package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.ManeuverModifier.LEFT
import com.mapbox.api.directions.v5.models.ManeuverModifier.RIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SHARP_LEFT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SHARP_RIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SLIGHT_LEFT
import com.mapbox.api.directions.v5.models.ManeuverModifier.SLIGHT_RIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.STRAIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.UTURN
import com.mapbox.api.directions.v5.models.StepManeuver.ARRIVE
import com.mapbox.api.directions.v5.models.StepManeuver.DEPART
import com.mapbox.api.directions.v5.models.StepManeuver.END_OF_ROAD
import com.mapbox.api.directions.v5.models.StepManeuver.EXIT_ROTARY
import com.mapbox.api.directions.v5.models.StepManeuver.EXIT_ROUNDABOUT
import com.mapbox.api.directions.v5.models.StepManeuver.FORK
import com.mapbox.api.directions.v5.models.StepManeuver.MERGE
import com.mapbox.api.directions.v5.models.StepManeuver.OFF_RAMP
import com.mapbox.api.directions.v5.models.StepManeuver.ON_RAMP
import com.mapbox.api.directions.v5.models.StepManeuver.ROTARY
import com.mapbox.api.directions.v5.models.StepManeuver.ROUNDABOUT
import com.mapbox.api.directions.v5.models.StepManeuver.ROUNDABOUT_TURN
import com.mapbox.api.directions.v5.models.StepManeuver.TURN
import com.mapbox.navigation.ui.maneuver.model.TurnIcon
import com.mapbox.navigation.ui.maneuver.model.TurnIconPair
import com.mapbox.navigation.ui.maneuver.model.TurnIconResources
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import kotlin.math.roundToInt

internal class TurnIconHelper(
    turnIconResources: TurnIconResources
) {

    companion object {
        const val DRIVING_SIDE_LEFT = "left"
        const val DRIVING_SIDE_RIGHT = "right"
        const val ROTATION_ANGLE_0 = 0f
        const val ROTATION_ANGLE_45 = 45f
        const val ROTATION_ANGLE_90 = 90f
        const val ROTATION_ANGLE_135 = 135f
        const val ROTATION_ANGLE_180 = 180f
        const val ROTATION_ANGLE_225 = 225f
        const val ROTATION_ANGLE_270 = 270f
        const val ROTATION_ANGLE_315 = 315f
        const val ROTATION_ANGLE_360 = 360f
    }

    // TurnIconPair<Type, Modifier>
    private val turnIconMap: Map<TurnIconPair, Int> = mapOf(
        // When type == null and modifier == null
        TurnIconPair(null, null) to turnIconResources.turnIconTurnStraight,
        TurnIconPair("", "") to turnIconResources.turnIconTurnStraight,

        // When type != null and modifier == null
        TurnIconPair(ARRIVE, null) to turnIconResources.turnIconArrive,
        TurnIconPair(DEPART, null) to turnIconResources.turnIconDepart,
        TurnIconPair(ON_RAMP, null) to turnIconResources.turnIconOnRamp,
        TurnIconPair(OFF_RAMP, null) to turnIconResources.turnIconOffRamp,
        TurnIconPair(FORK, null) to turnIconResources.turnIconFork,
        TurnIconPair(TURN, null) to turnIconResources.turnIconTurnStraight,
        TurnIconPair(MERGE, null) to turnIconResources.turnIconMergeStraight,
        TurnIconPair(END_OF_ROAD, null) to turnIconResources.turnIconEndRoadLeft,

        // When type = null and modifier != null
        TurnIconPair(null, LEFT) to turnIconResources.turnIconTurnLeft,
        TurnIconPair(null, RIGHT) to turnIconResources.turnIconTurnRight,
        TurnIconPair(null, STRAIGHT) to turnIconResources.turnIconTurnStraight,
        TurnIconPair(null, UTURN) to turnIconResources.turnIconUturn,
        TurnIconPair(null, SLIGHT_LEFT) to turnIconResources.turnIconTurnSlightLeft,
        TurnIconPair(null, SLIGHT_RIGHT) to turnIconResources.turnIconTurnSlightRight,
        TurnIconPair(null, SHARP_LEFT) to turnIconResources.turnIconTurnSharpLeft,
        TurnIconPair(null, SHARP_RIGHT) to turnIconResources.turnIconTurnSharpRight,

        // When type != null and modifier != null
        TurnIconPair(ARRIVE, LEFT) to turnIconResources.turnIconArriveLeft,
        TurnIconPair(ARRIVE, RIGHT) to turnIconResources.turnIconArriveRight,
        TurnIconPair(ARRIVE, STRAIGHT) to turnIconResources.turnIconArriveStraight,
        TurnIconPair(DEPART, LEFT) to turnIconResources.turnIconDepartLeft,
        TurnIconPair(DEPART, RIGHT) to turnIconResources.turnIconDepartRight,
        TurnIconPair(DEPART, STRAIGHT) to turnIconResources.turnIconDepartStraight,
        TurnIconPair(END_OF_ROAD, LEFT) to turnIconResources.turnIconEndRoadLeft,
        TurnIconPair(END_OF_ROAD, RIGHT) to turnIconResources.turnIconEndRoadRight,
        TurnIconPair(FORK, LEFT) to turnIconResources.turnIconForkLeft,
        TurnIconPair(FORK, RIGHT) to turnIconResources.turnIconForkRight,
        TurnIconPair(FORK, STRAIGHT) to turnIconResources.turnIconForkStraight,
        TurnIconPair(FORK, SLIGHT_LEFT) to turnIconResources.turnIconForkSlightLeft,
        TurnIconPair(FORK, SLIGHT_RIGHT) to turnIconResources.turnIconForkSlightRight,
        TurnIconPair(MERGE, LEFT) to turnIconResources.turnIconMergeLeft,
        TurnIconPair(MERGE, RIGHT) to turnIconResources.turnIconMergeRight,
        TurnIconPair(MERGE, STRAIGHT) to turnIconResources.turnIconMergeStraight,
        TurnIconPair(MERGE, SLIGHT_LEFT) to turnIconResources.turnIconMergeSlightLeft,
        TurnIconPair(MERGE, SLIGHT_RIGHT) to turnIconResources.turnIconMergeSlightRight,
        TurnIconPair(OFF_RAMP, LEFT) to turnIconResources.turnIconOffRampLeft,
        TurnIconPair(OFF_RAMP, RIGHT) to turnIconResources.turnIconOffRampRight,
        TurnIconPair(OFF_RAMP, SLIGHT_LEFT) to turnIconResources.turnIconOffRampSlightLeft,
        TurnIconPair(OFF_RAMP, SLIGHT_RIGHT) to turnIconResources.turnIconOffRampSlightRight,
        TurnIconPair(ON_RAMP, LEFT) to turnIconResources.turnIconOnRampLeft,
        TurnIconPair(ON_RAMP, RIGHT) to turnIconResources.turnIconOnRampRight,
        TurnIconPair(ON_RAMP, STRAIGHT) to turnIconResources.turnIconOnRampStraight,
        TurnIconPair(ON_RAMP, SLIGHT_LEFT) to turnIconResources.turnIconOnRampSlightLeft,
        TurnIconPair(ON_RAMP, SLIGHT_RIGHT) to turnIconResources.turnIconOnRampSlightRight,
        TurnIconPair(ON_RAMP, SHARP_LEFT) to turnIconResources.turnIconOnRampSharpLeft,
        TurnIconPair(ON_RAMP, SHARP_RIGHT) to turnIconResources.turnIconOnRampSharpRight,
        TurnIconPair(TURN, LEFT) to turnIconResources.turnIconTurnLeft,
        TurnIconPair(TURN, RIGHT) to turnIconResources.turnIconTurnRight,
        TurnIconPair(TURN, UTURN) to turnIconResources.turnIconUturn,
        TurnIconPair(TURN, STRAIGHT) to turnIconResources.turnIconTurnStraight,
        TurnIconPair(TURN, SLIGHT_LEFT) to turnIconResources.turnIconTurnSlightLeft,
        TurnIconPair(TURN, SLIGHT_RIGHT) to turnIconResources.turnIconTurnSlightRight,
        TurnIconPair(TURN, SHARP_LEFT) to turnIconResources.turnIconTurnSharpLeft,
        TurnIconPair(TURN, SHARP_RIGHT) to turnIconResources.turnIconTurnSharpRight
    )

    private val roundaboutIconMap: Map<TurnIconPair, Int> = mapOf(
        TurnIconPair(null, null) to turnIconResources.turnIconRoundabout,
        TurnIconPair(ROUNDABOUT, LEFT) to turnIconResources.turnIconRoundaboutLeft,
        TurnIconPair(ROUNDABOUT, RIGHT) to turnIconResources.turnIconRoundaboutRight,
        TurnIconPair(ROUNDABOUT, STRAIGHT) to turnIconResources.turnIconRoundaboutStraight,
        TurnIconPair(ROUNDABOUT, SHARP_LEFT) to turnIconResources.turnIconRoundaboutSharpLeft,
        TurnIconPair(ROUNDABOUT, SHARP_RIGHT) to turnIconResources.turnIconRoundaboutSharpRight,
        TurnIconPair(ROUNDABOUT, SLIGHT_LEFT) to turnIconResources.turnIconRoundaboutSlightLeft,
        TurnIconPair(ROUNDABOUT, SLIGHT_RIGHT) to turnIconResources.turnIconRoundaboutSlightRight,
        TurnIconPair(ROUNDABOUT_TURN, LEFT) to turnIconResources.turnIconRoundaboutLeft,
        TurnIconPair(ROUNDABOUT_TURN, RIGHT) to turnIconResources.turnIconRoundaboutRight,
        TurnIconPair(ROUNDABOUT_TURN, STRAIGHT) to
            turnIconResources.turnIconRoundaboutStraight,
        TurnIconPair(ROUNDABOUT_TURN, SHARP_LEFT) to
            turnIconResources.turnIconRoundaboutSharpLeft,
        TurnIconPair(ROUNDABOUT_TURN, SHARP_RIGHT) to
            turnIconResources.turnIconRoundaboutSharpRight,
        TurnIconPair(ROUNDABOUT_TURN, SLIGHT_LEFT) to
            turnIconResources.turnIconRoundaboutSlightLeft,
        TurnIconPair(ROUNDABOUT_TURN, SLIGHT_RIGHT) to
            turnIconResources.turnIconRoundaboutSlightRight,
        TurnIconPair(EXIT_ROUNDABOUT, LEFT) to turnIconResources.turnIconRoundaboutLeft,
        TurnIconPair(EXIT_ROUNDABOUT, RIGHT) to turnIconResources.turnIconRoundaboutRight,
        TurnIconPair(EXIT_ROUNDABOUT, STRAIGHT) to turnIconResources.turnIconRoundaboutStraight,
        TurnIconPair(EXIT_ROUNDABOUT, SHARP_LEFT) to
            turnIconResources.turnIconRoundaboutSharpLeft,
        TurnIconPair(EXIT_ROUNDABOUT, SHARP_RIGHT) to
            turnIconResources.turnIconRoundaboutSharpRight,
        TurnIconPair(EXIT_ROUNDABOUT, SLIGHT_LEFT) to
            turnIconResources.turnIconRoundaboutSlightLeft,
        TurnIconPair(EXIT_ROUNDABOUT, SLIGHT_RIGHT) to
            turnIconResources.turnIconRoundaboutSlightRight,
        TurnIconPair(ROTARY, LEFT) to turnIconResources.turnIconRoundaboutLeft,
        TurnIconPair(ROTARY, RIGHT) to turnIconResources.turnIconRoundaboutRight,
        TurnIconPair(ROTARY, STRAIGHT) to turnIconResources.turnIconRoundaboutStraight,
        TurnIconPair(ROTARY, SHARP_LEFT) to turnIconResources.turnIconRoundaboutSharpLeft,
        TurnIconPair(ROTARY, SHARP_RIGHT) to turnIconResources.turnIconRoundaboutSharpRight,
        TurnIconPair(ROTARY, SLIGHT_LEFT) to turnIconResources.turnIconRoundaboutSlightLeft,
        TurnIconPair(ROTARY, SLIGHT_RIGHT) to turnIconResources.turnIconRoundaboutSlightRight,
        TurnIconPair(EXIT_ROTARY, LEFT) to turnIconResources.turnIconRoundaboutLeft,
        TurnIconPair(EXIT_ROTARY, RIGHT) to turnIconResources.turnIconRoundaboutRight,
        TurnIconPair(EXIT_ROTARY, STRAIGHT) to turnIconResources.turnIconRoundaboutStraight,
        TurnIconPair(EXIT_ROTARY, SHARP_LEFT) to turnIconResources.turnIconRoundaboutSharpLeft,
        TurnIconPair(EXIT_ROTARY, SHARP_RIGHT) to turnIconResources.turnIconRoundaboutSharpRight,
        TurnIconPair(EXIT_ROTARY, SLIGHT_LEFT) to turnIconResources.turnIconRoundaboutSlightLeft,
        TurnIconPair(EXIT_ROTARY, SLIGHT_RIGHT) to turnIconResources.turnIconRoundaboutSlightRight
    )

    fun retrieveTurnIcon(
        type: String?,
        degrees: Float?,
        modifier: String?,
        drivingSide: String?,
    ): TurnIcon? {
        val isRoundabout = isManeuverRoundabout(type)
        return when {
            isRoundabout -> {
                generateRoundaboutIcons(type, degrees, drivingSide)
            }
            else -> {
                generateTurnIcons(type, degrees, modifier, drivingSide)
            }
        }
    }

    private fun generateRoundaboutIcons(
        type: String?,
        degrees: Float?,
        drivingSide: String?,
    ): TurnIcon? {
        return ifNonNull(degrees) { rotateBy ->
            val angleAfterRounding =
                ((rotateBy / ROTATION_ANGLE_45).roundToInt()) * ROTATION_ANGLE_45
            val shouldFlip = shouldFlipIcon(drivingSide)
            when (angleAfterRounding) {
                ROTATION_ANGLE_0, ROTATION_ANGLE_45 -> {
                    TurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[TurnIconPair(type, SHARP_RIGHT)]
                    )
                }
                ROTATION_ANGLE_90 -> {
                    TurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[TurnIconPair(type, RIGHT)]
                    )
                }
                ROTATION_ANGLE_135 -> {
                    TurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[TurnIconPair(type, SLIGHT_RIGHT)]
                    )
                }
                ROTATION_ANGLE_180 -> {
                    TurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[TurnIconPair(type, STRAIGHT)]
                    )
                }
                ROTATION_ANGLE_225 -> {
                    TurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[TurnIconPair(type, SLIGHT_LEFT)]
                    )
                }
                ROTATION_ANGLE_270 -> {
                    TurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[TurnIconPair(type, LEFT)]
                    )
                }
                ROTATION_ANGLE_315, ROTATION_ANGLE_360 -> {
                    TurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[TurnIconPair(type, SHARP_LEFT)]
                    )
                }
                else -> {
                    TurnIcon(
                        degrees,
                        drivingSide,
                        shouldFlip,
                        roundaboutIconMap[TurnIconPair(null, null)]
                    )
                }
            }
        } ?: TurnIcon(
            degrees,
            drivingSide,
            shouldFlipIcon(drivingSide),
            roundaboutIconMap[TurnIconPair(null, null)]
        )
    }

    private fun generateTurnIcons(
        type: String?,
        degrees: Float?,
        modifier: String?,
        drivingSide: String?
    ): TurnIcon? {
        return when {
            type.isNullOrEmpty() && modifier.isNullOrEmpty() -> {
                TurnIcon(
                    degrees,
                    drivingSide,
                    false,
                    turnIconMap[TurnIconPair(null, null)]
                )
            }
            type.isNullOrEmpty() && !modifier.isNullOrEmpty() -> {
                TurnIcon(
                    degrees,
                    drivingSide,
                    false,
                    getTurnIconWithNullType(modifier)
                )
            }
            !type.isNullOrEmpty() && modifier.isNullOrEmpty() -> {
                TurnIcon(
                    degrees,
                    drivingSide,
                    false,
                    getTurnIconWithNullModifier(type)
                )
            }
            !type.isNullOrEmpty() && !modifier.isNullOrEmpty() -> {
                TurnIcon(
                    degrees,
                    drivingSide,
                    false,
                    getTurnIconWithTypeAndModifier(type, modifier)
                )
            }
            else -> null
        }
    }

    private fun getTurnIconWithNullType(modifier: String): Int? {
        return when (modifier) {
            LEFT, RIGHT, STRAIGHT, UTURN, SLIGHT_RIGHT, SLIGHT_LEFT, SHARP_RIGHT, SHARP_LEFT -> {
                turnIconMap[TurnIconPair(null, modifier)]
            }
            else -> {
                turnIconMap[TurnIconPair(null, null)]
            }
        }
    }

    private fun getTurnIconWithNullModifier(type: String): Int? {
        return when (type) {
            ARRIVE, DEPART, ON_RAMP, OFF_RAMP, FORK, TURN, MERGE, END_OF_ROAD -> {
                turnIconMap[TurnIconPair(type, null)]
            }
            else -> {
                turnIconMap[TurnIconPair(null, null)]
            }
        }
    }

    private fun getTurnIconWithTypeAndModifier(type: String, modifier: String): Int? {
        return when {
            type == ARRIVE && modifier == LEFT ||
                type == ARRIVE && modifier == RIGHT ||
                type == ARRIVE && modifier == STRAIGHT ||
                type == DEPART && modifier == LEFT ||
                type == DEPART && modifier == RIGHT ||
                type == DEPART && modifier == STRAIGHT ||
                type == END_OF_ROAD && modifier == LEFT ||
                type == END_OF_ROAD && modifier == RIGHT ||
                type == FORK && modifier == LEFT ||
                type == FORK && modifier == RIGHT ||
                type == FORK && modifier == STRAIGHT ||
                type == FORK && modifier == SLIGHT_LEFT ||
                type == FORK && modifier == SLIGHT_RIGHT ||
                type == MERGE && modifier == LEFT ||
                type == MERGE && modifier == RIGHT ||
                type == MERGE && modifier == STRAIGHT ||
                type == MERGE && modifier == SLIGHT_LEFT ||
                type == MERGE && modifier == SLIGHT_RIGHT ||
                type == OFF_RAMP && modifier == LEFT ||
                type == OFF_RAMP && modifier == RIGHT ||
                type == OFF_RAMP && modifier == SLIGHT_LEFT ||
                type == OFF_RAMP && modifier == SLIGHT_RIGHT ||
                type == ON_RAMP && modifier == LEFT ||
                type == ON_RAMP && modifier == RIGHT ||
                type == ON_RAMP && modifier == STRAIGHT ||
                type == ON_RAMP && modifier == SLIGHT_LEFT ||
                type == ON_RAMP && modifier == SLIGHT_RIGHT ||
                type == ON_RAMP && modifier == SHARP_LEFT ||
                type == ON_RAMP && modifier == SHARP_RIGHT ||
                type == TURN && modifier == LEFT ||
                type == TURN && modifier == RIGHT ||
                type == TURN && modifier == UTURN ||
                type == TURN && modifier == STRAIGHT ||
                type == TURN && modifier == SLIGHT_LEFT ||
                type == TURN && modifier == SLIGHT_RIGHT ||
                type == TURN && modifier == SHARP_LEFT ||
                type == TURN && modifier == SHARP_RIGHT -> turnIconMap[TurnIconPair(type, modifier)]
            else -> {
                turnIconMap[TurnIconPair(null, null)]
            }
        }
    }

    private fun shouldFlipIcon(drivingSide: String?): Boolean {
        return when {
            !drivingSide.isNullOrEmpty() && drivingSide == DRIVING_SIDE_LEFT -> true
            !drivingSide.isNullOrEmpty() && drivingSide == DRIVING_SIDE_RIGHT -> false
            else -> false
        }
    }

    private fun isManeuverRoundabout(type: String?): Boolean {
        return when {
            !type.isNullOrEmpty()
                && (
                    type == ROUNDABOUT ||
                        type == ROUNDABOUT_TURN ||
                        type == EXIT_ROUNDABOUT ||
                        type == ROTARY ||
                        type == EXIT_ROTARY
                    ) -> {
                true
            }
            else -> false
        }
    }
}
