package com.example.seed2stem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataLoader implements CommandLineRunner {

    private final ChecklistRepository checklistRepo;
    private final ChecklistItemRepository itemRepo;
    private final TaskRepository taskRepo;
    private final DataSource dataSource;

    public DataLoader(ChecklistRepository checklistRepo,
                      ChecklistItemRepository itemRepo,
                      TaskRepository taskRepo,
                      DataSource dataSource) {
        this.checklistRepo = checklistRepo;
        this.itemRepo = itemRepo;
        this.taskRepo = taskRepo;
        this.dataSource = dataSource;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        migrateCheckConstraints();
        //createAMProductionAreaInspection();
        //createPMProductionAreaInspection();
    }

    private void migrateCheckConstraints() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Update checklist_run status constraint
            stmt.execute("ALTER TABLE checklist_run DROP CONSTRAINT IF EXISTS checklist_run_status_check");
            stmt.execute("ALTER TABLE checklist_run ADD CONSTRAINT checklist_run_status_check " +
                    "CHECK (status IN ('IN_PROGRESS','PENDING','APPROVED','REJECTED'))");

            // Drop old checklist_item constraints (don't re-add — let Hibernate manage)
            stmt.execute("ALTER TABLE checklist_item DROP CONSTRAINT IF EXISTS checklist_item_item_type_check");
            stmt.execute("ALTER TABLE checklist_item DROP CONSTRAINT IF EXISTS checklist_item_response_type_check");

            // Drop obsolete columns from task table
            stmt.execute("ALTER TABLE task DROP COLUMN IF EXISTS assigned_to_user_id");
            stmt.execute("ALTER TABLE task DROP COLUMN IF EXISTS completed");

        } catch (Exception e) {
            System.out.println("Check constraint migration skipped: " + e.getMessage());
        }
    }

    private ChecklistItem header(String text, int displayOrder, Checklist checklist) {
        ChecklistItem item = new ChecklistItem();
        item.setText(text);
        item.setItemType(ChecklistItemType.HEADER);
        item.setResponseType(ChecklistResponseType.NONE);
        item.setDisplayOrder(displayOrder);
        item.setChecklist(checklist);
        return item;
    }

    private ChecklistItem question(String text, ChecklistResponseType responseType,
                                   ChecklistItemCategory category,
                                   int questionOrder, int displayOrder, Checklist checklist) {
        ChecklistItem item = new ChecklistItem();
        item.setText(text);
        item.setItemType(ChecklistItemType.QUESTION);
        item.setResponseType(responseType);
        item.setCategory(category);
        item.setQuestionOrder(questionOrder);
        item.setDisplayOrder(displayOrder);
        item.setChecklist(checklist);
        return item;
    }

    private void createAMProductionAreaInspection() {

        Task task = new Task();
        task.setTitle("AM Production Area Inspection");
        task.setDescription("This task is to be performed every day first thing every morning.");

        Checklist checklist = new Checklist();
        checklist.setName("AM Production Area Inspection");
        checklist.setVersion(1);

        int displayOrder = 1;
        int questionOrder = 1;

        // ===== Room B1 =====
        checklist.addItem(header("Room B1 (Lights Off):", displayOrder++, checklist));

        checklist.addItem(question(
                "Record humidity level. (%)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.HUMIDITY,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record temperature level. (\u00b0C)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.TEMPERATURE,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir water level. (%)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.WATER_LEVEL,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir EC.",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.EC,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir pH.",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.PH,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Check nutrient levels of jugs at tables 1 to 5. Are all levels the same? Record nutrient levels.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.NUTRIENTS,
                questionOrder++, displayOrder++, checklist));

        // ===== Room B2 =====
        checklist.addItem(header("Room B2 (Lights On):", displayOrder++, checklist));

        checklist.addItem(question(
                "Are all lights working? If not, record which lights are defective.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.LIGHTS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are all fans working and pointed in the right direction? If not, record which fans are defective.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.FANS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record humidity level. (%)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.HUMIDITY,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record temperature level. (\u00b0C)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.TEMPERATURE,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir water level. (%)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.WATER_LEVEL,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir EC.",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.EC,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir pH.",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.PH,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Check nutrient levels of jugs at tables 1 to 5. Are all levels the same? Record nutrient levels.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.NUTRIENTS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are there any light plants? If yes, record location and number.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.PLANTS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are there any missing emitters? If yes, record location and number.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.EMITTERS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Is there any pooling water? If yes, record location and estimate of size of pool.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.WATER_LEAKS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Remove any wilting leaves.",
                ChecklistResponseType.NONE, ChecklistItemCategory.WILTING_LEAVES,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Ensure tables are all in proper alignment and foot stools are located at front of tables.",
                ChecklistResponseType.NONE, ChecklistItemCategory.TABLE_ALIGNMENT,
                questionOrder++, displayOrder++, checklist));

        // ===== Room B3 =====
        checklist.addItem(header("Room B3 (Lights Off):", displayOrder++, checklist));

        checklist.addItem(question(
                "Record humidity level. (%)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.HUMIDITY,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record temperature level. (\u00b0C)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.TEMPERATURE,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir water level. (%)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.WATER_LEVEL,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir EC.",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.EC,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir pH.",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.PH,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Check nutrient levels of jugs at tables 1 to 5. Are all levels the same? Record nutrient levels.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.NUTRIENTS,
                questionOrder++, displayOrder++, checklist));

        task.setChecklist(checklist);

        checklistRepo.save(checklist);
        taskRepo.save(task);

        System.out.println("Seeded checklist and task: " + task.getTitle());
    }

    private void createPMProductionAreaInspection() {

        Task task = new Task();
        task.setTitle("PM Production Area Inspection");
        task.setDescription("This task is to be performed every day last thing in the evening.");

        Checklist checklist = new Checklist();
        checklist.setName("PM Production Area Inspection");
        checklist.setVersion(1);

        int displayOrder = 1;
        int questionOrder = 1;

        // ===== Room B1 =====
        checklist.addItem(header("Room B1 (Lights On):", displayOrder++, checklist));

        checklist.addItem(question(
                "Are all lights working? If not, record which lights are defective.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.LIGHTS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are all fans working and pointed in the right direction? If not, record which fans are defective.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.FANS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record humidity level. (%)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.HUMIDITY,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record temperature level. (\u00b0C)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.TEMPERATURE,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir water level. (%)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.WATER_LEVEL,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir EC.",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.EC,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir pH.",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.PH,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Check nutrient levels of jugs at tables 1 to 5. Are all levels the same? Record nutrient levels.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.NUTRIENTS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are there any light plants? If yes, record location and number.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.PLANTS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are there any missing emitters? If yes, record location and number.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.EMITTERS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Is there any pooling water? If yes, record location and estimate of size of pool.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.WATER_LEAKS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Remove any wilting leaves.",
                ChecklistResponseType.NONE, ChecklistItemCategory.WILTING_LEAVES,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Ensure tables are all in proper alignment and foot stools are located at front of tables.",
                ChecklistResponseType.NONE, ChecklistItemCategory.TABLE_ALIGNMENT,
                questionOrder++, displayOrder++, checklist));

        // ===== Room B2 =====
        checklist.addItem(header("Room B2 (Lights Off):", displayOrder++, checklist));

        checklist.addItem(question(
                "Record humidity level. (%)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.HUMIDITY,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record temperature level. (\u00b0C)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.TEMPERATURE,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir water level. (%)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.WATER_LEVEL,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir EC.",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.EC,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir pH.",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.PH,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Check nutrient levels of jugs at tables 1 to 5. Are all levels the same? Record nutrient levels.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.NUTRIENTS,
                questionOrder++, displayOrder++, checklist));

        // ===== Room B3 =====
        checklist.addItem(header("Room B3 (Lights On):", displayOrder++, checklist));

        checklist.addItem(question(
                "Are all lights working? If not, record which lights are defective.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.LIGHTS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are all fans working and pointed in the right direction? If not, record which fans are defective.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.FANS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record humidity level. (%)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.HUMIDITY,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record temperature level. (\u00b0C)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.TEMPERATURE,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir water level. (%)",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.WATER_LEVEL,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir EC.",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.EC,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir pH.",
                ChecklistResponseType.INTEGER, ChecklistItemCategory.PH,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Check nutrient levels of jugs at tables 1 to 5. Are all levels the same? Record nutrient levels.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.NUTRIENTS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are there any light plants? If yes, record location and number.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.PLANTS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are there any missing emitters? If yes, record location and number.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.EMITTERS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Is there any pooling water? If yes, record location and estimate of size of pool.",
                ChecklistResponseType.BOOLEAN_TEXT, ChecklistItemCategory.WATER_LEAKS,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Remove any wilting leaves.",
                ChecklistResponseType.NONE, ChecklistItemCategory.WILTING_LEAVES,
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Ensure tables are all in proper alignment and foot stools are located at front of tables.",
                ChecklistResponseType.NONE, ChecklistItemCategory.TABLE_ALIGNMENT,
                questionOrder++, displayOrder++, checklist));

        task.setChecklist(checklist);

        checklistRepo.save(checklist);
        taskRepo.save(task);

        System.out.println("Seeded checklist and task: " + task.getTitle());
    }
}
