# Seed2Stem - New Batch Tracking Features

## Overview
This document describes the new batch tracking system added to Seed2Stem for managing cannabis cultivation from clone propagation through packaging.

## Database Schema

### Core Tables

#### 1. `batch` (Enhanced)
Tracks batch lifecycle with auto-generated batch codes.

**Key Fields:**
- `batch_code`: Unique code in format `YY-DDD-AAA-RR-316-CC`
  - YY = Year (2 digits)
  - DDD = Day of year (001-365)
  - AAA = Strain acronym
  - RR = Room (B1/B2/B3)
  - 316 = Constant
  - CC = Crop number (auto-incremented per year)
- `year`, `day_of_year`, `strain_name`, `strain_acronym`, `destination_room`
- `crop_number`: Auto-assigned sequential number (resets each year)
- `number_of_clones`: Initial clone count
- `status`: Current stage (CLONING, VEGETATIVE, FLOWERING, HARVESTED, etc.)

#### 2. `trigger_record` 
Records when plants are moved to flowering stage.

**Fields:**
- `batch_id`: Foreign key to batch
- `trigger_date`: Date of triggering
- `number_of_plants`: Plant count
- `performed_by_user_id`: Employee who performed action
- `checklist_run_id`: Link to checklist execution

#### 3. `destruction_record`
Tracks product destruction (live plants or dry product).

**Fields:**
- `batch_id`: Foreign key to batch
- `destruction_date`: Date of destruction
- `destruction_type`: Enum (LIVE_PLANTS or DRY_PRODUCT)
- `number_of_plants`: Used when type is LIVE_PLANTS
- `weight_in_grams`: Used when type is DRY_PRODUCT
- `performed_by_user_id`: Employee
- `reason`: Text explanation

#### 4. `harvest_record`
Records harvest data.

**Fields:**
- `batch_id`: Foreign key to batch
- `harvest_date`: Date of harvest
- `number_of_plants_harvested`: Plant count
- `total_weight_in_grams`: Total wet weight
- `coa_sample_weight_in_grams`: Certificate of Analysis sample weight
- `performed_by_user_id`: Employee
- `notes`: Additional information

#### 5. `debucking_session` (Multi-day)
Daily debucking sessions that accumulate until completion.

**Fields:**
- `batch_id`: Foreign key to batch
- `session_date`: Date of this session
- `weight_debucked_in_grams`: Weight processed this session
- `performed_by_user_id`: Employee
- `is_completed`: Boolean flag for final session
- `notes`: Session notes

**Total Weight Calculation:** Sum of all sessions for a batch

#### 6. `trimming_session` (Multi-day with worker tracking)
Daily trimming sessions with individual worker performance.

**Fields:**
- `batch_id`: Foreign key to batch
- `session_date`: Date of this session
- `is_completed`: Boolean flag for final session
- `notes`: Session notes

Related table: `trimming_worker_entry`
- `trimming_session_id`: Foreign key
- `worker_user_id`: Employee
- `weight_trimmed_in_grams`: Weight trimmed by this worker

**Performance Tracking:** Query by worker to see individual/team performance

#### 7. `packaging_session` (Multi-day)
Daily packaging sessions tracking bag sizes and counts.

**Fields:**
- `batch_id`: Foreign key to batch
- `session_date`: Date of this session
- `performed_by_user_id`: Employee
- `is_completed`: Boolean flag for final session
- `trim_waste_weight_in_grams`: Final trim waste (only on last session)
- `notes`: Session notes

Related table: `packaging_entry`
- `packaging_session_id`: Foreign key
- `bag_size_in_grams`: Size of bags (e.g., 500, 1000, 2000)
- `number_of_bags`: Count of full bags
- `partial_bag_weight_in_grams`: Weight of last uneven bag

**Total Weight Calculation:** Sum of (bag_size × number_of_bags) + partial_bag_weight across all entries

## Batch Lifecycle

