package io.karn.charter.entities

interface NumericData<T : Number> {
    val label: String
    val value: T
}
