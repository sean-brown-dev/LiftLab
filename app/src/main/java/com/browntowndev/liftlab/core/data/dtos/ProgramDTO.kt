package com.browntowndev.liftlab.core.data.dtos

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.data.entities.CustomLiftSet
import com.browntowndev.liftlab.core.data.entities.Lift
import com.browntowndev.liftlab.core.data.entities.Program
import com.browntowndev.liftlab.core.data.entities.Workout
import com.browntowndev.liftlab.core.data.entities.WorkoutLift

data class ProgramDto(
    @Embedded
    val program: Program,
    @Relation(parentColumn = "program_id", entityColumn = "programId", entity = Workout::class)
    var workouts: List<WorkoutDto>) {

    @Ignore
    var isDirty = false
    @Ignore
    private var _name: String = program.name
    @Ignore
    private var _currentMicrocycle: Int = program.currentMicrocycle
    @Ignore
    private var _currentMicrocyclePosition: Int = program.currentMicrocyclePosition

    @get:Ignore
    val id
        get() = this.program.id

    @get:Ignore
    var name
        get() = this._name
        set(newName: String) {
            this._name = newName
            this.isDirty = true
        }

    @get:Ignore
    var currentMicrocycle
        get() = this._currentMicrocycle
        set(newMicroCycle: Int) {
            this._currentMicrocycle = newMicroCycle
            this.isDirty = true
        }

    @get:Ignore
    var currentMicrocyclePosition
        get() = this._currentMicrocyclePosition
        set(newMicrocyclePosition: Int) {
            this._currentMicrocyclePosition = newMicrocyclePosition
            this.isDirty = true
        }

    data class WorkoutDto(
        @Embedded
        val workout: Workout,
        @Relation(parentColumn = "workout_id", entityColumn = "workoutId", entity = WorkoutLift::class)
        var lifts: List<WorkoutLiftDto>) {

        @Ignore
        var isDirty = false
        @Ignore
        private var _name = workout.name
        @Ignore
        private var _position = workout.position

        @get:Ignore
        val id
            get() = workout.id

        @get:Ignore
        val programId
            get() = workout.programId

        @get:Ignore
        var name
            get() = _name
            set(newName) {
                _name = newName
                this.isDirty = true
            }

        @get:Ignore
        var position
            get() = _position
            set(newPosition) {
                _position = newPosition
                this.isDirty = true
            }

        data class WorkoutLiftDto(
            @Embedded
            val workoutLift: WorkoutLift,
            @Relation(parentColumn = "liftId", entityColumn =  "lift_id", entity = Lift::class)
            val lift: LiftDto,
            @Relation(parentColumn = "workout_lift_id", entityColumn = "workoutLiftId", entity = CustomLiftSet::class)
            var customLiftSets: List<CustomLiftSetDto>
        ) {
            data class CustomLiftSetDto(
                @Embedded
                val customLiftSet: CustomLiftSet
            ) {
                @Ignore
                private var _position = customLiftSet.position

                var position
                    get() = _position
                    set(newPosition) {
                        this._position = newPosition
                    }
            }

            @Ignore
            var isDirty = true
            @Ignore
            private var _position = workoutLift.position
            @Ignore
            private var _setCount = workoutLift.setCount
            @Ignore
            private var _rpeTarget = workoutLift.rpeTarget
            @Ignore
            private var _repRangeBottom = workoutLift.repRangeBottom
            @Ignore
            private var _repRangeTop = workoutLift.repRangeTop
            @Ignore
            private var _progressionScheme = workoutLift.progressionScheme

            @get:Ignore
            val id
                get() = workoutLift.id

            @get:Ignore
            var position
                get() = _position
                set(position) {
                    _position = position
                    isDirty = true
                }

            @get:Ignore
            var setCount
                get() = _setCount
                set(newSetCount) {
                    _setCount = newSetCount
                    isDirty = true
                }

            @get:Ignore
            var rpeTarget
                get() = _rpeTarget
                set(newRpeTarget) {
                    _rpeTarget = newRpeTarget
                    isDirty = true
                }

            @get:Ignore
            var repRangeBottom
                get() = _repRangeBottom
                set(newRepRangeBottom) {
                    _repRangeBottom = newRepRangeBottom
                    isDirty = true
                }

            @get:Ignore
            var repRangeTop
                get() = _repRangeTop
                set(newRepRangeTop) {
                    _repRangeTop = newRepRangeTop
                    isDirty = true
                }

            @get:Ignore
            var progressionScheme
                get() = _progressionScheme
                set(newProgressionScheme) {
                    _progressionScheme = newProgressionScheme
                    isDirty = true
                }

            @get:Ignore
            val standardCustomLiftSets: List<CustomLiftSetDto> by lazy {
                customLiftSets.filter { it.customLiftSet.type == SetType.STANDARD_SET }
            }

            @get:Ignore
            val dropsetCustomLiftSets: List<CustomLiftSetDto> by lazy {
                customLiftSets.filter { it.customLiftSet.type == SetType.DROP_SET }
            }

            @get:Ignore
            val myorepSetCustomLiftSets: List<CustomLiftSetDto> by lazy {
                customLiftSets.filter { it.customLiftSet.type == SetType.MYOREP_SET }
            }
        }
    }
}