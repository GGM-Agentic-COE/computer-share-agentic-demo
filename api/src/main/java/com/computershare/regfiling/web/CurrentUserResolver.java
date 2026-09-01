package com.computershare.regfiling.web;

import com.computershare.regfiling.domain.User;
import com.computershare.regfiling.security.AuthFilter;
import jakarta.servlet.http.HttpServletRequest;

final class CurrentUserResolver {

    private CurrentUserResolver() {
    }

    static User require(HttpServletRequest request) {
        Object user = request.getAttribute(AuthFilter.CURRENT_USER_ATTR);
        if (user == null) {
            // AuthFilter runs before any controller and always rejects unauthenticated requests
            // first — reaching here without a user attribute would be a wiring bug, not a normal
            // request path.
            throw new IllegalStateException("No authenticated user on request — AuthFilter not applied?");
        }
        return (User) user;
    }
}
