package com.fimcraft;

import java.time.Instant;
import java.util.Random;

public class Verification {
    public static String generateVerificationToken(int length) {
        long time = System.currentTimeMillis();
        Random r = new Random(time);
        String code = "";
        for(int i = 0; i < length; i++) {
            int val = (int)Math.floor(r.nextInt(26)) + 65;
            code += (char)val;
        }
        return code;
    }
}
