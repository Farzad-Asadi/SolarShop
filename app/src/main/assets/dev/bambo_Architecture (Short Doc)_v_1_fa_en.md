
# 📐 Architecture (Short Doc)
## Bambo – High-level Technical Architecture

> Purpose: a short, project-friendly description of how the app is structured and how data flows.
> This document is intentionally brief and optimized to live inside Android Studio under `/Docs/`.

---

# 🇮🇷 معماری کوتاه (High-Level)

## 1) اصول طراحی
- **Single Source of Truth**: داده‌ها در Room ذخیره می‌شوند و UI از Flow تغذیه می‌شود.
- **Separation of Concerns**: UI فقط نمایش/تعامل؛ منطق دامنه در UseCase/Repository؛ دیتابیس در Data layer.
- **Domain Rules First**: قوانین DAG (بدون چرخه، محدودیت اتصال‌ها، حذف هوشمند/rewire) در لایه دامنه enforce می‌شود.

---

## 2) لایه‌ها و مسئولیت‌ها

### UI Layer (Compose)
- Screen/Composable
- ViewModel (StateFlow)
- رویدادها (User Intents) → فراخوانی UseCase/Repository

### Domain Layer (UseCases)
- عملیات دامنه مثل:
  - Create/Edit/Delete Question
  - Create/Edit/Delete Answer
  - Connect/Rewire Answer → NextQuestion
  - SmartDelete / JoinChild
- این لایه تضمین می‌کند گراف خراب نشود.

### Data Layer
- Room Entities/Dao
- Repository
- Mapping (Entity ↔ Domain Model در صورت نیاز)

---

## 3) ماژول‌های مفهومی (Feature Buckets)
> حتی اگر پروژه فعلاً multi-module نیست، این دسته‌بندی به عنوان Feature boundaries استفاده شود.

1. **Q&A Engine (Core / DAG)**
   - Question / Answer / CrossRef
   - قوانین گراف + عملیات rewire/delete

2. **Template Customization**
   - مدیریت سؤال/جواب‌های پیش‌فرض
   - مرتب‌سازی Answer و UX مرتبط

3. **Order (Next)**
   - Order + Customer
   - Selection (پاسخ‌های انتخاب‌شده در سفارش)

4. **Pricing (Next)**
   - قیمت‌گذاری بر اساس انتخاب‌ها

5. **Documents (Next)**
   - پیش‌فاکتور / فاکتور / قرارداد ساده

6. **Finance (Next)**
   - هزینه‌ها / دریافتی‌ها / مانده

7. **Media (Next)**
   - عکس‌های پروژه و گالری سفارش

---

## 4) جریان داده (Data Flow)

### Core (الان)
User Action → ViewModel → UseCase/Repository → Room (write)
Room (Flow) → Repository → ViewModel (uiState) → Compose UI

### آینده (MVP سفارش)
Template(Q&A DAG)
  → Selection (Order answers)
    → Pricing
      → Documents
        → Finance
          → Media

---

## 5) قراردادهای پیشنهادی در کد

### Naming
- `...Entity` برای Room
- `...Dao` برای DAO
- `...Repository` برای abstraction
- `...UseCase` برای منطق دامنه
- `...UiState` برای State مدل UI

### Error/Result Patterns
- برای عملیات حساس (حذف، اتصال، rewire):
  - Result sealed class (مثل SmartDeleteResult) با پیام‌های قابل نمایش

---

## 6) نکات کلیدی مخصوص پروژه
- IBID از نوع **Int** هستند (یکپارچگی در کل پروژه)
- مبالغ پولی در DB به **تومان** ذخیره می‌شوند
- برای ورود مبلغ در UI از **TomanAmountField** استفاده می‌شود

---

# 🇬🇧 Short Architecture (High-Level)

## 1) Design Principles
- **Single Source of Truth**: Room is the source; UI is driven by Flows.
- **Separation of Concerns**: UI renders; domain enforces rules; data persists.
- **Domain Rules First**: DAG constraints and smart operations (rewire/smart-delete) live in domain.

---

## 2) Layers & Responsibilities

### UI Layer (Compose)
- Screens/Composable
- ViewModels (StateFlow)
- User intents → UseCases/Repositories

### Domain Layer (UseCases)
- Core operations:
  - Create/Edit/Delete Question
  - Create/Edit/Delete Answer
  - Connect/Rewire Answer → NextQuestion
  - SmartDelete / JoinChild
- Guarantees the graph remains valid.

### Data Layer
- Room Entities/DAOs
- Repositories
- Optional mapping (Entity ↔ Domain model)

---

## 3) Conceptual Feature Buckets
(Use as boundaries even in a single-module project.)

1. **Q&A Engine (Core / DAG)**
2. **Template Customization**
3. **Order (Next)**
4. **Pricing (Next)**
5. **Documents (Next)**
6. **Finance (Next)**
7. **Media (Next)**

---

## 4) Data Flow

### Current
User Action → ViewModel → UseCase/Repo → Room (write)
Room (Flow) → Repo → ViewModel (uiState) → Compose UI

### MVP Order Flow (Next)
Template(Q&A DAG) → Selections(Order) → Pricing → Documents → Finance → Media

---

## 5) Suggested Code Conventions
- `...Entity`, `...Dao`, `...Repository`, `...UseCase`, `...UiState`
- Use sealed `Result` types for sensitive operations (delete/rewire)

---

## 6) Project-specific Notes
- All IDs are **Int**
- Monetary values are stored as **Toman**
- Use **TomanAmountField** for amount inputs

