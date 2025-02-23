package com.hackathon.blockchain.utils;

import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

public class SignatureUtil {
    public static boolean verifySignature(String data, String signature, PublicKey publicKey) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(data.getBytes());
            return sig.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
