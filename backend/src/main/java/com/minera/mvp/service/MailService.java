package com.minera.mvp.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * Envia el resumen por email. Si app.mail.enabled=false, solo lo muestra en consola
 * (modo local sin SMTP). Para activarlo, completar spring.mail.* en application.properties.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public MailService(@Value("${app.mail.enabled:false}") boolean enabled,
                       @Value("${app.mail.from:sistema@minera.mvp}") String from,
                       JavaMailSender mailSender) {
        this.enabled = enabled;
        this.from = from;
        this.mailSender = mailSender;
    }

    public void sendRunSummary(String to, String subject, String body, List<File> attachments) {
        if (!enabled) {
            log.info("EMAIL (simulado, sin SMTP): para={}, asunto={}", to, subject);
            log.info("   cuerpo: {}", body.replace("\n", " "));
            for (File f : attachments) log.info("   adjunto: {}", f.getName());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, !attachments.isEmpty());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            for (File f : attachments) {
                helper.addAttachment(f.getName(), new FileSystemResource(f));
            }
            mailSender.send(message);
            log.info("EMAIL enviado a {}", to);
        } catch (Exception ex) {
            log.warn("No se pudo enviar el email a {}: {}", to, ex.getMessage());
        }
    }
}