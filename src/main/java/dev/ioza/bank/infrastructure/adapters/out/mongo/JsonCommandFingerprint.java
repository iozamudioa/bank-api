package dev.ioza.bank.infrastructure.adapters.out.mongo;

import dev.ioza.bank.application.ports.out.CommandFingerprint;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JsonCommandFingerprint implements CommandFingerprint {
  private final JsonMapper mapper;

  public JsonCommandFingerprint(JsonMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public String fingerprint(String operation, Object command) {
    String json = mapper.writeValueAsString(command);
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest((operation + "\0" + json).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 no disponible", e);
    }
  }
}
