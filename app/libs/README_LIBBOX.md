# libbox.aar — sing-box Android Native Library

## ما هو libbox؟
`libbox` هو ملف `.aar` يحتوي على sing-box engine compiled كـ Go mobile binding.
يدعم بروتوكولات: VLESS, VMess, Trojan, Shadowsocks, Hysteria2, TUIC, WireGuard.

---

## كيف تحصل عليه؟

### الطريقة 1 — من GitHub Actions (أسهل)
1. افتح: https://github.com/SagerNet/sing-box/actions
2. ابحث عن workflow اسمه **"Build libbox for Android"**
3. حمّل artifact اسمه `libbox.aar`

### الطريقة 2 — من مشاريع مشابهة جاهزة
التطبيق الرسمي `sing-box-for-android` يبني libbox تلقائياً:
```bash
git clone https://github.com/SagerNet/sing-box.git
cd sing-box
make lib_install
```
ستجد `libbox.aar` في `release/`

### الطريقة 3 — بناء يدوي (إذا عندك Go مثبت)
```bash
go install golang.org/x/mobile/cmd/gomobile@latest
gomobile init
cd sing-box/experimental/libbox
gomobile bind -v -target android -androidapi 26 .
# يُنتج libbox.aar
```

---

## بعد الحصول على الملف
ضع `libbox.aar` في هذا المجلد (`app/libs/`) وأعد بناء المشروع.

## الإصدار الموصى به
`v1.10.7` أو أحدث (stable branch).
