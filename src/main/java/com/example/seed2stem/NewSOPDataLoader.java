package com.example.seed2stem;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NewSOPDataLoader implements CommandLineRunner {

    private final ChecklistRepository checklistRepo;
    private final ChecklistItemRepository itemRepo;
    private final TaskRepository taskRepo;

    public NewSOPDataLoader(ChecklistRepository checklistRepo,
                           ChecklistItemRepository itemRepo,
                           TaskRepository taskRepo) {
        this.checklistRepo = checklistRepo;
        this.itemRepo = itemRepo;
        this.taskRepo = taskRepo;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        createClonePropagationChecklist();
        createTriggerChecklist();
        createDestructionChecklist();
        createHarvestChecklist();
        createDebuckingChecklist();
        createTrimmingChecklist();
        createPackagingChecklist();
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

    private ChecklistItem question(String text, String responseType, int questionOrder, 
                                   int displayOrder, Checklist checklist) {
        ChecklistItem item = new ChecklistItem();
        item.setText(text);
        item.setItemType("QUESTION");
        item.setResponseType(responseType);
        item.setQuestionOrder(questionOrder);
        item.setDisplayOrder(displayOrder);
        item.setChecklist(checklist);
        return item;
    }

    // ==================== I. SOP: Clone Propagation ====================
    private void createClonePropagationChecklist() {
        Task task = new Task();
        task.setTitle("Clone Propagation");
        task.setDescription("Record clone propagation details and create new batch.");

        Checklist checklist = new Checklist();
        checklist.setName("Clone Propagation");

        int displayOrder = 1;
        int questionOrder = 1;

        checklist.addItem(header("Batch Information:", displayOrder++, checklist));

        checklist.addItem(question(
                "Day of the year (1-365)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Year (last two digits, e.g., 26 for 2026)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Strain Name",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Strain Acronym (3 letters)",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Destination Grow Room (B1, B2, or B3)",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Number of clones cut",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        task.setChecklist(checklist);
        checklistRepo.save(checklist);
        taskRepo.save(task);

        System.out.println("✓ Seeded checklist and task: " + task.getTitle());
    }

    // ==================== II. SOP: Trigger ====================
    private void createTriggerChecklist() {
        Task task = new Task();
        task.setTitle("Trigger");
        task.setDescription("Record when plants are triggered to flowering stage.");

        Checklist checklist = new Checklist();
        checklist.setName("Trigger");

        int displayOrder = 1;
        int questionOrder = 1;

        checklist.addItem(header("Trigger Information:", displayOrder++, checklist));

        checklist.addItem(question(
                "Batch Code",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Date (YYYY-MM-DD)",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Number of plants",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Employee who performed the action",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        task.setChecklist(checklist);
        checklistRepo.save(checklist);
        taskRepo.save(task);

        System.out.println("✓ Seeded checklist and task: " + task.getTitle());
    }

    // ==================== III. SOP: Destruction of Product ====================
    private void createDestructionChecklist() {
        Task task = new Task();
        task.setTitle("Destruction of Product");
        task.setDescription("Record destruction of live plants or dry product.");

        Checklist checklist = new Checklist();
        checklist.setName("Destruction of Product");

        int displayOrder = 1;
        int questionOrder = 1;

        checklist.addItem(header("Destruction Information:", displayOrder++, checklist));

        checklist.addItem(question(
                "Batch Code",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Type of product being destroyed (LIVE_PLANTS or DRY_PRODUCT)",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "If LIVE_PLANTS: Number of plants being destroyed (leave blank if DRY_PRODUCT)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "If DRY_PRODUCT: Weight of product being destroyed in grams (leave blank if LIVE_PLANTS)",
                "DECIMAL",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Reason for destruction",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        task.setChecklist(checklist);
        checklistRepo.save(checklist);
        taskRepo.save(task);

        System.out.println("✓ Seeded checklist and task: " + task.getTitle());
    }

    // ==================== IV. SOP: Harvest ====================
    private void createHarvestChecklist() {
        Task task = new Task();
        task.setTitle("Harvest");
        task.setDescription("Record harvest data for a batch.");

        Checklist checklist = new Checklist();
        checklist.setName("Harvest");

        int displayOrder = 1;
        int questionOrder = 1;

        checklist.addItem(header("Harvest Information:", displayOrder++, checklist));

        checklist.addItem(question(
                "Batch Code",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Date of harvest (YYYY-MM-DD)",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Number of plants harvested",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Total weight of plants harvested (grams)",
                "DECIMAL",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "CoA (Certificate of Analysis) sample plant weight (grams)",
                "DECIMAL",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Additional notes",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        task.setChecklist(checklist);
        checklistRepo.save(checklist);
        taskRepo.save(task);

        System.out.println("✓ Seeded checklist and task: " + task.getTitle());
    }

    // ==================== V. SOP: Debucking (Multi-day) ====================
    private void createDebuckingChecklist() {
        Task task = new Task();
        task.setTitle("Debucking Session");
        task.setDescription("Daily debucking session - can be performed multiple days until completion.");

        Checklist checklist = new Checklist();
        checklist.setName("Debucking Session");

        int displayOrder = 1;
        int questionOrder = 1;

        checklist.addItem(header("Daily Debucking Session:", displayOrder++, checklist));

        checklist.addItem(question(
                "Batch Code",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Session Date (YYYY-MM-DD)",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Weight of product debucked today (grams)",
                "DECIMAL",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Is this the final debucking session? (YES or NO)",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Notes",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        task.setChecklist(checklist);
        checklistRepo.save(checklist);
        taskRepo.save(task);

        System.out.println("✓ Seeded checklist and task: " + task.getTitle());
    }

    // ==================== VI. SOP: Trimming (Multi-day with worker tracking) ====================
    private void createTrimmingChecklist() {
        Task task = new Task();
        task.setTitle("Trimming Session");
        task.setDescription("Daily trimming session with individual worker performance tracking.");

        Checklist checklist = new Checklist();
        checklist.setName("Trimming Session");

        int displayOrder = 1;
        int questionOrder = 1;

        checklist.addItem(header("Daily Trimming Session:", displayOrder++, checklist));

        checklist.addItem(question(
                "Batch Code",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Session Date (YYYY-MM-DD)",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(header("Worker Performance (enter data for each worker):", 
                                displayOrder++, checklist));

        checklist.addItem(question(
                "Worker 1 Name",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Worker 1 Weight Trimmed (grams)",
                "DECIMAL",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Worker 2 Name (optional)",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Worker 2 Weight Trimmed (grams, optional)",
                "DECIMAL",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Worker 3 Name (optional)",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Worker 3 Weight Trimmed (grams, optional)",
                "DECIMAL",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Is this the final trimming session? (YES or NO)",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Notes",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        task.setChecklist(checklist);
        checklistRepo.save(checklist);
        taskRepo.save(task);

        System.out.println("✓ Seeded checklist and task: " + task.getTitle());
    }

    // ==================== VII. SOP: Packaging (Multi-day) ====================
    private void createPackagingChecklist() {
        Task task = new Task();
        task.setTitle("Packaging Session");
        task.setDescription("Daily packaging session - track bags packaged and trim waste.");

        Checklist checklist = new Checklist();
        checklist.setName("Packaging Session");

        int displayOrder = 1;
        int questionOrder = 1;

        checklist.addItem(header("Daily Packaging Session:", displayOrder++, checklist));

        checklist.addItem(question(
                "Batch Code",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Session Date (YYYY-MM-DD)",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(header("Bag Information:", displayOrder++, checklist));

        checklist.addItem(question(
                "Bag Size 1 (grams, e.g., 500, 1000, 2000)",
                "DECIMAL",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Number of bags of this size packaged today",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Bag Size 2 (grams, optional)",
                "DECIMAL",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Number of bags of this size packaged today (optional)",
                "INTEGER",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Partial/Uneven bag weight (grams, if applicable)",
                "DECIMAL",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Is this the final packaging session? (YES or NO)",
                "BOOLEAN_TEXT",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "If final session: Final weight of trim waste (grams)",
                "DECIMAL",
                questionOrder++, displayOrder++, checklist));

        checklist.addItem(question(
                "Notes",
                "TEXT",
                questionOrder++, displayOrder++, checklist));

        task.setChecklist(checklist);
        checklistRepo.save(checklist);
        taskRepo.save(task);

        System.out.println("✓ Seeded checklist and task: " + task.getTitle());
    }
}
