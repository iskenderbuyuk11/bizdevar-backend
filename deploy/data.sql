-- BizdeVar seed melumatlari. Her acilisda islenir, INSERT IGNORE ile tekrarlanmir.
-- Admin istifadeci Java tarefinde (DataSeeder) BCrypt hash ile yaradilir.

-- Kateqoriyalar
INSERT IGNORE INTO categories (slug, name, status) VALUES
  ('elektronika', 'Elektronika', 'active'),
  ('geyim',       'Geyim',        'active'),
  ('aksesuarlar', 'Aksesuarlar',  'active'),
  ('ev-yasam',    'Ev ve yasam',  'active'),
  ('kosmetika',   'Kosmetika',    'active');

-- Ayarlar
INSERT IGNORE INTO settings (setting_key, setting_value, group_name) VALUES
  ('commission_rate', '8',        'payments'),
  ('currency',        'AZN',      'general'),
  ('language',        'az',       'general'),
  ('marketplace_name','Buykon', 'general');

-- Resmi magaza (vendor id = 1)
INSERT IGNORE INTO vendors (id, seller_id, user_id, name, category, verification_status, status, store_type, revenue, rating, auto_named)
VALUES (1, NULL, NULL, 'Buykon Resmi', 'Marketplace', 'verified', 'active', 'online', 184500, 4.8, 0);

