package com.crm.matrix.config;

public final class PhoneNumberUtils {

    private PhoneNumberUtils() {
    }

    public static String normalize(String phone) {

        if (phone == null) {
            return null;
        }

        phone = phone.trim()
                .replaceAll("[^0-9+]", "");

        // Already has country code
        if (phone.startsWith("+")) {
            return phone;
        }

        // US number: 10 digits
        if (phone.length() == 10) {
            return "+1" + phone;
        }

        // Already starts with US country code without +
        if (phone.startsWith("1") && phone.length() == 11) {
            return "+" + phone;
        }

        return "+" + phone;
    }
}