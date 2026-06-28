# BizdəVar Backend (Java / Spring Boot)

Marketplace üçün REST API. Əvvəlki Go backend tam olaraq Java-ya (Spring Boot 3 + MySQL) köçürülüb.

## Tələblər

- **JDK 17+** (`java -version`)
- **Maven** (`mvn -version`)
- **XAMPP** — MySQL işləyir olmalıdır (port `3306`)

## Verilənlər bazası

Backend açılanda `bizdevar` adlı baza **avtomatik yaradılır** (`createDatabaseIfNotExist=true`).
Cədvəllər `schema.sql`, seed məlumatları (kateqoriyalar, məhsullar, kuponlar) `data.sql` ilə qurulur.

Standart bağlantı (XAMPP defaultu): istifadəçi `root`, şifrə **boş**.
Fərqlidirsə mühit dəyişənləri ilə dəyiş:

```
set DB_USER=root
set DB_PASSWORD=sənin_şifrən
```

## İşə salmaq

```bat
cd backend
run.bat
```

və ya birbaşa:

```bat
mvn spring-boot:run
```

API ünvanı: `http://localhost:8080/api`

## Admin girişi

- Email: `admin@bizdevar.com`
- Şifrə: `Admin123`

(Admin istifadəçisi açılışda `DataSeeder` tərəfindən BCrypt hash ilə yaradılır.)

## Google ilə giriş

`src/main/resources/application.yml` → `bizdevar.google.client-id` və `client-secret`
dəyərlərini Google Cloud Console-dan aldığın açarlarla doldur (və ya mühit dəyişəni):

```
set GOOGLE_CLIENT_ID=xxxxx.apps.googleusercontent.com
set GOOGLE_CLIENT_SECRET=xxxxx
```

Frontend tərəfdə `pages/login` və `pages/register` səhifələrindəki Google düyməsi
`<meta name="google-client-id">` dəyərini istifadə edir — ora da eyni Client ID yazılmalıdır.

## Struktur

```
src/main/java/com/bizdevar/
├── BizdevarApplication.java      # giriş nöqtəsi
├── config/                       # AppProperties, CORS, beans, seeder
├── common/                       # xəta idarəetmə, JSON helper, health
├── security/                     # JWT, cookie, auth filter
├── user/                         # istifadəçi modeli + repository
├── auth/                         # register/login/logout/session/google
├── catalog/                      # kateqoriya + məhsul (public)
├── cart/                         # səbət
├── favorites/                    # sevimlilər
├── order/                        # sifarişlər + promo
├── profile/                      # istifadəçi profili
├── seller/                       # satıcı paneli
└── admin/                        # admin paneli API
```
