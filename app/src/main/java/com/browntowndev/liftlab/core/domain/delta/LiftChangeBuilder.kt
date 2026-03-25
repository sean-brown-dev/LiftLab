package com.browntowndev.liftlab.core.domain.delta

import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift

class LiftChangeBuilder {
    private var workoutLiftId: Long
    private var insertWorkoutLift: GenericWorkoutLift? = null
    private var liftUpdate: LiftUpdate? = null
    private val setChanges: MutableList<SetChange> = mutableListOf()
    private var purgeAllSets: Boolean = false
    private val setIdsMarkedForRemoval: MutableList<Long> = mutableListOf()

    /**
     * Create a lift change builder that will contain set changes only
     */
    constructor(workoutLiftId: Long) {
        this.workoutLiftId = workoutLiftId
    }

    /**
     * Create a lift change builder that will insert a new workout lift. Adding set
     * changes will be duplicative.
     */
    constructor(insertWorkoutLift: GenericWorkoutLift) {
        this.workoutLiftId = 0L
        this.insertWorkoutLift = insertWorkoutLift
    }

    /**
     * Create a lift change builder that will update an existing workout lift and can
     * also contain set changes
     */
    constructor(workoutLiftId: Long, liftUpdate: LiftUpdate) {
        this.workoutLiftId = workoutLiftId
        this.liftUpdate = liftUpdate
    }

    /** Add a set upsert by value. */
    fun set(set: GenericLiftSet) {
        setChanges += SetChange(set)
    }

    /** Add a pre-built SetChange. */
    fun set(change: SetChange) {
        setChanges += change
    }

    /** Purge all sets under this workout-lift (e.g., switched to Standard). */
    fun removeAllSets() {
        purgeAllSets = true
        setIdsMarkedForRemoval.clear()
    }

    /** Mark specific sets for soft deletion by their ids. */
    fun removeSets(vararg setIds: Long) {
        setIdsMarkedForRemoval += setIds.toList()
        purgeAllSets = false
    }

    fun build(): LiftChange =
        LiftChange(
            workoutLiftId = workoutLiftId,
            insertLift = insertWorkoutLift,
            liftUpdate = liftUpdate,
            sets = setChanges.toList(),
            removeAllSets = purgeAllSets,
            removedSetIds = setIdsMarkedForRemoval.toList()
        )
}