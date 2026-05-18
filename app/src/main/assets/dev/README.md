

1
جدولهارو ویرایش کن   

جدولها و  dao  ها و دیتابیس و hilt را به روز رسانی کن



2.1
موقتاً createFromAsset را خاموش کن
قسمت 2.1 در اپلیکیشن فعال شود
اپ را انیستال و ران بگیر تا دیتابیس ایجاد شود



2.2
قسمت 2.1 در اپلیکیشن غیر فعال شود
قسمت 2.2 در اپلیکیشن فعال شود (overlayFromAssetsIfFirstRun())
در قسمت 2.2 اپلیکیشن قسمت اولین اجرای sharedPrefs غیر فعال شود
دیتابیس را یک‌بار پُر کن (سریع‌ترین: از بک‌آپ ZIP خودت)
اجرای دوباره اپلیکیشن
فعال کردن باتن CreateDumpSeed در SignInScreen و کلیک بر آن
رفتن به مسیر :
/sdcard/Android/data/<pkg>/files/seed/bambo_seed.db
و کپی کردن فایل دیتابیس به assets/seed
غیر فعال کردن باتن



3.1
فعال سازی createFromAsset
در کلاس اپلیکیشن
فعال نگه داشتن overlayFromAssetsIfFirstRun
در قسمت 2.2 اپلیکیشن قسمت اولین اجرای sharedPrefs  فعال شود