package com.browntowndev.liftlab.core.common.enums

enum class DropPercentageOptions(val doublePercentage: Double, val wholeNumberPercentage: Int, val stringPercentage: String) {
    FivePercent(.05, 5, "5%"),
    TenPercent(.1, 10, "10%"),
    FifteenPercent(.15, 15, "15%"),
    TwentyPercent(.2, 20,  "20%"),
    TwentyFivePercent(.25, 25, "25%"),
}

fun Double.toDropPercentageString(): String {
    return when (this) {
        DropPercentageOptions.FivePercent.doublePercentage -> DropPercentageOptions.FivePercent.stringPercentage
        DropPercentageOptions.TenPercent.doublePercentage -> DropPercentageOptions.TenPercent.stringPercentage
        DropPercentageOptions.FifteenPercent.doublePercentage -> DropPercentageOptions.FifteenPercent.stringPercentage
        DropPercentageOptions.TwentyPercent.doublePercentage -> DropPercentageOptions.TwentyPercent.stringPercentage
        DropPercentageOptions.TwentyFivePercent.doublePercentage -> DropPercentageOptions.TwentyFivePercent.stringPercentage
        else -> throw Exception("$this not a recognized drop percentage. Add it to DropPercentageOptions.")
    }
}