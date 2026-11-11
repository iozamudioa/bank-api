package dev.ioza.bank.application.ports.out;

public interface CommandFingerprint {
  String fingerprint(String operation, Object command);
}
