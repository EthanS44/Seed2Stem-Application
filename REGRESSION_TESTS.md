# Seed2Stem Regression Test Checklist

Manual test plan covering all the features built/touched in recent work.
Tick the boxes (`[x]`) as you go. Aim to run on **at least two device classes**:
laptop browser + phone browser. Where mobile-specific behavior matters,
it's flagged 📱.

Recommended browser/device matrix:
- **Laptop:** Chrome (or your daily) at full size, then resized to ~768 px
- **Tablet:** any tablet at portrait orientation
- **Phone:** iPhone (Safari) **and** Android (Chrome) — Chrome on Android
  may refuse plain HTTP from LAN; if so, just use Safari to verify mobile

Test accounts to keep handy (from current DB):
| Role | Email | Password |
|---|---|---|
| Developer | `d` | (your dev pw) |
| Manager | `m` | (your mgr pw) |
| Technician | `t` | (your tech pw) |
| Developer | `triton` | (your other dev pw) |

---

## 1. Authentication

### 1.1 Login
- [ ] Open `/auth/login` — page renders, logo visible, no console errors
- [ ] Login with valid existing user (e.g. `t` / correct pw) → lands on home dashboard
- [ ] Login with valid email format (e.g. `don@tritongrp.ca`) → succeeds
- [ ] Login with **wrong password** → bounced back to login with "Invalid email or password"
- [ ] Login with **unknown email** → same "Invalid email or password" (doesn't leak which is wrong)
- [ ] Login with email that's mixed-case (e.g. `Goldbud`) → still works (server lowercases)
- [ ] Hit a protected URL (`/dashboard/task-dashboard`) when logged out → redirects to login
- [ ] After successful login, browser back button does NOT re-show login form with credentials

### 1.2 Register
- [ ] Open `/auth/register`
- [ ] Register with a **valid email** + matching passwords + name → "Registration pending" screen
- [ ] Register with **non-email string** (e.g. `foo`) → server returns "Please enter a valid email address"
- [ ] Register with **mismatched passwords** → "Passwords do not match"
- [ ] Register with **email that already exists** as a user → "An account with this email already exists"
- [ ] Register with **email that already has a pending request** → "A registration request for this email is already pending"
- [ ] Email is **lowercased + trimmed** when stored (verify via developer "Registration Requests" page)

### 1.3 Forgot Password / Password Reset (developer flow)
- [ ] Open `/auth/forgot-password`
- [ ] Submit email + correct first/last name → "Submitted" screen
- [ ] Submit email + wrong name → "We couldn't find an account matching that information"
- [ ] Submit email that doesn't exist → same generic error (doesn't leak existence)
- [ ] Logged in as **developer**: open Password Reset Requests page → request is listed
- [ ] Type a new password → click Approve → request status flips to Approved
- [ ] Log in as that user with the new password → succeeds
- [ ] Reject a pending password reset → status flips to Denied

### 1.4 Logout
- [ ] Click Logout → returns to login with "Logged out successfully" flash
- [ ] Back button no longer reaches dashboard

---

## 2. Role-Based Access Control

### 2.1 Sidebar visibility
- [ ] As **TECHNICIAN**: sidebar shows Home, Tasks, Calendar, Time Clock, Settings, Logout. Does **not** show Management or Developer sections.
- [ ] As **MANAGER**: sidebar adds Batches + Technicians under Management
- [ ] As **DEVELOPER**: sidebar adds Developer section (User Created Tasks, Registration Requests, Password Reset Requests, Standard Tasks)

### 2.2 Direct URL access denial
- [ ] Technician hits `/developer/standard-tasks` → redirects to home (not 500)
- [ ] Manager hits `/developer/registration-requests` → redirects to home
- [ ] Technician hits `/admin/export.xlsx` → 403
- [ ] Manager hits `/admin/export.xlsx` → 403
- [ ] Technician hits `/technicians` → redirects to home

---

## 3. Dashboards

### 3.1 Technician dashboard
- [ ] Renders without error
- [ ] Stat cards show correct counts
- [ ] "Clocked In" status reflects current clock state

