package com.tms.user.service;

import com.tms.user.dto.UserRequest;
import com.tms.user.dto.UserResponse;
import com.tms.user.entity.TmsUser;
import com.tms.user.repository.TmsUserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final TmsUserRepository userRepository;

    public UserService(TmsUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getAll() {
        return userRepository.findAllByOrderByActiveDescNameAsc().stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse get(Long id) {
        return UserResponse.from(findById(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        String email = normalizeEmail(request.email());
        validateUniqueEmail(email, null);
        TmsUser user = new TmsUser(
                request.name().trim(),
                email,
                request.role(),
                request.active() == null || request.active()
        );
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        TmsUser user = findById(id);
        String email = normalizeEmail(request.email());
        validateUniqueEmail(email, id);
        user.update(
                request.name().trim(),
                email,
                request.role(),
                request.active() == null || request.active()
        );
        return UserResponse.from(user);
    }

    @Transactional
    public void deactivate(Long id) {
        findById(id).deactivate();
    }

    private TmsUser findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found. id=" + id));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) return null;
        return email.trim().toLowerCase();
    }

    private void validateUniqueEmail(String email, Long id) {
        if (email == null) return;
        boolean exists = id == null
                ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(email, id);
        if (exists) throw new IllegalArgumentException("이미 등록된 이메일입니다: " + email);
    }
}
