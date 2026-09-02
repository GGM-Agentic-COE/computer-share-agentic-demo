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
        userRepository.save(User.builder("exec-1", "J. Alvarez", UserRole.EXECUTIVE)
                .name("Alvarez", "Jordan", null)
                .address("482 Harborview Terrace", "Wilmington", "DE", "19801")
                .director()
                .baselineShareholding(12400)
                .build());
        userRepository.save(User.builder("exec-2", "R. Chen", UserRole.EXECUTIVE)
                .name("Chen", "Riley", "M")
                .address("77 Prairie Wind Lane", "Austin", "TX", "78701")
                .officer("Chief Financial Officer")
                .baselineShareholding(8750)
                .build());
        userRepository.save(User.builder("exec-3", "M. Okafor", UserRole.EXECUTIVE)
                .name("Okafor", "Maya", "N")
                .address("1290 Silverleaf Court", "Denver", "CO", "80202")
                .tenPercentOwner()
                .baselineShareholding(21900)
                .build());
        userRepository.save(User.builder("legal-1", "S. Kapoor", UserRole.LEGAL_COMPLIANCE).build());
        userRepository.save(User.builder("legal-2", "D. Whitfield", UserRole.LEGAL_COMPLIANCE).build());
    }
}
