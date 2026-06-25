package com.helpdesk.service;

import com.helpdesk.model.Attachment;
import com.helpdesk.model.Ticket;
import com.helpdesk.repository.AttachmentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService {

    private final AttachmentRepository attachmentRepository;
    private final Path fileStorageLocation;

    public LocalStorageService(
            AttachmentRepository attachmentRepository,
            @Value("${app.file.upload-dir:./uploads}") String uploadDir) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create upload directory.", ex);
        }
    }

    public Attachment store(MultipartFile file, Ticket ticket) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            originalFilename = "unknown_file";
        }

        // Generate unique filename to prevent collisions
        String storedFilename = UUID.randomUUID().toString() + "_" + originalFilename;
        Path targetLocation = this.fileStorageLocation.resolve(storedFilename);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + storedFilename, ex);
        }

        /* 
         * NOTE: If your Attachment model is immutable (like your AuditLog), 
         * you must use a constructor or Builder pattern here instead of setters.
         */
        Attachment attachment = new Attachment();
        attachment.setOriginalFilename(originalFilename);
        attachment.setStoredFilename(storedFilename);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setFilePath(targetLocation.toString());
        attachment.setTicket(ticket);

        return attachmentRepository.save(attachment);
    }
}