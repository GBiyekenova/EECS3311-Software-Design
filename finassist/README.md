# FinAssist (Java 11+, no external libraries)

## Build and run
```
sh build.sh                # compiles to out/ and runs the 45 self-checks
java -cp out finassist.Main gui --demo      # window, with sample data
java -cp out finassist.Main cli --demo      # command line
```
Options: `--demo` (sample data), `--data file` (default `finassist-data.ser`, data is saved there after every change).
Set `OPENAI_API_KEY` (and optionally `OPENAI_MODEL`, default gpt-4o-mini) to use OpenAI GPT and the speech-to-text model.
Without a key an offline stand-in answers and voice entry becomes "type the sentence you would say".

## Design patterns -> classes (package `finassist.*`)
| Pattern | Classes |
|---|---|
| MVC | View: `view.FinancialView`, `GUIView`, `CLIView`. Controller: `controller.FinancialController`. Model: `model.*`, `service.FinancialService`, `service.DashboardService` |
| Facade | `facade.FinAssistFacade` (hides FinancialService, DashboardService, SubscriptionService, AgentOrchestrator, VoiceInputService, SavingsAnalyzer, FinancialRepository) |
| Strategy | Context `service.SavingsAnalyzer`; `strategy.SavingsStrategy`, `GoalBasedSavingsStrategy`, `ExpenseBasedSavingsStrategy` |
| Observer | Subject `observer.FinancialDataSubject` (extended by `FinancialService`); `observer.FinancialObserver`; observers `DashboardService`, `SavingsAnalyzer`, `agent.ExpenseDetectionAgent` |
| Factory Method | Creator `agent.AgentCreator`; `ExpenseDetectionAgentCreator`, `FinancialCoachAgentCreator`; Product `agent.FinancialAgent` (`ExpenseDetectionAgent`, `FinancialCoachAgent`); client `AgentOrchestrator` |

## Features -> code
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

## Differences from the class diagram (add them to the diagram)
* `FinancialView` also declares `showDashboard` and `getUserInput`.
* `FinancialController`: added `addGoal`, `removeSubscription`, `requestSubscriptions`, `requestMonthlySummary`, `setView`; constructor takes the facade.
* `FinAssistFacade`: added `addGoal`, `getMonthlySummary`, `getSubscriptionSummary`, `getPendingVoiceEntry`, `confirmVoiceEntry`, `setView`.
* `ExpenseDetectionAgent`: added `setView` (shows alerts itself) and `hasEnoughHistory`. `VoiceInputService`: `setFallbackInput`.
* `MonthlySummary` has extra attributes (category totals, unusual expenses, subscription cost, goals, interpretation).
* `Date` is `java.time.LocalDate`. Helper classes not in the diagram: `OpenAILLMService`, `MockLLMService` (the two `LLMService` implementations), `ValidationException`, `LLMUnavailableException`, `DashboardFormatter`, `Stats`.
