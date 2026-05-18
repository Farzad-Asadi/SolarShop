package com.example.solarShop.data.seeder

import com.example.solarShop.data.room.tables.user.userData.userWorkflowStep.UserWorkflowStepEntity


fun createDefaultWorkflowSteps(
    userKey: String? = null
): List<UserWorkflowStepEntity> =
    listOf(
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "ثبت سفارش",
            weightPercent = 0,
            isLocked = true,
            systemKey = "ORDER_CREATED"
        ),
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "تخمین قیمت",
            weightPercent = 0,
            isLocked = true,
            systemKey = "PRICE_ESTIMATE_CREATED"
        ),
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "ویرایش تخمین قیمت",
            weightPercent = 0,
            isLocked = true,
            systemKey = "PRICE_ESTIMATE_EDITED"
        ),
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "انتخاب کاتالوگ توسط مشتری",
            weightPercent = 5,
            isLocked = true,
            systemKey = "CATALOG_SELECTED"
        ),
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "ویرایش کاتالوگ توسط مشتری",
            weightPercent = 0,
            isLocked = true,
            systemKey = "CATALOG_CHANGED"
        ),
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "صدور و ارسال پیش‌فاکتور",
            weightPercent = 0,
            isLocked = true,
            systemKey = "PRE_INVOICE_CREATED"
        ),
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "صدور و ارسال فاکتور",
            weightPercent = 0,
            isLocked = true,
            systemKey = "INVOICE_CREATED"
        ),
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "صدور و ارسال قرارداد",
            weightPercent = 0,
            isLocked = true,
            systemKey = "CONTRACT_CREATED"
        ),
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "امضای قرارداد توسط مشتری",
            weightPercent = 0
        ),
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "طراحی پروژه",
            weightPercent = 15
        ),
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "ویرایش طرح پروژه",
            weightPercent = 0
        ),
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "خرید مواد و لوازم پروژه",
            weightPercent = 10
        ),
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "برش‌کاری قطعات",
            weightPercent = 15
        ),
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "مونتاژ یونیت‌ها",
            weightPercent = 30
        ),
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "نصب پروژه در محل",
            weightPercent = 20
        ),
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "خاتمه‌کاری پروژه",
            weightPercent = 0
        ),
        UserWorkflowStepEntity(
            userKey = userKey,
            title = "تسویه کامل",
            weightPercent = 5
        ),
    ).mapIndexed { index, it ->
        it.copy(sortOrder = index)
    }
