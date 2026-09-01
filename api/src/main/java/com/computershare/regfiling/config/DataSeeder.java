package com.computershare.regfiling.config;

import com.computershare.regfiling.domain.User;
import com.computershare.regfiling.domain.UserRole;
import com.computershare.regfiling.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// WU-09 / docs/sdlc/phase-5-test-strategy/synthetic-data.json — fabricated demo users only,
// no real EquatePlus/Computershare customer data.
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.save(new User("exec-1", "J. Alvarez", UserRole.EXECUTIVE));
        userRepository.save(new User("exec-2", "R. Chen", UserRole.EXECUTIVE));
        userRepository.save(new User("exec-3", "M. Okafor", UserRole.EXECUTIVE));
        userRepository.save(new User("legal-1", "S. Kapoor", UserRole.LEGAL_COMPLIANCE));
        userRepository.save(new User("legal-2", "D. Whitfield", UserRole.LEGAL_COMPLIANCE));
    }
}
