package fr.monkeynotes.mn.data.event;

/**
 * Published whenever a user account is created, so that per-user setup can be
 * seeded without the creating service having to know about the consumers.
 *
 * Exists to keep the bean graph acyclic: UserService is the UserDetailsService
 * that AuthService depends on, and PreferencesService depends on AuthService,
 * so a direct UserService -> PreferencesService injection would form a cycle and
 * spring boot rejects those at startup by default.
 */
public record UserCreatedEvent(String username) {
}
