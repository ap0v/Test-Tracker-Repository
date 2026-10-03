import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

/**
 * Standalone helper, excluded from the application build.
 * Run from test-tracker with the Maven dependency classpath:
 * java --class-path "<classpath>" tools/password-hash/GeneratePasswordHash.java
 * An optional argument selects another .env file.
 *
 * The file must contain PASSWORD_TO_HASH=value. Matching single or double
 * quotes are removed; characters inside them are preserved literally.
 * Blank lines and full-line # comments are ignored. No variable expansion,
 * escape sequences, or inline comments are processed.
 */
public class GeneratePasswordHash {
    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: GeneratePasswordHash.java [path/to/.env]");
            System.exit(1);
        }

        Path envFile = Path.of(args.length == 1 ? args[0] : "tools/password-hash/.env");
        try {
            String password = readPassword(envFile);
            var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
            System.out.println(encoder.encode(password));
        } catch (IOException exception) {
            System.err.println("Cannot read the .env file. Run from test-tracker or pass its path.");
            System.exit(1);
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            System.exit(1);
        }
    }

    static String readPassword(Path envFile) throws IOException {
        String password = null;
        String contents = Files.readString(envFile, StandardCharsets.UTF_8);
        if (contents.startsWith("\uFEFF")) {
            contents = contents.substring(1);
        }

        for (String line : contents.lines().toList()) {
            String entry = line.strip();
            if (entry.isEmpty() || entry.startsWith("#")) continue;

            int separator = entry.indexOf('=');
            if (separator < 0 || !entry.substring(0, separator).strip().equals("PASSWORD_TO_HASH")) continue;
            if (password != null) {
                throw new IllegalArgumentException("Define PASSWORD_TO_HASH only once.");
            }

            password = entry.substring(separator + 1).strip();
            if (password.startsWith("\"") || password.startsWith("'")) {
                char quote = password.charAt(0);
                if (password.length() < 2 || password.charAt(password.length() - 1) != quote) {
                    throw new IllegalArgumentException("PASSWORD_TO_HASH has unmatched quotes.");
                }
                password = password.substring(1, password.length() - 1);
            }
        }

        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Set a non-empty PASSWORD_TO_HASH in the .env file.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("PASSWORD_TO_HASH must be at most 72 UTF-8 bytes for BCrypt.");
        }
        return password;
    }
}
