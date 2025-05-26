package com.api.bank.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.api.bank.entities.user.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    UserDetails findByEmail(String email);
}
