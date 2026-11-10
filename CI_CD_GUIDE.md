# Τελική εργασία 3688 – GitHub / GitHub Actions (CI/CD)

Το έργο βασίζεται **αυτούσιο** στην πρώτη εργασία (REST υπηρεσία μητρώου πολιτών). Έχουν προστεθεί μόνο ο αγωγός CI/CD, ένας βοηθητικός παραγωγός αναφορών και οδηγίες υποβολής. Δεν απαιτείται Docker, εξωτερική βάση δεδομένων, προσωπικό access token μέσα στον κώδικα ή απομακρυσμένος server.

## 1. Τι περιέχει το CI/CD workflow

Το αρχείο βρίσκεται στο **`.github/workflows/ci.yml`**. Εκτελείται **αυτόματα μόνο σε push (commits) προς τον κλάδο `develop`**. Δεν υπάρχει trigger για `main`, pull requests ή χειροκίνητη εκτέλεση.

1. Checkout από το αποθετήριο (`actions/checkout`).
2. JDK 17 Temurin και cache Maven dependencies (`actions/setup-java`).
3. Compilation / packaging μέσω `mvn --batch-mode --no-transfer-progress clean package -DskipTests`.
4. Unit tests μέσω `mvn --batch-mode --no-transfer-progress test` (JUnit 5 / Maven Surefire).
5. API/integration tests μέσω `mvn --batch-mode --no-transfer-progress verify` (Rest-Assured / Maven Failsafe). Η φάση `verify` εκτελεί ξανά και τα Surefire tests, επιπλέον των API tests, όπως ορίζει ο τυπικός κύκλος ζωής του Maven.
6. Δημιουργία δύο φιλικών προς ανάγνωση HTML αναφορών από τα πραγματικά XML αποτελέσματα και εμφάνιση συνοπτικών μετρήσεων στο **Job Summary**.
7. **Upload** δύο ξεχωριστών GitHub Actions Artifacts με τις XML αναφορές, τα τυχόν TXT logs και την αντίστοιχη HTML αναφορά:
   - `unit-test-reports`
   - `api-integration-test-reports`

Οι αρχικές JUnit XML αναφορές δημιουργούνται αυτόματα στους φακέλους:

- `citizen-service/target/surefire-reports/TEST-*.xml`
- `citizen-integration-tests/target/failsafe-reports/TEST-*.xml`

Τα GitHub Artifacts διατηρούνται για **30 ημέρες** (εκτός και αν οι ρυθμίσεις του αποθετηρίου επιβάλλουν μικρότερο όριο). Το workflow δεν προσθέτει ψεύτικα αποτελέσματα: αν δεν δημιουργηθούν οι απαιτούμενες XML αναφορές, ο έλεγχος επαλήθευσης αποτυγχάνει.

## 2. Δημιουργία δημόσιου GitHub αποθετηρίου

1. Κάνουμε σύνδεση στο https://github.com/ και επιλέγουμε **New repository** (https://github.com/new).
2. Όνομα: `citizen-registry-ci-cd` (ενδεικτικό, μπορούμε να βάλουμε ότι όνομα θέλουμε).
3. Επιλέγουμε **Public**.
4. **Δεν** επιλέγουμε δημιουργία README, `.gitignore` ή License από το GitHub, επειδή υπάρχουν ήδη τα αρχεία στο ZIP.
5. Κάνουμε **Create repository**.
6. Αποσυμπιέζουμε το `citizen-registry-github-ready.zip`. Μετά την αποσυμπίεση, ανοίγουμε PowerShell **μέσα στον φάκελο `citizen-registry`** (εκεί που βρίσκεται το κεντρικό `pom.xml`).

Εκτελούμε, αντικαθιστώντας το `YOUR_GITHUB_USERNAME` με το πραγματικό GitHub username μας π.χ. mdaramas:

```powershell
git init -b main
git add .
git commit -m "Initial citizen registry project with CI pipeline"
git remote add origin https://github.com/mdaramas/citizen-registry-ci-cd.git
git push -u origin main

git switch -c develop
git push -u origin develop
```

**Σημαντικό:** Στο πρώτο push του `main` ο αγωγός **δεν** θα εκτελεστεί, όπως ζητά η εκφώνηση. Με το `git push -u origin develop` ο αγωγός θα εκτελεστεί αυτόματα για πρώτη φορά. Στη συνέχεια, για οποιαδήποτε νέα αλλαγή:

