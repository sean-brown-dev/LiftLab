package com.browntowndev.liftlab.core.common.enums

enum class DropPercentageOptions(val floatPercentage: Float, val wholeNumberPercentage: Int, val stringPercentage: String) {
    FivePercent(.05f, 5, "5%"),
    TenPercent(.1f, 10, "10%"),
    FifteenPercent(.15f, 15, "15%"),
    TwentyPercent(.2f, 20,  "20%"),
    TwentyFivePercent(.25f, 25, "25%"),
}

fun Float.toDropPercentageString(): String {
    return when (this) {
        DropPercentageOptions.FivePercent.floatPercentage -> DropPercentageOptions.FivePercent.stringPercentage
        DropPercentageOptions.TenPercent.floatPercentage -> DropPercentageOptions.TenPercent.stringPercentage
        DropPercentageOptions.FifteenPercent.floatPercentage -> DropPercentageOptions.FifteenPercent.stringPercentage
        DropPercentageOptions.TwentyPercent.floatPercentage -> DropPercentageOptions.TwentyPercent.stringPercentage
        DropPercentageOptions.TwentyFivePercent.floatPercentage -> DropPercentageOptions.TwentyFivePercent.stringPercentage
        else -> throw Exception("$this not a recognized drop percentage. Add it to DropPercentageOptions.")
    }
}