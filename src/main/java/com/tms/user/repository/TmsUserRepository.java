package com.tms.user.repository;

import com.tms.user.entity.TmsUser;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmsUserRepository extends JpaRepository<TmsUser, Long> {
    List<TmsUser> findAllByOrderByActiveDescNameAsc();
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
