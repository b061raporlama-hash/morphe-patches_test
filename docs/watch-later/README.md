# Daha sonra izle kısayolları

Patcher'da YouTube için **Watch later shortcuts** patch'ini seçin. Patch varsayılan seçime dahil değildir. Mevcut **Save to Watch later** ve **Navigation bar** bağımlılıkları otomatik eklenir.

Uygulamada **Ayarlar → Morphe → Akış → Daha sonra izle kısayolları** anahtarı iki işlevi birlikte yönetir. Patch seçildiğinde anahtar başlangıçta açıktır; değiştirdikten sonra uygulamayı yeniden başlatın.

- Tam genişlikteki yatay video kartlarının sol üstünde koyu, yarı saydam zemin üzerinde saat simgesi gösterilir. Dokununca karttaki video, oturum açılmış YouTube hesabının gerçek **Daha sonra izle** listesine kaydedilir; video açılmaz.
- Alt menüdeki **Shorts** öğesinin simgesi, etiketi ve dokunma davranışı **Daha sonra izle** olarak değiştirilir. Düğme `playlist?list=WL` adresini aynı YouTube uygulamasında açar. Shorts'u gizleme ayarı bu yeni düğmeyi gizlemez.
- Başarılı kayıtta onay işareti gösterilir. Ağ hatasında kayıt tamamlanmış sayılmaz ve tekrar denenebilir. Aynı videoya tekrarlı dokunmalar tek isteğe indirilir; başka videolara yapılan kayıtlar düşürülmez.

Mockup'taki kırmızı çerçeveler ve açıklama okları tasarım işaretleridir; uygulama arayüzüne eklenmez.

## Kapsam ve doğrulama

Kart kimliği Litho filtreleme bağlamındaki küçük resim URL'sinden alınır ve görünmez metin span'ıyla ilgili karta taşınır. Aktif oynatıcının video kimliği kullanılmaz. Karışık/çok videolu tamponlarda, kimliği bulunamayan kartlarda, Shorts raflarında ve kompakt yatay satırlarda konum ya da video tahmini yapılmaz. Yerleşim tam genişlikte, küçük resmi üstte olan standart kartlar içindir.

`java tests/watchlater/WatchLaterRegression.java` gerçek kimlik ayrıştırıcı ve kaydetme kaynaklarını deterministik ağ/iş parçacığı örnekleriyle derleyip sınar. CI ayrıca Kotlin testlerini ve Android extension/bundle derlemesini çalıştırır. Otomatik kontroller gerçek APK üzerinde cihaz doğrulaması yerine geçmez.

Cihazda doğrulanacaklar: ana sayfayı hızlı kaydırırken doğru videonun kaydedilmesi; ekran döndürme ve tema değişimi; listeden geri dönüş; oturum kapalıyken ve ağ bağlantısı yokken davranış; Shorts gizliyken yeni düğmenin görünmesi; anahtar kapatılıp yeniden başlatıldığında özgün görünümün dönmesi. Sunucu tarafından değiştirilen Litho bağlamı veya kart yerleşimi ayrıca APK üzerinde sınanmalıdır.

Kök dizindeki `patches-list.json`, README patch tablosu ve yayımlanmış `.mpp` paketleri release sürecinde üretilir; kaynak değişikliğinin ardından yeni bundle derlenmeden kurulu Patcher'a kendiliğinden eklenmez.
