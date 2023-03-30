package io.hackfest.error;

public enum ErrorCode {
    DEVICE_ID_UNKNOWN(100, "Device id unknown for serial: {0}");

    private final int code;
    private final String messsage;

    ErrorCode(int code, String title) {
        this.code = code;
        this.messsage = title;
    }

    public int getCode() {
        return code;
    }

    public String getMesssage() {
        return messsage;
    }
}
