package com.nmamit.payments.service;

import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Returns the database User (with department loaded) for whoever is logged in right now. */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new NotAllowedException("No user is logged in");
        }
        return userRepository.findWithDepartmentByEmail(auth.getName())
                .orElseThrow(() -> new NotAllowedException("Logged-in user not found"));
    }
}
