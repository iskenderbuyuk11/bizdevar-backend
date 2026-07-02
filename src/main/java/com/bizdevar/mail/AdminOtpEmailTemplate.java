package com.bizdevar.mail;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

final class AdminOtpEmailTemplate {

    private static final String LOGO_URL = "https://buykon.com/images/logo.png";
    private static final String BRAND_ORANGE = "#ff9100";
    private static final String BRAND_GOLD = "#ffae00";

    private AdminOtpEmailTemplate() {}

    static String subject() {
        return "Buykon Admin — giriş təsdiq kodu";
    }

    static String plainText(String code) {
        return "Buykon Admin panel\n\n"
                + "Giriş kodunuz: " + code + "\n\n"
                + "Kod 10 dəqiqə ərzində etibarlıdır.\n"
                + "Bu kodu heç kimlə paylaşmayın.\n\n"
                + "— Buykon Marketplace";
    }

    static String html(String toEmail, String code) {
        String digits = renderDigitBoxes(code);
        String formatted = formatCode(code);
        int year = Year.now().getValue();

        return """
<!DOCTYPE html>
<html lang="az">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Buykon Admin OTP</title>
</head>
<body style="margin:0;padding:0;background:#f4f4f7;font-family:Arial,Helvetica,sans-serif;color:#1a1a1a;">
  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f4f7;padding:32px 16px;">
    <tr>
      <td align="center">
        <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="max-width:600px;width:100%%;">

          <!-- Header -->
          <tr>
            <td style="background:linear-gradient(135deg,%1$s,%2$s);border-radius:16px 16px 0 0;padding:28px 32px;text-align:center;">
              <img src="%3$s" alt="Buykon" width="120" style="display:block;margin:0 auto 16px;max-width:120px;height:auto;" />
              <div style="display:inline-block;background:rgba(255,255,255,0.22);border-radius:999px;padding:8px 16px;">
                <span style="font-size:13px;font-weight:700;color:#fff;letter-spacing:0.04em;text-transform:uppercase;">Admin Panel</span>
              </div>
            </td>
          </tr>

          <!-- Body -->
          <tr>
            <td style="background:#ffffff;padding:36px 32px 28px;border-left:1px solid #ececf1;border-right:1px solid #ececf1;">
              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                <tr>
                  <td align="center" style="padding-bottom:20px;">
                    <div style="width:56px;height:56px;border-radius:16px;background:#fff7ed;border:1px solid #ffe0b2;line-height:56px;font-size:26px;text-align:center;">
                      🛡️
                    </div>
                  </td>
                </tr>
                <tr>
                  <td align="center" style="padding-bottom:8px;">
                    <h1 style="margin:0;font-size:24px;line-height:1.3;font-weight:800;color:#111827;">Giriş təsdiq kodu</h1>
                  </td>
                </tr>
                <tr>
                  <td align="center" style="padding-bottom:28px;">
                    <p style="margin:0;font-size:15px;line-height:1.6;color:#6b7280;">
                      <strong style="color:#374151;">%4$s</strong> üçün admin panelə daxil olmaq üçün aşağıdakı kodu daxil edin.
                    </p>
                  </td>
                </tr>
                <tr>
                  <td align="center" style="padding-bottom:10px;">
                    %5$s
                  </td>
                </tr>
                <tr>
                  <td align="center" style="padding-bottom:28px;">
                    <p style="margin:0;font-size:13px;color:#9ca3af;letter-spacing:0.12em;font-weight:600;">%6$s</p>
                  </td>
                </tr>

                <!-- Info cards -->
                <tr>
                  <td>
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                      <tr>
                        <td width="48%%" valign="top" style="padding-right:8px;">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f9fafb;border:1px solid #eef0f4;border-radius:12px;">
                            <tr>
                              <td style="padding:16px;">
                                <div style="font-size:20px;line-height:1;margin-bottom:8px;">⏱️</div>
                                <div style="font-size:13px;font-weight:700;color:#111827;margin-bottom:4px;">10 dəqiqə</div>
                                <div style="font-size:12px;line-height:1.5;color:#6b7280;">Kod müddəti bitdikdən sonra yenidən OTP istəyin.</div>
                              </td>
                            </tr>
                          </table>
                        </td>
                        <td width="48%%" valign="top" style="padding-left:8px;">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f9fafb;border:1px solid #eef0f4;border-radius:12px;">
                            <tr>
                              <td style="padding:16px;">
                                <div style="font-size:20px;line-height:1;margin-bottom:8px;">🔒</div>
                                <div style="font-size:13px;font-weight:700;color:#111827;margin-bottom:4px;">Məxfi kod</div>
                                <div style="font-size:12px;line-height:1.5;color:#6b7280;">Kodu heç kimlə paylaşmayın — Buykon bunu heç vaxt soruşmur.</div>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>

                <!-- Warning -->
                <tr>
                  <td style="padding-top:18px;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#fff7ed;border:1px solid #ffedd5;border-radius:12px;">
                      <tr>
                        <td style="padding:14px 16px;">
                          <table role="presentation" cellspacing="0" cellpadding="0">
                            <tr>
                              <td valign="top" style="padding-right:10px;font-size:18px;line-height:1;">⚠️</td>
                              <td style="font-size:12px;line-height:1.55;color:#9a3412;">
                                Bu sorğu sizdən deyilsə, emaili nəzərə almayın və admin panelə giriş cəhdlərini yoxlayın.
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td style="background:#111827;border-radius:0 0 16px 16px;padding:24px 32px;text-align:center;border:1px solid #111827;border-top:none;">
              <p style="margin:0 0 8px;font-size:13px;font-weight:700;color:#ffffff;">Buykon Marketplace</p>
              <p style="margin:0 0 14px;font-size:12px;line-height:1.6;color:#9ca3af;">
                Təhlükəsiz admin giriş bildirişi · <a href="https://buykon.com" style="color:%1$s;text-decoration:none;">buykon.com</a>
              </p>
              <p style="margin:0;font-size:11px;color:#6b7280;">© %7$d Buykon. Bütün hüquqlar qorunur.</p>
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>
""".formatted(BRAND_ORANGE, BRAND_GOLD, LOGO_URL, escapeHtml(toEmail), digits, formatted, year);
    }

    private static String renderDigitBoxes(String code) {
        if (code == null || code.isBlank()) {
            return "<span style=\"font-size:28px;font-weight:800;color:#111827;\">------</span>";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" style=\"margin:0 auto;\"><tr>");
        for (char c : code.trim().toCharArray()) {
            if (!Character.isDigit(c)) continue;
            sb.append("""
                <td style="padding:0 4px;">
                  <div style="width:44px;height:52px;line-height:52px;text-align:center;border-radius:10px;background:#fff7ed;border:2px solid #ffd699;font-size:28px;font-weight:800;color:#111827;font-family:Consolas,Monaco,monospace;">%s</div>
                </td>
                """.formatted(c));
        }
        sb.append("</tr></table>");
        return sb.toString();
    }

    private static String formatCode(String code) {
        if (code == null) return "";
        List<Character> digits = new ArrayList<>();
        for (char c : code.trim().toCharArray()) {
            if (Character.isDigit(c)) digits.add(c);
        }
        if (digits.size() <= 3) return code.trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.size(); i++) {
            if (i > 0 && i % 3 == 0) sb.append(' ');
            sb.append(digits.get(i));
        }
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
