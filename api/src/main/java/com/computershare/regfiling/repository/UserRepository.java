package com.computershare.regfiling.repository;

import com.computershare.regfiling.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}
