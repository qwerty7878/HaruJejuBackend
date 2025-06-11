package com.goodda.jejuday.Auth.service.impl;

import com.goodda.jejuday.Auth.entity.Language;
import com.goodda.jejuday.Auth.entity.Platform;
import com.goodda.jejuday.Auth.entity.TemporaryUser;
import com.goodda.jejuday.Auth.repository.TemporaryUserRepository;
import com.goodda.jejuday.Auth.service.TemporaryUserService;
import com.goodda.jejuday.Auth.util.exception.DuplicateEmailException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TemporaryUserServiceImpl implements TemporaryUserService {

    private final TemporaryUserRepository temporaryUserRepository;

    @Override
    public void save(String name, String email, String password, Language language, Platform platform) {

        if (temporaryUserRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("이미 존재하는 이메일입니다.");
        }

        TemporaryUser temporaryUser = TemporaryUser.builder()
                .name(name)
                .email(email)
                .password(password)
                .language(language)
                .platform(platform)
                .build();
        temporaryUserRepository.save(temporaryUser);
    }

    @Override
    public Optional<TemporaryUser> findByEmail(String email) {
        return temporaryUserRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return temporaryUserRepository.existsByEmail(email);
    }

    @Override
    public void deleteByTemporaryUserId(Long temporaryUserId) {
        temporaryUserRepository.deleteByTemporaryUserId(temporaryUserId);
    }

    @Override
    public List<TemporaryUser> findByCreatedAtBefore(LocalDateTime time) {
        return temporaryUserRepository.findByCreatedAtBefore(time);
    }
}
