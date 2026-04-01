package com.bidify.common.util;

import java.util.UUID;

public class IdGenerator {
    private IdGenerator(){}

    public static String genRequestId() {
        return "REQ-" + UUID.randomUUID().toString().substring(0, 12);
    }
}
