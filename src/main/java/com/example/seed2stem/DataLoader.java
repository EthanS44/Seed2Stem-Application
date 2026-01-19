package com.example.seed2stem;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataLoader implements CommandLineRunner {

    private final ChecklistRepository checklistRepo;
    private final ChecklistItemRepository itemRepo;
    private final TaskRepository taskRepo;

    public DataLoader(ChecklistRepository checklistRepo,
                      ChecklistItemRepository itemRepo,
                      TaskRepository taskRepo) {
        this.checklistRepo = checklistRepo;
        this.itemRepo = itemRepo;
        this.taskRepo = taskRepo;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        //createAMProductionAreaInspection();
        //createPMProductionAreaInspection();

        // add more tasks/checklists here
    }

    private ChecklistItem header(String text, int displayOrder, Checklist checklist) {
        ChecklistItem item = new ChecklistItem();
        item.setText(text);
        item.setItemType("HEADER");
        item.setResponseType("NONE");
        item.setDisplayOrder(displayOrder);
        item.setChecklist(checklist);
        return item;
    }

    private ChecklistItem question(String text, String responseType, int questionOrder, int displayOrder, Checklist checklist) {
        ChecklistItem item = new ChecklistItem();
        item.setText(text);
        item.setItemType("QUESTION");
        item.setResponseType(responseType);
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

        int displayOrder = 1;
        int questionOrder = 1;

        // ===== Room B1 =====
        checklist.addItem(header("Room B1 (Lights Off):", displayOrder++, checklist));

        checklist.addItem(question(
                "Record humidity level. (%)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record temperature level. (°C)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir water level. (%)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir EC.",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir pH.",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Check nutrient levels of jugs at tables 1 to 5. Are all levels the same? Record nutrient levels.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        // ===== Room B2 =====
        checklist.addItem(header("Room B2 (Lights On):", displayOrder++, checklist));

        checklist.addItem(question(
                "Are all lights working? If not, record which lights are defective.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are all fans working and pointed in the right direction? If not, record which fans are defective.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record humidity level. (%)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record temperature level. (°C)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir water level. (%)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir EC.",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir pH.",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Check nutrient levels of jugs at tables 1 to 5. Are all levels the same? Record nutrient levels.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are there any light plants? If yes, record location and number.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are there any missing emitters? If yes, record location and number.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Is there any pooling water? If yes, record location and estimate of size of pool.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Remove any wilting leaves.",
                "NONE",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Ensure tables are all in proper alignment and foot stools are located at front of tables.",
                "NONE",
                questionOrder++, displayOrder++, checklist));



        // ===== Room B3 =====
        checklist.addItem(header("Room B3 (Lights Off):", displayOrder++, checklist));

        checklist.addItem(question(
                "Record humidity level. (%)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record temperature level. (°C)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir water level. (%)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir EC.",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir pH.",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Check nutrient levels of jugs at tables 1 to 5. Are all levels the same? Record nutrient levels.",
                "BOOLEAN_TEXT",
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

        int displayOrder = 1;
        int questionOrder = 1;

        // ===== Room B1 =====
        checklist.addItem(header("Room B1 (Lights On):", displayOrder++, checklist));

        checklist.addItem(question(
                "Are all lights working? If not, record which lights are defective.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are all fans working and pointed in the right direction? If not, record which fans are defective.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record humidity level. (%)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record temperature level. (°C)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir water level. (%)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir EC.",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir pH.",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Check nutrient levels of jugs at tables 1 to 5. Are all levels the same? Record nutrient levels.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are there any light plants? If yes, record location and number.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are there any missing emitters? If yes, record location and number.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Is there any pooling water? If yes, record location and estimate of size of pool.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Remove any wilting leaves.",
                "NONE",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Ensure tables are all in proper alignment and foot stools are located at front of tables.",
                "NONE",
                questionOrder++, displayOrder++, checklist));



        // ===== Room B2 =====
        checklist.addItem(header("Room B2 (Lights Off):", displayOrder++, checklist));

        checklist.addItem(question(
                "Record humidity level. (%)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record temperature level. (°C)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir water level. (%)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir EC.",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir pH.",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Check nutrient levels of jugs at tables 1 to 5. Are all levels the same? Record nutrient levels.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        // ===== Room B3 =====
        checklist.addItem(header("Room B3 (Lights On):", displayOrder++, checklist));

        checklist.addItem(question(
                "Are all lights working? If not, record which lights are defective.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are all fans working and pointed in the right direction? If not, record which fans are defective.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record humidity level. (%)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record temperature level. (°C)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir water level. (%)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir EC.",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Record reservoir pH.",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Check nutrient levels of jugs at tables 1 to 5. Are all levels the same? Record nutrient levels.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are there any light plants? If yes, record location and number.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Are there any missing emitters? If yes, record location and number.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Is there any pooling water? If yes, record location and estimate of size of pool.",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Remove any wilting leaves.",
                "NONE",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Ensure tables are all in proper alignment and foot stools are located at front of tables.",
                "NONE",
                questionOrder++, displayOrder++, checklist));

        task.setChecklist(checklist);

        checklistRepo.save(checklist);
        taskRepo.save(task);

        System.out.println("Seeded checklist and task: " + task.getTitle());
    }
}
