package com.hackathon.blockchain.utils;

import java.security.Key;
import java.util.Base64;

public class PemUtil {
    public static String toPEMFormat(Key key, String keyType) {
        String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
        return String.format("-----BEGIN %s KEY-----\n%s\n-----END %s KEY-----", 
            keyType, base64Key, keyType);
    }
}
