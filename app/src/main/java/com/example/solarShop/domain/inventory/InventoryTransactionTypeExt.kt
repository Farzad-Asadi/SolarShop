package com.example.solarShop.domain.inventory

import com.example.solarShop.InventoryTransactionType

fun InventoryTransactionType.labelFa(): String {
    return when (this) {
        InventoryTransactionType.PURCHASE -> "خرید / ورود"
        InventoryTransactionType.SALE -> "فروش / خروج"
        InventoryTransactionType.ADJUSTMENT -> "اصلاح موجودی"
        InventoryTransactionType.PURCHASE_RETURN -> "مرجوعی خرید"
        InventoryTransactionType.SALE_RETURN -> "مرجوعی فروش"
    }
}

fun InventoryTransactionType.signedQuantity(quantity: Double): Double {
    return when (this) {
        InventoryTransactionType.PURCHASE,
        InventoryTransactionType.SALE_RETURN -> quantity

        InventoryTransactionType.SALE,
        InventoryTransactionType.PURCHASE_RETURN -> -quantity

        InventoryTransactionType.ADJUSTMENT -> quantity
    }
}