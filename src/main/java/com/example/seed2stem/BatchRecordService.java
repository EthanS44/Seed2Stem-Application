package com.example.seed2stem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Batch records: turns the batch fields of a task run into a BatchRecordEntry,
 * validates it on submit, and applies it to a batch when a manager approves the
 * run. Form fields are named br_* so they never collide with checklist answers.
 */
@Service
public class BatchRecordService {

    static final String NEW_VARIETY = "__new";
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final Pattern VARIETY_CODE = Pattern.compile("^[A-Z]{2,3}$");
    private static final int MIN_MERV = 8;
    private static final double EPS = 0.0005;

    private final BatchRepository batchRepo;
    private final BatchRecordEntryRepository entryRepo;
    private final VarietyRepository varietyRepo;
    private final ChecklistRunRepository runRepo;
    private final ChecklistRunService runService;

    public BatchRecordService(BatchRepository batchRepo,
                              BatchRecordEntryRepository entryRepo,
                              VarietyRepository varietyRepo,
                              ChecklistRunRepository runRepo,
                              ChecklistRunService runService) {
        this.batchRepo = batchRepo;
        this.entryRepo = entryRepo;
        this.varietyRepo = varietyRepo;
        this.runRepo = runRepo;
        this.runService = runService;
    }

    /** YYMMDD-VVV-Room, e.g. 261008-ABC-B1. */
    public static String buildBatchCode(LocalDate date, String varietyCode, Room room) {
        return date.format(CODE_DATE) + "-" + varietyCode + "-" + room.name();
    }

    // ---------------------------------------------------------------- queries

    public List<Batch> allBatches() {
        return batchRepo.findAllByOrderByStartDateDescIdDesc();
    }

    public Optional<Batch> findBatch(Long id) {
        return batchRepo.findById(id);
    }

    public Optional<BatchRecordEntry> entryForRun(Long runId) {
        return entryRepo.findByRunId(runId);
    }

    public BatchTotals totalsFor(Batch batch) {
        return BatchTotals.of(entryRepo.findByBatchAndStatus(batch, BatchEntryStatus.APPROVED));
    }

