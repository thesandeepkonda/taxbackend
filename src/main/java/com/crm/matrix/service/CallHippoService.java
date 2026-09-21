package com.crm.matrix.service;

import com.crm.matrix.config.PhoneNumberUtils;
import com.crm.matrix.dto.CallHippoActivityFeedRequest;
import com.crm.matrix.dto.CallHippoEmbeddedConfig;
import com.crm.matrix.entity.CallHistory;
import com.crm.matrix.entity.Client;
import com.crm.matrix.entity.User;
import com.crm.matrix.repository.CallHistoryRepository;
import com.crm.matrix.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
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
    private final UserRepository userRepository;

    public CallHippoService(
            @Qualifier("defaultRestTemplate")
            RestTemplate restTemplate,

            @Qualifier("patchRestTemplate")
            RestTemplate patchRestTemplate,

            CallHistoryRepository callHistoryRepository,

            ObjectMapper objectMapper, UserRepository userRepository
    ) {
        this.restTemplate = restTemplate;
        this.patchRestTemplate = patchRestTemplate;
        this.callHistoryRepository = callHistoryRepository;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;

    }


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

    @Transactional(readOnly = true)
    public CallHippoEmbeddedConfig getEmbeddedConfig(
            Authentication authentication
    ) {

        String employeeCode = authentication.getName();

        User user = userRepository
                .findByEmployeeCode(employeeCode)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Logged-in employee not found: "
                                        + employeeCode
                        )
                );

        if (user.getCallHippoApiToken() == null
                || user.getCallHippoApiToken().isBlank()) {

            throw new RuntimeException(
                    "CallHippo API token is not configured for this employee"
            );
        }

        return new CallHippoEmbeddedConfig(
                user.getCallHippoApiToken(),
                user.getEmail(),
                user.getCallHippoAgentId()
        );
    }

    public Object createReminder(
            User user,
            Client client,
            Integer reminderTime
    ) {

        // =====================================================
        // VALIDATION
        // =====================================================

        if (user == null) {
            throw new IllegalArgumentException(
                    "User cannot be null"
            );
        }

        if (client == null) {
            throw new IllegalArgumentException(
                    "Client cannot be null"
            );
        }

        if (reminderTime == null || reminderTime < 1) {
            throw new IllegalArgumentException(
                    "Reminder time must be greater than 0"
            );
        }

        if (user.getEmail() == null
                || user.getEmail().isBlank()) {

            throw new IllegalStateException(
                    "Logged-in employee email is not configured"
            );
        }

        if (user.getCallHippoApiToken() == null
                || user.getCallHippoApiToken().isBlank()) {

            throw new IllegalStateException(
                    "CallHippo API token is not configured for this employee"
            );
        }

        if (client.getPhone() == null
                || client.getPhone().isBlank()) {

            throw new IllegalStateException(
                    "Client phone number is not configured"
            );
        }


        // =====================================================
        // NORMALIZE CLIENT PHONE
        // =====================================================

        String phoneNumber =
                PhoneNumberUtils.normalize(
                        client.getPhone()
                );

        if (phoneNumber == null
                || phoneNumber.isBlank()) {

            throw new IllegalStateException(
                    "Invalid client phone number"
            );
        }


        // =====================================================
        // LOG
        // =====================================================

        System.out.println("==========================================");
        System.out.println("CALLHIPPO CREATE REMINDER");
        System.out.println("==========================================");

        System.out.println(
                "Employee Code : "
                        + user.getEmployeeCode()
        );

        System.out.println(
                "Employee Email: "
                        + user.getEmail()
        );

        System.out.println(
                "Client Name   : "
                        + client.getName()
        );

        System.out.println(
                "Phone Number  : "
                        + phoneNumber
        );

        System.out.println(
                "Reminder Time : "
                        + reminderTime
                        + " minutes"
        );

        /*
         * DO NOT PRINT THE ACTUAL API TOKEN.
         */
        System.out.println(
                "API Token     : configured"
        );


        // =====================================================
        // REQUEST BODY
        // =====================================================

        Map<String, Object> requestBody =
                new HashMap<>();

        requestBody.put(
                "contactName",
                client.getName()
        );

        requestBody.put(
                "phoneNumber",
                phoneNumber
        );

        requestBody.put(
                "agentEmail",
                user.getEmail()
        );

        requestBody.put(
                "reminderTime",
                reminderTime
        );


        // =====================================================
        // HEADERS
        // =====================================================

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.setAccept(
                java.util.List.of(
                        MediaType.APPLICATION_JSON
                )
        );

        /*
         * IMPORTANT:
         *
         * CallHippo expects:
         *
         * apiToken: YOUR_TOKEN
         *
         * NOT:
         *
         * Authorization: Bearer YOUR_TOKEN
         */
        headers.set(
                "apiToken",
                user.getCallHippoApiToken()
        );


        // =====================================================
        // HTTP REQUEST
        // =====================================================

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(
                        requestBody,
                        headers
                );


        // =====================================================
        // CALL CALLHIPPO
        // =====================================================

        String reminderUrl =
                "https://web.callhippo.com/v1/callReminder/add";

        try {

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            reminderUrl,
                            HttpMethod.POST,
                            entity,
                            String.class
                    );


            // =================================================
            // RESPONSE LOG
            // =================================================

            System.out.println(
                    "CallHippo Reminder HTTP Status: "
                            + response.getStatusCode()
            );

            System.out.println(
                    "CallHippo Reminder Response: "
                            + response.getBody()
            );

            System.out.println(
                    "=========================================="
            );


            // =================================================
            // RETURN CALLHIPPO RESPONSE
            // =================================================

            return response.getBody();

        } catch (HttpStatusCodeException ex) {

            System.err.println(
                    "=========================================="
            );

            System.err.println(
                    "CALLHIPPO REMINDER API ERROR"
            );

            System.err.println(
                    "HTTP Status: "
                            + ex.getStatusCode()
            );

            System.err.println(
                    "Response Body: "
                            + ex.getResponseBodyAsString()
            );

            System.err.println(
                    "=========================================="
            );

            throw new RuntimeException(
                    "CallHippo reminder API failed: "
                            + ex.getResponseBodyAsString(),
                    ex
            );

        } catch (Exception ex) {

            System.err.println(
                    "Unable to create CallHippo reminder: "
                            + ex.getMessage()
            );

            throw new RuntimeException(
                    "Unable to create CallHippo reminder: "
                            + ex.getMessage(),
                    ex
            );
        }
    }

    public ResponseEntity<byte[]> getRecording(
            User user,
            Long callHistoryId
    ) {

        validateCallHippoConfiguration(user);

        CallHistory callHistory =
                callHistoryRepository
                        .findById(callHistoryId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Call history not found: "
                                                + callHistoryId
                                )
                        );

        /*
         * Security:
         * Make sure the logged-in employee owns this call.
         */
        if (callHistory.getUser() == null
                || !callHistory.getUser()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You are not authorized to access this recording"
            );
        }

        String recordingUrl =
                callHistory.getRecordingUrl();

        if (recordingUrl == null
                || recordingUrl.isBlank()) {

            throw new RuntimeException(
                    "Recording is not available for this call"
            );
        }

        System.out.println(
                "======================================"
        );

        System.out.println(
                "CALLHIPPO RECORDING"
        );

        System.out.println(
                "Call History ID: "
                        + callHistoryId
        );

        System.out.println(
                "Call SID: "
                        + callHistory.getCallSid()
        );

        System.out.println(
                "Recording URL: "
                        + recordingUrl
        );

        System.out.println(
                "======================================"
        );

        HttpHeaders headers =
                new HttpHeaders();

        headers.set(
                "apiToken",
                user.getCallHippoApiToken()
        );

        headers.setAccept(
                java.util.List.of(
                        MediaType.ALL
                )
        );

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        try {

            ResponseEntity<byte[]> response =
                    restTemplate.exchange(
                            recordingUrl,
                            HttpMethod.GET,
                            request,
                            byte[].class
                    );

            byte[] body =
                    response.getBody();

            if (body == null || body.length == 0) {

                throw new RuntimeException(
                        "CallHippo returned an empty recording"
                );
            }

            MediaType contentType =
                    response
                            .getHeaders()
                            .getContentType();

            /*
             * If CallHippo returned HTML instead of audio,
             * it usually means we received the login page.
             */
            if (contentType != null
                    && contentType
                    .toString()
                    .toLowerCase()
                    .contains("text/html")) {

                throw new RuntimeException(
                        "CallHippo returned a login/web page instead "
                                + "of the recording. The recording URL "
                                + "is not a direct media URL."
                );
            }

            HttpHeaders responseHeaders =
                    new HttpHeaders();

            if (contentType != null) {

                responseHeaders.setContentType(
                        contentType
                );

            } else {

                responseHeaders.setContentType(
                        MediaType.parseMediaType(
                                "audio/mpeg"
                        )
                );
            }

            responseHeaders.setContentLength(
                    body.length
            );

            /*
             * This is important.
             *
             * Browser can play the recording directly.
             */
            responseHeaders.set(
                    "Content-Disposition",
                    "inline; filename=\"call-"
                            + callHistory.getCallSid()
                            + ".mp3\""
            );

            responseHeaders.set(
                    "Accept-Ranges",
                    "bytes"
            );

            return new ResponseEntity<>(
                    body,
                    responseHeaders,
                    HttpStatus.OK
            );

        } catch (HttpStatusCodeException ex) {

            System.err.println(
                    "CallHippo recording HTTP error: "
                            + ex.getStatusCode()
            );

            System.err.println(
                    ex.getResponseBodyAsString()
            );

            throw new RuntimeException(
                    "Unable to retrieve CallHippo recording: "
                            + ex.getStatusCode(),
                    ex
            );
        }
    }

    public String getActivityFeed(
            User user,
            String callSid,
            String startDate,
            String endDate,
            String crmUniqueId,
            String skip,
            String limit
    ) {

        validateCallHippoApiToken(user);

        Map<String, Object> requestBody =
                new HashMap<>();

        requestBody.put(
                "skip",
                skip == null || skip.isBlank()
                        ? "0"
                        : skip
        );

        requestBody.put(
                "limit",
                limit == null || limit.isBlank()
                        ? "20"
                        : limit
        );

        requestBody.put(
                "startDate",
                startDate
        );

        requestBody.put(
                "endDate",
                endDate
        );

        requestBody.put(
                "crmUniqueId",
                crmUniqueId == null
                        ? ""
                        : crmUniqueId
        );

        requestBody.put(
                "callSid",
                callSid == null
                        ? ""
                        : callSid
        );

        HttpHeaders headers =
                new HttpHeaders();

        headers.set(
                "apiToken",
                user.getCallHippoApiToken()
        );

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.setAccept(
                java.util.List.of(
                        MediaType.APPLICATION_JSON
                )
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        requestBody,
                        headers
                );

        String url =
                "https://web.callhippo.com/v1/activityfeed";

        try {

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            request,
                            String.class
                    );

            System.out.println(
                    "CallHippo Activity Feed Response: "
                            + response.getBody()
            );

            return response.getBody();

        } catch (HttpStatusCodeException ex) {

            System.err.println(
                    "CallHippo Activity Feed Error: "
                            + ex.getStatusCode()
            );

            System.err.println(
                    ex.getResponseBodyAsString()
            );

            throw new RuntimeException(
                    "CallHippo activity feed failed: "
                            + ex.getResponseBodyAsString()
            );
        }
    }

    public void syncActivityFeed(
            User user,
            LocalDate startDate,
            LocalDate endDate,
            int skip,
            int limit
    ) {

        validateCallHippoConfiguration(user);

        String activityFeedUrl =
                "https://web.callhippo.com/v1/activityfeed";

        Map<String, String> requestBody =
                new HashMap<>();

        requestBody.put(
                "skip",
                String.valueOf(skip)
        );

        requestBody.put(
                "limit",
                String.valueOf(limit)
        );

        requestBody.put(
                "startDate",
                startDate.toString().replace("-", "/")
        );

        requestBody.put(
                "endDate",
                endDate.toString().replace("-", "/")
        );

        requestBody.put(
                "crmUniqueId",
                ""
        );

        /*
         * Do NOT send callSid here.
         *
         * This allows us to synchronize all calls
         * for the requested employee/date range.
         */

        HttpHeaders headers =
                new HttpHeaders();

        headers.set(
                "apiToken",
                user.getCallHippoApiToken()
        );

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.setAccept(
                java.util.List.of(
                        MediaType.APPLICATION_JSON
                )
        );

        HttpEntity<Map<String, String>> entity =
                new HttpEntity<>(
                        requestBody,
                        headers
                );

        try {

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            activityFeedUrl,
                            HttpMethod.POST,
                            entity,
                            String.class
                    );

            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "CALLHIPPO ACTIVITY FEED"
            );

            System.out.println(
                    "Employee : "
                            + user.getEmployeeCode()
            );

            System.out.println(
                    "Start    : "
                            + startDate
            );

            System.out.println(
                    "End      : "
                            + endDate
            );

            System.out.println(
                    "Response : "
                            + response.getBody()
            );

            System.out.println(
                    "=========================================="
            );

            syncActivityFeedResponse(
                    user,
                    response.getBody()
            );

        } catch (HttpStatusCodeException ex) {

            throw new RuntimeException(
                    "CallHippo Activity Feed failed: "
                            + ex.getResponseBodyAsString(),
                    ex
            );

        } catch (Exception ex) {

            throw new RuntimeException(
                    "Unable to sync CallHippo Activity Feed: "
                            + ex.getMessage(),
                    ex
            );
        }
    }

    private void syncActivityFeedResponse(
            User user,
            String responseBody
    ) {

        try {

            JsonNode root =
                    objectMapper.readTree(
                            responseBody
                    );

            boolean success =
                    root.path("success")
                            .asBoolean(false);

            if (!success) {

                throw new RuntimeException(
                        "CallHippo Activity Feed returned success=false: "
                                + responseBody
                );
            }

            JsonNode callLogs =
                    root.path("data")
                            .path("callLogs");

            /*
             * callLogs can be an object for a single result
             * or an array depending on the API response.
             */

            if (callLogs.isMissingNode()
                    || callLogs.isNull()) {

                System.out.println(
                        "No callLogs returned by CallHippo."
                );

                return;
            }

            if (callLogs.isArray()) {

                for (JsonNode callLog : callLogs) {

                    syncSingleActivityFeedCall(
                            user,
                            callLog
                    );
                }

            } else {

                syncSingleActivityFeedCall(
                        user,
                        callLogs
                );
            }

        } catch (Exception ex) {

            throw new RuntimeException(
                    "Unable to process CallHippo Activity Feed: "
                            + ex.getMessage(),
                    ex
            );
        }
    }

    private void syncSingleActivityFeedCall(
            User user,
            JsonNode callLog
    ) {

        String callSid =
                getText(
                        callLog,
                        "callSid"
                );

        if (callSid == null) {

            System.out.println(
                    "Skipping Activity Feed record: callSid missing"
            );

            return;
        }

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "SYNCING CALL"
        );

        System.out.println(
                "Call SID: "
                        + callSid
        );

        /*
         * Find existing CRM CallHistory.
         */
        CallHistory callHistory =
                callHistoryRepository
                        .findByCallSid(callSid)
                        .orElse(null);

        if (callHistory == null) {

            System.out.println(
                    "CallHistory not found for callSid: "
                            + callSid
            );

            /*
             * We don't create a new record here because
             * the webhook should normally create the
             * initial CallHistory.
             *
             * Activity Feed is being used to enrich it
             * with the real MP3 recording URL.
             */
            return;
        }

        /*
         * =====================================================
         * REAL MP3 RECORDING URL
         * =====================================================
         */

        String recordingUrl =
                getText(
                        callLog,
                        "recordingUrl"
                );

        if (recordingUrl != null) {

            callHistory.setRecordingUrl(
                    recordingUrl
            );

            System.out.println(
                    "MP3 Recording URL:"
            );

            System.out.println(
                    recordingUrl
            );
        }

        /*
         * =====================================================
         * UPDATE OTHER FIELDS
         * =====================================================
         */

        String status =
                getText(
                        callLog,
                        "callStatus"
                );

        if (status != null) {

            callHistory.setStatus(
                    status
            );
        }

        String callType =
                getText(
                        callLog,
                        "callType"
                );

        if (callType != null) {

            callHistory.setCallType(
                    callType
            );
        }

        String from =
                getText(
                        callLog,
                        "from"
                );

        if (from != null) {

            callHistory.setFromNumber(
                    from
            );
        }

        String to =
                getText(
                        callLog,
                        "to"
                );

        if (to != null) {

            callHistory.setToNumber(
                    to
            );
        }

        String duration =
                getText(
                        callLog,
                        "callDuration"
                );

        if (duration != null) {

            /*
             * Activity Feed:
             *
             * "00:00:37"
             *
             * Your CRM currently uses:
             *
             * "00:37"
             *
             * You can store it directly if your
             * database column is String.
             */
            callHistory.setDuration(
                    duration
            );
        }

        String hangupBy =
                getText(
                        callLog,
                        "hangupBy"
                );

        if (hangupBy != null) {

            callHistory.setHangupBy(
                    hangupBy
            );
        }

        JsonNode totalDuration =
                callLog.get(
                        "totalCallDuration"
                );

        if (totalDuration != null
                && totalDuration.isNumber()) {

            callHistory.setDurationSeconds(
                    totalDuration.asInt()
            );
        }

        JsonNode callCost =
                callLog.get(
                        "callCost"
                );

        if (callCost != null
                && callCost.isNumber()) {

            callHistory.setCallCharge(
                    callCost.asText()
            );
        }

        callHistoryRepository.save(
                callHistory
        );

        System.out.println(
                "CallHistory synchronized successfully."
        );

        System.out.println(
                "ID: "
                        + callHistory.getId()
        );

        System.out.println(
                "Call SID: "
                        + callHistory.getCallSid()
        );

        System.out.println(
                "Recording: "
                        + callHistory.getRecordingUrl()
        );

        System.out.println(
                "=========================================="
        );
    }

    private void validateCallHippoApiToken(User user) {

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (user.getCallHippoApiToken() == null
                || user.getCallHippoApiToken().isBlank()) {

            throw new RuntimeException(
                    "CallHippo API token is not configured for this employee"
            );
        }
    }
}