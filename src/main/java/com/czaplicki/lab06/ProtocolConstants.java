package com.czaplicki.lab06;

public final class ProtocolConstants {
    private ProtocolConstants() {
    }

    public static final String REQUEST_REGISTER = "r:";    // e.g. "r:host,port,capacity"
    public static final String REQUEST_ORDER = "o:";    // e.g. "o:houseHost,housePort"
    public static final String REQUEST_SET_READY = "sr:";   // e.g. "sr:tankerId"
    public static final String REQUEST_SEWAGE_PUMP_IN = "spi:";  // e.g. "spi:tankerId,volume"
    public static final String REQUEST_SEWAGE_PAYOFF = "spo:";   // e.g. "spo:tankerId"
    public static final String REQUEST_GET_PUMP_OUT = "gp:";   // e.g. "gp:max"
    public static final String REQUEST_SET_JOB = "sj:";   // e.g. "sj:houseHost,housePort"
}