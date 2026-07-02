package com.bizdevar.mail;

import com.bizdevar.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final AppProperties props;
    private final JavaMailSender mailSender;

    public EmailService(AppProperties props, @Autowired(required = false) JavaMailSender mailSender) {
        this.props = props;
        this.mailSender = mailSender;
    }

    public void sendAdminOtp(String toEmail, String code) {
        if (mailSender != null && hasSmtpHost()) {
            try {
                String subject = AdminOtpEmailTemplate.subject();
                String plain = AdminOtpEmailTemplate.plainText(code);
                String html = AdminOtpEmailTemplate.html(toEmail, code);

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(props.getMail().getFrom());
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(plain, html);
                mailSender.send(message);
                log.info("Admin OTP email sent to {}", toEmail);
                return;
            } catch (Exception e) {
                log.error("Admin OTP email gonderile bilmedi ({}): {}", toEmail, e.getMessage());
            }
        } else {
            log.warn("SMTP konfiqurasiyasi tapilmadi — OTP email gonderile bilmedi ({})", toEmail);
        }

        if (props.getMail().isDevLogOtp()) {
            log.warn("=== ADMIN OTP (dev) === email={} code={}", toEmail, code);
        }
    }

    public void sendSellerOtp(String toEmail, String storeName, String code) {
        if (mailSender != null && hasSmtpHost()) {
            try {
                String subject = SellerOtpEmailTemplate.subject(storeName);
                String plain = SellerOtpEmailTemplate.plainText(storeName, code);
                String html = SellerOtpEmailTemplate.html(storeName, code);

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(props.getMail().getFrom());
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(plain, html);
                mailSender.send(message);
                log.info("Seller OTP email sent to {}", toEmail);
                return;
            } catch (Exception e) {
                log.error("Seller OTP email gonderile bilmedi ({}): {}", toEmail, e.getMessage());
            }
        } else {
            log.warn("SMTP konfiqurasiyasi tapilmadi — seller OTP email gonderile bilmedi ({})", toEmail);
        }

        if (props.getMail().isDevLogOtp()) {
            log.warn("=== SELLER OTP (dev) === email={} store={} code={}", toEmail, storeName, code);
        }
    }

    public void sendSellerApproved(String toEmail, String storeCode, String ownerName, String loginUrl) {
        if (mailSender != null && hasSmtpHost()) {
            try {
                String subject = SellerApprovedEmailTemplate.subject();
                String plain = SellerApprovedEmailTemplate.plainText(storeCode, ownerName, loginUrl);
                String html = SellerApprovedEmailTemplate.html(storeCode, ownerName, loginUrl);

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(props.getMail().getFrom());
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(plain, html);
                mailSender.send(message);
                log.info("Seller approved email sent to {}", toEmail);
                return;
            } catch (Exception e) {
                log.error("Seller approved email gonderile bilmedi ({}): {}", toEmail, e.getMessage());
            }
        } else {
            log.warn("SMTP konfiqurasiyasi tapilmadi — seller approved email gonderile bilmedi ({})", toEmail);
        }

        if (props.getMail().isDevLogOtp()) {
            log.warn("=== SELLER APPROVED (dev) === email={} code={} owner={} url={}",
                    toEmail, storeCode, ownerName, loginUrl);
        }
    }

    public void sendStaffInvite(String toEmail, String storeName, String inviteUrl) {
        if (mailSender == null) {
            log.warn("SMTP yoxdur — staff devet emaili gonderile bilmedi: {} url={}", toEmail, inviteUrl);
            return;
        }
        try {
            String subject = SellerStaffInviteEmailTemplate.subject(storeName);
            String plain = SellerStaffInviteEmailTemplate.plainText(storeName, inviteUrl);
            String html = SellerStaffInviteEmailTemplate.html(storeName, inviteUrl);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(props.getMail().getFrom());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(plain, html);
            mailSender.send(message);
            log.info("Staff invite email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Staff invite email gonderile bilmedi ({}): {}", toEmail, e.getMessage());
        }
    }

    private boolean hasSmtpHost() {
        return mailSender != null;
    }
}
