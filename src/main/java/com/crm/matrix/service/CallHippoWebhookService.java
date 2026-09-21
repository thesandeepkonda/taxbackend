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
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CallHippoWebhookService {

    private final CallHistoryRepository callHistoryRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;


    // =========================================================
    // PROCESS CALLHIPPO WEBHOOK
    // =========================================================

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

        System.out.println(
                "Activity Type      : "
                        + request.getActivityType()
        );

        System.out.println(
                "Call SID           : "
                        + request.getCallSid()
        );

        System.out.println(
                "From Number        : "
                        + request.getFromNumber()
        );

        System.out.println(
                "To Number          : "
                        + request.getToNumber()
        );

        System.out.println(
                "Call Type          : "
                        + request.getCallType()
        );

        System.out.println(
                "Status             : "
                        + request.getStatus()
        );

        System.out.println(
                "Email              : "
                        + request.getEmail()
        );

        System.out.println(
                "Caller Name        : "
                        + request.getCallerName()
        );

        System.out.println(
                "Duration           : "
                        + request.getDuration()
        );

//        System.out.println(
//                "Duration Seconds   : "
//                        + request.getD
//        );

        System.out.println(
                "Recording URL      : "
                        + request.getRecordingUrl()
        );

        System.out.println(
                "Reason             : "
                        + request.getReason()
        );

        System.out.println(
                "======================================"
        );




        Client client = findClient(request);

        if (client != null) {

            System.out.println(
                    "Client found:"
                            + " ID="
                            + client.getId()
                            + ", Name="
                            + client.getName()
            );

        } else {

            System.err.println(
                    "Client NOT found."
                            + " FromNumber="
                            + request.getFromNumber()
                            + ", ToNumber="
                            + request.getToNumber()
            );
        }




        User user = findUser(request);

        if (user != null) {

            System.out.println(
                    "User found:"
                            + " ID="
                            + user.getId()
                            + ", Employee Code="
                            + user.getEmployeeCode()
            );

        } else {

            System.err.println(
                    "User NOT found."
                            + " Email="
                            + request.getEmail()
            );
        }




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
                        "Existing CallHistory found"
                                + " using callSid."
                                + " ID="
                                + callHistory.getId()
                );
            }
        }




        if (callHistory == null
                && client != null
                && user != null
                && hasText(request.getToNumber())) {

            String normalizedToNumber =
                    PhoneNumberUtils.normalize(
                            request.getToNumber()
                    );

            callHistory =
                    callHistoryRepository
                            .findTopByClientIdAndUserIdAndToNumberOrderByCallTimeDesc(
                                    client.getId(),
                                    user.getId(),
                                    normalizedToNumber
                            )
                            .orElse(null);

            if (callHistory != null) {

                System.out.println(
                        "Existing CallHistory found"
                                + " using client + user + phone."
                                + " ID="
                                + callHistory.getId()
                );
            }
        }


        if (callHistory == null) {

            System.out.println(
                    "No existing CallHistory found."
            );



            if (client == null) {

                throw new IllegalStateException(
                        "Cannot create CallHistory: "
                                + "client not found for phone "
                                + request.getToNumber()
                );
            }

            if (user == null) {

                throw new IllegalStateException(
                        "Cannot create CallHistory: "
                                + "employee not found for email "
                                + request.getEmail()
                );
            }

            callHistory =
                    new CallHistory();

            callHistory.setClient(
                    client
            );

            callHistory.setUser(
                    user
            );

            System.out.println(
                    "Creating NEW CallHistory"
            );

        } else {

            System.out.println(
                    "Updating EXISTING CallHistory"
            );



            if (callHistory.getClient() == null
                    && client != null) {

                callHistory.setClient(
                        client
                );
            }

            /*
             * Fill missing user if required.
             */

            if (callHistory.getUser() == null
                    && user != null) {

                callHistory.setUser(
                        user
                );
            }
        }



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


        if (hasText(request.getCallSid())) {

            callHistory.setCallSid(
                    request.getCallSid()
            );
        }




        if (hasText(request.getFromNumber())) {

            callHistory.setFromNumber(
                    request.getFromNumber()
            );
        }


        // =====================================================
        // 9. TO NUMBER
        // =====================================================

        if (hasText(request.getToNumber())) {

            callHistory.setToNumber(
                    request.getToNumber()
            );
        }


        if (hasText(request.getCallType())) {

            callHistory.setCallType(
                    request.getCallType()
            );
        }



        if (hasText(request.getStatus())) {

            callHistory.setStatus(
                    request.getStatus()
            );
        }



        if (hasText(request.getDuration())) {

            callHistory.setDuration(
                    request.getDuration()
            );
        }




//        Integer durationSeconds =
//                request.getDurationSeconds();

