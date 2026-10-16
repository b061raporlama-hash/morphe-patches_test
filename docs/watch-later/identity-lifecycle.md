# İlk karttan sonra kaybolan saat: neden ve doğrulama

Önceki sürümde `WatchLaterCardIdentity`, yol/şablon hash'ini bir video kimliğine bağlayan bir önbellek kullanıyordu. Aynı şablondan ikinci video gelince bu kayıt kalıcı olarak belirsizleşiyordu. Kopyalanmış alt bağlamlar tam nesne eşleşmesi bulamadığında bu kayda düşüyor; metin önbelleği tekrar yüklendiğinde `onLithoTextLoaded` eski video span'ını da siliyordu. Böylece ilk kartın saati görünüp sonraki kartlar ve yeniden işlenen ilk kart kimliksiz kalabiliyordu. Yerel eski kodla ikinci ortak-şablon kartı için hata yeniden üretildi.

Kullanıcının sağladığı orijinal YouTube **21.20.405 / 1561173354** DEX'i incelendi. Bu APK'da `Lvbr;` dönüşüm bağlamı, `Lvbq;` oluşturucudur. `Lvbq;-><init>(Lvbr;)V` bağlamı kopyalar; `Lvbq;->a()Lvbr;` yeni bağlam oluşturur. Yeni patch bunları isimleriyle sabitlemez: bağlam fingerprint'i ve kurucu/dönüş tipi ilişkisiyle bulur, iki sınıfa kapsam alanı ve erişim arayüzü ekler, her iki kopyalama noktasına kapsam aktarımını bağlar. Yeni kart kökü yeni kapsam üretir; aynı şablonla ilgisiz kartlar birbirine karışmaz. Kapsamı sahipleri ve metin span'ları tutar; kimlik için global zayıf referans veya yol önbelleğine ihtiyaç yoktur.

## Yerel gerçek APK kontrolü

JDK, Android SDK ve GitHub Packages bağımlılık erişimiyle:

```sh
./gradlew :patches:buildAndroid :patches:validateWatchLater \
  -PyoutubeApk=/absolute/path/original-youtube.apk \
  -PyoutubeOutput=/absolute/path/local-unsigned.apk
```

Çıktı parametresi isteğe bağlıdır. Görev Watch later, bağımlılıkları ve GmsCore support patch'lerini uygular; gerçek bağlam/oluşturucudaki alanları, erişim yöntemlerini ve her iki kopyalama kancasını denetler. Patcher sonuçları Android SDK DEX doğrulamasından geçirilir. APK ve çalışma dosyaları yerel kalır; kaynak veya Release'e yüklenmez.

Bu kontrol patch uygulanabilirliğini ve DEX doğrulamasını sınar. YouTube sunucusunun gerçek kart düzenleri, hesabın Daha sonra izle listesine kayıt ve fiziksel telefonda gezinme ayrıca doğrulanmalıdır. Android UI regresyonları gerçek Android görünüm/span/overlay kodunu, sentetik Litho mount ve ağ verileriyle çalıştırır; gerçek YouTube çalıştırması olarak sunulmaz.

`WatchLaterCarrierTest`, patcher'ın önceden açılmış tam/filtrelenmiş üye listeleriyle de kapsam alanını ve erişim yöntemlerini DEX'e yazıp tekrar okuyarak denetler. Alan eklenirken hem tam alan kümesi hem örnek alan kümesi güncellenir; erişim yöntemleri de tam ve sanal yöntem kümelerinde tutulur. Bu, bellek içindeki bir koleksiyonda görünen değişikliğin yazılan DEX'te kaybolmasını önler.

## cep.14 açılış çökmesi ve ART kontrolü

Fiziksel telefondan gelen beş günlük aynı hatayı gösterdi: `Lvbq;-><init>(Lvbr;)V`, `0xA7`, `v2 has type PositiveShortConstant but expected Reference java.lang.Object`. Kullanıcının ürettiği APK'nın `classes9.dex` dosyasında `0xA3` komutu `v2`/`p1` değerini `1023` yapıyor; `0xA7`'ye eklenen `copy(p1,p0)` artık nesne yerine bu sayıyı geçiriyordu. Kopyalama çağrısının DEX'te bulunması ve SDK araçlarının başarılı olması, kurucunun ART'ta doğrulanabildiğini kanıtlamamıştı. Önceki arayüz testi bu gerçek DEX kurucusunu çalıştırmıyordu.

Üretimdeki `hookWatchLaterContextConstructor`, çağrıyı doğrulanmış `Object.<init>()` komutundan hemen sonra ekler. Böylece `this` başlatılmıştır ve `p1` özgün nesnedir. Kurucu girişinin beklenen yapıda olmaması patch hatasıdır. Çıkışa çağrı eklenmez; kurucunun özgün register sayısı değişmez.

`:patches:generateWatchLaterDexFixtures` üretimdeki `instrumentWatchLaterContextCopies` dönüşümünü küçük bir DEX'e uygular. Örnek hem kurucuda `p1`'i, hem `build` içinde `p0`'ı sayısal geçici register olarak kullanır. İkinci DEX cep.14 hatasının negatif kontrolüdür. `WatchLaterDexTest`, Android 15'te bunları `InMemoryDexClassLoader` ile yükler: eski kurucu ART tarafından reddedilmelidir; düzeltilen kurucu ve build çalışmalı, 40 kopyada kapsam/özgün veri korunmalı ve bağımsız/boş kapsamlar karışmamalıdır. Bu iki test yayın önkoşuludur. DEX dosyaları yalnız androidTest varlıklarına girer; release APK'ya girmez.
