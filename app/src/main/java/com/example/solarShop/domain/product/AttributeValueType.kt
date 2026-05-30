package com.example.solarShop.domain.product

enum class AttributeValueType(
    val label: String
) {
    TEXT("متن"),
    NUMBER("عدد"),
    BOOLEAN("بله/خیر"),
    ENUM("لیست انتخابی")
}