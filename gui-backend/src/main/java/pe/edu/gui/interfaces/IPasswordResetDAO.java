package pe.edu.gui.interfaces;

import java.util.*;

public interface IPasswordResetDAO {
  Optional<String> request(String email, UUID id, byte[] hash);

  boolean attempt(UUID id, byte[] hash);

  boolean complete(UUID id, byte[] hash, String encodedPassword);
}
