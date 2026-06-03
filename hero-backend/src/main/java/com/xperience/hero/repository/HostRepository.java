package com.xperience.hero.repository;

import com.xperience.hero.entity.Host;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HostRepository extends JpaRepository<Host, Long> {
    Optional<Host> findByEmail(String email);
    boolean existsByEmail(String email);
}
