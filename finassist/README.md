# FinAssist - personal finance assistant with AI agents

Java application with a Swing GUI and a command-line interface, no external libraries.

## 1. Prerequisites
* **JDK 11 or newer** (`javac -version` must work; tested with OpenJDK 17+). Swing is part of the JDK; the GUI needs a desktop (on a headless server use the CLI).
* A shell: `sh`/`bash` on Linux or macOS, or `build.bat` on Windows. `make` is optional.
* Optional, for real AI: an OpenAI API key and internet access. A microphone is optional (voice entry).

## 2. Dependency installation
None. The project uses only the JDK (`java.net.http`, `javax.swing`, `javax.sound.sampled`). Nothing to download; there is no Maven or Gradle step. The dependency list is intentionally empty so the build is reproducible with just a JDK.

## 3. Build and test
```
bash build.sh        # Linux/macOS: compiles src/ into out/ and runs the 45 self-checks ("All 45 checks passed.")
build.bat            # Windows equivalent
make                 # same as build.sh (make test / make clean also exist)
```
The build uses `bash build.sh` (not `./build.sh`) because some file systems mount the files without the execute bit.

## 4. Local configuration
Configuration is through environment variables only; no secrets are stored in the repository (`.env.example` shows the names, and `.gitignore` excludes `.env` and data files).

| Variable | Meaning | Default |
|---|---|---|
| `OPENAI_API_KEY` | Enables the real LLM (chat completions) and speech-to-text (whisper-1) | not set: offline stand-in AI and typed voice input |
| `OPENAI_MODEL` | Chat model name | `gpt-4o-mini` |

```
export OPENAI_API_KEY=sk-...          # Linux/macOS (your own key; never commit it)
set OPENAI_API_KEY=sk-...             # Windows cmd
```
Program options: `gui` (default) or `cli`; `--demo` loads safe sample data; `--data <file>` chooses the data file (default `finassist-data.ser` in the current folder, saved after every change).

## 5. Launch commands
```
java -cp out finassist.Main gui --demo     # GUI with sample data
java -cp out finassist.Main cli --demo     # command line with sample data
java -cp out finassist.Main gui            # GUI, your own data
java -cp out finassist.Main cli --data my-data.ser
```
Use `cli` instead of `gui` for the command line.

## 6. Safe sample data
`--demo` (see `Main.seed`) adds fictional data dated in the current month: salary 3000, five lunches (11-15), rent 900, groceries 120, a "Vacation" goal (2000 target, 500 saved, due in 6 months) and a Netflix subscription. It contains no real personal information. To start fresh, delete the data file. A captured example run is in `docs/sample/cli-session.txt`.

## 7. Example usage
GUI: tabs *Log entry*, *Dashboard*, *Goals*, *Subscriptions*, *AI coach*. Pick "Expense", enter amount, date, category and merchant, press submit; open *Dashboard*, choose month and year, press show.

CLI (menu numbers): `1` log expense, `2` log income, `3` voice entry, `4` dashboard, `5` AI monthly summary, `6` add goal, `7` goal summary, `8` savings advice, `9` add subscription, `10` remove subscription, `11` list subscriptions, `12` AI coach (empty line to leave), `0` exit.
```
$ java -cp out finassist.Main cli --demo
Choose: 4
Month (1-12): 11
Year: 2026
Income:        $3000.00
Expenses:      $1085.00
Profit:        $1915.00
Savings rate:  63.8%
...
```
Voice entry (`3`): speak, or without a key type a sentence such as `I spent 12.50 on coffee at Tim Hortons`; the program answers "I understood ... Save it? (yes/no)". Unusual expenses (amount above mean + 2 standard deviations of past ones) trigger an alert after saving. AI coach examples: `How much did I spend on food?`, `Can I afford my vacation goal?`.

## 8. Project layout
```
src/finassist/{model,repository,view,controller,facade,service,agent,strategy,observer,llm,exception,test}  Java sources
build.sh  build.bat  Makefile      build files (no dependencies)
docs/diagrams/                     UML class and sequence diagrams (PNG/SVG)
docs/sample/cli-session.txt        example CLI run with --demo
stage2-report.docx / .pdf          Stage 2 implementation report
```

## 9. Design patterns -> classes (package `finassist.*`)
| Pattern | Classes |
|---|---|
| MVC | View: `view.FinancialView`, `GUIView`, `CLIView`. Controller: `controller.FinancialController`. Model: `model.*`, `service.FinancialService`, `service.DashboardService` |
| Facade | `facade.FinAssistFacade` (hides FinancialService, DashboardService, SubscriptionService, AgentOrchestrator, VoiceInputService, SavingsAnalyzer, FinancialRepository) |
| Strategy | Context `service.SavingsAnalyzer`; `strategy.SavingsStrategy`, `GoalBasedSavingsStrategy`, `ExpenseBasedSavingsStrategy` |
| Observer | Subject `observer.FinancialDataSubject` (extended by `FinancialService`); `observer.FinancialObserver`; observers `DashboardService`, `SavingsAnalyzer`, `agent.ExpenseDetectionAgent` |
| Factory Method | Creator `agent.AgentCreator`; `ExpenseDetectionAgentCreator`, `FinancialCoachAgentCreator`; Product `agent.FinancialAgent` (`ExpenseDetectionAgent`, `FinancialCoachAgent`); client `AgentOrchestrator` |

## 10. Features -> code
| Feature | Where |
|---|---|
| F01 Voice logging | `VoiceInputService.captureSpeech / validateTranscript / convertToEntry`, `FinAssistFacade.logVoiceEntry`, `FinancialController.logVoiceEntry` (asks the user to confirm before saving) |
| F02 / F04 Expense and income | `FinancialService.addExpense / addIncome` (validation, save, `notifyObservers`) |
| F03 Unusual expense | `ExpenseDetectionAgent.update / detectUnusualExpense / analyzeExpense` (statistics in code, explanation from the LLM) |
| F05 End-of-month summary | `FinAssistFacade.getMonthlySummary` -> `DashboardService` + `AgentOrchestrator` -> `FinancialCoachAgent` |
| F06 Goal summary | `FinancialGoal.getProgress / getRemainingAmount / getRequiredMonthlySavings`, `FinAssistFacade.addGoal / getGoalSummary` |
| F07 Dashboard | `DashboardService.generateMonthlyDashboard` (refreshed through `update()`), shown by `showDashboard` |
| F08 Savings analyzer | `SavingsAnalyzer.selectStrategy / setStrategy / analyze` |
| F09 Subscriptions | `SubscriptionService`, `Subscription.getMonthlyCost / getAnnualCost` |
| F10 AI coach | `FinancialCoachAgent.process / prepareContext / generateAdvice` (keeps the conversation for follow-ups) |


The updated UML class diagram (generated from the compiled code) is `docs/diagrams/class_diagram_full.png`; the differences from the Stage 1 design are listed in section 4 of `stage2-report`. Helper classes: `OpenAILLMService`, `MockLLMService` (the two `LLMService` implementations), `ValidationException`, `LLMUnavailableException`, `DashboardFormatter`, `Stats`.
