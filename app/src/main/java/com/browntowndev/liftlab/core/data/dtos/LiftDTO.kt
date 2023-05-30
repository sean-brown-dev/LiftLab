package com.browntowndev.liftlab.core.data.dtos

import androidx.room.Embedded
import androidx.room.Ignore
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.data.entities.Lift

data class LiftDto(
    @Embedded
    val lift: Lift
) {
    @Ignore
    var isDirty: Boolean = false;
    @Ignore
    private var _name = this.lift.name
    @Ignore
    private var _incrementOverride = this.lift.incrementOverride
    @Ignore
    private var _isHidden = this.lift.isHidden
    @Ignore
    private var _isBodyweight = this.lift.isBodyweight

    @get:Ignore
    val id
        get() = this.lift.id

    @get:Ignore
    val category
        get() = this.lift.category

    @get:Ignore
    val categoryDisplayName: String
        get() = this.lift.category.displayName()

    @get:Ignore
    var name
        get() = this._name
        set(newName) {
            this._name = newName;
            this.isDirty = true
        }

    @get:Ignore
    var incrementOverride
        get() = this._incrementOverride
        set(newIncrementOverride) {
            this._incrementOverride = newIncrementOverride
            this.isDirty = true
        }

    @get:Ignore
    var isBodyweight
        get() = this._isBodyweight
        set(isBodyweight) {
            this._isBodyweight = isBodyweight;
            this.isDirty = true
        }

    @get:Ignore
    var isHidden
        get() = this._isHidden
        set(isHidden) {
            this._isHidden = isHidden
            this.isDirty = true
        }
}