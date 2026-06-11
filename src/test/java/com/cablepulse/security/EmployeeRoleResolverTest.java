package com.cablepulse.security;

import com.cablepulse.model.Employee;
import com.cablepulse.model.EmployeeRole;
import com.cablepulse.service.EmployeeReconciliationService;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeRoleResolverTest {

    @Mock
    private EmployeeReconciliationService employeeReconciliationService;

    private EmployeeRoleResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new EmployeeRoleResolver(employeeReconciliationService);
    }

    @Test
    void resolveAuthorities_unknownEmployee_defaultsToCollectionBoy() {
        FirebaseToken token = mockToken("uid-unknown", Map.of());
        when(employeeReconciliationService.resolveEmployee(token)).thenReturn(null);

        List<GrantedAuthority> authorities = resolver.resolveAuthorities(token);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_COLLECTION_BOY");
    }

    @Test
    void resolveAuthorities_reconciliationFailure_defaultsToCollectionBoy() {
        FirebaseToken token = mockToken("uid-error", Map.of());
        when(employeeReconciliationService.resolveEmployee(token))
                .thenThrow(new RuntimeException("db down"));

        List<GrantedAuthority> authorities = resolver.resolveAuthorities(token);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_COLLECTION_BOY");
    }

    @Test
    void resolveAuthorities_ownerEmployee_returnsOwner() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getClaims()).thenReturn(Map.of());
        when(employeeReconciliationService.resolveEmployee(token))
                .thenReturn(new Employee("uid-owner", "Owner User", EmployeeRole.OWNER));

        List<GrantedAuthority> authorities = resolver.resolveAuthorities(token);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_OWNER");
    }

    @Test
    void resolveAuthorities_firebaseClaimTakesPrecedence() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getClaims()).thenReturn(Map.of("role", "OWNER"));

        List<GrantedAuthority> authorities = resolver.resolveAuthorities(token);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_OWNER");
    }

    @Test
    void resolveRoleClaim_firebaseOwnerClaim_returnsOwnerWithoutEmployeeRow() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getClaims()).thenReturn(Map.of("role", "OWNER"));

        assertThat(resolver.resolveRoleClaim(token)).isEqualTo("ROLE_OWNER");
    }

    private static FirebaseToken mockToken(String uid, Map<String, Object> claims) {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn(uid);
        when(token.getClaims()).thenReturn(claims);
        return token;
    }
}
