package com.example.seed2stem;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for entity classes, enums, and DTOs.
 */
class EntityTest {

    // --- User ---

    @Test
    void user_defaultConstructor_createsEmptyUser() {
        User user = new User();
        assertNull(user.getUsername());
        assertNull(user.getId());
    }

    @Test
    void user_parameterizedConstructor_setsAllFields() {
        User user = new User("john", "hashed", "John", "Doe", AccountType.TECHNICIAN);
        assertEquals("john", user.getUsername());
        assertEquals("hashed", user.getPassword());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals(AccountType.TECHNICIAN, user.getAccountType());
    }

    @Test
    void user_getName_returnsConcatenatedName() {
        User user = new User("john", "hashed", "John", "Doe", AccountType.TECHNICIAN);
        assertEquals("John Doe", user.getName());
    }

    @Test
    void user_setters_updateFields() {
        User user = new User();
        user.setId(1L);
        user.setUsername("jane");
        user.setPassword("secret");
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setAccountType(AccountType.MANAGER);

        assertEquals(1L, user.getId());
        assertEquals("jane", user.getUsername());
        assertEquals("secret", user.getPassword());
        assertEquals("Jane", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals(AccountType.MANAGER, user.getAccountType());
    }

    // --- Batch ---

    @Test
    void batch_defaultStatus_isCloning() {
        Batch batch = new Batch();
        assertEquals(BatchStatus.CLONING, batch.getStatus());
    }

    @Test
    void batch_settersAndGetters_work() {
        Batch batch = new Batch();
        batch.setId(1L);
        batch.setBatchCode("26-074-BLD-B1-316-01");
        batch.setStrain("Blue Dream");
        batch.setStrainAcronym("BLD");
        batch.setRoom("B1");
        batch.setPlantCount(50);
        batch.setStartDate(LocalDate.of(2026, 3, 15));
        batch.setHarvestDate(LocalDate.of(2026, 6, 1));
        batch.setHarvestWeightGrams(500.0);
        batch.setCoaSampleWeightGrams(10.0);
        batch.setNotes("Test batch");

        assertEquals(1L, batch.getId());
        assertEquals("26-074-BLD-B1-316-01", batch.getBatchCode());
        assertEquals("Blue Dream", batch.getStrain());
        assertEquals("BLD", batch.getStrainAcronym());
        assertEquals("B1", batch.getRoom());
        assertEquals(50, batch.getPlantCount());
        assertEquals(LocalDate.of(2026, 3, 15), batch.getStartDate());
        assertEquals(LocalDate.of(2026, 6, 1), batch.getHarvestDate());
        assertEquals(500.0, batch.getHarvestWeightGrams());
        assertEquals(10.0, batch.getCoaSampleWeightGrams());
        assertEquals("Test batch", batch.getNotes());
    }

    // --- ChecklistRun ---

    @Test
    void checklistRun_defaultStatus_isPending() {
        ChecklistRun run = new ChecklistRun();
        assertEquals(ChecklistRunStatus.PENDING, run.getStatus());
    }

    @Test
    void checklistRun_settersAndGetters_work() {
        ChecklistRun run = new ChecklistRun();
        User tech = new User("tech", "h", "T", "U", AccountType.TECHNICIAN);
        User mgr = new User("mgr", "h", "M", "U", AccountType.MANAGER);
        LocalDateTime now = LocalDateTime.now();

        run.setCompletedBy(tech);
        run.setStartTime(now);
        run.setEndTime(now.plusHours(1));
        run.setAuthorizedBy(mgr);
        run.setAuthorizedAt(now.plusHours(2));
        run.setManagerComments("Good work");
        run.setStatus(ChecklistRunStatus.APPROVED);
        run.setChecklistName("AM Inspection");
        run.setChecklistVersion(1);

        assertEquals(tech, run.getCompletedBy());
        assertEquals(now, run.getStartTime());
        assertEquals(now.plusHours(1), run.getEndTime());
        assertEquals(mgr, run.getAuthorizedBy());
        assertEquals(now.plusHours(2), run.getAuthorizedAt());
        assertEquals("Good work", run.getManagerComments());
        assertEquals(ChecklistRunStatus.APPROVED, run.getStatus());
        assertEquals("AM Inspection", run.getChecklistName());
        assertEquals(1, run.getChecklistVersion());
    }

    // --- ChecklistItem ---

    @Test
    void checklistItem_settersAndGetters_work() {
        ChecklistItem item = new ChecklistItem();
        item.setId(1L);
        item.setText("Check temperature");
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(ChecklistResponseType.DECIMAL);
        item.setCategory(ChecklistItemCategory.TEMPERATURE);
        item.setDisplayOrder(5);
        item.setQuestionOrder(3);

        assertEquals(1L, item.getId());
        assertEquals("Check temperature", item.getText());
        assertEquals(ChecklistItemType.QUESTION, item.getItemType());
        assertEquals(ChecklistResponseType.DECIMAL, item.getResponseType());
        assertEquals(ChecklistItemCategory.TEMPERATURE, item.getCategory());
        assertEquals(5, item.getDisplayOrder());
        assertEquals(3, item.getQuestionOrder());
    }

    // --- ChecklistResponse ---

    @Test
    void checklistResponse_allAnswerTypes() {
        ChecklistResponse resp = new ChecklistResponse();
        resp.setId(1L);
        resp.setBooleanAnswer(true);
        resp.setTextAnswer("Notes here");
        resp.setNumericAnswer(72.5);

        assertEquals(1L, resp.getId());
        assertTrue(resp.getBooleanAnswer());
        assertEquals("Notes here", resp.getTextAnswer());
        assertEquals(72.5, resp.getNumericAnswer());
    }

    @Test
    void checklistResponse_relationships() {
        ChecklistResponse resp = new ChecklistResponse();
        ChecklistRun run = new ChecklistRun();
        ChecklistItem item = new ChecklistItem();

        resp.setChecklistRun(run);
        resp.setChecklistItem(item);

        assertEquals(run, resp.getChecklistRun());
        assertEquals(item, resp.getChecklistItem());
    }

    // --- BatchStatus enum ---

    @Test
    void batchStatus_allValuesExist() {
        assertEquals(9, BatchStatus.values().length);
    }

    @Test
    void batchStatus_displayNames() {
        assertEquals("Cloning", BatchStatus.CLONING.getDisplayName());
        assertEquals("Vegetative", BatchStatus.VEGETATIVE.getDisplayName());
        assertEquals("Flowering", BatchStatus.FLOWERING.getDisplayName());
        assertEquals("Harvested", BatchStatus.HARVESTED.getDisplayName());
        assertEquals("Debucking", BatchStatus.DEBUCKING.getDisplayName());
        assertEquals("Trimming", BatchStatus.TRIMMING.getDisplayName());
        assertEquals("Packaging", BatchStatus.PACKAGING.getDisplayName());
        assertEquals("Packaged", BatchStatus.PACKAGED.getDisplayName());
        assertEquals("Destroyed", BatchStatus.DESTROYED.getDisplayName());
    }

    // --- Task (userCreated fields) ---

    @Test
    void task_userCreatedFields() {
        Task task = new Task();
        User creator = new User("tech", "h", "Tech", "User", AccountType.TECHNICIAN);

        task.setUserCreated(true);
        task.setCreatedBy(creator);

        assertTrue(task.isUserCreated());
        assertEquals(creator, task.getCreatedBy());
    }

    @Test
    void task_defaultUserCreated_isFalse() {
        Task task = new Task();
        assertFalse(task.isUserCreated());
    }

    // --- RegistrationRequest ---

    @Test
    void registrationRequest_settersAndGetters_work() {
        RegistrationRequest req = new RegistrationRequest();
        LocalDateTime now = LocalDateTime.now();

        req.setId(1L);
        req.setFirstName("Jane");
        req.setLastName("Smith");
        req.setUsername("jsmith");
        req.setPassword("hashedpass");
        req.setAccountType(AccountType.TECHNICIAN);
        req.setStatus(RegistrationStatus.PENDING);
        req.setCreatedAt(now);

        assertEquals(1L, req.getId());
        assertEquals("Jane", req.getFirstName());
        assertEquals("Smith", req.getLastName());
        assertEquals("jsmith", req.getUsername());
        assertEquals("hashedpass", req.getPassword());
        assertEquals(AccountType.TECHNICIAN, req.getAccountType());
        assertEquals(RegistrationStatus.PENDING, req.getStatus());
        assertEquals(now, req.getCreatedAt());
    }

    @Test
    void registrationRequest_defaultStatus_isPending() {
        RegistrationRequest req = new RegistrationRequest();
        assertEquals(RegistrationStatus.PENDING, req.getStatus());
    }

    // --- RegistrationStatus enum ---

    @Test
    void registrationStatus_allValuesExist() {
        assertEquals(3, RegistrationStatus.values().length);
        assertNotNull(RegistrationStatus.valueOf("PENDING"));
        assertNotNull(RegistrationStatus.valueOf("APPROVED"));
        assertNotNull(RegistrationStatus.valueOf("DENIED"));
    }

    // --- AccountType enum ---

    @Test
    void accountType_allValuesExist() {
        assertEquals(4, AccountType.values().length);
        assertNotNull(AccountType.valueOf("TECHNICIAN"));
        assertNotNull(AccountType.valueOf("MANAGER"));
        assertNotNull(AccountType.valueOf("ADMIN"));
        assertNotNull(AccountType.valueOf("DEVELOPER"));
    }

    // --- ChecklistRunStatus enum ---

    @Test
    void checklistRunStatus_allValuesExist() {
        assertEquals(4, ChecklistRunStatus.values().length);
        assertNotNull(ChecklistRunStatus.valueOf("IN_PROGRESS"));
        assertNotNull(ChecklistRunStatus.valueOf("PENDING"));
        assertNotNull(ChecklistRunStatus.valueOf("APPROVED"));
        assertNotNull(ChecklistRunStatus.valueOf("REJECTED"));
    }

    // --- ChecklistItemType enum ---

    @Test
    void checklistItemType_allValuesExist() {
        assertEquals(2, ChecklistItemType.values().length);
        assertNotNull(ChecklistItemType.valueOf("HEADER"));
        assertNotNull(ChecklistItemType.valueOf("QUESTION"));
    }

    // --- ChecklistResponseType enum ---

    @Test
    void checklistResponseType_allValuesExist() {
        assertEquals(6, ChecklistResponseType.values().length);
        assertNotNull(ChecklistResponseType.valueOf("BOOLEAN_TEXT"));
        assertNotNull(ChecklistResponseType.valueOf("TEXT"));
        assertNotNull(ChecklistResponseType.valueOf("INTEGER"));
        assertNotNull(ChecklistResponseType.valueOf("NUMBER"));
        assertNotNull(ChecklistResponseType.valueOf("DECIMAL"));
        assertNotNull(ChecklistResponseType.valueOf("NONE"));
    }

    // --- CalendarEventDTO ---

    @Test
    void calendarEventDTO_defaultConstructor() {
        CalendarEventDTO dto = new CalendarEventDTO();
        assertNull(dto.getId());
        assertNull(dto.getTitle());
    }

    @Test
    void calendarEventDTO_parameterizedConstructor() {
        CalendarEventDTO dto = new CalendarEventDTO(
                "batch-1", "Batch Started", "2026-03-15", null,
                true, "#2e7d32", "/batches/1", "batch-start");

        assertEquals("batch-1", dto.getId());
        assertEquals("Batch Started", dto.getTitle());
        assertEquals("2026-03-15", dto.getStart());
        assertNull(dto.getEnd());
        assertTrue(dto.isAllDay());
        assertEquals("#2e7d32", dto.getColor());
        assertEquals("/batches/1", dto.getUrl());
        assertEquals("batch-start", dto.getEventType());
    }
}