    /**
     * The batch's running log: approved lines plus lines still awaiting approval,
     * including seeds/cloning entries that will join this batch number.
     */
    public List<BatchRecordEntry> logFor(Batch batch) {
        List<BatchRecordEntry> log = new ArrayList<>(entryRepo.findByBatchAndStatusInOrderByEntryDateAscRecordedAtAscIdAsc(
                batch, EnumSet.of(BatchEntryStatus.APPROVED, BatchEntryStatus.PENDING)));
        log.addAll(entryRepo.findByProposedBatchCodeAndBatchIsNullAndStatus(batch.getBatchCode(), BatchEntryStatus.PENDING));
        log.sort(Comparator.comparing(BatchRecordEntry::getEntryDate)
                .thenComparing(BatchRecordEntry::getRecordedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(BatchRecordEntry::getId));
        return log;
    }

    /** Seeds/cloning entries waiting for approval whose batch number doesn't exist yet, grouped by that number. */
    public Map<String, List<BatchRecordEntry>> pendingNewBatches() {
        Map<String, List<BatchRecordEntry>> byCode = new LinkedHashMap<>();
        for (BatchAction action : List.of(BatchAction.RECEIVE_SEEDS, BatchAction.CLONING)) {
            for (Room room : Room.values()) {
                for (BatchRecordEntry e : entryRepo.findByActionAndRoomAndStatusOrderByEntryDateDescRecordedAtDesc(
                        action, room, BatchEntryStatus.PENDING)) {
                    if (e.getBatch() != null || e.getProposedBatchCode() == null) continue;
                    if (batchRepo.findByBatchCode(e.getProposedBatchCode()).isPresent()) continue;
                    byCode.computeIfAbsent(e.getProposedBatchCode(), k -> new ArrayList<>()).add(e);
                }
            }
        }
        return byCode;
    }

    public FilterStatus filterStatus(Room room) {
        LocalDate last = entryRepo.findByActionAndRoomAndStatusOrderByEntryDateDescRecordedAtDesc(
                        BatchAction.FILTER_CHANGE, room, BatchEntryStatus.APPROVED)
                .stream().findFirst().map(BatchRecordEntry::getEntryDate).orElse(null);
        return new FilterStatus(room, last, LocalDate.now());
    }

    public Map<Room, FilterStatus> filterStatuses() {
        Map<Room, FilterStatus> map = new LinkedHashMap<>();
        for (Room room : Room.values()) map.put(room, filterStatus(room));
        return map;
    }

    // ------------------------------------------------------------- task form

    /**
     * Model attributes for the batch section of a task form. JS data is built
     * from plain maps/lists so Thymeleaf can serialize it without Jackson.
     */
    public Map<String, Object> formModel(Task task, ChecklistRun run) {
        BatchAction action = task.getBatchAction();
        LocalDate entryDate = entryDateFor(run);

        Map<String, List<Map<String, Object>>> batchesByRoom = new LinkedHashMap<>();
        for (Room room : Room.values()) {
            List<Map<String, Object>> options = new ArrayList<>();
            for (Batch b : batchRepo.findByRoomOrderByStartDateDescIdDesc(room)) {
                BatchTotals t = totalsFor(b);
                Map<String, Object> o = new LinkedHashMap<>();
                o.put("id", b.getId());
                o.put("code", b.getBatchCode());
                o.put("variety", b.getVariety().getName());
                o.put("stage", t.getStage());
                o.put("livePlants", t.getLivePlants());
                o.put("transferable", Math.max(0, t.getPlantsStarted() - t.getPlantsDestroyed() - t.getPlantsTransferred()));
                o.put("onHandStems", t.getOnHandKg(DestructionMaterial.STEMS));
                o.put("onHandFan", t.getOnHandKg(DestructionMaterial.FAN_LEAVES));
                o.put("onHandTrim", t.getOnHandKg(DestructionMaterial.TRIM));
                o.put("wetKg", t.getWetKg());
                o.put("fanKg", t.getFanLeafKg());
                o.put("stemKg", t.getStemKg());
                o.put("trimKg", t.getTrimKg());
                o.put("dryKg", t.getDryKg());
                options.add(o);
            }
            batchesByRoom.put(room.name(), options);
        }

        Map<String, String> varietyCodes = new LinkedHashMap<>();
        List<Variety> varieties = varietyRepo.findAllByOrderByNameAsc();
        for (Variety v : varieties) varietyCodes.put(String.valueOf(v.getId()), v.getCode());

        Map<String, Map<String, Object>> filters = new LinkedHashMap<>();
        for (FilterStatus fs : filterStatuses().values()) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("summary", fs.getSummary());
            f.put("state", fs.getState());
            filters.put(fs.getRoom().name(), f);
        }

        BatchRecordEntry entry = run != null ? entryRepo.findByRun(run).orElse(null) : null;
        if (entry == null) entry = new BatchRecordEntry();

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("batchAction", action);
        model.put("batchEntry", entry);
        model.put("rooms", Room.values());
        model.put("varieties", varieties);
        model.put("filterWorkOptions", FilterWork.values());
        model.put("materialOptions", DestructionMaterial.values());
        model.put("batchEntryDate", entryDate);
        model.put("batchCodeDate", entryDate.format(CODE_DATE));
        model.put("batchesByRoom", batchesByRoom);
        model.put("varietyCodes", varietyCodes);
        model.put("filterStatusByRoom", filters);
        model.put("newVarietyValue", NEW_VARIETY);
        return model;
    }

