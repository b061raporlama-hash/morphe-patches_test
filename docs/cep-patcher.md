# Cep Patcher

Bu, Morphe Manager'ın bağımsız ve değiştirilmiş bir türevidir. Resmî Morphe sürümü değildir.
Android uygulama kimliği: `io.github.b061raporlama.ceppatcher`; Android 8.0+ (API 26).
Üst kaynak, sabit commit ve değişiklikler: [manager/UPSTREAM.md](../manager/UPSTREAM.md).

## Telefona kurulum

1. Bu deponun en son **Cep Patcher** Release'inden `cep-patcher.apk` dosyasını indirin.
2. Android isterse dosyayı açan tarayıcı/dosya yöneticisine bilinmeyen uygulama yükleme izni verin, APK'yı kurun.
3. Cep Patcher'ı açın. **Cep Patches** APK'nın içinde gelir; kaynak eklemeniz veya GitHub token'ı girmeniz gerekmez.
4. Uzman modunda YouTube'u seçin; kaynakta desteklenen bir sürümün orijinal APK dosyasını seçin.
5. Patch listesinde **Watch later shortcuts** öğesini açıkça işaretleyin. Bu patch varsayılan olarak seçili değildir. **Save to Watch later** ve **Navigation bar** bağımlılıkları otomatik eklenir.
6. Patch işlemini tamamlayıp üretilen uygulamayı kurun. Root kullanmadan kurulum için uygun GmsCore desteği ve GmsCore uygulaması gerekebilir; APK listesinde önerilen sürüm ve imza bilgilerini izleyin.
7. YouTube'da **Ayarlar → Morphe → Akış → Daha sonra izle kısayolları** açık olmalı. Buradaki Morphe ifadesi üst kaynağın YouTube patch ayarlarıdır; yönetici uygulamasının adı Cep Patcher'dır.

## Patch dağıtımı ve güncellemeler

Varsayılan kaynak:
`https://github.com/b061raporlama-hash/morphe-patches_test/releases/latest/download/cep-patches-bundle.json`

Her başarılı `Cep Patcher APK` Actions çalışması `.mpp` paketini bu deponun patch kaynaklarından yeniden derler,
APK'ya koyar, test eder ve yeni bir Release oluşturur. JSON içindeki paket adresi değişmez sürüm etiketine bağlıdır.
Güncellemeler resmî patch paketine geri dönmez. Kaynak denetimlerinde yeni sürüm bu Release üzerinden alınır.
Paket ve APK sürümleri bağımsızdır: `1.47.0-cep.N` / `1.0.N`.

Anahtar `CEP_KEYSTORE_B64` ve parolası `CEP_KEYSTORE_PASSWORD` GitHub Actions secrets içinde saklanır;
aynı anahtar kullanıldığı sürece yeni APK eskisinin üzerine kurulabilir. Anahtarı değiştirmek Android güncellemelerini
bozar. Anahtar kaynak koduna veya Release'e eklenmez. `SHA256SUMS`, `apk-signature.txt` ve `build-info.json`
Release dosyalarının doğrulanması ve kaynak commit'inin izlenmesi içindir.

## Android Studio / Gradle ile derleme

JDK 21, Android SDK 37, CMake 3.22.1 ve Gradle wrapper kullanılır. GitHub Packages'ın herkese açık
Morphe bağımlılıkları için bile okuma kimlik doğrulaması gerekir. Yerel ortamda `GITHUB_ACTOR` ve
`GITHUB_TOKEN` (`read:packages`) ayarlayın; token'ı dosyalara veya kaynak koduna koymayın.

Önce kök projede:

```sh
java tests/watchlater/WatchLaterRegression.java
./gradlew :patches:test :patches:buildAndroid :patches:generatePatchesList
cp patches/build/libs/patches-*.mpp manager/app/src/main/assets/cep-patches.mpp
```

Sonra `manager/` dizinini Android Studio'da açın veya:

```sh
cd manager
./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
```

Windows'ta wrapper `gradlew.bat` ve kopyalama komutu `Copy-Item` olarak kullanılabilir.
Yerel geliştirme imzası için upstream Gradle'ın varsayılan debug anahtarı kullanılabilir; yayımlanan APK
Actions secrets içindeki özel Cep Patcher anahtarıyla imzalanır. Aynı uygulama kimliğine sahip farklı imzalı
yerel derleme yayımlanan sürümün üzerine kurulamaz.

`manager/` kendi Gradle projesidir; kök projenin alt modülü değildir. Upstream Kotlin paket adları
ikili uyumluluk ve bakımı kolaylaştırmak için korunmuştur, Android uygulama kimliği farklıdır.

## Kontroller ve cihazda kalan doğrulama

Workflow; patch regresyonlarını ve birim testlerini, manager birim testlerini, release lint'ini,
APK imzasını/adını/kimliğini, paket manifestini ve DEX içeriğini sınar. Android 15 emülatör testi,
APK'ya gömülü paketi yöneticinin gerçek `PatchBundle.Loader` yükleyicisinden geçirip **Watch later shortcuts**
patch'ini ve bağımlılıklarını doğrular. Test başarılı olmadan Release yayımlanmaz.

Gerçek telefonda YouTube patch işlemi, kurulum, hesapla kayıt, hızlı akış kaydırma, kart/video eşleşmesi,
Daha sonra izle navigasyonu, Shorts gizleme seçeneği, ekran döndürme, temalar, çevrimdışı/oturumsuz kullanım
ve root/Shizuku yolları henüz doğrulanmış sayılmaz. Ayrıntılı senaryolar: [watch-later/README.md](watch-later/README.md).
Kaynak yükleme testi YouTube'un gerçek APK'sına patch uygulama veya sunucu kontrollü arayüzün çalışması kanıtı değildir.

## Lisans ve marka

Üst kaynakların `LICENSE`, `NOTICE`, telif başlıkları ve katkı bilgileri korunur. APK içinde
`assets/legal/` dizininde iki projenin tam lisans ve bildirimleri bulunur; yönetici açık kaynak lisans ekranı
üst kaynak bildirimini göstermeye devam eder. Hakkında ekranı bağımsız türev olduğunu ve kaynak adresini açıklar.
Her Release tam karşılık gelen kaynak arşivini içerir. Özgün cep/saat vektör simgeleri GPL-3.0-only'dir.
Morphe markası veya simgeleri için hak iddia edilmez.
