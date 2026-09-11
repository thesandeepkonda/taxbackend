package com.crm.matrix.service;

import com.crm.matrix.config.PhoneNumberUtils;
import com.crm.matrix.dto.CallHippoWebhookRequest;
import com.crm.matrix.entity.CallHistory;
import com.crm.matrix.entity.Client;
import com.crm.matrix.entity.User;
import com.crm.matrix.repository.CallHistoryRepository;
import com.crm.matrix.repository.ClientRepository;
import com.crm.matrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CallHippoWebhookService {

    private final CallHistoryRepository callHistoryRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;


    @Transactional
    public void process(CallHippoWebhookRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "CallHippo webhook request is null"
            );
        }

        System.out.println("======================================");
        System.out.println("CALLHIPPO WEBHOOK");
        System.out.println("======================================");

        System.out.println("Type       : " + request.getType());
        System.out.println("Call SID   : " + request.getCallSid());
        System.out.println("From       : " + request.getFrom());
        System.out.println("To         : " + request.getTo());
        System.out.println("Status     : " + request.getStatus());
        System.out.println("Call Type  : " + request.getCallType());
        System.out.println("Email      : " + request.getEmail());
        //System.out.println("Agent ID   : " + request.getAgentId());




        String agentId = null;

        if (request.getExtraParams() != null) {

            Object value =
                    request.getExtraParams().get("agentId");

            if (value != null) {
                agentId = value.toString();
            }
        }

        if (!hasText(agentId)
                && request.getExtraParams() != null) {

            Object value =
                    request.getExtraParams().get("agentId");

            if (value != null) {
                agentId = value.toString();
            }
        }

        System.out.println("Resolved Agent ID: " + agentId);


        // =====================================================
        // 2. FIND CLIENT
        // =====================================================

        Client client = findClient(request);

        if (client != null) {

            System.out.println(
                    "Client found: ID="
                            + client.getId()
                            + ", Name="
                            + client.getName()
            );

        } else {

            System.err.println(
                    "Client NOT found. From="
                            + request.getFrom()
                            + ", To="
                            + request.getTo()
            );
        }


        // =====================================================
        // 3. FIND USER
        // =====================================================

        User user = findUser(request, agentId);

        if (user != null) {

            System.out.println(
                    "User found: ID="
                            + user.getId()
                            + ", Employee Code="
                            + user.getEmployeeCode()
            );

        } else {

            System.err.println(
                    "User NOT found. Agent ID="
                            + agentId
            );
        }


        // =====================================================
        // 4. FIND EXISTING CALL HISTORY BY CALL SID
        // =====================================================

        CallHistory callHistory = null;

        if (hasText(request.getCallSid())) {

            callHistory =
                    callHistoryRepository
                            .findByCallSid(
                                    request.getCallSid()
                            )
                            .orElse(null);

            if (callHistory != null) {

                System.out.println(
                        "Existing CallHistory found by callSid. ID="
                                + callHistory.getId()
                );
            }
        }


        // =====================================================
        // 5. FALLBACK:
        // FIND EXISTING CALL WITHOUT CALL SID
        // =====================================================

        if (callHistory == null
                && client != null
                && user != null) {

            callHistory =
                    callHistoryRepository
                            .findTopByClientIdAndUserIdAndToNumberOrderByCallTimeDesc(
                                    client.getId(),
                                    user.getId(),
                                    request.getTo()
                            )
                            .orElse(null);

            if (callHistory != null) {

                System.out.println(
                        "Existing CallHistory found using "
                                + "client + user + phone. ID="
                                + callHistory.getId()
                );
            }
        }


        // =====================================================
        // 6. IF NO EXISTING RECORD -> CREATE
        // =====================================================

        if (callHistory == null) {

            System.out.println(
                    "No existing CallHistory found."
            );

            /*
             * Because client_id and user_id are NOT NULL
             * in your database, we MUST have both.
             */

            if (client == null) {

                throw new IllegalStateException(
                        "Cannot create CallHistory: "
                                + "client not found for phone "
                                + request.getTo()
                );
            }

            if (user == null) {

                throw new IllegalStateException(
                        "Cannot create CallHistory: "
                                + "employee not found for agentId "
                                + agentId
                );
            }

            callHistory = new CallHistory();

            callHistory.setClient(client);
            callHistory.setUser(user);

            System.out.println(
                    "Creating new CallHistory"
            );

        } else {

            System.out.println(
                    "Updating existing CallHistory"
            );

            /*
             * Existing client
             */

            if (callHistory.getClient() == null
                    && client != null) {

                callHistory.setClient(client);
            }

            /*
             * Existing user
             */

            if (callHistory.getUser() == null
                    && user != null) {

                callHistory.setUser(user);
            }
        }


        // =====================================================
        // 7. FINAL SAFETY CHECK
        // =====================================================

        if (callHistory.getClient() == null) {

            throw new IllegalStateException(
                    "CallHistory client cannot be null"
            );
        }

        if (callHistory.getUser() == null) {

            throw new IllegalStateException(
                    "CallHistory user cannot be null"
            );
        }


        // =====================================================
        // 8. AGENT ID
        // =====================================================

        if (hasText(agentId)) {

            callHistory.setAgentId(agentId);
        }


        // =====================================================
        // 9. CALL SID
        // =====================================================

        if (hasText(request.getCallSid())) {

            callHistory.setCallSid(
                    request.getCallSid()
            );
        }


        // =====================================================
        // 10. PHONE NUMBERS
        // =====================================================

        if (hasText(request.getFrom())) {

            callHistory.setFromNumber(
                    request.getFrom()
            );
        }

        if (hasText(request.getTo())) {

            callHistory.setToNumber(
                    request.getTo()
            );
        }


        // =====================================================
        // 11. CALL TYPE
        // =====================================================

        if (hasText(request.getCallType())) {

            callHistory.setCallType(
                    request.getCallType()
            );
        }


        // =====================================================
        // 12. STATUS
        // =====================================================

        if (hasText(request.getStatus())) {

            callHistory.setStatus(
                    request.getStatus()
            );
        }


        // =====================================================
        // 13. DURATION
        // =====================================================

        if (hasText(request.getDuration())) {

            callHistory.setDuration(
                    request.getDuration()
            );
        }

        if (request.getDurationSeconds() != null) {

            callHistory.setDurationSeconds(
                    request.getDurationSeconds()
            );
        }


        // =====================================================
        // 14. RECORDING
        // =====================================================

        if (hasText(request.getRecordingUrl())) {

            callHistory.setRecordingUrl(
                    request.getRecordingUrl()
            );
        }


        // =====================================================
        // 15. HANGUP
        // =====================================================

        if (hasText(request.getHangupBy())) {

            callHistory.setHangupBy(
                    request.getHangupBy()
            );
        }


        // =====================================================
        // 16. ANSWERED DEVICE
        // =====================================================

        if (hasText(request.getAnsweredDevice())) {

            callHistory.setAnsweredDevice(
                    request.getAnsweredDevice()
            );
        }


        // =====================================================
        // 17. BILLING
        // =====================================================

        if (request.getBilledMinutes() != null) {

            callHistory.setBilledMinutes(
                    request.getBilledMinutes()
            );
        }

        if (hasText(request.getCallCharge())) {

            callHistory.setCallCharge(
                    request.getCallCharge()
            );
        }


        // =====================================================
        // 18. COUNTRY
        // =====================================================

        if (hasText(request.getCountryName())) {

            callHistory.setCountryName(
                    request.getCountryName()
            );
        }


        // =====================================================
        // 19. DATES
        // =====================================================

        LocalDateTime callTime =
                parseDate(request.getTime());

        if (callTime != null) {

            callHistory.setCallTime(
                    callTime
            );
        }


        LocalDateTime startTime =
                parseDate(request.getStartTime());

        if (startTime != null) {

            callHistory.setStartTime(
                    startTime
            );
        }


        LocalDateTime endTime =
                parseDate(request.getEndTime());

        if (endTime != null) {

            callHistory.setEndTime(
                    endTime
            );
        }


        // =====================================================
        // 20. SAVE
        // =====================================================

        CallHistory saved =
                callHistoryRepository.save(
                        callHistory
                );

        System.out.println(
                "======================================"
        );

        System.out.println(
                "CallHistory saved successfully"
        );

        System.out.println(
                "ID       : " + saved.getId()
        );

        System.out.println(
                "Client ID: "
                        + saved.getClient().getId()
        );

        System.out.println(
                "User ID  : "
                        + saved.getUser().getId()
        );

        System.out.println(
                "Call SID : "
                        + saved.getCallSid()
        );

        System.out.println(
                "Status   : "
                        + saved.getStatus()
        );

        System.out.println(
                "======================================"
        );
    }


    // =========================================================
    // FIND CLIENT
    // =========================================================

    private Client findClient(
            CallHippoWebhookRequest request
    ) {

        /*
         * For OUTGOING calls:
         *
         * to = client phone
         *
         * So check TO first.
         */

        String phone = request.getTo();

        if (hasText(phone)) {

            String normalizedPhone =
                    PhoneNumberUtils.normalize(phone);

            System.out.println(
                    "Searching client using TO phone: "
                            + normalizedPhone
            );

            Optional<Client> client =
                    clientRepository.findByPhone(
                            normalizedPhone
                    );

            if (client.isPresent()) {
                return client.get();
            }
        }


        /*
         * Fallback to FROM
         */

        phone = request.getFrom();

        if (hasText(phone)) {

            String normalizedPhone =
                    PhoneNumberUtils.normalize(phone);

            System.out.println(
                    "Searching client using FROM phone: "
                            + normalizedPhone
            );

            Optional<Client> client =
                    clientRepository.findByPhone(
                            normalizedPhone
                    );

            if (client.isPresent()) {
                return client.get();
            }
        }

        return null;
    }


    // =========================================================
    // FIND USER
    // =========================================================

    private User findUser(
            CallHippoWebhookRequest request,
            String agentId
    ) {

        /*
         * First try agentId
         */

        if (hasText(agentId)) {

            Optional<User> user =
                    userRepository
                            .findByCallHippoAgentId(
                                    agentId
                            );

            if (user.isPresent()) {
                return user.get();
            }
        }


        /*
         * Fallback to employee email
         */

        if (hasText(request.getEmail())) {

            Optional<User> user =
                    userRepository
                            .findByEmail(
                                    request.getEmail()
                            );

            if (user.isPresent()) {
                return user.get();
            }
        }

        return null;
    }


    // =========================================================
    // CHECK TEXT
    // =========================================================

    private boolean hasText(String value) {

        return value != null
                && !value.isBlank();
    }


    // =========================================================
    // PARSE DATE
    // =========================================================

    private LocalDateTime parseDate(
            String value
    ) {

        if (!hasText(value)) {
            return null;
        }

        try {

            return OffsetDateTime
                    .parse(value)
                    .toLocalDateTime();

        } catch (Exception ignored) {

            try {

                return LocalDateTime.parse(value);

            } catch (Exception ignored2) {

                return null;
            }
        }
    }
}