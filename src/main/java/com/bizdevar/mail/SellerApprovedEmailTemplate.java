package com.bizdevar.mail;

import java.time.Year;

final class SellerApprovedEmailTemplate {

    private SellerApprovedEmailTemplate() {}

    static String subject() {
        return "Buykon — Mağazanız təsdiqləndi";
    }

    static String plainText(String storeCode, String ownerName, String loginUrl) {
        return "MAGAZANIZ TESDIQLENDI\n\n"
                + "Magaza nomresi : " + storeCode + "\n"
                + "Magaza Sahibi : " + ownerName + "\n\n"
                + "Indi magaza kodunuz ve sifreniz ile satıcı paneline daxil ola bilersiniz:\n"
                + loginUrl + "\n\n"
                + "— Buykon Marketplace";
    }

    static String html(String storeCode, String ownerName, String loginUrl) {
        int year = Year.now().getValue();
        String safeOwner = ownerName == null || ownerName.isBlank() ? "Magaza sahibi" : ownerName;
        return """
<!DOCTYPE html>
<html lang="az">
<head><meta charset="UTF-8"/><meta name="viewport" content="width=device-width,initial-scale=1.0"/></head>
<body style="margin:0;padding:24px;background:#f4f4f7;font-family:Arial,Helvetica,sans-serif;color:#1a1a1a;">
  <div style="max-width:520px;margin:0 auto;background:#fff;border-radius:16px;padding:32px;box-shadow:0 8px 24px rgba(0,0,0,.08);">
    <h1 style="margin:0 0 16px;font-size:22px;color:#059669;">MAGAZANIZ TESDIQLENDI</h1>
    <p style="margin:0 0 8px;font-size:16px;"><strong>Magaza nomresi:</strong> <span style="font-size:20px;letter-spacing:2px;color:#4f46e5;">%s</span></p>
    <p style="margin:0 0 24px;font-size:16px;"><strong>Magaza Sahibi:</strong> %s</p>
    <p style="margin:0 0 20px;color:#666;line-height:1.6;">Magaza kodunuz ve qeydiyyat zamanı teyin etdiyiniz magaza sifresi ile satıcı paneline daxil ola bilersiniz. Ilk girisde sexsi sifre teyin edib uz tanima qeydiyyatindan kececeksiniz.</p>
    <a href="%s" style="display:inline-block;padding:14px 28px;background:#4f46e5;color:#fff;text-decoration:none;border-radius:10px;font-weight:600;">Satıcı paneline keç</a>
    <p style="margin:24px 0 0;color:#999;font-size:12px;">© %d Buykon</p>
  </div>
</body>
</html>
""".formatted(storeCode, safeOwner, loginUrl, year);
    }
}
