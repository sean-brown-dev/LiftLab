package com.browntowndev.liftlab.core.common

import java.text.DecimalFormatSymbols

private fun testCharacter(
    c: Char,
    currencySymbol: String,
    decimalFormatSymbols: DecimalFormatSymbols,
    indexIntoPossibleCurrencySymbolMatch: Int
): CharacterTestResult {
    if (c.isDigit()
        || c == decimalFormatSymbols.decimalSeparator
        || c == decimalFormatSymbols.groupingSeparator
    ) {
        return CharacterTestResult.ALLOWED
    }

    // If we reached here it means either we are looking at the currency symbol or something that is
    // not allowed.
    if (indexIntoPossibleCurrencySymbolMatch < currencySymbol.length) {
        if (c == currencySymbol[indexIntoPossibleCurrencySymbolMatch]) {
            return if (indexIntoPossibleCurrencySymbolMatch == currencySymbol.length - 1) {
                // We just finished matching currency symbol
                CharacterTestResult.ALLOWED_CURRENCY_SYMBOL
            } else {
                // We might be in progress of matching a currency symbol
                CharacterTestResult.POSSIBLE_CURRENCY_SYMBOL_MATCH
            }
        }
    }

    return CharacterTestResult.NOT_ALLOWED
}

private enum class CharacterTestResult {
    NOT_ALLOWED,
    ALLOWED,
    ALLOWED_CURRENCY_SYMBOL,
    POSSIBLE_CURRENCY_SYMBOL_MATCH
}