```powershell
git switch develop
git add .
git commit -m "Update citizen registry CI configuration"
git push origin develop
```

Αν δεν έχουμε πραγματοποιήσει κάποια αλλαγή αλλά θέλουμε ένα νέο push για να πάρουμε ένα screenshot την εκτέλεση, μπορούμρ να κάνουμε **ένα κενό commit**:

```powershell
git switch develop
git commit --allow-empty -m "Run CI on develop"
git push origin develop
```

### Αν χρειαστεί να ορίσουμε στοιχεία για τα Git commits

```powershell
git config --global user.name "Your Name"
git config --global user.email "your-email@example.com"
```

## 3. Έλεγχος επιτυχίας στο GitHub Actions

1. Ανοίγουμε το δημόσιο repository στο GitHub.
2. Πηγαίνουμε στην καρτέλα **Actions**.
3. Επιλέγουμε το workflow **Citizen Registry CI**.
4. Ανοίγουμε το πιο πρόσφατο run που ενεργοποιήθηκε από push στο `develop`.
5. Επιβεβαιώνουμε ότι το job **Build, unit tests and REST API tests** ολοκληρώθηκε με πράσινη ένδειξη επιτυχίας.
6. Στο Job Summary εμφανίζεται πίνακας με τις μετρήσεις των unit και API/integration tests.
7. Στην ενότητα **Artifacts** της σελίδας του run κατεβάζουμε **και τα δύο** αρχεία:
   - `unit-test-reports` (ZIP που δημιουργεί το GitHub)
   - `api-integration-test-reports` (ZIP που δημιουργεί το GitHub)
8. Αποσυμπιέζουμε **κάθε artifact σε ξεχωριστό φάκελο** του υπολογιστή μας, π.χ. `C:\CI-Reports\unit` και `C:\CI-Reports\api`.
9. Παίρνουμε ένα screenshot από το επιτυχές run, όπου φαίνονται ο κλάδος `develop`, το πράσινο status και το Job Summary / Artifacts. Έπειτα το αποθηκεύουμε ως `pipeline-screenshot.png`. Αν δεν φαίνονται όλα αυτά σε ένα screenshot, παίρνουμε και ένα δεύτερο και το ονομάζουμε `pipeline-screenshot2.png`.


## 4. Οι δύο σύνδεσμοι που απαιτεί η εργασία

**URL 1 – δημόσιο repository:**

```text
https://github.com/mdaramas/citizen-registry-ci-cd
```

**URL 2 – αρχείο GitHub Actions στον κλάδο develop:**

```text
https://github.com/mdaramas/citizen-registry-ci-cd/blob/develop/.github/workflows/ci.yml
```

## 5. Δημιουργία του τελικού ZIP για το eClass

Δεν υποβάλουμε το ZIP του κώδικα ως το τελικό ZIP της δεύτερης εργασίας. Η εκφώνηση ζητά μόνο:

```text
Emmanouil_Daramas.zip
├── links.txt
├── pipeline-screenshot.png
├── unit-test-reports/
│   ├── TEST-....xml
│   └── unit-tests.html
└── api-integration-test-reports/
    ├── TEST-....xml
    └── api-integration-tests.html
```

Το script `scripts/create-eclass-zip.ps1` δημιουργεί αυτόματα αυτή τη δομή. Παράδειγμα εκτέλεσης από PowerShell μέσα στο project:

```powershell
.\scripts\create-eclass-zip.ps1 `
  -GitHubUsername "mdaramas" `
  -Repository "citizen-registry-ci-cd" `
  -ScreenshotPath "C:\CI-Reports\pipeline-screenshot.png", "C:\CI-Reports\pipeline-screenshot2.png" `
  -UnitReportsPath "C:\CI-Reports\unit" `
  -ApiReportsPath "C:\CI-Reports\api"
```

Το αποτέλεσμα είναι το αρχείο `eclass-output\Emmanouil_Daramas.zip`. 

## 6. Τοπικοί έλεγχοι (προαιρετικά)

Έχοντας JDK 17 και Maven εγκατεστημένα, από τη ρίζα του έργου:

```powershell
mvn clean verify
python scripts/make_test_reports.py
```

Το `mvn verify` εκτελεί τόσο τα unit tests όσο και τα API/integration tests. Οι δύο αναφορές HTML δημιουργούνται στον φάκελο `ci-reports` (αγνοείται από το Git).