package com.bizdevar.mail;

final class SellerStaffInviteEmailTemplate {

    private SellerStaffInviteEmailTemplate() {}

    static String subject(String storeName) {
        return storeName + " — Buykon satıcı panelinə dəvət";
    }

    static String plainText(String storeName, String inviteUrl) {
        return "Salam!\n\n"
                + storeName + " sizi Buykon satıcı panelinə dəvət edir.\n\n"
                + "Qəbul et: " + inviteUrl + "\n\n"
                + "Link 7 gün etibarlıdır.\n\n"
                + "Buykon.com";
    }

    static String html(String storeName, String inviteUrl) {
        return """
                <!DOCTYPE html>
                <html lang="az">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#f4f6fb;font-family:Inter,Arial,sans-serif;color:#0f172a;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f6fb;padding:32px 16px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border-radius:20px;overflow:hidden;box-shadow:0 16px 48px rgba(15,23,42,.08);">
                        <tr><td style="background:linear-gradient(135deg,#111827,#1f2937);padding:28px 32px;">
                          <div style="font-size:13px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;color:#f97316;">Buykon Business</div>
                          <h1 style="margin:12px 0 0;font-size:24px;line-height:1.3;color:#ffffff;">Satıcı panelinə dəvət</h1>
                        </td></tr>
                        <tr><td style="padding:32px;">
                          <p style="margin:0 0 16px;font-size:16px;line-height:1.7;color:#334155;">Salam,</p>
                          <p style="margin:0 0 24px;font-size:16px;line-height:1.7;color:#334155;"><strong style="color:#0f172a;">%s</strong> sizi mağaza komandasına qoşulmağa dəvət edir. Dəvəti qəbul edib şifrənizi təyin etdikdən sonra satıcı panelinə daxil ola bilərsiniz.</p>
                          <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 auto 24px;">
                            <tr><td style="border-radius:14px;background:linear-gradient(135deg,#f97316,#fb923c);">
                              <a href="%s" style="display:inline-block;padding:16px 32px;color:#111827;font-size:16px;font-weight:800;text-decoration:none;">Dəvəti qəbul et</a>
                            </td></tr>
                          </table>
                          <p style="margin:0;font-size:13px;line-height:1.6;color:#64748b;">Link 7 gün etibarlıdır. Əgər bu dəvəti gözləmirdinizsə, bu emaili nəzərə almayın.</p>
                        </td></tr>
                        <tr><td style="padding:20px 32px 28px;border-top:1px solid #e2e8f0;background:#f8fafc;">
                          <p style="margin:0;font-size:12px;color:#94a3b8;text-align:center;">© Buykon.com · Threesome Group</p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(escape(storeName), escape(inviteUrl));
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
