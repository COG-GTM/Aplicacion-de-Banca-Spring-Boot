package com.coding.exercise.bankapp.config;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Authorizes access to a customer record: allowed for administrators, or for the
 * authenticated principal whose username is the requested customer number.
 */
@Component("customerAccess")
public class CustomerAccessEvaluator {

	public static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

	public boolean isAdmin(Authentication authentication) {
		return isAuthenticated(authentication) && authentication.getAuthorities().stream()
				.anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
	}

	public boolean canAccess(Authentication authentication, Long customerNumber) {
		if (!isAuthenticated(authentication) || customerNumber == null) {
			return false;
		}
		return isAdmin(authentication) || customerNumber.toString().equals(authentication.getName());
	}

	private boolean isAuthenticated(Authentication authentication) {
		return authentication != null && authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
	}
}