```
1. CLONING (Clone Propagation)
   ↓
2. VEGETATIVE (optional stage)
   ↓
3. FLOWERING (after Trigger)
   ↓
4. HARVESTED (after Harvest)
   ↓
5. DEBUCKING (multi-day process)
   ↓
6. TRIMMING (multi-day process with worker tracking)
   ↓
7. PACKAGING (multi-day process)
   ↓
8. PACKAGED (final state)

Alt: DESTROYED (can happen at any stage)
```

## Auto-Generated Batch Codes

The system automatically generates unique batch codes when creating a new batch.

**Example:**
```
26-045-LPP-B2-316-03

Where:
- 26 = Year 2026
- 045 = 45th day of year (February 14)
- LPP = Strain acronym (e.g., "Lemon Pie Pound")
- B2 = Destination room
- 316 = Constant identifier
- 03 = 3rd crop of 2026
```

**Crop Number Logic:**
- Automatically increments per year (1, 2, 3, ...)
- Resets to 1 at start of new year
- Handled by `BatchService.createBatch()`

## Checklists (SOPs)

### I. Clone Propagation
**Data Collected:**
- Day of year (1-365)
- Year (2 digits)
- Strain name
- Strain acronym (3 letters)
- Destination room (B1, B2, B3)
- Number of clones cut

**System Actions:**
- Auto-generates crop number
- Creates batch with unique batch code
- Sets status to CLONING

### II. Trigger
**Data Collected:**
- Batch code
- Date
- Number of plants
- Employee name

**System Actions:**
- Updates batch status to FLOWERING
- Records trigger event

### III. Destruction of Product
**Data Collected:**
- Batch code
- Type: LIVE_PLANTS or DRY_PRODUCT
- Number of plants (if live) OR weight (if dry)
- Reason for destruction

**System Actions:**
- Records destruction event
- Updates batch status to DESTROYED

### IV. Harvest
**Data Collected:**
- Batch code
- Date of harvest
- Number of plants harvested
- Total weight (grams)
- CoA sample weight (grams)

**System Actions:**
- Updates batch status to HARVESTED
- Records harvest data

### V. Debucking (Multi-day)
**Data Collected per session:**
- Batch code
- Session date
- Weight debucked today (grams)
- Is final session? (YES/NO)

**System Actions:**
- Creates new session record
- Accumulates total weight across all sessions
- When completed: Updates batch status to DEBUCKING complete

### VI. Trimming (Multi-day with workers)
**Data Collected per session:**
- Batch code
- Session date
- Worker names and individual weights
- Is final session? (YES/NO)

**System Actions:**
- Creates session with worker entries
- Tracks individual worker performance
- Accumulates total weight across all sessions
- Enables performance reports by worker

### VII. Packaging (Multi-day)
**Data Collected per session:**
- Batch code
- Session date
- Bag sizes and counts (e.g., 500g × 10 bags, 1000g × 5 bags)
- Partial bag weight (for last uneven bag)
- Is final session? (YES/NO)
- If final: Trim waste weight (grams)

**System Actions:**
- Creates packaging entries for each bag type
- Calculates total weight: (size × count) + partial
- Records trim waste on final session
- Updates batch status to PACKAGED when complete

## Usage Examples

### Creating a New Batch (Clone Propagation)

```java
// In a controller or service
@Autowired
private BatchService batchService;

public Batch createNewBatch(ClonePropagationRequest request) {
    // Service automatically:
    // - Gets next crop number for the year
    // - Generates batch code
    // - Sets created date and status
    
    Batch batch = batchService.createBatch(
        26,                    // year (2026)
        45,                    // day of year
        "Lemon Pie Pound",     // strain name
        "LPP",                 // strain acronym
        "B2",                  // destination room
        150                    // number of clones
    );
    
    // Returns: Batch with code "26-045-LPP-B2-316-03"
    return batch;
}
```

### Recording a Trigger Event

