# Roadmap – Retrospective Development Plan
## Bambo App

---

# 🇮🇷 Roadmap توسعه (مستندسازی مسیر طی‌شده)

## فاز 0 — Foundations
**هدف:** آماده‌سازی اسکلت فنی پروژه

- انتخاب Kotlin + Jetpack Compose + Room + Hilt
- تعریف ساختار پکیج‌ها و معماری تمیز
- پیاده‌سازی ViewModel + Flow / StateFlow
- ایجاد Repository pattern

**وضعیت:** انجام‌شده

---

## فاز 1 — Core Engine: سؤال–جواب (DAG)
**هدف:** ایجاد هسته شفافیت سفارش

- طراحی QuestionEntity
- طراحی AnswerEntity
- طراحی AnswerNextQuestionCrossRef
- اعمال قوانین گراف بدون چرخه
- محدودیت اتصال هر Answer به یک Question فرزند
- عملیات افزودن، ویرایش، حذف، rewire و حذف هوشمند

**وضعیت:** انجام‌شده

---

## فاز 2 — UI گراف سؤال–جواب
**هدف:** ویرایش بصری Template

- نمایش نودها به صورت Card
- لایه‌بندی با الگوریتم Topological (Kahn)
- رسم یال‌های orthogonal
- بهبود فاصله خطوط و جلوگیری از هم‌پوشانی

**وضعیت:** انجام‌شده

---

## فاز 3 — مدیریت Answer و تعامل UI
**هدف:** تجربه کاربری پایدار

- reorderable list برای Answer
- سازگاری با LazyColumn + imePadding
- مدیریت افزودن Answer جدید بدون اختلال ترتیب

**وضعیت:** انجام‌شده / در حال بهبود

---

## فاز 4 — Data Flow و Performance
**هدف:** UI سریع و بدون lag

- بهینه‌سازی FLOW
- رفع lag Snackbar با _pendingSuggestion

**وضعیت:** انجام‌شده

---

## فاز 5 — Template Customization
**هدف:** شخصی‌سازی سؤال–جواب‌ها

- ویرایش، حذف و افزودن سؤال و جواب پیش‌فرض
- تثبیت قوانین دامنه

**وضعیت:** انجام‌شده

---

# 🇬🇧 Development Roadmap (Retrospective)

## Phase 0 — Foundations
**Goal:** Establish technical foundations

- Kotlin, Jetpack Compose, Room, Hilt
- Clean architecture setup
- ViewModel + Flow / StateFlow
- Repository pattern

**Status:** Completed

---

## Phase 1 — Core Q&A Engine (DAG)
**Goal:** Order transparency core

- QuestionEntity
- AnswerEntity
- AnswerNextQuestionCrossRef
- DAG rules enforcement
- Smart delete & rewire operations

**Status:** Completed

---

## Phase 2 — Graph-based Q&A Editor UI
**Goal:** Visual template editing

- Card-based nodes
- Topological layered layout
- Orthogonal edge drawing
- Parallel edge spacing improvements

**Status:** Completed

---

## Phase 3 — Answer Management UX
**Goal:** Stable user interaction

- Reorderable answer lists
- IME-safe scrolling
- Predictable add behavior

**Status:** Completed / Improving

---

## Phase 4 — Data Flow & Performance
**Goal:** Responsive UI

- Flow optimization
- Snackbar lag fix via pending state

**Status:** Completed

---

## Phase 5 — Template Customization
**Goal:** User-defined Q&A templates

- Editable default questions and answers
- Domain rule stabilization

**Status:** Completed

