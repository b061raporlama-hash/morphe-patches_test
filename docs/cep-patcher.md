# Cep Patcher

## Doğrulanan güncel sürüm: 1.0.11

- [Cep Patcher 1.0.11 APK doğrudan indirme](https://github.com/b061raporlama-hash/morphe-patches_test/releases/download/cep-v1.0.11/cep-patcher.apk).
- [Başarılı Gradle, Android ve yayın çalışması](https://github.com/b061raporlama-hash/morphe-patches_test/actions/runs/38918969562); APK kaynak commit'i `902f4f83f`, patch sürümü `1.47.0-cep.11`.
- Eksik saat simgeleri için kart bağlamı aktarımı, iç içe kart keşfi, boş metin taşıyıcıları ve çizim döngüsü düzeltildi. Aynı şablonu paylaşan kartlar yalnız metin anahtarına göre eşleştirilmez; çelişkili kimliklerde kayıt düğmesi gizlenir.
- 48 Watch later regresyon kontrolü ve 419 yönetici birim testi geçti. Patch Kotlin testlerinde 3 geçti, gerçek APK isteyen 5 test atlandı. Release lint: 0 hata, 124 uyarı, 2 bilgi notu.
- Android 15'te **3 instrumentation testi** geçti (2,495 saniye): gerçek bundle yükleyicisi, çevrimdışı kaynak yükseltme ve üretimdeki saat arayüzü. Arayüz testi aynı şablona sahip iki farklı kartta iki saat, gerçek ekran dokunuşu, doğru kimliğe kayıt isteği, kart geri dönüşümü, kimliksiz/ekran dışı kartları ve oynatıcı/Shorts ekranında gizlemeyi doğruladı. Ağ ve Litho mount verileri sentetiktir; gerçek Android görünümleri/span/overlay/kaydetme kodu kullanılır. `cep-android-loader-result` çıktısındaki ekran görüntüsü de kontrol edildi.
- Eski Morphe test paketleri korunarak Cep YouTube test APK'sı ve gerçek imzalı Cep Patcher APK'sı yan yana kuruldu. İmzalama sertifikası önceki sürümlerle aynı. APK SHA-256: `eb32d674df970cb5a1268c47902666a9934897f41191af66296c58b19b8ac708`.
- Düzeltme mevcut YouTube'a kendiliğinden eklenmez: **Cep Patcher'ı verilerini silmeden güncelleyin**, kaynak sürümünün `1.47.0-cep.11` olduğunu kontrol edin, desteklenen orijinal YouTube APK'sını **Watch later shortcuts** seçili şekilde yeniden yamalayın ve yeni Cep YouTube APK'sını mevcut kurulumun üzerine kurun.
- Kullanıcı önceki sürümde telefona kurulumu ve alt menü kısayolunu doğruladı. Bu sürümün saat simgeleri, gerçek YouTube hesabına kayıt, sunucu kontrollü kart düzenleri ve root/Shizuku davranışı fiziksel telefonda henüz doğrulanmadı.

## Önceki sürüm 1.0.6 doğrulaması

- [Cep Patcher 1.0.6 APK doğrudan indirme](https://github.com/b061raporlama-hash/morphe-patches_test/releases/download/cep-v1.0.6/cep-patcher.apk).
- [Başarılı derleme ve kurulum kontrolleri](https://github.com/b061raporlama-hash/morphe-patches_test/actions/runs/38909678701); APK kaynak commit'i `75eccb830`, patch sürümü `1.47.0-cep.6`.
- 419 yönetici birim testi ve 31 Watch later regresyon kontrolü geçti. Patch Kotlin testlerinde 3 geçti, gerçek APK isteyen 5 test atlandı. Release lint: 0 hata, 124 uyarı, 2 bilgi notu.
- Android 15'te eski Morphe paketlerini temsil eden farklı imzalı test APK'larıyla `INSTALL_FAILED_UPDATE_INCOMPATIBLE` yeniden üretildi. Üretimdeki clone/GmsCore manifest kodundan çıkan `app.cep.android.youtube` test APK'sı kurulup aynı anahtarla yeniden yüklenebildi; eski Morphe paketlerinin yolları değişmedi.
- Gerçek, imzalı Cep Patcher release APK'sı da bu paketler kurulu tutulurken yüklenip başlatıldı. İki instrumentation testi gömülü paketin DEX yüklemesini, kaynak kaydını ve eski salt-okunur gömülü paketin çevrimdışı yenilenmesini doğruladı.
- Yönetici imzalama sertifikası 1.0.3 ile aynı. APK SHA-256: `ef225d9c98c2a0eaeba070b9c77f36ec9ad061eb81fefeef036bd66be4604bf5`.
- APK'daki gömülü `.mpp`, yayımlanan güncelleme paketiyle birebir aynı; Custom branding varsayılan adı `Cep YouTube` olarak doğrulandı.
- Sentetik kurulum testi gerçek YouTube'un tüm yamalarını veya fiziksel telefondaki oynatma, hesap, Watch later ve root/Shizuku davranışını doğrulamaz.

## Morphe ile yan yana kurulum

Yerleşik Cep Patches kaynağındaki **GmsCore support**, YouTube için artık `app.cep.android.youtube`
kimliğini kullanır. **Custom branding** varsayılan adı **Cep YouTube** olur. Böylece mevcut
`app.morphe.android.youtube` ve `app.morphe.manager` kurulumları korunarak yeni uygulama kurulabilir.
GmsCore aynı kalır ve paylaşılabilir. Bu yol normal APK yüklemesi içindir; root mount ayrı bir kurulum yöntemidir.

Önce yeni Cep Patcher APK'sını eskisinin üzerine kurun; Cep Patcher'ın verilerini silmeyin.
Gömülü kaynak eskiyse APK güncellemesiyle çevrimdışı yenilenir; daha yeni çevrimiçi paket geri alınmaz.
Orijinal YouTube APK'sını yeniden yamalayın ve oluşan yeni uygulamayı kurun. Önceden üretilen APK'ların
kimliği değişmez. Eski Cep sürümünde açıkça kaydedilmiş Morphe paket adı yerleşik kaynak için yeni
kimliğe taşınır; kullanıcının farklı özel clone adları korunur.

Yeni kurulumun uygulama verileri ayrıdır; hesabınıza yeniden giriş yapmanız gerekebilir. Özel bir clone
paket adını kendiniz değiştirirseniz, o adla kurulu farklı imzalı bir uygulamayla yine çakışabilirsiniz.

CI, üretimde kullanılan clone ve GmsCore manifest dönüşüm kodunu sentetik bir APK'ya uygular.
Android 15'te farklı imzalı eski Morphe paketleri kurulu tutulurken imza çakışması yeniden üretilir,
Cep kimlikli APK ve sonraki aynı anahtarlı yükleme başarıyla kurulmalıdır. Gerçek imzalı Cep Patcher
release APK'sı da bu eski paketlerle birlikte kurulup başlatılır. Bu test gerçek YouTube APK'sının
işlevlerini veya fiziksel telefon davranışını doğrulamaz.

## Önceki sürüm 1.0.3 doğrulaması

- [Cep Patcher 1.0.3 APK doğrudan indirme](https://github.com/b061raporlama-hash/morphe-patches_test/releases/download/cep-v1.0.3/cep-patcher.apk) — 30.124.745 bayt; dört Android işlemci mimarisini içerir.
- [Başarılı Gradle/Android test ve yayın çalışması](https://github.com/b061raporlama-hash/morphe-patches_test/actions/runs/38860098604).
- APK ve kaynak arşivi commit'i: `93630c8ef`; patch paketi: `1.47.0-cep.3`.
- 31 Watch later regresyon kontrolü ve 415 yönetici birim testi geçti. Patch Kotlin testlerinde 3 geçti, gerçek APK girdisi isteyen 5 test atlandı.
- Release lint: sıfır hata; 124 uyarı ve 2 bilgi notu var. Tam rapor Actions `cep-check-reports` çıktısında bulunur.
- Android 15 emülatöründe bir instrumentation testi geçti: gerçek DEX yükleyicisi, patch bağımlılıkları, ilk açılışta Room kaydı, kaynak adresi ve patch listesi denetlendi. Bu test debug test APK'sını kullanır; fiziksel telefonda release APK ve YouTube patch işlemi henüz denenmedi.
- Yayımlanan APK'nın Android imzası yerel `apksigner verify` ile de doğrulandı. SHA-256: `ab6fd97c7da0cd4fa963341738c1a55a872b4b77eb80f551bb7fbed17f5a3890`.
- Herkese açık, tokensız APK ve `.mpp` indirmelerinin hash'leri test edilmiş Actions çıktılarıyla eşleşti. APK'ya gömülü paket, yayımlanan güncelleme paketiyle birebir aynı.

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
cp patches/build/libs/patches-1.47.0.mpp manager/app/src/main/assets/cep-patches.mpp
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
