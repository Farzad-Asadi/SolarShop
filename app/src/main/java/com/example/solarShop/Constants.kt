package com.example.solarShop



const val SignInScreenRoute = "signInScreen"

//userPaymentState
const val USER_PAYMENT_STATE_NOT_PAY = "userPaymentStateNotPay"
const val USER_PAYMENT_STATE_PAYED = "userPaymentStatePayed"

//userRole
const val USER_ROLE_CABINET_MAKER = "کابینت ساز"
const val USER_ROLE_CUSTOMER = "مشتری"

//shairePreferences
const val PREF_IS_SIGNED_IN = "is_signed_in"

//OrderStatus
const val PENDING = "Pending"
const val IN_PROGRESS = "InProgress"
const val COMPLETED = "Completed"
const val CANCELED= "Cancelled"

const val ANCHOR_EPS = 0.5f // نیم پیکسل: حساسیت

const val TAG = "QuestionVM"

const val SESSION_PREFS = "session.preferences_pb"

const val EPS = 1e-6


val ARTICLE_TITLE_REGEX = Regex("^\\s*ماده\\s+(\\d+)\\s*-\\s*(.*)$")
val ARTICLE_PREFIX = Regex("""^\s*ماده\s+(\d+)\s*[-ـ]\s*""")