    /**
     * Saves whatever batch fields were typed, leniently, as a DRAFT. Used on
     * pause and as the first step of submit so a validation error never loses
     * the technician's input.
     */
    @Transactional
    public BatchRecordEntry saveDraft(Task task, ChecklistRun run, MultiValueMap<String, String> params) {
        BatchRecordEntry e = entryRepo.findByRun(run).orElseGet(BatchRecordEntry::new);
        e.setRun(run);
        e.setAction(task.getBatchAction());
        e.setStatus(BatchEntryStatus.DRAFT);
        e.setTechnician(run.getCompletedBy());
        e.setEntryDate(entryDateFor(run));
        e.setRecordedAt(LocalDateTime.now());
        e.setRoom(parseEnum(Room.class, params.getFirst("br_room")));

        if (task.getBatchAction().isCreatesBatch()) {
            e.setBatch(null);
            String varietyId = text(params.getFirst("br_varietyId"));
            if (NEW_VARIETY.equals(varietyId)) {
                e.setVariety(null);
                e.setNewVarietyName(text(params.getFirst("br_newVarietyName")));
                String code = text(params.getFirst("br_newVarietyCode"));
                e.setNewVarietyCode(code != null ? code.toUpperCase() : null);
            } else {
                e.setVariety(parseLong(varietyId) != null ? varietyRepo.findById(parseLong(varietyId)).orElse(null) : null);
                e.setNewVarietyName(null);
                e.setNewVarietyCode(null);
            }
            e.setProposedBatchCode(proposedCode(e));
        } else {
            Long batchId = parseLong(params.getFirst("br_batchId"));
            e.setBatch(batchId != null ? batchRepo.findById(batchId).orElse(null) : null);
            e.setProposedBatchCode(null);
        }

        e.setPlantCount(parseInt(params.getFirst("br_plantCount")));
        e.setWeightKg(parseKg(params.getFirst("br_weightKg")));
        e.setFanLeafKg(parseKg(params.getFirst("br_fanLeafKg")));
        e.setStemKg(parseKg(params.getFirst("br_stemKg")));
        e.setTrimKg(parseKg(params.getFirst("br_trimKg")));
        e.setFlowerTrimmedKg(parseKg(params.getFirst("br_flowerTrimmedKg")));
        e.setSource(text(params.getFirst("br_source")));
        e.setLotNumber(text(params.getFirst("br_lotNumber")));
        e.setFilterWork(parseEnum(FilterWork.class, params.getFirst("br_filterWork")));
        e.setFilterCount(parseInt(params.getFirst("br_filterCount")));
        e.setFilterRating(parseInt(params.getFirst("br_filterRating")));
        e.setMaterial(parseEnum(DestructionMaterial.class, params.getFirst("br_material")));
        e.setDestructionMethod(text(params.getFirst("br_destructionMethod")));
        e.setWitness(text(params.getFirst("br_witness")));
        e.setNotes(text(params.getFirst("br_notes")));
        return entryRepo.save(e);
    }

    // ------------------------------------------------- submit / approve / reject

    /**
     * Validates the run's batch entry and submits the run, in one transaction.
     * Throws {@link BatchRecordException} (and changes nothing) when invalid.
     */
    @Transactional
    public ChecklistRun submitRun(Long runId, List<ChecklistResponse> responses) {
        ChecklistRun run = runRepo.findById(runId)
                .orElseThrow(() -> new BatchRecordException("Task run not found."));
        BatchRecordEntry entry = entryRepo.findByRun(run)
                .orElseThrow(() -> new BatchRecordException("Fill in the batch record details."));

        validateForSubmit(entry);
        entry.setStatus(BatchEntryStatus.PENDING);
        entry.setRecordedAt(LocalDateTime.now());
        entryRepo.save(entry);

        return runService.submitWithResponses(runId, responses);
    }

    /**
     * Approves the run and writes its entry to the batch record, creating or
     * joining the batch for seeds/cloning. Rolls back both if a rule fails.
     */
    @Transactional
    public ChecklistRun approveRun(Long runId, User manager, String comments) {
        entryRepo.findByRunId(runId)
                .filter(e -> e.getStatus() == BatchEntryStatus.PENDING)
                .ifPresent(this::applyApproval);
        return runService.authorizeByManager(runId, manager, comments);
    }

    @Transactional
    public ChecklistRun rejectRun(Long runId, User manager, String comments) {
        ChecklistRun run = runService.rejectByManager(runId, manager, comments);
        entryRepo.findByRunId(runId).ifPresent(e -> {
            e.setStatus(BatchEntryStatus.REJECTED);
            entryRepo.save(e);
        });
        return run;
    }

