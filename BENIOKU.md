# Mapfre Oto Cevap

Şirket telefonunda **sadece Mapfre robot numarası** aradığında aramayı otomatik açar ve **#** tuşuna basar.

- Telefonun kendi arama uygulaması, SMS'ler ve diğer uygulamalar **aynen kalır**.
- Uygulama varsayılan telefon / SMS / spam uygulaması **olmaz**, hiçbir rol almaz.
- Başka numaralardan gelen aramalara hiç dokunmaz.

## Nasıl çalışır
1. Telefon çaldığında numaraya bakar. Hedef numara değilse hiçbir şey yapmaz.
2. Hedef numaraysa aramayı sistem komutuyla açar (kaydırma gerekmez).
3. Açılmazsa ekrandaki "Cevapla" düğmesine basar, o da olmazsa ayarlardaki kaydırmayı yapar.
4. Arama bağlanınca ayarlanan süre kadar bekler (varsayılan 4 sn), arama ekranında tuş takımını açıp **#**'e basar.

## APK'yı derlemek

### Yol A — Android Studio (bilgisayarda)
1. Android Studio'yu kur, bu klasörü **Open** ile aç.
2. Senkronizasyon bitince: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
3. APK: `app/build/outputs/apk/debug/app-debug.apk`

### Yol B — GitHub (bilgisayara bir şey kurmadan)
1. GitHub'da özel (private) bir repo aç, bu klasördeki dosyaları yükle.
2. **Actions** sekmesinde "APK derle" otomatik çalışır (~5 dk).
3. Bitince çalıştırmanın altındaki **MapfreOtoCevap-apk** dosyasını indir.

## Telefonda kurulum
1. APK'yı telefona at, "bilinmeyen kaynaklardan yüklemeye izin ver"i aç, kur.
2. Uygulamayı aç ve sırayla:
   - **Telefon izinlerini ver** (3 izin)
   - **Erişilebilirlik servisini aç** → listede "Mapfre Oto Cevap" → aç
     (Android 13+ telefonlarda "kısıtlı ayar" uyarısı çıkarsa: Ayarlar → Uygulamalar → Mapfre Oto Cevap → sağ üst ⋮ → "Kısıtlı ayarlara izin ver")
   - **Pil kısıtlamasını kaldır**
   - Robot numarasını yaz → **Kaydet**
3. Durum kısmında hepsi ✅ olunca hazır.

**Xiaomi / Redmi / Poco:** Ayarlar → Uygulamalar → Mapfre Oto Cevap → **Otomatik başlat** açık olsun, pil tasarrufu "Kısıtlama yok" olsun.

## Sorun olursa
Uygulamadaki **Son işlemler** logu her adımı yazar:
- "# tuşu bulunamadı" → Telefonun arama ekranındaki tuş takımı düğmesinin adı farklı. Bana markayı/modeli söyle, ekleyelim.
- "# basıldı ✔" ama robot kabul etmiyor → bekleme süresini artır (ör. 6 sn).
- Hiç log yok → numara yanlış girilmiş ya da erişilebilirlik servisi kapanmış.