```java
@Autowired
private TriggerRecordRepository triggerRepo;
@Autowired
private BatchService batchService;
@Autowired
private UserRepository userRepo;

public TriggerRecord recordTrigger(String batchCode, int numberOfPlants, Long userId) {
    Batch batch = batchService.findByBatchCode(batchCode);
    User employee = userRepo.findById(userId).orElseThrow();
    
    TriggerRecord trigger = new TriggerRecord(
        batch,
        LocalDate.now(),
        numberOfPlants,
        employee
    );
    
    triggerRepo.save(trigger);
    
    // Update batch status
    batchService.updateBatchStatus(batchCode, BatchStatus.FLOWERING);
    
    return trigger;
}
```

### Multi-day Debucking

```java
@Autowired
private DebuckingSessionRepository debuckingRepo;

// Day 1
public void debuckingDay1(String batchCode, double weight, Long userId) {
    Batch batch = batchService.findByBatchCode(batchCode);
    User employee = userRepo.findById(userId).orElseThrow();
    
    DebuckingSession session = new DebuckingSession(
        batch,
        LocalDate.now(),
        weight,  // e.g., 1500.5 grams
        employee
    );
    session.setIsCompleted(false);
    
    debuckingRepo.save(session);
}

// Day 2 (final)
public void debuckingFinal(String batchCode, double weight, Long userId) {
    Batch batch = batchService.findByBatchCode(batchCode);
    User employee = userRepo.findById(userId).orElseThrow();
    
    DebuckingSession session = new DebuckingSession(
        batch,
        LocalDate.now(),
        weight,  // e.g., 800.3 grams
        employee
    );
    session.setIsCompleted(true);  // Mark as final
    
    debuckingRepo.save(session);
    
    // Get total weight across all sessions
    Double totalWeight = debuckingRepo.calculateTotalWeightDebucked(batch);
    System.out.println("Total debucked: " + totalWeight + "g");
}
```

### Trimming with Worker Tracking

```java
@Autowired
private TrimmingSessionRepository trimmingRepo;

public void recordTrimmingSession(String batchCode, 
                                  Map<Long, Double> workerWeights,
                                  boolean isFinal) {
    Batch batch = batchService.findByBatchCode(batchCode);
    
    TrimmingSession session = new TrimmingSession(batch, LocalDate.now());
    
    // Add worker entries
    for (Map.Entry<Long, Double> entry : workerWeights.entrySet()) {
        User worker = userRepo.findById(entry.getKey()).orElseThrow();
        TrimmingWorkerEntry workerEntry = new TrimmingWorkerEntry(
            worker,
            entry.getValue()  // weight trimmed
        );
        session.addWorkerEntry(workerEntry);
    }
    
    session.setIsCompleted(isFinal);
    trimmingRepo.save(session);
    
    // Session total
    System.out.println("Today's total: " + session.getTotalWeightTrimmed() + "g");
}

// Performance report
public Map<String, Double> getWorkerPerformance(String batchCode) {
    Batch batch = batchService.findByBatchCode(batchCode);
    List<Object[]> results = workerEntryRepo.calculateWorkerPerformanceByBatch(batch);
    
    Map<String, Double> performance = new HashMap<>();
    for (Object[] row : results) {
        User worker = (User) row[0];
        Double totalWeight = (Double) row[1];
        performance.put(worker.getName(), totalWeight);
    }
    return performance;
}
```

### Packaging with Multiple Bag Sizes

```java
@Autowired
private PackagingSessionRepository packagingRepo;

public void recordPackaging(String batchCode, Long userId, 
                           List<BagInfo> bags, 
                           Double trimWaste,
                           boolean isFinal) {
    Batch batch = batchService.findByBatchCode(batchCode);
    User employee = userRepo.findById(userId).orElseThrow();
    
    PackagingSession session = new PackagingSession(batch, LocalDate.now(), employee);
    
    // Add bag entries
    for (BagInfo bag : bags) {
        PackagingEntry entry = new PackagingEntry(
            bag.sizeInGrams,        // e.g., 500, 1000, 2000
            bag.numberOfBags,       // e.g., 10
            bag.partialWeight       // e.g., 350.5 (for last uneven bag)
        );
        session.addEntry(entry);
    }
    
    if (isFinal) {
        session.setIsCompleted(true);
        session.setTrimWasteWeightInGrams(trimWaste);
        batchService.updateBatchStatus(batchCode, BatchStatus.PACKAGED);
    }
    
    packagingRepo.save(session);
    
    System.out.println("Packaged today: " + session.getTotalWeightPackaged() + "g");
}
```

