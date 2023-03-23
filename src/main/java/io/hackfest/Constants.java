package io.hackfest;

public class Constants {
    public final static String K8S_CA_ISSUER = "tailorshift-ca-issuer";
    public final static String K8S_KEYSTORE_PASSWORD_SECRET_NAME = "java-keystore";
    public final static String K8S_KEYSTORE_PASSWORD_SECRET_KEY = "password";
    public final static String POS_EDGE_NAME_PREFIX = "tailorshift-pos-device-";
    public final static String DNS_ROOT = "tailorshift.com";
    public final static String DEVICE_ID_LABEL_KEY = "device-id";
    public final static String POS_EDGE_SECRET_LABEL_KEY = "usage";
    public final static String POS_EDGE_SECRET_LABEL_VALUE = "pos-edge";

    private Constants() {
        // static class
    }
}