//        if (durationSeconds == null
//                && hasText(request.getDuration())) {
//
//            durationSeconds =
//                    parseDurationToSeconds(
//                            request.getDuration()
//                    );
//        }
//
//        if (durationSeconds != null) {
//
//            callHistory.setDurationSeconds(
//                    durationSeconds
//            );
//        }

        if (hasText(request.getRecordingUrl())) {

            String recordingUrl =
                    request.getRecordingUrl();


            if (recordingUrl.contains("media.callhippo.com")
                    || recordingUrl.toLowerCase().endsWith(".mp3")) {

                callHistory.setRecordingUrl(
                        recordingUrl
                );

                System.out.println(
                        "Saving direct MP3 recording URL: "
                                + recordingUrl
                );

            } else {

                System.out.println(
                        "Ignoring non-direct recording URL: "
                                + recordingUrl
                );
            }
        }


        if (hasText(request.getHangupBy())) {

            callHistory.setHangupBy(
                    request.getHangupBy()
            );
        }



        if (hasText(request.getAnsweredDevice())) {

            callHistory.setAnsweredDevice(
                    request.getAnsweredDevice()
            );
        }



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



        if (hasText(request.getCountryName())) {

            callHistory.setCountryName(
                    request.getCountryName()
            );
        }




        LocalDateTime callTime =
                parseDate(
                        request.getTime()
                );

        if (callTime != null) {

            callHistory.setCallTime(
                    callTime
            );
        }



        LocalDateTime startTime =
                parseDate(
                        request.getStartTime()
                );

        if (startTime != null) {

            callHistory.setStartTime(
                    startTime
            );
        }


        LocalDateTime endTime =
                parseDate(
                        request.getEndTime()
                );

        if (endTime != null) {

            callHistory.setEndTime(
                    endTime
            );
        }



        if (hasText(request.getReason())) {



            // callHistory.setReason(request.getReason());
        }




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
                "ID       : "
                        + saved.getId()
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
                "Duration : "
                        + saved.getDuration()
        );

        System.out.println(
                "Recording: "
                        + saved.getRecordingUrl()
        );

        System.out.println(
                "======================================"
        );
    }



    private Client findClient(
            CallHippoWebhookRequest request
    ) {


        String phone =
                request.getToNumber();

        if (hasText(phone)) {

            String normalizedPhone =
                    PhoneNumberUtils.normalize(
                            phone
                    );

            System.out.println(
                    "Searching client using TO number: "
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


        phone =
                request.getFromNumber();

        if (hasText(phone)) {

            String normalizedPhone =
                    PhoneNumberUtils.normalize(
                            phone
                    );

            System.out.println(
                    "Searching client using FROM number: "
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




    private User findUser(
            CallHippoWebhookRequest request
    ) {

        /*
         * CallHippo webhook provides:
         *
         * email
         *
         * Example:
         *
         * "email": "test@example.com"
         *
         * Use this to find the CRM employee.
         */

        if (hasText(request.getEmail())) {

            System.out.println(
                    "Searching employee using email: "
                            + request.getEmail()
            );

            Optional<User> user =
                    userRepository.findByEmail(
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

    private boolean hasText(
            String value
    ) {

        return value != null
                && !value.isBlank();
    }


    // =========================================================
    // PARSE CALLHIPPO DATE
    // =========================================================

    private LocalDateTime parseDate(
            String value
    ) {

        if (!hasText(value)) {

            return null;
        }


        /*
         * CallHippo example:
         *
         * 2020-05-12T00:53:57.688+0000
         */

        try {

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
                    );

            return OffsetDateTime
                    .parse(
                            value,
                            formatter
                    )
                    .toLocalDateTime();

        } catch (Exception ignored) {

            /*
             * Try standard ISO format.
             */

            try {

                return OffsetDateTime
                        .parse(value)
                        .toLocalDateTime();

            } catch (Exception ignored2) {

                /*
                 * Try LocalDateTime.
                 */

                try {

                    return LocalDateTime.parse(
                            value
                    );

                } catch (Exception ignored3) {

                    System.err.println(
                            "Unable to parse CallHippo date: "
                                    + value
                    );

                    return null;
                }
            }
        }
    }


    // =========================================================
    // PARSE DURATION
    // =========================================================

    private Integer parseDurationToSeconds(
            String duration
    ) {

        if (!hasText(duration)) {

            return null;
        }

        try {

            String[] parts =
                    duration.trim().split(":");

            /*
             * HH:mm:ss
             */

            if (parts.length == 3) {

                int hours =
                        Integer.parseInt(
                                parts[0]
                        );

                int minutes =
                        Integer.parseInt(
                                parts[1]
                        );

                int seconds =
                        Integer.parseInt(
                                parts[2]
                        );

                return
                        (hours * 3600)
                                + (minutes * 60)
                                + seconds;
            }


            /*
             * mm:ss
             */

            if (parts.length == 2) {

                int minutes =
                        Integer.parseInt(
                                parts[0]
                        );

                int seconds =
                        Integer.parseInt(
                                parts[1]
                        );

                return
                        (minutes * 60)
                                + seconds;
            }


            /*
             * Seconds only.
             */

            if (parts.length == 1) {

                return Integer.parseInt(
                        parts[0]
                );
            }

        } catch (Exception ex) {

            System.err.println(
                    "Unable to parse duration: "
                            + duration
            );
        }

        return null;
    }
}