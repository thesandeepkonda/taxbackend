package com.crm.matrix.service;

import com.crm.matrix.dto.BulkEmployeeError;
import com.crm.matrix.dto.BulkEmployeeResponse;
import com.crm.matrix.dto.CreateEmployeeRequest;
import com.crm.matrix.dto.CreateEmployeeResponse;
import com.crm.matrix.enums.WorkMode;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BulkEmployeeService {

    private final UserService userService;

    private static final String[] EXPECTED_HEADERS = {

            "Employee Code",

            "First Name",

            "Last Name",

            "Email",

            "Phone",

            "Department ID",

            "Team ID",

            "Role ID",

            "Attendance Policy ID",

            "Work Mode"};


    @Transactional
    public BulkEmployeeResponse uploadEmployees(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException("Excel file is required");
        }


        String fileName = file.getOriginalFilename();


        if (fileName == null || !fileName.toLowerCase().endsWith(".xlsx")) {

            throw new IllegalArgumentException("Only .xlsx Excel files are supported");
        }


        List<BulkEmployeeError> errors = new ArrayList<>();


        List<CreateEmployeeRequest> requests = new ArrayList<>();


        try (InputStream inputStream = file.getInputStream();

             Workbook workbook = WorkbookFactory.create(inputStream)) {

            if (workbook.getNumberOfSheets() == 0) {

                throw new IllegalArgumentException("Excel file does not contain any sheet");
            }


            Sheet sheet = workbook.getSheetAt(0);


            // =================================================
            // HEADER
            // =================================================

            Row headerRow = sheet.getRow(0);


            validateHeaders(headerRow, errors);


            if (!errors.isEmpty()) {

                return buildErrorResponse(0, errors);
            }


            // =================================================
            // READ EMPLOYEES
            // =================================================

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

                Row row = sheet.getRow(rowIndex);


                if (isEmptyRow(row)) {
                    continue;
                }


                int rowNumber = rowIndex + 1;


                CreateEmployeeRequest request = readEmployeeRow(row, rowNumber, errors);


                if (request != null) {

                    requests.add(request);
                }
            }


        } catch (IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new IllegalArgumentException("Failed to read Excel file: " + e.getMessage(), e);
        }


        // =====================================================
        // NO EMPLOYEES
        // =====================================================

        if (requests.isEmpty() && errors.isEmpty()) {

            throw new IllegalArgumentException("Excel file contains no employee data");
        }


        // =====================================================
        // IF EXCEL VALIDATION FAILED
        // =====================================================

        if (!errors.isEmpty()) {

            return buildErrorResponse(requests.size(), errors);
        }


        // =====================================================
        // CREATE ALL EMPLOYEES
        // =====================================================

        /*
         * We reuse the existing UserService.
         *
         * This means:
         *
         * Excel
         *   ↓
         * CreateEmployeeRequest
         *   ↓
         * UserService.createEmployee()
         *
         * All your existing employee rules remain
         * in one place.
         */

        List<CreateEmployeeResponse> createdEmployees = new ArrayList<>();


        for (CreateEmployeeRequest request : requests) {

            try {

                CreateEmployeeResponse response = userService.createEmployee(request);

                createdEmployees.add(response);

            } catch (IllegalArgumentException e) {

                /*
                 * IMPORTANT:
                 *
                 * If one employee fails,
                 * @Transactional will roll back
                 * the complete bulk operation.
                 */

                int index = createdEmployees.size();

                int excelRowNumber = findExcelRowNumber(requests, request);


                errors.add(BulkEmployeeError.builder().rowNumber(excelRowNumber).field("employee").value(request.getEmployeeCode()).message(e.getMessage()).build());


                throw e;
            }
        }


        // =====================================================
        // SUCCESS
        // =====================================================

        return BulkEmployeeResponse.builder()

                .success(true)

                .totalRows(requests.size())

                .successRows(createdEmployees.size())

                .errorRows(0)

                .errors(Collections.emptyList())

                .build();
    }


    // =========================================================
    // READ EMPLOYEE ROW
    // =========================================================

    private CreateEmployeeRequest readEmployeeRow(Row row, int rowNumber, List<BulkEmployeeError> errors) {

        String employeeCode = getCellValue(row.getCell(0));

        String firstName = getCellValue(row.getCell(1));

        String lastName = getCellValue(row.getCell(2));

        String email = getCellValue(row.getCell(3));

        String phone = getCellValue(row.getCell(4));

        String departmentIdValue = getCellValue(row.getCell(5));

        String teamIdValue = getCellValue(row.getCell(6));

        String roleIdValue = getCellValue(row.getCell(7));

        String attendancePolicyIdValue = getCellValue(row.getCell(8));

        String workModeValue = getCellValue(row.getCell(9));


        // =====================================================
        // REQUIRED FIELDS
        // =====================================================

        if (employeeCode.isBlank()) {

            addError(errors, rowNumber, "employeeCode", "", "Employee code is required");
        }


        if (firstName.isBlank()) {

            addError(errors, rowNumber, "firstName", "", "First name is required");
        }


        if (email.isBlank()) {

            addError(errors, rowNumber, "email", "", "Email is required");
        }


        if (phone.isBlank()) {

            addError(errors, rowNumber, "phone", "", "Phone is required");
        }


        if (departmentIdValue.isBlank()) {

            addError(errors, rowNumber, "departmentId", "", "Department is required");
        }


        if (attendancePolicyIdValue.isBlank()) {

            addError(errors, rowNumber, "attendancePolicyId", "", "Attendance policy is required");
        }


        if (workModeValue.isBlank()) {

            addError(errors, rowNumber, "workMode", "", "Work mode is required");
        }


        // =====================================================
        // BASIC VALIDATION
        // =====================================================

        if (!employeeCode.isBlank() && employeeCode.length() > 30) {

            addError(errors, rowNumber, "employeeCode", employeeCode, "Employee code cannot exceed 30 characters");
        }


        if (!firstName.isBlank() && firstName.length() > 100) {

            addError(errors, rowNumber, "firstName", firstName, "First name cannot exceed 100 characters");
        }


        if (!lastName.isBlank() && lastName.length() > 100) {

            addError(errors, rowNumber, "lastName", lastName, "Last name cannot exceed 100 characters");
        }


        if (!email.isBlank() && !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

            addError(errors, rowNumber, "email", email, "Invalid email address");
        }


        if (!phone.isBlank() && !phone.matches("^[0-9]{10}$")) {

            addError(errors, rowNumber, "phone", phone, "Phone number must contain exactly 10 digits");
        }


        // =====================================================
        // PARSE DEPARTMENT
        // =====================================================

        Long departmentId = parseLong(departmentIdValue, rowNumber, "departmentId", errors);


        // =====================================================
        // PARSE TEAM
        // =====================================================

        Long teamId = null;

        if (!teamIdValue.isBlank()) {

            teamId = parseLong(teamIdValue, rowNumber, "teamId", errors);
        }


        // =====================================================
        // PARSE ROLE
        // =====================================================

        Long roleId = null;

        if (!roleIdValue.isBlank()) {

            roleId = parseLong(roleIdValue, rowNumber, "roleId", errors);
        }


        // =====================================================
        // PARSE ATTENDANCE POLICY
        // =====================================================

        Long attendancePolicyId = parseLong(attendancePolicyIdValue, rowNumber, "attendancePolicyId", errors);


        // =====================================================
        // WORK MODE
        // =====================================================

        WorkMode workMode = null;


        if (!workModeValue.isBlank()) {

            try {

                workMode = WorkMode.valueOf(workModeValue.trim().toUpperCase());

            } catch (IllegalArgumentException e) {

                addError(errors, rowNumber, "workMode", workModeValue, "Invalid work mode. Allowed values: " + java.util.Arrays.toString(WorkMode.values()));
            }
        }


        // =====================================================
        // DON'T CREATE INVALID REQUEST
        // =====================================================

        if (hasErrorForRow(errors, rowNumber)) {

            return null;
        }


        // =====================================================
        // BUILD REQUEST
        // =====================================================

        CreateEmployeeRequest request = new CreateEmployeeRequest();


        request.setEmployeeCode(employeeCode.trim());


        request.setFirstName(firstName.trim());


        request.setLastName(lastName.isBlank() ? null : lastName.trim());


        request.setEmail(email.trim().toLowerCase());


        request.setPhone(phone.trim());


        request.setDepartmentId(departmentId);


        request.setTeamId(teamId);


        request.setRoleId(roleId);


        request.setAttendancePolicyId(attendancePolicyId);


        request.setWorkMode(workMode);


        return request;
    }


    // =========================================================
    // PARSE LONG
    // =========================================================

    private Long parseLong(String value, int rowNumber, String field, List<BulkEmployeeError> errors) {

        try {

            return Long.parseLong(value.trim());

        } catch (NumberFormatException e) {

            addError(errors, rowNumber, field, value, field + " must be a valid number");

            return null;
        }
    }


    // =========================================================
    // HEADER VALIDATION
    // =========================================================

    private void validateHeaders(Row headerRow, List<BulkEmployeeError> errors) {

        if (headerRow == null) {

            addError(errors, 1, "header", "", "Header row is required");

            return;
        }


        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {

            String actual = getCellValue(headerRow.getCell(i));


            if (!EXPECTED_HEADERS[i].equalsIgnoreCase(actual.trim())) {

                addError(errors, 1, "column " + (i + 1), actual, "Expected header: " + EXPECTED_HEADERS[i]);
            }
        }
    }


    // =========================================================
    // CELL VALUE
    // =========================================================

    private String getCellValue(Cell cell) {

        if (cell == null) {
            return "";
        }


        DataFormatter formatter = new DataFormatter();


        return formatter.formatCellValue(cell).trim();
    }


    // =========================================================
    // EMPTY ROW
    // =========================================================

    private boolean isEmptyRow(Row row) {

        if (row == null) {
            return true;
        }


        for (int i = 0; i < 10; i++) {

            if (!getCellValue(row.getCell(i)).isBlank()) {

                return false;
            }
        }


        return true;
    }


    // =========================================================
    // CHECK ROW ERROR
    // =========================================================

    private boolean hasErrorForRow(List<BulkEmployeeError> errors, int rowNumber) {

        return errors.stream().anyMatch(error -> error.getRowNumber() == rowNumber);
    }


    // =========================================================
    // ADD ERROR
    // =========================================================

    private void addError(List<BulkEmployeeError> errors, int rowNumber, String field, String value, String message) {

        errors.add(BulkEmployeeError.builder()

                .rowNumber(rowNumber)

                .field(field)

                .value(value)

                .message(message)

                .build());
    }


    // =========================================================
    // FIND ROW NUMBER
    // =========================================================

    private int findExcelRowNumber(List<CreateEmployeeRequest> requests, CreateEmployeeRequest request) {

        /*
         * This is only used if UserService validation
         * catches something that wasn't detected while
         * reading Excel.
         *
         * Since the request itself doesn't contain
         * the Excel row number, return the request's
         * position + 2.
         */

        int index = requests.indexOf(request);

        return index >= 0 ? index + 2 : -1;
    }


    // =========================================================
    // BUILD ERROR RESPONSE
    // =========================================================

    private BulkEmployeeResponse buildErrorResponse(int totalRows, List<BulkEmployeeError> errors) {

        int errorRows = (int) errors.stream().map(BulkEmployeeError::getRowNumber).distinct().count();


        return BulkEmployeeResponse.builder()

                .success(false)

                .totalRows(totalRows)

                .successRows(0)

                .errorRows(errorRows)

                .errors(errors)

                .build();
    }
}