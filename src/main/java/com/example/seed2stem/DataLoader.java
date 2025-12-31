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
        //createProductionAreaInspection();

        // add more tasks/checklists here
    }

    private void createProductionAreaInspection() {
        Task task = new Task();
        task.setTitle("Production Area Inspection");
        task.setDescription("This task is to be performed every day first thing every morning and last thing every afternoon.");

        Checklist checklist = new Checklist();
        checklist.setName("Production Area Inspection");

        ChecklistItem checklistItem1 = new ChecklistItem();
        checklistItem1.setQuestion("Are all lights working in lit rooms? If no, record which lights are defective.");
        checklistItem1.setResponseType("BOOLEAN_TEXT");
        checklistItem1.setItemOrder(1);
        checklistItem1.setChecklist(checklist);
        checklist.addItem(checklistItem1);

        ChecklistItem checklistItem2 = new ChecklistItem();
        checklistItem2.setQuestion("Are all fans working and pointed in the right direction? If no, record which fans are defective.");
        checklistItem2.setResponseType("BOOLEAN_TEXT");
        checklistItem2.setItemOrder(2);
        checklistItem2.setChecklist(checklist);
        checklist.addItem(checklistItem2);

        ChecklistItem checklistItem3 = new ChecklistItem();
        checklistItem3.setQuestion("Record humidity level. (%)");
        checklistItem3.setResponseType("INTEGER");
        checklistItem3.setItemOrder(3);
        checklistItem3.setChecklist(checklist);
        checklist.addItem(checklistItem3);

        ChecklistItem checklistItem4 = new ChecklistItem();
        checklistItem4.setQuestion("Record temperature level. (°C)");
        checklistItem4.setResponseType("INTEGER");
        checklistItem4.setItemOrder(4);
        checklistItem4.setChecklist(checklist);
        checklist.addItem(checklistItem4);

        ChecklistItem checklistItem5 = new ChecklistItem();
        checklistItem5.setQuestion("Check nutrient levels of jugs at tables 1 to 5. Are all levels the same? Record nutrient levels.");
        checklistItem5.setResponseType("BOOLEAN_TEXT");
        checklistItem5.setItemOrder(5);
        checklistItem5.setChecklist(checklist);
        checklist.addItem(checklistItem5);

        ChecklistItem checklistItem6 = new ChecklistItem();
        checklistItem6.setQuestion("Walk down each isle and check for:");
        checklistItem6.setResponseType("NONE");
        checklistItem6.setItemOrder(6);
        checklistItem6.setChecklist(checklist);
        checklist.addItem(checklistItem6);

        ChecklistItem checklistItem7 = new ChecklistItem();
        checklistItem7.setQuestion("Are there any light plants? If yes, record location and number.");
        checklistItem7.setResponseType("BOOLEAN_TEXT");
        checklistItem7.setItemOrder(7);
        checklistItem7.setChecklist(checklist);
        checklist.addItem(checklistItem7);

        ChecklistItem checklistItem8 = new ChecklistItem();
        checklistItem8.setQuestion("Are there any missing emitters? If yes, record location and number.");
        checklistItem8.setResponseType("BOOLEAN_TEXT");
        checklistItem8.setItemOrder(8);
        checklistItem8.setChecklist(checklist);
        checklist.addItem(checklistItem8);

        ChecklistItem checklistItem9 = new ChecklistItem();
        checklistItem9.setQuestion("Is there any pooling water? If yes, record location and estimate of size of pool.");
        checklistItem9.setResponseType("BOOLEAN_TEXT");
        checklistItem9.setItemOrder(9);
        checklistItem9.setChecklist(checklist);
        checklist.addItem(checklistItem9);

        ChecklistItem checklistItem10 = new ChecklistItem();
        checklistItem10.setQuestion("Are there any wilting leaves? If yes, remove them.");
        checklistItem10.setResponseType("NONE");
        checklistItem10.setItemOrder(10);
        checklistItem10.setChecklist(checklist);
        checklist.addItem(checklistItem10);

        ChecklistItem checklistItem11 = new ChecklistItem();
        checklistItem11.setQuestion("Ensure tables are all in proper alignment and foot stools are located at front of tables.");
        checklistItem11.setResponseType("NONE");
        checklistItem11.setItemOrder(11);
        checklistItem11.setChecklist(checklist);
        checklist.addItem(checklistItem11);

        task.setChecklist(checklist);

        checklistRepo.save(checklist);
        taskRepo.save(task);

        System.out.println("Seeded checklist and task: " + task.getTitle());
    }
}