    void validateForSubmit(BatchRecordEntry e) {
        BatchAction action = e.getAction();
        require(e.getRoom() != null, "Choose a room.");

        if (action.isCreatesBatch()) {
            Variety variety = checkVariety(e);
            if (variety != null) {
                e.setVariety(variety);
                e.setNewVarietyName(null);
                e.setNewVarietyCode(null);
            }
            String code = buildBatchCode(e.getEntryDate(),
                    variety != null ? variety.getCode() : e.getNewVarietyCode(), e.getRoom());
            e.setProposedBatchCode(code);
            checkJoinable(code, action);

            if (action == BatchAction.RECEIVE_SEEDS) {
                require(positive(e.getPlantCount()), "Enter the seed count.");
                require(e.getSource() != null, "Enter the supplier or source.");
                require(e.getLotNumber() != null, "Enter the lot number.");
            } else {
                require(positive(e.getPlantCount()), "Enter the number of clones cut.");
                require(e.getSource() != null, "Enter the mother plant or source batch.");
            }
            return;
        }

        Batch batch = e.getBatch();
        require(batch != null, "Choose a batch.");
        require(batch.getRoom() == e.getRoom(),
                "Batch " + batch.getBatchCode() + " isn't in room " + e.getRoom().getDisplayName() + ".");

        switch (action) {
            case TRANSFER_TO_FLOWER -> require(positive(e.getPlantCount()), "Enter the number of plants moved.");
            case FILTER_CHANGE -> {
                require(e.getFilterWork() != null, "Choose whether the filters were cleaned or changed.");
                require(positive(e.getFilterCount()), "Enter the number of filters.");
                require(e.getFilterRating() != null && e.getFilterRating() >= MIN_MERV,
                        "Filter rating must be MERV " + MIN_MERV + " or higher.");
            }
            case HARVEST -> {
                require(positive(e.getPlantCount()), "Enter the number of plants cut down.");
                require(positive(e.getWeightKg()), "Enter the wet weight in kg.");
            }
            case DEBUCK_DESTEM -> {
                require(positive(e.getPlantCount()), "Enter the number of plants processed.");
                require(nonNegative(e.getFanLeafKg()), "Enter the fan leaf weight in kg (0 if none).");
                require(nonNegative(e.getStemKg()), "Enter the stem weight in kg (0 if none).");
            }
            case TRIMMING -> {
                require(nonNegative(e.getTrimKg()), "Enter the trim weight in kg (0 if none).");
                require(nonNegative(e.getFlowerTrimmedKg()), "Enter the amount of flower trimmed in kg.");
            }
            case DRY_WEIGHT -> require(positive(e.getWeightKg()), "Enter the dried flower weight in kg.");
            case DESTRUCTION -> {
                require(e.getMaterial() != null, "Choose the material type.");
                require(positive(e.getWeightKg()), "Enter the weight destroyed in kg.");
                if (e.getMaterial() == DestructionMaterial.WHOLE_PLANTS) {
                    require(positive(e.getPlantCount()), "Enter the number of plants destroyed.");
                }
                require(e.getDestructionMethod() != null, "Enter the destruction method.");
                require(e.getWitness() != null, "Enter the witness.");
            }
            default -> { }
        }
        checkCaps(e, true);
    }

    private void applyApproval(BatchRecordEntry e) {
        if (e.getAction().isCreatesBatch()) {
            Variety variety = e.getVariety() != null ? e.getVariety() : createVariety(e);
            String code = buildBatchCode(e.getEntryDate(), variety.getCode(), e.getRoom());
            BatchSource source = e.getAction() == BatchAction.RECEIVE_SEEDS ? BatchSource.SEEDS : BatchSource.CLONES;
            Batch batch = batchRepo.findByBatchCode(code).orElse(null);
            if (batch == null) {
                batch = new Batch();
                batch.setBatchCode(code);
                batch.setVariety(variety);
                batch.setRoom(e.getRoom());
                batch.setSource(source);
                batch.setStartDate(e.getEntryDate());
                batch.setCreatedAt(LocalDateTime.now());
                batch = batchRepo.save(batch);
            } else if (batch.getSource() != source) {
                throw new BatchRecordException("Batch " + code + " already exists as a "
                        + batch.getSource().getDisplayName().toLowerCase() + " batch.");
            }
            e.setVariety(variety);
            e.setNewVarietyName(null);
            e.setNewVarietyCode(null);
            e.setProposedBatchCode(code);
            e.setBatch(batch);
        } else {
            checkCaps(e, false);
        }
        e.setStatus(BatchEntryStatus.APPROVED);
        entryRepo.save(e);
    }

    /**
     * Plant and material limits. On submit, other entries still awaiting
     * approval also count against the limit so two technicians can't claim the
     * same plants; on approval only approved entries count.
     */
    private void checkCaps(BatchRecordEntry e, boolean includePending) {
        Batch batch = e.getBatch();
        if (batch == null) return;
        BatchTotals approved = totalsFor(batch);
        List<BatchRecordEntry> pending = includePending
                ? entryRepo.findByBatchAndStatus(batch, BatchEntryStatus.PENDING).stream()
                        .filter(p -> !p.getId().equals(e.getId())).toList()
                : List.of();
        String code = batch.getBatchCode();
        int plants = e.getPlantCount() != null ? e.getPlantCount() : 0;

        switch (e.getAction()) {
            case TRANSFER_TO_FLOWER -> {
                int left = approved.getPlantsStarted() - approved.getPlantsDestroyed()
                        - approved.getPlantsTransferred() - pendingPlants(pending, BatchAction.TRANSFER_TO_FLOWER);
                require(plants <= left, "Only " + Math.max(0, left) + " plants in " + code + " are left to transfer.");
            }
            case HARVEST -> {
                int left = approved.getLivePlants() - pendingPlants(pending, BatchAction.HARVEST);
                require(plants <= left, "Only " + Math.max(0, left) + " plants in " + code + " are left to harvest.");
            }
            case DESTRUCTION -> {
                DestructionMaterial m = e.getMaterial();
                if (m == DestructionMaterial.WHOLE_PLANTS) {
                    int left = approved.getLivePlants() - pending.stream()
                            .filter(p -> p.getAction() == BatchAction.DESTRUCTION && p.getMaterial() == m)
                            .mapToInt(p -> p.getPlantCount() != null ? p.getPlantCount() : 0).sum();
                    require(plants <= left, "Only " + Math.max(0, left) + " live plants are recorded in " + code + ".");
                } else {
                    double left = approved.getOnHandKg(m) - pending.stream()
                            .filter(p -> p.getAction() == BatchAction.DESTRUCTION && p.getMaterial() == m)
                            .mapToDouble(p -> p.getWeightKg() != null ? p.getWeightKg() : 0.0).sum();
                    require(e.getWeightKg() <= left + EPS, "Only " + String.format("%.3f", Math.max(0.0, left))
                            + " kg of " + m.getDisplayName().toLowerCase() + " is on hand for " + code + ".");
                }
            }
            default -> { }
        }
    }