-- Mehsullar (hamisi vendor_id = 1)
INSERT IGNORE INTO products (vendor_id, category_slug, name, slug, price, base_price, discount_percent, popular, stock, image_url, images_json, specs_json, description, status) VALUES
  (1, 'elektronika', 'Smartfon BizPhone 13 Pro 256GB', 'bizphone-13-pro-256', 2199, 2499, 12, 95, 40, 'https://picsum.photos/seed/bizphone13/600/600', '["https://picsum.photos/seed/bizphone13/600/600","https://picsum.photos/seed/bizphone13b/600/600"]', '{"Ekran":"6.7 OLED","Yaddas":"256GB","Kamera":"48MP"}', 'Yuksek performansli smartfon, gunduz-gece ela kamera.', 'active'),
  (1, 'elektronika', 'Notebook BizBook Air 14"', 'bizbook-air-14', 3299, NULL, 0, 90, 25, 'https://picsum.photos/seed/bizbookair/600/600', '["https://picsum.photos/seed/bizbookair/600/600"]', '{"CPU":"8 nuve","RAM":"16GB","SSD":"512GB"}', 'Yungul ve davamli noutbuk, butun gun batareya.', 'active'),
  (1, 'elektronika', 'Simsiz Qulaqliq BizBuds Pro', 'bizbuds-pro', 249, 329, 24, 88, 120, 'https://picsum.photos/seed/bizbuds/600/600', '["https://picsum.photos/seed/bizbuds/600/600"]', '{"Tip":"TWS","ANC":"Beli","Batareya":"30 saat"}', 'Aktiv ses izolyasiyali simsiz qulaqliq.', 'active'),
  (1, 'elektronika', 'Smart Saat BizWatch 5', 'bizwatch-5', 459, 549, 16, 80, 60, 'https://picsum.photos/seed/bizwatch5/600/600', '["https://picsum.photos/seed/bizwatch5/600/600"]', '{"Ekran":"AMOLED","Suya davam":"5ATM","GPS":"Beli"}', 'Saglamliq ve idman izlemesi olan smart saat.', 'active'),
  (1, 'elektronika', '4K Smart TV 55"', 'smart-tv-55-4k', 1599, 1899, 16, 70, 18, 'https://picsum.photos/seed/smarttv55/600/600', '["https://picsum.photos/seed/smarttv55/600/600"]', '{"Olcu":"55 duym","Qetnamə":"4K UHD","HDR":"Beli"}', 'Genis rengler ve kontrastli 4K televizor.', 'active'),
  (1, 'elektronika', 'Oyun Konsolu BizStation X', 'bizstation-x', 1399, NULL, 0, 65, 22, 'https://picsum.photos/seed/bizstationx/600/600', '["https://picsum.photos/seed/bizstationx/600/600"]', '{"Yaddas":"1TB","FPS":"120","4K":"Beli"}', 'Yeni nesil oyun konsolu, 4K 120FPS.', 'active'),

  (1, 'geyim', 'Kisi Pencek Klassik', 'kisi-pencek-klassik', 189, 259, 27, 60, 50, 'https://picsum.photos/seed/kisipencek/600/600', '["https://picsum.photos/seed/kisipencek/600/600"]', '{"Material":"Yun qarisigi","Reng":"Tund goy"}', 'Resmi ve gundelik geyim ucun klassik pencek.', 'active'),
  (1, 'geyim', 'Qadin Trenc Palto', 'qadin-trenc-palto', 229, 299, 23, 72, 35, 'https://picsum.photos/seed/trenc/600/600', '["https://picsum.photos/seed/trenc/600/600"]', '{"Material":"Pambiq","Mövsüm":"Yaz/Payiz"}', 'Zerif ve isti trenc palto.', 'active'),
  (1, 'geyim', 'Uniseks Hudi Pambiq', 'uniseks-hudi', 69, 89, 22, 85, 200, 'https://picsum.photos/seed/hudi/600/600', '["https://picsum.photos/seed/hudi/600/600"]', '{"Material":"100% pambiq","Olculer":"S-XXL"}', 'Rahat ve isti pambiq hudi.', 'active'),
  (1, 'geyim', 'Idman Ayaqqabisi RunFast', 'idman-ayaqqabi-runfast', 149, 199, 25, 78, 90, 'https://picsum.photos/seed/runfast/600/600', '["https://picsum.photos/seed/runfast/600/600"]', '{"Tip":"Qacis","Cins":"Uniseks"}', 'Yungul ve nefes alan idman ayaqqabisi.', 'active'),
  (1, 'geyim', 'Cins Salvar Slim Fit', 'cins-salvar-slim', 99, NULL, 0, 55, 110, 'https://picsum.photos/seed/cinssalvar/600/600', '["https://picsum.photos/seed/cinssalvar/600/600"]', '{"Fason":"Slim","Material":"Streç cins"}', 'Modern slim fit cins salvar.', 'active'),

  (1, 'aksesuarlar', 'Deri Kemer Klassik', 'deri-kemer-klassik', 45, 59, 24, 50, 150, 'https://picsum.photos/seed/kemer/600/600', '["https://picsum.photos/seed/kemer/600/600"]', '{"Material":"Tebii deri","Reng":"Qara"}', 'Tebii deriden kisi kemeri.', 'active'),
  (1, 'aksesuarlar', 'Gunes Eynəyi UV400', 'gunes-eynəyi-uv400', 79, 109, 28, 68, 80, 'https://picsum.photos/seed/eynek/600/600', '["https://picsum.photos/seed/eynek/600/600"]', '{"Qoruma":"UV400","Cerceve":"Metal"}', 'UV400 qorumali gunes eynəyi.', 'active'),
  (1, 'aksesuarlar', 'Çanta Şəhər Backpack', 'canta-seher-backpack', 119, 149, 20, 74, 65, 'https://picsum.photos/seed/backpack/600/600', '["https://picsum.photos/seed/backpack/600/600"]', '{"Hecm":"22L","Noutbuk":"15.6 duym"}', 'Gundelik istifade ucun seher bel cantasi.', 'active'),
  (1, 'aksesuarlar', 'Qol Saati Minimal', 'qol-saati-minimal', 159, NULL, 0, 48, 40, 'https://picsum.photos/seed/qolsaati/600/600', '["https://picsum.photos/seed/qolsaati/600/600"]', '{"Mexanizm":"Kvars","Su":"3ATM"}', 'Minimalist dizaynli qol saati.', 'active'),

  (1, 'ev-yasam', 'Aromatik Şam Dəsti 3-lü', 'aromatik-sam-desti', 39, 55, 29, 62, 130, 'https://picsum.photos/seed/sam/600/600', '["https://picsum.photos/seed/sam/600/600"]', '{"Say":"3 eded","Yanma":"25 saat"}', 'Ev ucun aromatik şam desti.', 'active'),
  (1, 'ev-yasam', 'Pambıq Yataq Dəsti 2 nəfərlik', 'yataq-desti-2', 129, 169, 24, 70, 55, 'https://picsum.photos/seed/yataq/600/600', '["https://picsum.photos/seed/yataq/600/600"]', '{"Material":"100% pambiq","Olcu":"200x220"}', 'Yumsaq pambiq yataq desti.', 'active'),
  (1, 'ev-yasam', 'Mətbəx Bıçaq Dəsti 6-lı', 'metbex-bicaq-desti', 89, 119, 25, 58, 75, 'https://picsum.photos/seed/bicaq/600/600', '["https://picsum.photos/seed/bicaq/600/600"]', '{"Say":"6 eded","Material":"Paslanmayan polad"}', 'Paslanmayan poladdan metbex bicaq desti.', 'active'),
  (1, 'ev-yasam', 'Ağıllı LED Lampa RGB', 'agilli-led-lampa-rgb', 29, 39, 26, 66, 240, 'https://picsum.photos/seed/ledlampa/600/600', '["https://picsum.photos/seed/ledlampa/600/600"]', '{"Idarə":"Telefon","Reng":"16M"}', 'Wi-Fi ile idare olunan RGB lampa.', 'active'),

  (1, 'kosmetika', 'Nəmləndirici Krem Hyaluron', 'nemlendirici-krem-hyaluron', 35, 49, 29, 76, 180, 'https://picsum.photos/seed/krem/600/600', '["https://picsum.photos/seed/krem/600/600"]', '{"Hecm":"50ml","Tip":"Butun deri"}', 'Hyaluron tursulu nemlendirici uz kremi.', 'active'),
  (1, 'kosmetika', 'Ətir Eau de Parfum 50ml', 'etir-edp-50', 119, 159, 25, 82, 90, 'https://picsum.photos/seed/etir/600/600', '["https://picsum.photos/seed/etir/600/600"]', '{"Hecm":"50ml","Tip":"EDP"}', 'Uzunmuddetli qoxulu unisex etir.', 'active'),
  (1, 'kosmetika', 'Saç Baxım Şampunu 400ml', 'sac-baxim-sampunu', 19, 27, 30, 60, 220, 'https://picsum.photos/seed/sampun/600/600', '["https://picsum.photos/seed/sampun/600/600"]', '{"Hecm":"400ml","Tip":"Butun sac"}', 'Gundelik istifade ucun besleyici sampun.', 'active'),
  (1, 'kosmetika', 'Makiyaj Dəsti Professional', 'makiyaj-desti-pro', 159, 219, 27, 71, 45, 'https://picsum.photos/seed/makiyaj/600/600', '["https://picsum.photos/seed/makiyaj/600/600"]', '{"Say":"12 eded","Tip":"Goz+dodaq"}', 'Professional makiyaj desti.', 'active');

-- Resmi magaza adini yenile (satici silinmir)
UPDATE vendors SET name = 'Buykon Resmi', verification_status = 'verified', status = 'active' WHERE id = 1;

-- Kuponlar
INSERT IGNORE INTO coupons (code, discount_percent, active) VALUES
  ('XOSGELDIN', 10, 1),
  ('BIZDE20',   20, 1),
  ('YAY15',     15, 1);

-- Catdirilma providerleri
INSERT IGNORE INTO shipping_providers (name, zone, rate, status) VALUES
  ('BizdeVar Express', 'Baki', 3.5, 'active'),
  ('Region Catdirilma', 'Regionlar', 6.0, 'active');

-- CMS sehifeler
INSERT IGNORE INTO cms_pages (slug, title, content_type, status) VALUES
  ('haqqimizda', 'Haqqimizda', 'page', 'published'),
  ('gizlilik',   'Gizlilik Siyaseti', 'page', 'published'),
  ('faq',        'Tez-tez verilen suallar', 'faq', 'draft');