### 3.2 Manager dashboard
- [ ] Renders without error
- [ ] **No** Export to Excel button (Manager doesn't get export)
- [ ] Pending Approvals count = number of PENDING checklist runs

### 3.3 Developer dashboard
- [ ] Renders without error
- [ ] **Export to Excel** button visible (top-right)
- [ ] Click Export → downloads `seed2stem-export-YYYY-MM-DD.xlsx`
- [ ] Open the file → has 5 sheets: Tasks, Checklist Runs, Checklist Responses, Task Pauses, Time Entries
- [ ] No sheet titled "Users" or anything similar; no emails appear inside

### 3.4 Task dashboard
- [ ] **As technician:** Active Tasks shows only own in-progress runs
- [ ] **As manager:** Active Tasks shows **all** in-progress runs across techs, each row shows technician name + Active/Paused badge
- [ ] **As developer:** same all-active view as manager
- [ ] On manager/dev view, **own** runs are clickable as resume links; **others'** runs are clickable as read-only progress view
- [ ] Pending Approvals section visible only to manager/developer
- [ ] Available Tasks list shows non-user-created (seeded + developer-built) tasks

---

## 4. Standard Task Creation (Developer)

- [ ] Open Standard Tasks page → existing standard tasks listed
- [ ] Click "New Standard Task" button on that page
- [ ] Page renders: Task Name, Description, SOP file input, Add Header / Add Question buttons
- [ ] Add a Header → row appears with text input
- [ ] Add a Question → row appears with text + response-type dropdown
- [ ] Dropdown shows: **Pass/Fail, Text, Number, Checkbox** (NOT "None" — should be renamed to "Checkbox")
- [ ] Remove button on any row removes that row
- [ ] Submit with **no items** → error "At least one checklist item is required"
- [ ] Submit with **no title** → error
- [ ] Submit with title + items, **no PDF** → success; new task in list; SOP not attached
- [ ] Submit with valid PDF (`.pdf` extension, content-type `application/pdf`, real PDF bytes) → success
- [ ] Submit with `.exe` file → rejected with error
- [ ] Submit with file > 10 MB → rejected with "SOP file is too large" friendly message
- [ ] Submit with a renamed text file as `.pdf` (bad magic bytes) → "Uploaded SOP is not a valid PDF document"

---

## 5. Task View (with SOP) + Start Gate

For a task with an SOP attached:
- [ ] Open the task from the task dashboard
- [ ] Description shown
- [ ] SOP section labeled "Standard Operating Procedure" with "Open in new tab" button
- [ ] PDF renders inline, page width fits the container
- [ ] "Open in new tab" link opens the raw PDF
- [ ] Start Task button is **disabled** initially (greyed out)
- [ ] Hint "Scroll to the bottom of the SOP to enable Start Task" visible below
- [ ] Tapping disabled Start button → click is intercepted, container scrolls back to top of SOP
- [ ] Scroll the SOP container to the bottom → Start button **enables**; hint disappears
- [ ] Multi-page PDF: each page rendered as a canvas, all stacked vertically
- [ ] Single-page PDF that fits the container → button enables immediately on load (no scroll needed)

For a task **without** an SOP:
- [ ] No SOP section rendered
- [ ] Start Task button is enabled immediately

---

## 6. Checklist Run Lifecycle (Technician)

### 6.1 Start + fill out
- [ ] Click Start Task → checklist screen renders
- [ ] Headers shown as section dividers
- [ ] Each question type renders correctly:
  - [ ] Pass/Fail → PASS/FAIL buttons + optional text
  - [ ] Text → textarea
  - [ ] Number → numeric input
  - [ ] Checkbox (formerly NONE) → checkbox with "Mark complete" label

### 6.2 Pause with reason
- [ ] Click Pause → modal appears
- [ ] Type a reason (e.g., "Lunch break") → click Pause Task
- [ ] Redirected to task dashboard
- [ ] Task appears in Active Tasks list with **Paused** badge (yellow)

### 6.3 Resume
- [ ] Click the paused task from Active Tasks → returns to checklist
- [ ] Previously-filled answers are **pre-populated**
- [ ] (Behind the scenes) the pause is closed on resume

### 6.4 Submit
- [ ] Try to submit with a **required Pass/Fail unanswered** → blocked, redirected back to resume with error
- [ ] Try to submit with a **Checkbox-type question unchecked** → blocked with "Please check: \<item>"
- [ ] Try to submit with **numeric field empty** → blocked
- [ ] Fill everything correctly → submit → run status changes to PENDING; dashboard updates

### 6.5 User-created task (no checklist, just work log)
- [ ] As technician, click "Create Task" → fill title + description
- [ ] Lands on the In Progress page (single textarea for work log)
- [ ] Pause with reason → modal works the same way
- [ ] Submit → goes to PENDING for manager review

---

## 7. Manager Review

- [ ] Pending Approvals shows submitted runs
- [ ] Click a pending run → review page shows
  - [ ] Tech name, start/end time, duration
  - [ ] Each question + answer grouped by section header
  - [ ] Checkbox-type items show green ✓ icon next to label (if checked)
  - [ ] Approve/Reject buttons visible
- [ ] Type manager comments + Approve → status flips to APPROVED; comment saved
- [ ] On another run, Reject with comment → status REJECTED; comment saved
- [ ] Approved/Rejected runs no longer in Pending Approvals

---

## 8. Manager Active-Task View (read-only)

(Tester accounts: log in as a tech first and start a task, fill some answers, pause with a reason; then log in as manager.)

- [ ] On Task Dashboard's Active Tasks, click another tech's active row
- [ ] Lands on the review-style page showing:
  - [ ] **IN PROGRESS** blue badge (not PENDING)
  - [ ] If currently paused, **yellow "Currently paused" banner** with reason + start time
  - [ ] Saved responses so far (only items the tech has filled in via Save/Pause)
  - [ ] No Approve/Reject buttons (those are PENDING-only)
- [ ] End Time is hidden (run not finished)
- [ ] If tech has not paused yet, "Technician hasn't saved any progress yet" hint shown

---

## 9. Time Clock + Timelines

- [ ] Open Time Clock → current state shown (clocked out)
- [ ] Click Clock In → status flips, time recorded
- [ ] Click Clock Out → status flips back, total hours calculated
- [ ] Time Clock History shows past entries
- [ ] As manager/dev, open a technician's profile (`/technicians/<id>`)
- [ ] Profile shows tech info, recent completed runs, time entries
- [ ] Click a time entry → time entry detail page renders
- [ ] Timeline bar shows colored segments: green=task, yellow=pause, grey=downtime
- [ ] Tooltips on segments show label + duration + start–end
- [ ] Pause segments show reason ("Pause: Lunch break")
- [ ] Summary cards at bottom show totals (Task, Pause, Downtime, Total Shift)
- [ ] Manager edits a time entry (clock in/out times) → saved; totals recalc

---

## 10. Batches (if used)

- [ ] Open Batches list → renders
- [ ] Create new batch → succeeds
- [ ] Open batch detail → status updates work (Planted → Vegetative → etc.)
- [ ] Activity log appends entries

---

## 11. Calendar

- [ ] Open Calendar → loads without error
- [ ] Events appear on the right dates
- [ ] Filter / legend toggles work

---

## 12. Settings

- [ ] Open Settings → account info displayed (first/last name, email, account type)
- [ ] **Email** field shows (not "Username")
- [ ] Change Password form:
  - [ ] Wrong current password → "Current password is incorrect"
  - [ ] Blank new password → "New password cannot be blank"
  - [ ] Correct + new password → success; log out, log in with new password works

---

## 13. Data Export (Developer-only)

- [ ] As **developer**: dashboard → click Export to Excel → file downloads
- [ ] Open file in Excel/Numbers/LibreOffice
  - [ ] **Tasks** sheet has rows for every task; columns: Task ID, Title, Description, Checklist Name, Item Count, User-Created, Created By
  - [ ] **Checklist Runs** sheet has rows for every run with technician name (not email)
  - [ ] **Checklist Responses** sheet has rows of question + answer
  - [ ] **Task Pauses** sheet has rows with reason + start/end/duration
  - [ ] **Time Entries** sheet has rows with technician name + hours
  - [ ] Ctrl-F any user's email — should NOT be found anywhere in the workbook
- [ ] As **manager**: navigate to `/admin/export.xlsx` directly → 403
- [ ] As **technician**: navigate to `/admin/export.xlsx` directly → 403

---

## 14. 📱 Mobile-Specific Layout

Open the app on a phone (or Chrome DevTools → iPhone SE 375×667).

### 14.1 Topbar
- [ ] Topbar is **one row** (not wrapped to two)
- [ ] Hamburger icon visible top-left
- [ ] Brand text "Triton Cannabis Group" is **hidden** on phone
- [ ] Logo visible top-right
- [ ] Topbar background white, ~56 px tall

### 14.2 Sidebar drawer
- [ ] Tap hamburger → drawer **slides in** from the left
- [ ] Backdrop appears (dim grey over content)
- [ ] Drawer background is **green** (not white/transparent)
- [ ] All nav links visible (Home, Tasks, etc.) — color contrast OK
- [ ] **Tap a nav link → page navigates** and drawer closes (this was the broken case)
- [ ] Tap the X close button → drawer closes
- [ ] Tap the grey backdrop area → drawer closes
- [ ] Open drawer, tap a section heading (non-link) → drawer stays open (correct — they're not links)

### 14.3 Content layout
- [ ] Top of page content is **visible without scrolling** (no need to scroll past empty space)
- [ ] Sidebar margin is gone — content fills full width
- [ ] Dashboards: summary tiles show **2 per row** (not 1)
- [ ] Tables horizontally scroll within their container without breaking the page
- [ ] Forms (login, register, settings) usable; inputs full-width

### 14.4 SOP viewer on phone
- [ ] Open a task with SOP on phone
- [ ] SOP viewer is **shorter** (~60% of viewport height, not 700 px)
- [ ] PDF page width fits the viewer
- [ ] Scroll-to-bottom gate still works (Start enables after scroll)
- [ ] "Open in new tab" still works

### 14.5 Pass/Fail buttons on phone
- [ ] Open a standard task on phone, fill in a Pass/Fail question
- [ ] PASS and FAIL buttons are **side-by-side on one row**, sharing the width

### 14.6 Timeline page on phone
- [ ] Open a time entry detail on phone
- [ ] Timeline bar **scrolls horizontally** within its own container
- [ ] Breakdown table renders without breaking layout
- [ ] Summary cards readable (font may shrink, padding tighter)

### 14.7 Resize back to desktop
- [ ] Drag browser ≥ 768 px → hamburger disappears, brand text reappears, sidebar pinned to left
- [ ] Layout matches what laptops/tablets see (no visual surprises)

---

## 15. Cross-Cutting

- [ ] Browser console clear of red errors on every page (warnings OK)
- [ ] No raw stack traces or Whitelabel Error Pages user-facing
- [ ] Refresh any deep URL (e.g. `/checklist-runs/runs/123`) while logged in → page renders correctly (no session drop)
- [ ] Refresh same URL while **not** logged in → redirected to login
- [ ] Hit the URL of an old task that no longer exists → graceful redirect (not 500)

---

## 16. Server-Side Smoke

- [ ] `./mvnw test -Dtest='!Seed2StemApplicationTests'` → green, 256+ tests pass
- [ ] App boots without exception (check `/tmp/s2s-app.log` or your terminal)
- [ ] DB column types correct (`sop_data` is `bytea` not `oid`, `email` exists in users/registration_request/password_reset_request)

---

## Sign-off

| Tester | Date | Browser/Device | All green? |
|---|---|---|---|
|  |  | Chrome desktop |  |
|  |  | Safari iPhone |  |
|  |  | Chrome Android |  |
|  |  | iPad |  |

If anything fails, note the test number + what you saw and we'll fix it.
