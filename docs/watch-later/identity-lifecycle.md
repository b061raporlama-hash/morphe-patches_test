# İlk karttan sonra kaybolan saat: neden ve doğrulama

Önceki sürümde `WatchLaterCardIdentity`, yol/şablon hash'ini bir video kimliğine bağlayan bir önbellek kullanıyordu. Aynı şablondan ikinci video gelince bu kayıt kalıcı olarak belirsizleşiyordu. Kopyalanmış alt bağlamlar tam nesne eşleşmesi bulamadığında bu kayda düşüyor; metin önbelleği tekrar yüklendiğinde `onLithoTextLoaded` eski video span'ını da siliyordu. Böylece ilk kartın saati görünüp sonraki kartlar ve yeniden işlenen ilk kart kimliksiz kalabiliyordu. Yerel eski kodla ikinci ortak-şablon kartı için hata yeniden üretildi.

Kullanıcının sağladığı orijinal YouTube **21.20.405 / 1561173354** DEX'i incelendi. Bu APK'da `Lvbr;` dönüşüm bağlamı, `Lvbq;` oluşturucudur. `Lvbq;-><init>(Lvbr;)V` bağlamı kopyalar; `Lvbq;->a()Lvbr;` yeni bağlam oluşturur. Patch bunları isimleriyle sabitlemez: bağlam fingerprint'i ve kurucu/dönüş tipi ilişkisiyle bulur, iki sınıfa kapsam alanı ve erişim arayüzü ekler, her iki kopyalama noktasına bağlam ilişkisi aktarımını bağlar. Alanlar Android görünümü veya bağlam nesnesi tutmayan düğümler içerir. Metin span'ı kendi düğümünü ve beklenen kart yolunu tutar; video kimliği üst düğüme sonradan bağlanabilir. Kimlik için global zayıf referans veya şablon/yol önbelleği kullanılmaz.

## Gerçek çalışma sırası: önce kopyalar, sonra kimlik

17 Ekim 2026'da Android 15 üzerinde gerçek YouTube çalıştırıldı. Oluşturucu açılış hatası tekrarlanmadı, fakat `cep.16` ile saatler eksikti. Yerel tanılama derlemesi şu sırayı kaydetti: kimliksiz kart bağlamından alt bağlamlar ve metin bağlamı oluşturuldu; kök filtre daha sonra video kimliğini üst bağlama yazdı; metin kancası alt bağlamdaki eski boş değeri gördü. Gerçek `ComponentHost` metinlerinde normal stil span'ları varken video kimliği span'ı yoktu. Saat katmanı ve kart boyutu koşulları çalışıyordu. Yalnız `CellType|` kök ekini tanımak bu eksik bağlantıyı düzeltmedi.

Her kopya artık kaynağına bağlı ayrı bir düğüm alır. Yeni kartın çıplak kök yolundan `CellType|` yoluna girişinde eski kartın üst zinciri kesilir; aynı `CellType|` içindeki klonlar kendi kart ailesinde kalır. Kök ve klon gözlemleri bu ailenin ortak kimliğini günceller. Metne kimlik henüz bilinmeden de kapsam işareti eklenir; sonradan gelen kimlik mevcut span'dan okunabilir. Kart içinde çelişkili/belirsiz veri kayıt düğmesini gizler. Sahipler ve span'lar bırakıldığında düğümler de toplanabilir; farklı kartların aynı şablon hash'ini paylaşması onları birleştirmez.

219 regresyon kontrolü; kimlikten önce oluşturulan alt bağlam/metin, üst bağlamdan önce çalışan kardeş kök bildirimi, aynı şablonda 30 yeni kart, eski kart kimliğinin yeni karta sızmaması ve tüm ailede belirsizliğin gizlenmesini kapsar. Android arayüz testi de metni üst kimlik bağlanmadan önce üretir. ART testi, gerçek patch dönüşümünün ürettiği kurucu/build üzerinden kimlikten önce alt bağlam oluşturup sonradan kimliğe erişmeyi sınar.

## Yerel gerçek APK kontrolü

JDK, Android SDK ve GitHub Packages bağımlılık erişimiyle:

```sh
./gradlew :patches:buildAndroid :patches:validateWatchLater \
  -PyoutubeApk=/absolute/path/original-youtube.apk \
  -PyoutubeOutput=/absolute/path/local-unsigned.apk \
  -PyoutubeBundle=/absolute/path/cep-patches.mpp
```

