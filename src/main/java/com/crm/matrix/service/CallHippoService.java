package com.crm.matrix.service;

import com.crm.matrix.entity.CallHistory;
import com.crm.matrix.entity.Client;
import com.crm.matrix.entity.User;
import com.crm.matrix.repository.CallHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class CallHippoService {

    private static final String CALL_URL =
            "https://web.callhippo.com/v1/call";

    private static final String USER_STATUS_URL =
            "https://web.callhippo.com/v1/user/status";

    private final RestTemplate restTemplate;
    private final RestTemplate patchRestTemplate;
    private final CallHistoryRepository callHistoryRepository;
    private final ObjectMapper objectMapper;

    public CallHippoService(
            @Qualifier("defaultRestTemplate")
            RestTemplate restTemplate,

            @Qualifier("patchRestTemplate")
            RestTemplate patchRestTemplate,

            CallHistoryRepository callHistoryRepository,

            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.patchRestTemplate = patchRestTemplate;
        this.callHistoryRepository = callHistoryRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * ============================================================
     * MAKE CALL
     * ============================================================
     *
     * Flow:
     *
     * CRM
     *   ↓
     * Set CallHippo employee Available
     *   ↓
     * POST /v1/call
     *   ↓
     * Save CallHistory
     */
    public CallHistory makeCall(User user, Client client) {

        validateCallHippoConfiguration(user);

        String clientPhone = normalizePhone(client.getPhone());

        if (clientPhone == null || clientPhone.isBlank()) {
            throw new RuntimeException(
                    "Client phone number is missing"
            );
        }

        System.out.println("==========================================");
        System.out.println("CALLHIPPO CALL");
        System.out.println("Employee Code : " + user.getEmployeeCode());
        System.out.println("Employee Email: " + user.getEmail());
        System.out.println("To Number     : " + clientPhone);
        System.out.println("From Number   : " + user.getCallHippoFromNumber());
        System.out.println("Agent ID      : " + user.getCallHippoAgentId());
        System.out.println("==========================================");

        /*
         * STEP 1
         *
         * Make the CallHippo agent available.
         */
        boolean available = setUserAvailable(user);

        if (!available) {
            throw new RuntimeException(
                    "Unable to make CallHippo employee Available"
            );
        }

        /*
         * STEP 2
         *
         * Prepare CallHippo request.
         */
        Map<String, String> requestBody = new HashMap<>();

        requestBody.put(
                "toNumber",
                clientPhone
        );

        requestBody.put(
                "fromNumber",
                user.getCallHippoFromNumber()
        );

        requestBody.put(
                "agentId",
                user.getCallHippoAgentId()
        );

        /*
         * STEP 3
         *
         * Headers.
         *
         * IMPORTANT:
         * CallHippo expects apiToken as a HEADER.
         *
         * NOT:
         *
         * Authorization: Bearer token
         *
         * Instead:
         *
         * apiToken: actual-token
         */
        HttpHeaders headers = new HttpHeaders();

        headers.set(
                "apiToken",
                user.getCallHippoApiToken()
        );

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.setAccept(
                java.util.List.of(MediaType.APPLICATION_JSON)
        );

        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(
                        requestBody,
                        headers
                );

        try {

            /*
             * IMPORTANT:
             *
             * Use the DEFAULT RestTemplate here.
             *
             * Do NOT use patchRestTemplate for /v1/call.
             */
            ResponseEntity<String> response =
                    restTemplate.exchange(
                            CALL_URL,
                            HttpMethod.POST,
                            request,
                            String.class
                    );

            System.out.println(
                    "CallHippo HTTP Status: "
                            + response.getStatusCode()
            );

            System.out.println(
                    "CallHippo Response: "
                            + response.getBody()
            );

            return processCallResponse(
                    user,
                    client,
                    response.getBody()
            );

        } catch (HttpStatusCodeException ex) {

            System.err.println(
                    "CallHippo HTTP Error: "
                            + ex.getStatusCode()
            );

            System.err.println(
                    "CallHippo Error Body: "
                            + ex.getResponseBodyAsString()
            );

            throw new RuntimeException(
                    "CallHippo API failed: "
                            + ex.getResponseBodyAsString()
            );

        } catch (Exception ex) {

            ex.printStackTrace();

            throw new RuntimeException(
                    "Unable to initiate CallHippo call: "
                            + ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * ============================================================
     * SET USER AVAILABLE
     * ============================================================
     *
     * PATCH:
     *
     * https://web.callhippo.com/v1/user/status
     *
     * Body:
     *
     * {
     *     "email": "employee@email.com",
     *     "status": "Available"
     * }
     *
     * Header:
     *
     * apiToken: TOKEN
     */
    public boolean setUserAvailable(User user) {

        return updateUserStatus(
                user,
                "Available"
        );
    }

    /**
     * ============================================================
     * SET USER UNAVAILABLE
     * ============================================================
     */
    public boolean setUserUnavailable(User user) {

        return updateUserStatus(
                user,
                "UnAvailable"
        );
    }

    /**
     * ============================================================
     * UPDATE CALLHIPPO USER STATUS
     * ============================================================
     */
    private boolean updateUserStatus(
            User user,
            String status
    ) {

        validateCallHippoConfiguration(user);

        Map<String, String> body = new HashMap<>();

        body.put("email", user.getEmail());
        body.put("status", status);

        HttpHeaders headers = new HttpHeaders();

        headers.set(
                "apiToken",
                user.getCallHippoApiToken()
        );

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.setAccept(
                java.util.List.of(MediaType.APPLICATION_JSON)
        );

        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(body, headers);

        try {

            ResponseEntity<String> response =
                    patchRestTemplate.exchange(
                            USER_STATUS_URL,
                            HttpMethod.PATCH,
                            request,
                            String.class
                    );

            String responseBody = response.getBody();

            System.out.println(
                    "CallHippo Status Response: "
                            + responseBody
            );

            if (responseBody == null ||
                    responseBody.isBlank()) {

                throw new RuntimeException(
                        "Empty response from CallHippo"
                );
            }

            JsonNode json =
                    objectMapper.readTree(responseBody);

            /*
             * Normal success
             */
            if (json.path("success").asBoolean(false)) {
                return true;
            }

            /*
             * CallHippo returns this when the employee
             * is ALREADY in the requested status.
             */
            String nestedError =
                    json.path("error")
                            .path("error")
                            .asText("");

            if (nestedError.contains(
                    "Your current status is the same"
            )) {

                System.out.println(
                        "Employee is already "
                                + status
                                + ". Continuing..."
                );

                return true;
            }

            /*
             * Any other error is a real error.
             */
            String message =
                    json.path("message")
                            .asText("");

            if (message.isBlank()) {
                message = nestedError;
            }

            if (message.isBlank()) {
                message = responseBody;
            }

            throw new RuntimeException(
                    "CallHippo status API failed: "
                            + message
            );

        } catch (HttpStatusCodeException ex) {

            String errorBody =
                    ex.getResponseBodyAsString();

            /*
             * Also handle same-status if CallHippo
             * returns it as an HTTP error.
             */
            if (errorBody != null &&
                    errorBody.contains(
                            "Your current status is the same"
                    )) {

                System.out.println(
                        "Employee is already "
                                + status
                                + ". Continuing..."
                );

                return true;
            }

            throw new RuntimeException(
                    "CallHippo status API failed: "
                            + errorBody
            );

        } catch (Exception ex) {

            throw new RuntimeException(
                    "Unable to update CallHippo status: "
                            + ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * ============================================================
     * PROCESS CALL RESPONSE
     * ============================================================
     */
    private CallHistory processCallResponse(
            User user,
            Client client,
            String responseBody
    ) {

        try {

            JsonNode json =
                    objectMapper.readTree(
                            responseBody
                    );

            /*
             * CallHippo can return:
             *
             * {
             *     "success": true,
             *     ...
             * }
             *
             * or:
             *
             * {
             *     "success": false,
             *     "error": ...
             * }
             */

            boolean success =
                    json.path("success")
                            .asBoolean(false);

            if (!success) {

                String errorMessage =
                        extractErrorMessage(json);

                throw new RuntimeException(
                        "CallHippo API failed: "
                                + errorMessage
                );
            }

            /*
             * Get call SID.
             */
            String callSid =
                    getText(
                            json,
                            "callSid"
                    );

            if (callSid == null) {

                callSid =
                        getText(
                                json,
                                "sid"
                        );
            }

            /*
             * Get status.
             */
            String status =
                    getText(
                            json,
                            "status"
                    );

            if (status == null) {
                status = "INITIATED";
            }

            /*
             * Create CallHistory.
             */
            CallHistory callHistory =
                    new CallHistory();

            callHistory.setClient(client);
            callHistory.setUser(user);

            callHistory.setCallSid(
                    callSid
            );

            callHistory.setFromNumber(
                    user.getCallHippoFromNumber()
            );

            callHistory.setToNumber(
                    normalizePhone(client.getPhone())
            );

            callHistory.setAgentId(
                    user.getCallHippoAgentId()
            );

            callHistory.setCallType(
                    "OUTGOING"
            );

            callHistory.setStatus(
                    status
            );

            callHistory.setCallTime(
                    LocalDateTime.now()
            );

            /*
             * Recording may not be available
             * immediately.
             *
             * Webhook will update it later.
             */
            String recordingUrl =
                    getText(
                            json,
                            "recordingUrl"
                    );

            if (recordingUrl != null) {

                callHistory.setRecordingUrl(
                        recordingUrl
                );
            }

            return callHistoryRepository.save(
                    callHistory
            );

        } catch (RuntimeException ex) {

            throw ex;

        } catch (Exception ex) {

            throw new RuntimeException(
                    "Unable to process CallHippo response: "
                            + ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * ============================================================
     * ERROR MESSAGE EXTRACTION
     * ============================================================
     */
    private String extractErrorMessage(
            JsonNode json
    ) {

        /*
         * Example:
         *
         * {
         *   "success": false,
         *   "error": "API token not found."
         * }
         */
        if (json.has("error")) {

            JsonNode error =
                    json.get("error");

            if (error.isTextual()) {
                return error.asText();
            }

            if (error.has("message")) {
                return error
                        .path("message")
                        .asText();
            }

            return error.toString();
        }

        /*
         * Example:
         *
         * {
         *   "success": false,
         *   "message": "..."
         * }
         */
        if (json.has("message")) {

            return json
                    .path("message")
                    .asText();
        }

        return "Unknown CallHippo error";
    }

    /**
     * ============================================================
     * GET JSON STRING
     * ============================================================
     */
    private String getText(
            JsonNode json,
            String field
    ) {

        JsonNode node =
                json.get(field);

        if (node == null ||
                node.isNull()) {

            return null;
        }

        String value =
                node.asText();

        if (value == null ||
                value.isBlank()) {

            return null;
        }

        return value;
    }

    /**
     * ============================================================
     * VALIDATE CALLHIPPO CONFIG
     * ============================================================
     */
    private void validateCallHippoConfiguration(
            User user
    ) {

        if (user == null) {
            throw new RuntimeException(
                    "User not found"
            );
        }

        if (user.getEmail() == null ||
                user.getEmail().isBlank()) {

            throw new RuntimeException(
                    "Employee email is missing"
            );
        }

        if (user.getCallHippoApiToken() == null ||
                user.getCallHippoApiToken().isBlank()) {

            throw new RuntimeException(
                    "CallHippo API token is not configured for this employee"
            );
        }

        if (user.getCallHippoFromNumber() == null ||
                user.getCallHippoFromNumber().isBlank()) {

            throw new RuntimeException(
                    "CallHippo from number is not configured for this employee"
            );
        }

        if (user.getCallHippoAgentId() == null ||
                user.getCallHippoAgentId().isBlank()) {

            throw new RuntimeException(
                    "CallHippo agent ID is not configured for this employee"
            );
        }
    }

    /**
     * ============================================================
     * PHONE NORMALIZATION
     * ============================================================
     */
    public static String normalizePhone(
            String phone
    ) {

        if (phone == null) {
            return null;
        }

        phone = phone
                .trim()
                .replaceAll(
                        "[^0-9+]",
                        ""
                );

        if (phone.isBlank()) {
            return null;
        }

        /*
         * Already international.
         */
        if (phone.startsWith("+")) {
            return phone;
        }

        /*
         * US 10 digit number.
         */
        if (phone.length() == 10) {
            return "+1" + phone;
        }

        /*
         * US 11 digit number beginning with 1.
         */
        if (phone.startsWith("1")
                && phone.length() == 11) {

            return "+" + phone;
        }

        /*
         * Otherwise assume number already
         * contains country code.
         */
        return "+" + phone;
    }
}