package com.example.solarShop.utils.currency

import java.text.NumberFormat
import java.util.Locale



//اکستنشن برای سه رقم جدا کننده
fun Double?.toPriceString(): String =
    if (this == null) "-"
    else NumberFormat.getNumberInstance(Locale.US).format(this)

fun Long?.toPriceString(): String =
    if (this == null) "-"
    else NumberFormat.getNumberInstance(Locale.US).format(this)