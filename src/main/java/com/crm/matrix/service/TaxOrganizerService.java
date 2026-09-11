package com.crm.matrix.service;

import com.crm.matrix.dto.DependentRequest;
import com.crm.matrix.dto.SpouseRequest;
import com.crm.matrix.dto.TaxOrganizerRequest;
import com.crm.matrix.dto.TaxOrganizerResponse;
import com.crm.matrix.entity.Client;
import com.crm.matrix.entity.ClientAssignment;
import com.crm.matrix.entity.TaxOrganizer;
import com.crm.matrix.entity.TaxOrganizerDependent;
import com.crm.matrix.entity.TaxOrganizerSpouse;
import com.crm.matrix.entity.User;
import com.crm.matrix.enums.TaxOrganizerStatus;
import com.crm.matrix.repository.ClientAssignmentRepository;
import com.crm.matrix.repository.ClientRepository;
import com.crm.matrix.repository.TaxOrganizerDependentRepository;
import com.crm.matrix.repository.TaxOrganizerRepository;
import com.crm.matrix.repository.TaxOrganizerSpouseRepository;
import com.crm.matrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxOrganizerService {

    private final TaxOrganizerRepository taxOrganizerRepository;
    private final TaxOrganizerSpouseRepository spouseRepository;
    private final TaxOrganizerDependentRepository dependentRepository;

    private final ClientRepository clientRepository;
    private final ClientAssignmentRepository clientAssignmentRepository;
    private final UserRepository userRepository;

    private final Path uploadDirectory =
            Paths.get("uploads/tax-organizers");

    private User getLoggedInUser(
            Authentication authentication) {

        if (authentication == null) {
            throw new RuntimeException(
                    "Authentication required"
            );
        }

        return userRepository
                .findByEmployeeCode(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Logged-in employee not found"
                        ));
    }



    private ClientAssignment getEmployeeAssignment(
            Long clientId,
            Authentication authentication) {

        User employee =
                getLoggedInUser(authentication);

        return clientAssignmentRepository
                .findByClientIdAndEmployeeAndActiveTrue(
                        clientId,
                        employee
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Client is not assigned to you"
                        ));
    }


    // =========================================================
    // GET TAX ORGANIZER
    // =========================================================

    @Transactional(readOnly = true)
    public TaxOrganizerResponse getOrganizer(
            Long clientId,
            Authentication authentication) {

        // Verify employee owns client
        getEmployeeAssignment(
                clientId,
                authentication
        );

        TaxOrganizer organizer =
                taxOrganizerRepository
                        .findByClientId(clientId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Tax Organizer not found"
                                ));

        return mapResponse(organizer);
    }



    @Transactional
    public TaxOrganizerResponse saveOrganizer(
            Long clientId,
            TaxOrganizerRequest request,
            Authentication authentication) {

        ClientAssignment assignment =
                getEmployeeAssignment(
                        clientId,
                        authentication
                );

        User employee =
                assignment.getEmployee();

        Client client =
                assignment.getClient();


        TaxOrganizer organizer =
                taxOrganizerRepository
                        .findByClientId(clientId)
                        .orElse(null);




        if (organizer == null) {

            organizer =
                    new TaxOrganizer();

            organizer.setClient(client);

            organizer.setEmployee(employee);

            organizer.setCreatedAt(
                    LocalDateTime.now()
            );

            organizer.setStatus(
                    TaxOrganizerStatus.DRAFT
            );
        }


        // =====================================================
        // UPDATE MAIN INFORMATION
        // =====================================================

        organizer.setFirstName(
                request.getFirstName()
        );

        organizer.setMiddleName(
                request.getMiddleName()
        );

        organizer.setLastName(
                request.getLastName()
        );

        organizer.setDateOfBirth(
                request.getDateOfBirth()
        );

        organizer.setSsnItin(
                request.getSsnItin()
        );

        organizer.setVisaStatus(
                request.getVisaStatus()
        );

        organizer.setVisaStatusChanged(
                request.getVisaStatusChanged()
        );

        organizer.setMaritalStatus(
                request.getMaritalStatus()
        );

        organizer.setCurrentAddress(
                request.getCurrentAddress()
        );

        organizer.setEmailAddress(
                request.getEmailAddress()
        );

        organizer.setPhoneNumber(
                request.getPhoneNumber()
        );

        organizer.setOccupation(
                request.getOccupation()
        );

        organizer.setStatesLivedIn2026(
                request.getStatesLivedIn2026()
        );

        organizer.setHasDependents(
                request.getHasDependents()
        );

        organizer.setUpdatedAt(
                LocalDateTime.now()
        );


        TaxOrganizer saved =
                taxOrganizerRepository.save(
                        organizer
                );


        // =====================================================
        // SPOUSE
        // =====================================================

        saveSpouse(
                saved,
                request.getSpouse()
        );


        // =====================================================
        // DEPENDENTS
        // =====================================================

        saveDependents(
                saved,
                request.getDependents()
        );


        return mapResponse(saved);
    }


    // =========================================================
    // SAVE SPOUSE
    // =========================================================

    private void saveSpouse(
            TaxOrganizer organizer,
            SpouseRequest request) {

        boolean spouseRequired =
                isSpouseRequired(
                        organizer.getMaritalStatus()
                );


        TaxOrganizerSpouse existing =
                spouseRepository
                        .findByTaxOrganizerId(
                                organizer.getId()
                        )
                        .orElse(null);


        // No spouse required
        if (!spouseRequired) {

            if (existing != null) {
                spouseRepository.delete(existing);
            }

            return;
        }


        if (request == null) {

            throw new RuntimeException(
                    "Spouse information is required for this marital status"
            );
        }


        TaxOrganizerSpouse spouse =
                existing != null
                        ? existing
                        : new TaxOrganizerSpouse();


        spouse.setTaxOrganizer(
                organizer
        );

        spouse.setFirstName(
                request.getFirstName()
        );

        spouse.setMiddleName(
                request.getMiddleName()
        );

        spouse.setLastName(
                request.getLastName()
        );

        spouse.setDateOfBirth(
                request.getDateOfBirth()
        );

        spouse.setSsnItin(
                request.getSsnItin()
        );

        spouse.setVisaStatus(
                request.getVisaStatus()
        );

        spouse.setVisaStatusChanged(
                request.getVisaStatusChanged()
        );

        spouse.setOccupation(
                request.getOccupation()
        );

        spouse.setEmail(
                request.getEmail()
        );

        spouse.setHasSsn(
                request.getHasSsn()
        );

        spouse.setHasItin(
                request.getHasItin()
        );

        spouseRepository.save(spouse);
    }


    // =========================================================
    // SAVE DEPENDENTS
    // =========================================================

    private void saveDependents(
            TaxOrganizer organizer,
            List<DependentRequest> requests) {

        List<TaxOrganizerDependent> existing =
                dependentRepository
                        .findByTaxOrganizerIdOrderByIdAsc(
                                organizer.getId()
                        );

        if (existing != null &&
                !existing.isEmpty()) {

            dependentRepository.deleteAll(
                    existing
            );
        }


        if (!Boolean.TRUE.equals(
                organizer.getHasDependents())) {

            return;
        }


        if (requests == null ||
                requests.isEmpty()) {

            throw new RuntimeException(
                    "Dependent information is required"
            );
        }


        for (DependentRequest request :
                requests) {

            TaxOrganizerDependent dependent =
                    new TaxOrganizerDependent();

            dependent.setTaxOrganizer(
                    organizer
            );

            dependent.setFirstName(
                    request.getFirstName()
            );

            dependent.setMiddleName(
                    request.getMiddleName()
            );

            dependent.setLastName(
                    request.getLastName()
            );

            dependent.setDateOfBirth(
                    request.getDateOfBirth()
            );

            dependent.setSsnItin(
                    request.getSsnItin()
            );

            dependent.setRelationship(
                    request.getRelationship()
            );

            dependent.setMonthsLivedWithYou(
                    request.getMonthsLivedWithYou()
            );

            dependent.setUsCitizenResident(
                    request.getUsCitizenResident()
            );

            dependent.setChildCareExpenses(
                    request.getChildCareExpenses()
            );

            dependent.setSsnStatus(
                    request.getSsnStatus()
            );

            dependent.setItinApplicationRequired(
                    request.getItinApplicationRequired()
            );

            dependentRepository.save(
                    dependent
            );
        }
    }


    // =========================================================
    // SUBMIT ORGANIZER
    // =========================================================
    public TaxOrganizerResponse submitOrganizer(
            Long clientId,
            Authentication authentication) {

        TaxOrganizer organizer = prepareSubmission(clientId, authentication);

        generateDocx(organizer);

        return getOrganizer(clientId, authentication);
    }


    // =========================================================
    // VALIDATE
    // =========================================================

    private void validateBeforeSubmit(
            TaxOrganizer organizer) {

        if (isBlank(organizer.getFirstName())) {
            throw new RuntimeException(
                    "First Name is required"
            );
        }

        if (isBlank(organizer.getLastName())) {
            throw new RuntimeException(
                    "Last Name is required"
            );
        }

        if (organizer.getDateOfBirth() == null) {
            throw new RuntimeException(
                    "Date of Birth is required"
            );
        }

        if (isBlank(organizer.getSsnItin())) {
            throw new RuntimeException(
                    "SSN/ITIN is required"
            );
        }

        if (isBlank(organizer.getVisaStatus())) {
            throw new RuntimeException(
                    "Visa Status is required"
            );
        }

        if (organizer.getVisaStatusChanged() == null) {
            throw new RuntimeException(
                    "Visa Status Changed is required"
            );
        }

        if (isBlank(organizer.getMaritalStatus())) {
            throw new RuntimeException(
                    "Marital Status is required"
            );
        }

        if (isBlank(organizer.getCurrentAddress())) {
            throw new RuntimeException(
                    "Current Address is required"
            );
        }

        if (isBlank(organizer.getEmailAddress())) {
            throw new RuntimeException(
                    "Email Address is required"
            );
        }

        if (isBlank(organizer.getPhoneNumber())) {
            throw new RuntimeException(
                    "Phone Number is required"
            );
        }

        if (isBlank(organizer.getOccupation())) {
            throw new RuntimeException(
                    "Occupation is required"
            );

        }

        if (isBlank(
                organizer.getStatesLivedIn2026()
        )) {
            throw new RuntimeException(
                    "States lived in during 2026 is required"
            );
        }
    }


    private boolean isSpouseRequired(
            String maritalStatus) {

        if (maritalStatus == null) {
            return false;
        }

        String value =
                maritalStatus
                        .trim()
                        .toLowerCase();

        return value.equals(
                "married filing jointly"
        ) ||
        value.equals(
                "married filing separately"
        );
    }


    private boolean isBlank(String value) {
        return value == null ||
                value.trim().isEmpty();
    }


    // =========================================================
    // GENERATE DOCX
    // =========================================================

    private void generateDocx(
            TaxOrganizer organizer) {

        try {

            Path clientDirectory =
                    uploadDirectory.resolve(
                            String.valueOf(
                                    organizer
                                            .getClient()
                                            .getId()
                            )
                    );

            Files.createDirectories(
                    clientDirectory
            );


            String safeClientName =
                    organizer
                            .getClient()
                            .getName()
                            .replaceAll(
                                    "[^a-zA-Z0-9_-]",
                                    "_"
                            );


            String fileName =
                    "Tax_Organizer_2026_"
                            + safeClientName
                            + ".docx";


            Path filePath =
                    clientDirectory.resolve(
                            fileName
                    );


            try (
                    XWPFDocument document =
                            new XWPFDocument()
            ) {

                addTitle(
                        document,
                        "Tax Organizer 2026"
                );

                addText(
                        document,
                        "Client: "
                                + organizer
                                .getClient()
                                .getName()
                );

                addText(
                        document,
                        ""
                );


                addSectionTitle(
                        document,
                        "Personal Information"
                );

                addField(
                        document,
                        "First Name",
                        organizer.getFirstName()
                );

                addField(
                        document,
                        "Middle Name",
                        organizer.getMiddleName()
                );

                addField(
                        document,
                        "Last Name",
                        organizer.getLastName()
                );

                addField(
                        document,
                        "Date of Birth",
                        String.valueOf(
                                organizer.getDateOfBirth()
                        )
                );

                addField(
                        document,
                        "SSN / ITIN",
                        organizer.getSsnItin()
                );

                addField(
                        document,
                        "Visa Status",
                        organizer.getVisaStatus()
                );

                addField(
                        document,
                        "Visa Status Changed",
                        String.valueOf(
                                organizer
                                        .getVisaStatusChanged()
                        )
                );

                addField(
                        document,
                        "Marital Status",
                        organizer.getMaritalStatus()
                );

                addField(
                        document,
                        "Current Address",
                        organizer.getCurrentAddress()
                );

                addField(
                        document,
                        "Email Address",
                        organizer.getEmailAddress()
                );

                addField(
                        document,
                        "Phone Number",
                        organizer.getPhoneNumber()
                );

                addField(
                        document,
                        "Occupation",
                        organizer.getOccupation()
                );

                addField(
                        document,
                        "States Lived In During 2026",
                        organizer.getStatesLivedIn2026()
                );


                // =================================================
                // SPOUSE
                // =================================================

                TaxOrganizerSpouse spouse =
                        spouseRepository
                                .findByTaxOrganizerId(
                                        organizer.getId()
                                )
                                .orElse(null);

                if (spouse != null) {

                    addSectionTitle(
                            document,
                            "Spouse Information"
                    );

                    addField(
                            document,
                            "First Name",
                            spouse.getFirstName()
                    );

                    addField(
                            document,
                            "Middle Name",
                            spouse.getMiddleName()
                    );

                    addField(
                            document,
                            "Last Name",
                            spouse.getLastName()
                    );

                    addField(
                            document,
                            "Date of Birth",
                            String.valueOf(
                                    spouse.getDateOfBirth()
                            )
                    );

                    addField(
                            document,
                            "SSN / ITIN",
                            spouse.getSsnItin()
                    );

                    addField(
                            document,
                            "Visa Status",
                            spouse.getVisaStatus()
                    );

                    addField(
                            document,
                            "Visa Status Changed",
                            String.valueOf(
                                    spouse
                                            .getVisaStatusChanged()
                            )
                    );

                    addField(
                            document,
                            "Occupation",
                            spouse.getOccupation()
                    );

                    addField(
                            document,
                            "Email",
                            spouse.getEmail()
                    );
                }


                // =================================================
                // DEPENDENTS
                // =================================================

                List<TaxOrganizerDependent> dependents =
                        dependentRepository
                                .findByTaxOrganizerIdOrderByIdAsc(
                                        organizer.getId()
                                );

                if (dependents != null &&
                        !dependents.isEmpty()) {

                    addSectionTitle(
                            document,
                            "Dependents"
                    );

                    addField(
                            document,
                            "Has Dependents",
                            String.valueOf(
                                    organizer
                                            .getHasDependents()
                            )
                    );


                    int index = 1;

                    for (
                            TaxOrganizerDependent dependent :
                            dependents) {

                        addSectionTitle(
                                document,
                                "Dependent "
                                        + index
                        );

                        addField(
                                document,
                                "First Name",
                                dependent.getFirstName()
                        );

                        addField(
                                document,
                                "Middle Name",
                                dependent.getMiddleName()
                        );

                        addField(
                                document,
                                "Last Name",
                                dependent.getLastName()
                        );

                        addField(
                                document,
                                "Date of Birth",
                                String.valueOf(
                                        dependent
                                                .getDateOfBirth()
                                )
                        );

                        addField(
                                document,
                                "SSN / ITIN",
                                dependent.getSsnItin()
                        );

                        addField(
                                document,
                                "Relationship",
                                dependent.getRelationship()
                        );

                        addField(
                                document,
                                "Months Lived With You",
                                String.valueOf(
                                        dependent
                                                .getMonthsLivedWithYou()
                                )
                        );

                        addField(
                                document,
                                "US Citizen / Resident",
                                String.valueOf(
                                        dependent
                                                .getUsCitizenResident()
                                )
                        );

                        addField(
                                document,
                                "Child Care Expenses",
                                String.valueOf(
                                        dependent
                                                .getChildCareExpenses()
                                )
                        );

                        addField(
                                document,
                                "SSN Status",
                                dependent.getSsnStatus()
                        );

                        addField(
                                document,
                                "ITIN Application Required",
                                String.valueOf(
                                        dependent
                                                .getItinApplicationRequired()
                                )
                        );

                        index++;
                    }
                }


                try (
                        FileOutputStream outputStream =
                                new FileOutputStream(
                                        filePath.toFile()
                                )
                ) {

                    document.write(
                            outputStream
                    );
                }
            }


            organizer.setFileName(
                    fileName
            );

            organizer.setFilePath(
                    filePath
                            .toAbsolutePath()
                            .toString()
            );

            organizer.setUpdatedAt(
                    LocalDateTime.now()
            );

            taxOrganizerRepository.save(
                    organizer
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to generate Tax Organizer file",
                    e
            );
        }
    }


    // =========================================================
    // DOCX HELPERS
    // =========================================================

    private void addTitle(
            XWPFDocument document,
            String text) {

        XWPFParagraph paragraph =
                document.createParagraph();

        XWPFRun run =
                paragraph.createRun();

        run.setBold(true);
        run.setFontSize(18);
        run.setText(text);
    }


    private void addSectionTitle(
            XWPFDocument document,
            String text) {

        XWPFParagraph paragraph =
                document.createParagraph();

        XWPFRun run =
                paragraph.createRun();

        run.setBold(true);
        run.setFontSize(14);
        run.setText(text);
    }


    private void addText(
            XWPFDocument document,
            String text) {

        XWPFParagraph paragraph =
                document.createParagraph();

        paragraph.createRun()
                .setText(text);
    }


    private void addField(
            XWPFDocument document,
            String label,
            String value) {

        XWPFParagraph paragraph =
                document.createParagraph();

        XWPFRun run =
                paragraph.createRun();

        run.setBold(true);

        run.setText(
                label
                        + ": "
                        + (value == null
                        ? ""
                        : value)
        );
    }


    // =========================================================
    // VIEW FILE
    // =========================================================

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> viewOrganizer(
            Long clientId,
            Authentication authentication) {

        TaxOrganizer organizer =
                getAuthorizedOrganizer(
                        clientId,
                        authentication
                );

        return serveFile(
                organizer,
                false
        );
    }


    // =========================================================
    // DOWNLOAD FILE
    // =========================================================

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadOrganizer(
            Long clientId,
            Authentication authentication) {

        TaxOrganizer organizer =
                getAuthorizedOrganizer(
                        clientId,
                        authentication
                );

        return serveFile(
                organizer,
                true
        );
    }


    private TaxOrganizer getAuthorizedOrganizer(
            Long clientId,
            Authentication authentication) {

        getEmployeeAssignment(
                clientId,
                authentication
        );

        return taxOrganizerRepository
                .findByClientId(clientId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Tax Organizer not found"
                        ));
    }


    private ResponseEntity<Resource> serveFile(
            TaxOrganizer organizer,
            boolean download) {

        if (organizer.getFilePath() == null) {

            throw new RuntimeException(
                    "Tax Organizer file has not been generated"
            );
        }

        try {

            Path path =
                    Paths.get(
                            organizer.getFilePath()
                    );

            Resource resource =
                    new UrlResource(
                            path.toUri()
                    );

            if (!resource.exists() ||
                    !resource.isReadable()) {

                throw new RuntimeException(
                        "Tax Organizer file not found"
                );
            }

            String contentType =
                    Files.probeContentType(path);

            if (contentType == null) {
                contentType =
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }

            String disposition =
                    download
                            ? "attachment"
                            : "inline";

            return ResponseEntity.ok()
                    .contentType(
                            MediaType.parseMediaType(
                                    contentType
                            )
                    )
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            disposition
                                    + "; filename=\""
                                    + organizer.getFileName()
                                    + "\""
                    )
                    .body(resource);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to read Tax Organizer file",
                    e
            );
        }
    }


    // =========================================================
    // MAP RESPONSE
    // =========================================================

    private TaxOrganizerResponse mapResponse(
            TaxOrganizer organizer) {

        Client client =
                organizer.getClient();

        User employee =
                organizer.getEmployee();


        TaxOrganizerSpouse spouse =
                spouseRepository
                        .findByTaxOrganizerId(
                                organizer.getId()
                        )
                        .orElse(null);


        List<TaxOrganizerDependent> dependents =
                dependentRepository
                        .findByTaxOrganizerIdOrderByIdAsc(
                                organizer.getId()
                        );


        TaxOrganizerResponse response =
                TaxOrganizerResponse.builder()

                        .id(
                                organizer.getId()
                        )

                        .clientId(
                                client.getId()
                        )

                        .clientName(
                                client.getName()
                        )

                        .employeeId(
                                employee == null
                                        ? null
                                        : employee.getId()
                        )

                        .employeeName(
                                employee == null
                                        ? null
                                        : employee.getFirstName()
                                                + " "
                                                + employee.getLastName()
                        )

                        .firstName(
                                organizer.getFirstName()
                        )

                        .middleName(
                                organizer.getMiddleName()
                        )

                        .lastName(
                                organizer.getLastName()
                        )

                        .dateOfBirth(
                                organizer.getDateOfBirth()
                        )

                        .ssnItin(
                                organizer.getSsnItin()
                        )

                        .visaStatus(
                                organizer.getVisaStatus()
                        )

                        .visaStatusChanged(
                                organizer.getVisaStatusChanged()
                        )

                        .maritalStatus(
                                organizer.getMaritalStatus()
                        )

                        .currentAddress(
                                organizer.getCurrentAddress()
                        )

                        .emailAddress(
                                organizer.getEmailAddress()
                        )

                        .phoneNumber(
                                organizer.getPhoneNumber()
                        )

                        .occupation(
                                organizer.getOccupation()
                        )

                        .statesLivedIn2026(
                                organizer.getStatesLivedIn2026()
                        )

                        .hasDependents(
                                organizer.getHasDependents()
                        )

                        .fileName(
                                organizer.getFileName()
                        )

                        .viewUrl(
                                organizer.getFileName() == null
                                        ? null
                                        : "/api/doc/clients/"
                                                + client.getId()
                                                + "/tax-organizer/view"
                        )

                        .downloadUrl(
                                organizer.getFileName() == null
                                        ? null
                                        : "/api/doc/clients/"
                                                + client.getId()
                                                + "/tax-organizer/download"
                        )

                        .status(
                                organizer.getStatus()
                        )

                        .createdAt(
                                organizer.getCreatedAt()
                        )

                        .updatedAt(
                                organizer.getUpdatedAt()
                        )

                        .submittedAt(
                                organizer.getSubmittedAt()
                        )

                        .build();


        // =====================================================
        // SPOUSE RESPONSE
        // =====================================================

        if (spouse != null) {

            response.setSpouse(
                    TaxOrganizerResponse.SpouseResponse
                            .builder()

                            .id(
                                    spouse.getId()
                            )

                            .firstName(
                                    spouse.getFirstName()
                            )

                            .middleName(
                                    spouse.getMiddleName()
                            )

                            .lastName(
                                    spouse.getLastName()
                            )

                            .dateOfBirth(
                                    spouse.getDateOfBirth()
                            )

                            .ssnItin(
                                    spouse.getSsnItin()
                            )

                            .visaStatus(
                                    spouse.getVisaStatus()
                            )

                            .visaStatusChanged(
                                    spouse.getVisaStatusChanged()
                            )

                            .occupation(
                                    spouse.getOccupation()
                            )

                            .email(
                                    spouse.getEmail()
                            )

                            .hasSsn(
                                    spouse.getHasSsn()
                            )

                            .hasItin(
                                    spouse.getHasItin()
                            )

                            .build()
            );
        }


        // =====================================================
        // DEPENDENTS RESPONSE
        // =====================================================

        List<TaxOrganizerResponse.DependentResponse>
                dependentResponses =
                new ArrayList<>();

        for (
                TaxOrganizerDependent dependent :
                dependents) {

            dependentResponses.add(
                    TaxOrganizerResponse.DependentResponse
                            .builder()

                            .id(
                                    dependent.getId()
                            )

                            .firstName(
                                    dependent.getFirstName()
                            )

                            .middleName(
                                    dependent.getMiddleName()
                            )

                            .lastName(
                                    dependent.getLastName()
                            )

                            .dateOfBirth(
                                    dependent.getDateOfBirth()
                            )

                            .ssnItin(
                                    dependent.getSsnItin()
                            )

                            .relationship(
                                    dependent.getRelationship()
                            )

                            .monthsLivedWithYou(
                                    dependent
                                            .getMonthsLivedWithYou()
                            )

                            .usCitizenResident(
                                    dependent
                                            .getUsCitizenResident()
                            )

                            .childCareExpenses(
                                    dependent
                                            .getChildCareExpenses()
                            )

                            .ssnStatus(
                                    dependent.getSsnStatus()
                            )

                            .itinApplicationRequired(
                                    dependent
                                            .getItinApplicationRequired()
                            )

                            .build()
            );
        }

        response.setDependents(
                dependentResponses
        );

        return response;
    }

    @Transactional(readOnly = true)
    public TaxOrganizerResponse getAdminOrganizer(
            Long clientId) {

        TaxOrganizer organizer =
                taxOrganizerRepository
                        .findByClientId(clientId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Tax Organizer not found"
                                ));

        return mapResponse(organizer);
    }
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> viewAdminOrganizer(
            Long clientId) {

        TaxOrganizer organizer =
                taxOrganizerRepository
                        .findByClientId(clientId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Tax Organizer not found"
                                ));

        return serveFile(
                organizer,
                false
        );
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadAdminOrganizer(
            Long clientId) {

        TaxOrganizer organizer =
                taxOrganizerRepository
                        .findByClientId(clientId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Tax Organizer not found"
                                ));

        return serveFile(
                organizer,
                true
        );
    }
    @Transactional
    protected TaxOrganizer prepareSubmission(
            Long clientId,
            Authentication authentication) {

        ClientAssignment assignment = getEmployeeAssignment(clientId, authentication);

        TaxOrganizer organizer = taxOrganizerRepository.findByClientId(clientId)
                .orElseThrow(() -> new RuntimeException("Tax Organizer not found"));

        validateBeforeSubmit(organizer);

        organizer.setStatus(TaxOrganizerStatus.SUBMITTED);
        organizer.setSubmittedAt(LocalDateTime.now());
        organizer.setUpdatedAt(LocalDateTime.now());

        // CRITICAL: Force initialize the lazy Client proxy so generateDocx doesn't crash
        organizer.getClient().getId();
        organizer.getClient().getName();

        return taxOrganizerRepository.save(organizer);
    }

}