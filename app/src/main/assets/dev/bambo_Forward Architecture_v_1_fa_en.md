
# 🧭 Forward Architecture Notes – Sprint Next (Order + Selections)
## هدف
این بخش مشخص می‌کند چطور از هسته‌ی موجود
**Template/Q&A (DAG)** به یک **سفارش واقعی (Order)** می‌رسیم، بدون اینکه معماری فعلی به‌هم بریزد.

---

# 🇮🇷 یادداشت‌های معماری برای Sprint بعدی

## 1) مفاهیم کلیدی

### Template (موجود)
- مجموعه سؤال‌ها و جواب‌ها که ساختار DAG دارد.
- قابل ویرایش و نسخه‌بندی (فعلاً حداقل: قابل انتخاب).

### Order (جدید)
- یک پروژه/سفارش واقعی برای یک مشتری.
- به یک Template مشخص متصل است.

### Selection (جدید)
- «پاسخ انتخاب‌شده مشتری» برای هر سؤال در یک سفارش.
- Selection همان چیزی هستند که **اجماع سفارش** را می‌سازند.

---

## 2) مدل داده پیشنهادی (حداقل MVP)
> نام‌ها پیشنهادی‌اند؛ می‌توانند با نام‌گذاری فعلی پروژه هماهنگ شوند.

### OrderEntity
- `id: Int`
- `title: String` (اختیاری)
- `customerId: Int?` (اگر Customer جداست)
- `templateRootQuestionId: Int` یا `templateId: Int?` (یکی را انتخاب کن)
- `createdAt: Long`
- `updatedAt: Long`
- `status: Int` (Draft / Confirmed / Archived)

### OrderSelectionEntity
- `id: Int`
- `orderId: Int`
- `questionId: Int`
- `answerId: Int` (پاسخ انتخاب‌شده)
- `note: String?` (اختیاری)
- `createdAt: Long`

#### Constraints (مهم)
- برای هر `(orderId, questionId)` فقط **یک Selection فعال** داشته باشیم (Unique Index).

---

## 3) DAO / Query ضروری

### OrderDao
- Insert/Update/Delete Order
- Get Order by id
- List Orders (Draft/Recent)

### OrderSelectionDao
- Upsert selection (با unique index)
- Get selections for an order (by orderId)
- Observe selections as Flow

### Query ترکیبی برای UI
- `Order + TemplateQuestions + Answers + Selections`
- خروجی: مدل صفحه‌ی «پاسخ‌دهی سفارش» و «خلاصه سفارش»

---

## 4) Data Flow (از Template به Order)

### A) ساخت سفارش جدید
1) User → "New Order"
2) انتخاب Template (یا پیش‌فرض)
3) ایجاد OrderEntity با reference به Template

### B) پاسخ‌دادن مشتری (Selection)
1) UI سؤال جاری را نمایش می‌دهد (بر اساس DAG)
2) کاربر یک Answer را انتخاب می‌کند
3) ViewModel → `SetSelection(orderId, questionId, answerId)`
4) Repository → Upsert در OrderSelectionEntity
5) مسیر سؤال بعدی از روی AnswerNextQuestionCrossRef مشخص می‌شود

### C) خلاصه سفارش
- UI از ترکیب:
  - سوال‌ها/جواب‌ها (Template)
  - Selection (Order)
  یک لیست «سؤال + پاسخ انتخاب‌شده» می‌سازد.

---

## 5) Boundary Rule: Template vs Order

### اصل مهم
- **Template** قابل تغییر است، ولی سفارش‌های واقعی نباید با تغییر Template خراب شوند.

### راهکار MVP (ساده و عملی)
- وقتی Order ساخته شد،
- فقط `templateRootQuestionId` را نگه می‌داریم و فرض می‌کنیم Template در کوتاه‌مدت تغییر بحرانی ندارد.

### راهکار نسخه بعد (بهتر)
- Snapshot یا Versioning:
  - `templateVersion` روی Order
  - یا کپی کردن یک نسخه readonly از Template برای Order

(فعلاً در MVP لازم نیست؛ فقط در Doc به عنوان مسیر ارتقا ثبت می‌شود.)

---

## 6) UI Screens پیشنهادی برای Sprint

### Screen 1: OrdersList
- لیست سفارش‌ها (Draft/Recent)
- دکمه ساخت سفارش جدید

### Screen 2: CreateOrder
- انتخاب Template (یا انتخاب Root Question)
- ثبت اطلاعات پایه (اختیاری)

### Screen 3: OrderWizard (Question Runner)
- نمایش سؤال جاری
- نمایش Answer
- ثبت Selection
- رفتن به سؤال بعدی با DAG
- Back/Undo (اختیاری)

### Screen 4: OrderSummary
- نمایش همه سؤال‌ها + پاسخ انتخاب‌شده
- وضعیت تکمیل (answered / unanswered)

---

## 7) UseCarcase پیشنهادی
- `CreateOrderUseCase`
- `SetSelectionUseCase` (Upsert)
- `GetOrderSummaryUseCase`
- `GetNextQuestionUseCase` (با استفاده از CrossRefer)

---

## 8) Definition of Done (Sprint)
- بتوان یک Order ساخت
- بتوان با Template موجود سؤال‌ها را پیمایش کرد
- بتوان برای هر سؤال یک Answer انتخاب کرد و ذخیره شود
- بتوان Summary سفارش را دید (سؤال + پاسخ انتخاب‌شده)

---

# 🇬🇧 Forward Notes for the Next Sprint

## Goal
Connect the existing **Template/Q&A DAG** to a real **Order** by introducing **Selections** as the single source of truth for customer choices.

### Key Concepts
- **Template (existing):** Questions/answers graph (DAG).
- **Order (new):** A real project tied to one template.
- **Selection (new):** The chosen answer for each question within an order.

### Minimal Data Model
- `OrderEntity(id, templateRootQuestionId or templateId, createdAt, status, ...)`
- `OrderSelectionEntity(id, orderId, questionId, answerId, ...)`
- Unique constraint: one active selection per (orderId, questionId)

### Flow
Create Order → Run questions using DAG → Upsert selections → Build summary by joining Template + Selections.

### Screens
OrdersList → CreateOrder → OrderWizard → OrderSummary

### Done Criteria
Create an order, walk the DAG, save selections, and display a full order summary.

