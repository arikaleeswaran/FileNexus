
package com.capstone.filenexus.service;

import com.capstone.filenexus.entity.FileEntity;
import com.capstone.filenexus.entity.User;
import com.capstone.filenexus.entity.enums.UserRole;
import com.capstone.filenexus.exception.ResourceNotFoundException;
import com.capstone.filenexus.repository.FileRepository;
import com.capstone.filenexus.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FileAuthorizationService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    public FileAuthorizationService(
            FileRepository fileRepository,
            UserRepository userRepository
    ) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
    }

    public User getActiveUser(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        if (!user.isActive()) {
            throw new ResourceNotFoundException(
                    "User not found"
            );
        }

        return user;
    }

    public FileEntity getFileForRead(
            Long fileId,
            String email,
            UserRole role
    ) {
        FileEntity file = findFile(fileId);

        if (role == UserRole.ADMIN ||
                role == UserRole.SECURITY_ANALYST) {
            return file;
        }

        User user = getActiveUser(email);

        if (!file.getUser().getUserId().equals(user.getUserId())) {
            throw new ResourceNotFoundException(
                    "File not found"
            );
        }

        return file;
    }

    public FileEntity getFileForModification(
            Long fileId,
            String email,
            UserRole role
    ) {
        FileEntity file = findFile(fileId);

        if (role == UserRole.ADMIN) {
            return file;
        }

        if (role != UserRole.USER) {
            throw new ResourceNotFoundException(
                    "File not found"
            );
        }

        User user = getActiveUser(email);

        if (!file.getUser().getUserId().equals(user.getUserId())) {
            throw new ResourceNotFoundException(
                    "File not found"
            );
        }

        return file;
    }

    private FileEntity findFile(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "File not found"
                        )
                );
    }
}