## Querying and Reporting

### Get Batch History
```java
// Get all events for a batch
Batch batch = batchService.findByBatchCode("26-045-LPP-B2-316-03");

TriggerRecord trigger = triggerRepo.findByBatch(batch).get(0);
HarvestRecord harvest = harvestRepo.findByBatch(batch).get(0);
List<DebuckingSession> debuckingSessions = debuckingRepo.findByBatchOrderBySessionDateAsc(batch);
List<TrimmingSession> trimmingSessions = trimmingRepo.findByBatchOrderBySessionDateAsc(batch);
List<PackagingSession> packagingSessions = packagingRepo.findByBatchOrderBySessionDateAsc(batch);
```

### Weight Tracking Throughout Lifecycle
```java
// Track weight changes through the process
Double harvestWeight = harvest.getTotalWeightInGrams();
Double debuckedWeight = debuckingRepo.calculateTotalWeightDebucked(batch);
Double trimmedWeight = trimmingRepo.calculateTotalWeightTrimmed(batch);
Double packagedWeight = packagingRepo.calculateTotalWeightPackaged(batch);

// Calculate losses
Double debuckingLoss = harvestWeight - debuckedWeight;
Double trimmingLoss = debuckedWeight - trimmedWeight;
```

## Response Types in Checklists

The checklists use these response types:
- `INTEGER`: Whole numbers
- `DECIMAL`: Numbers with decimals (for weights)
- `TEXT`: Free text (names, notes, dates as strings)
- `BOOLEAN_TEXT`: Yes/No with optional text explanation
- `NONE`: No response needed (informational items)

## Key Design Decisions

1. **Multi-day Tasks**: Debucking, Trimming, and Packaging are implemented as multiple session records that accumulate data, allowing the task to span several days.

2. **Worker Performance**: Trimming tracks individual workers within each session, enabling both individual and team performance reports.

3. **Flexible Packaging**: Supports multiple bag sizes per session and handles partial/uneven bags.

4. **Batch Code Auto-generation**: Ensures unique, sequential batch codes that reset yearly.

5. **Status Tracking**: Batch status field enables tracking lifecycle stage and filtering by stage.

6. **Audit Trail**: All records link to users (performedBy) and can link to ChecklistRun for full audit trail.

## Next Steps

1. **Controllers**: Create REST endpoints for each operation
2. **Frontend**: Build UI for data entry forms
3. **Reports**: Create dashboards showing batch status, worker performance, yield calculations
4. **Validation**: Add business logic validation (e.g., can't trigger a batch that's already flowering)
5. **Notifications**: Alert when multi-day tasks are incomplete
6. **Analytics**: Calculate yield percentages, loss ratios, worker efficiency metrics

## Files Created

### Entities
- `Batch.java` (enhanced)
- `BatchStatus.java` (enhanced)
- `TriggerRecord.java`
- `DestructionRecord.java`
- `DestructionType.java`
- `HarvestRecord.java`
- `DebuckingSession.java`
- `TrimmingSession.java`
- `TrimmingWorkerEntry.java`
- `PackagingSession.java`
- `PackagingEntry.java`

### Repositories
- `BatchRepository.java`
- `TriggerRecordRepository.java`
- `DestructionRecordRepository.java`
- `HarvestRecordRepository.java`
- `DebuckingSessionRepository.java`
- `TrimmingSessionRepository.java`
- `TrimmingWorkerEntryRepository.java`
- `PackagingSessionRepository.java`
- `PackagingEntryRepository.java`

### Services
- `BatchService.java`

### Data Loaders
- `NewSOPDataLoader.java` (creates all checklist templates)

---

**Total: 21 new/modified files** ready for integration with your Spring Boot application!
