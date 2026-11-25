package tn.gezenthi.notification;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Sends e-mails through the agency's SMTP account (gezenthi.notifications.email.*).
 * A JavaMailSender bean, if present (tests), takes precedence over the configured SMTP.
 */
@Component
class EmailSender {

    private final NotificationProperties.Email config;
    private final JavaMailSender mailer;

    EmailSender(NotificationProperties props, ObjectProvider<JavaMailSender> override) {
        this.config = props.getEmail();
        JavaMailSender bean = override.getIfAvailable();
        this.mailer = bean != null ? bean : config.ready() ? smtp(config) : null;
    }

    boolean ready() {
        return config.isEnabled() && mailer != null && !config.getFrom().isBlank();
    }

    void send(String to, MessageTemplates.Mail mail) throws Exception {
        MimeMessage msg = mailer.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());
        h.setFrom(config.getFrom(), "Gezenthi Travel");
        h.setTo(to);
        h.setSubject(mail.subject());
        h.setText(mail.text(), mail.html());
        mailer.send(msg);
    }

    private static JavaMailSender smtp(NotificationProperties.Email c) {
        JavaMailSenderImpl m = new JavaMailSenderImpl();
        m.setHost(c.getHost());
        m.setPort(c.getPort());
        m.setUsername(c.getUsername());
        m.setPassword(c.getPassword());
        m.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties p = m.getJavaMailProperties();
        p.put("mail.smtp.auth", String.valueOf(!c.getUsername().isBlank()));
        p.put("mail.smtp.starttls.enable", String.valueOf(c.isStartTls()));
        p.put("mail.smtp.ssl.enable", String.valueOf(c.isSsl()));
        p.put("mail.smtp.connectiontimeout", "10000");
        p.put("mail.smtp.timeout", "15000");
        p.put("mail.smtp.writetimeout", "15000");
        return m;
    }
}