Çıktı parametresi isteğe bağlıdır. Bundle parametresi verilirse, paketin bu YouTube sürümüne uyumlu varsayılan patch'leri de eklenir; bu parametre çıktı parametresini gerektirir. Bundle verilmediğinde görev Watch later, bağımlılıkları ve GmsCore support patch'lerini uygular. Gerçek bağlam/oluşturucudaki alanlar, erişim yöntemleri ve her iki kopyalama kancası denetlenir. Patcher sonuçları Android SDK DEX doğrulamasından geçirilir. APK ve çalışma dosyaları yerel kalır; kaynak veya Release'e yüklenmez.

17 Ekim 2026'da kullanıcı orijinal APK'yı yeniden sağladı: **21.20.405 / 1561173354**, SHA-256 `4b3ca9de0eb6857d43fa8766a724a2eba6405dfdbcdfb20eef6cb8950e805e2c`. Yayımlanan **1.47.0-cep.16** paketiyle 96 varsayılan patch ve Watch later/dependencies uygulandı. Gradle görevi **14 dakika 56 saniyede başarılı** tamamlandı; 12 DEX / 57.884 sınıf ve dosyalar arası sınıf hiyerarşisi SDK denetiminden geçti. Son APK'da `app.cep.android.youtube` / **Cep YouTube** kimliği, iki sınıftaki kapsam alanı/erişim yöntemleri, oluşturucuda `Object.<init>` hemen sonrasındaki kopyalama ve build kopyası yeniden okundu. Oluşturucu register sayısı 3 olarak korundu. Bu kayıt APK üretimi ve içerik kontrolüdür; Android'de açılış sonucundan ayrıdır.

Bu kontrol patch uygulanabilirliğini ve DEX doğrulamasını sınar. YouTube sunucusunun gerçek kart düzenleri, hesabın Daha sonra izle listesine kayıt ve fiziksel telefonda gezinme ayrıca doğrulanmalıdır. Android UI regresyonları gerçek Android görünüm/span/overlay kodunu, sentetik Litho mount ve ağ verileriyle çalıştırır; gerçek YouTube çalıştırması olarak sunulmaz.

`WatchLaterCarrierTest`, patcher'ın önceden açılmış tam/filtrelenmiş üye listeleriyle de kapsam alanını ve erişim yöntemlerini DEX'e yazıp tekrar okuyarak denetler. Alan eklenirken hem tam alan kümesi hem örnek alan kümesi güncellenir; erişim yöntemleri de tam ve sanal yöntem kümelerinde tutulur. Bu, bellek içindeki bir koleksiyonda görünen değişikliğin yazılan DEX'te kaybolmasını önler.

## cep.14 açılış çökmesi ve ART kontrolü

Fiziksel telefondan gelen beş günlük aynı hatayı gösterdi: `Lvbq;-><init>(Lvbr;)V`, `0xA7`, `v2 has type PositiveShortConstant but expected Reference java.lang.Object`. Kullanıcının ürettiği APK'nın `classes9.dex` dosyasında `0xA3` komutu `v2`/`p1` değerini `1023` yapıyor; `0xA7`'ye eklenen `copy(p1,p0)` artık nesne yerine bu sayıyı geçiriyordu. Kopyalama çağrısının DEX'te bulunması ve SDK araçlarının başarılı olması, kurucunun ART'ta doğrulanabildiğini kanıtlamamıştı. Önceki arayüz testi bu gerçek DEX kurucusunu çalıştırmıyordu.

Üretimdeki `hookWatchLaterContextConstructor`, çağrıyı doğrulanmış `Object.<init>()` komutundan hemen sonra ekler. Böylece `this` başlatılmıştır ve `p1` özgün nesnedir. Kurucu girişinin beklenen yapıda olmaması patch hatasıdır. Çıkışa çağrı eklenmez; kurucunun özgün register sayısı değişmez.

`:patches:generateWatchLaterDexFixtures` üretimdeki `instrumentWatchLaterContextCopies` dönüşümünü küçük bir DEX'e uygular. Örnek hem kurucuda `p1`'i, hem `build` içinde `p0`'ı sayısal geçici register olarak kullanır. İkinci DEX cep.14 hatasının negatif kontrolüdür. `WatchLaterDexTest`, Android 15'te bunları `InMemoryDexClassLoader` ile yükleyip kurucuları gerçekten çağırır: eski kurucu ART tarafından reddedilmelidir; düzeltilen kurucu ve build çalışmalı, 40 kopyada kapsam/özgün veri korunmalı ve bağımsız/boş kapsamlar karışmamalıdır. `loadClass` ve `getDeclaredConstructors` tek başına doğrulamayı başlatmadığından negatif kontrol de `newInstance` kullanır. Bu iki test yayın önkoşuludur. DEX dosyaları yalnız androidTest varlıklarına girer; release APK'ya girmez.
