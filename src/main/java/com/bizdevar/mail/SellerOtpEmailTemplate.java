package com.bizdevar.mail;

import java.time.Year;

final class SellerOtpEmailTemplate {

    private SellerOtpEmailTemplate() {}

    static String subject(String storeName) {
        return "Buykon — " + (storeName == null || storeName.isBlank() ? "Mağaza" : storeName) + " giriş kodu";
    }

    static String plainText(String storeName, String code) {
        return "Buykon Satıcı Paneli\n\n"
                + "Mağaza: " + (storeName == null ? "" : storeName) + "\n"
                + "Giriş kodunuz: " + code + "\n\n"
                + "Kod 10 dəqiqə ərzində etibarlıdır.\n"
                + "Bu kodu heç kimlə paylaşmayın.\n\n"
                + "— Buykon Marketplace";
    }

    static String html(String storeName, String code) {
        int year = Year.now().getValue();
        String safeStore = storeName == null || storeName.isBlank() ? "Mağaza" : storeName;
        return """
<!DOCTYPE html>
<html lang="az">
<head><meta charset="UTF-8"/><meta name="viewport" content="width=device-width,initial-scale=1.0"/></head>
<body style="margin:0;padding:24px;background:#f4f4f7;font-family:Arial,Helvetica,sans-serif;color:#1a1a1a;">
  <div style="max-width:520px;margin:0 auto;background:#fff;border-radius:16px;padding:32px;box-shadow:0 8px 24px rgba(0,0,0,.08);">
    <h1 style="margin:0 0 8px;font-size:22px;">Satıcı giriş kodu</h1>
    <p style="margin:0 0 20px;color:#666;">%s panelinə daxil olmaq üçün OTP kodunuz:</p>
    <div style="font-size:32px;font-weight:700;letter-spacing:6px;text-align:center;padding:20px;background:#fff8eb;border-radius:12px;color:#ff9100;">%s</div>
    <p style="margin:20px 0 0;color:#666;font-size:14px;line-height:1.6;">Kod 10 dəqiqə etibarlıdır. Heç kimlə paylaşmayın.</p>
    <p style="margin:24px 0 0;color:#999;font-size:12px;">© %d Buykon</p>
  </div>
</body>
</html>
""".formatted(safeStore, code, year);
    }
}
