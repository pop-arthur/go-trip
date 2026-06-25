package gotrip.service.auth

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

object TokenHashing:
  def sha256(token: String): String =
    val digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))
    Base64.getUrlEncoder.withoutPadding().encodeToString(digest)