    private static int pendingPlants(List<BatchRecordEntry> pending, BatchAction action) {
        return pending.stream().filter(p -> p.getAction() == action)
                .mapToInt(p -> p.getPlantCount() != null ? p.getPlantCount() : 0).sum();
    }

    /** Returns the existing variety to use, or null when a valid new one will be created on approval. */
    private Variety checkVariety(BatchRecordEntry e) {
        if (e.getVariety() != null) return e.getVariety();
        String name = e.getNewVarietyName();
        String code = e.getNewVarietyCode();
        require(name != null || code != null, "Choose a variety, or add a new one.");
        require(name != null, "Enter the new variety's name.");
        require(code != null && VARIETY_CODE.matcher(code).matches(), "Variety code must be 2 or 3 letters.");

        Optional<Variety> byName = varietyRepo.findByNameIgnoreCase(name);
        if (byName.isPresent()) {
            require(byName.get().getCode().equals(code), byName.get().getName() + " already exists with code "
                    + byName.get().getCode() + ". Pick it from the list.");
            return byName.get();
        }
        varietyRepo.findByCode(code).ifPresent(v -> {
            throw new BatchRecordException("Code " + code + " is already used by " + v.getName() + ".");
        });
        return null;
    }

    private Variety createVariety(BatchRecordEntry e) {
        Variety existing = checkVariety(e);
        if (existing != null) return existing;
        return varietyRepo.save(new Variety(e.getNewVarietyName(), e.getNewVarietyCode()));
    }

    private void checkJoinable(String code, BatchAction action) {
        BatchSource source = action == BatchAction.RECEIVE_SEEDS ? BatchSource.SEEDS : BatchSource.CLONES;
        batchRepo.findByBatchCode(code).ifPresent(b -> require(b.getSource() == source,
                "Batch " + code + " already exists as a " + b.getSource().getDisplayName().toLowerCase() + " batch."));
    }

    private String proposedCode(BatchRecordEntry e) {
        String code = e.getVariety() != null ? e.getVariety().getCode() : e.getNewVarietyCode();
        if (code == null || e.getRoom() == null || !VARIETY_CODE.matcher(code).matches()) return null;
        return buildBatchCode(e.getEntryDate(), code, e.getRoom());
    }

    private static LocalDate entryDateFor(ChecklistRun run) {
        return run != null && run.getStartTime() != null ? run.getStartTime().toLocalDate() : LocalDate.now();
    }

    // ---------------------------------------------------------------- parsing

    private static void require(boolean ok, String message) {
        if (!ok) throw new BatchRecordException(message);
    }

    private static boolean positive(Integer v) { return v != null && v > 0; }
    private static boolean positive(Double v) { return v != null && v > 0; }
    private static boolean nonNegative(Double v) { return v != null && v >= 0; }

    private static String text(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static Long parseLong(String v) {
        try { return v == null || v.isBlank() ? null : Long.valueOf(v.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private static Integer parseInt(String v) {
        try { return v == null || v.isBlank() ? null : Integer.valueOf(v.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    /** kg rounded to 3 decimals, matching the B300. */
    private static Double parseKg(String v) {
        try { return v == null || v.isBlank() ? null : BatchTotals.round3(Double.parseDouble(v.trim())); }
        catch (NumberFormatException e) { return null; }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String v) {
        try { return v == null || v.isBlank() ? null : Enum.valueOf(type, v.trim()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
