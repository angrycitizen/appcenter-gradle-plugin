package ru.mosgorpass.gradle.appcenter.utils

import java.util.Locale

fun String.truncate(maxLength: Int) =
    substring(0, maxLength.coerceAtMost(length))


fun String.capitalized(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}