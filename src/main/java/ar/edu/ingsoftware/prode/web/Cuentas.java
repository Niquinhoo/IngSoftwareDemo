package ar.edu.ingsoftware.prode.web;

import ar.edu.ingsoftware.prode.ProdeException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
class Cuentas implements UserDetailsService {
    private static final Pattern EMAIL = Pattern.compile("[^\\s@\\p{Cntrl}]+@[^\\s@\\p{Cntrl}]+\\.[^\\s@\\p{Cntrl}]+");
    private final JdbcClient jdbc;
    private final PasswordEncoder encoder;

    Cuentas(JdbcClient jdbc, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return jdbc.sql("SELECT email, password_hash, rol FROM cuentas WHERE email = :email")
                .param("email", normalizarEmail(email))
                .query((rs, fila) -> User.withUsername(rs.getString("email"))
                        .password(rs.getString("password_hash"))
                        .roles(rs.getString("rol"))
                        .build())
                .optional()
                .orElseThrow(() -> new UsernameNotFoundException("Cuenta inexistente"));
    }

    void registrar(String email, String password) {
        String normalizado = validar(email, password);
        try {
            insertar(normalizado, password, "USER");
        } catch (DuplicateKeyException e) {
            throw new ProdeException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo.");
        }
    }

    void prepararAdministrador(String email, String password) {
        if (email.isBlank() && password.isEmpty()) {
            return;
        }
        if (email.isBlank() || password.isEmpty()) {
            throw new IllegalStateException("Configurá PRODE_ADMIN_EMAIL y PRODE_ADMIN_PASSWORD juntos.");
        }
        String normalizado = validar(email, password);
        try {
            insertar(normalizado, password, "ADMIN");
        } catch (DuplicateKeyException e) {
            if (loadUserByUsername(normalizado).getAuthorities().stream()
                    .noneMatch(autoridad -> "ROLE_ADMIN".equals(autoridad.getAuthority()))) {
                throw new IllegalStateException("El correo configurado para administración ya pertenece a un participante.");
            }
        }
    }

    private void insertar(String email, String password, String rol) {
        jdbc.sql("INSERT INTO cuentas (email, password_hash, rol) VALUES (:email, :hash, :rol)")
                .param("email", email)
                .param("hash", encoder.encode(password))
                .param("rol", rol)
                .update();
    }

    private static String validar(String email, String password) {
        String normalizado = normalizarEmail(email);
        if (normalizado.length() > 254 || !EMAIL.matcher(normalizado).matches()) {
            throw new ProdeException(HttpStatus.BAD_REQUEST, "Ingresá un correo electrónico válido.");
        }
        if (!passwordValida(password)) {
            throw new ProdeException(HttpStatus.BAD_REQUEST,
                    "La contraseña debe tener al menos 8 caracteres y ocupar hasta 72 bytes.");
        }
        return normalizado;
    }

    private static String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    static boolean passwordValida(CharSequence password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            return false;
        }
        String texto = password.toString();
        return texto.codePointCount(0, texto.length()) >= 8
                && texto.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
