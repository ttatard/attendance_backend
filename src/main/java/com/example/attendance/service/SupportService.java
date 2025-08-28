package com.example.attendance.service;

import com.example.attendance.entity.SupportTicket;
import com.example.attendance.entity.User;
import com.example.attendance.repository.SupportTicketRepository;
import com.example.attendance.repository.UserRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;

@Service
public class SupportService {
    private final SupportTicketRepository supportTicketRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final String supportEmail = "attendancesystems10@gmail.com";

    public SupportService(SupportTicketRepository supportTicketRepository, 
                        UserRepository userRepository,
                        JavaMailSender mailSender) {
        this.supportTicketRepository = supportTicketRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    public SupportTicket createSupportTicket(Long userId, String concernType, 
                                          String message, MultipartFile attachment) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        SupportTicket ticket = new SupportTicket();
        ticket.setUser(user);
        ticket.setConcernType(concernType);
        ticket.setMessage(message);
        
        if (attachment != null && !attachment.isEmpty()) {
            String attachmentPath = saveAttachment(userId, attachment);
            ticket.setAttachmentPath(attachmentPath);
        }
        
        SupportTicket savedTicket = supportTicketRepository.save(ticket);
        sendSupportEmail(savedTicket);
        return savedTicket;
    }

    private String saveAttachment(Long userId, MultipartFile attachment) {
        try {
            String uploadDir = "support-attachments/" + userId;
            Path uploadPath = Paths.get(uploadDir);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            String fileName = System.currentTimeMillis() + "_" + attachment.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(attachment.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store attachment", e);
        }
    }

    private void sendSupportEmail(SupportTicket ticket) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(supportEmail);
            helper.setSubject("New Support Ticket: " + ticket.getConcernType());
            helper.setText(buildEmailContent(ticket), true);
            
            if (ticket.getAttachmentPath() != null && !ticket.getAttachmentPath().isEmpty()) {
                FileSystemResource file = new FileSystemResource(ticket.getAttachmentPath());
                helper.addAttachment("SupportAttachment", file);
            }
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send support email", e);
        }
    }

    private String buildEmailContent(SupportTicket ticket) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    return "<html><body>" +
           "<h2>New Support Ticket Received</h2>" +
           "<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse;'>" +
           "<tr><th style='background-color: #f2f2f2; padding: 8px;'>Field</th><th style='background-color: #f2f2f2; padding: 8px;'>Value</th></tr>" +
           "<tr><td style='padding: 8px;'><strong>User Name</strong></td><td style='padding: 8px;'>" + ticket.getUser().getName() + "</td></tr>" +
           "<tr><td style='padding: 8px;'><strong>User Email</strong></td><td style='padding: 8px;'>" + ticket.getUser().getEmail() + "</td></tr>" +
           "<tr><td style='padding: 8px;'><strong>Account Type</strong></td><td style='padding: 8px;'>" + ticket.getUser().getAccountType().name() + "</td></tr>" +
           "<tr><td style='padding: 8px;'><strong>Concern Type</strong></td><td style='padding: 8px;'>" + ticket.getConcernType() + "</td></tr>" +
           "<tr><td style='padding: 8px;'><strong>Message</strong></td><td style='padding: 8px;'>" + ticket.getMessage() + "</td></tr>" +
           "<tr><td style='padding: 8px;'><strong>Submitted At</strong></td><td style='padding: 8px;'>" + ticket.getCreatedAt().format(formatter) + "</td></tr>" +
           "</table></body></html>";
}
}