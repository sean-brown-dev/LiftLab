import os
import re

base = 'app/src/test/java/com/browntowndev/liftlab'

def fix_use_case_tests():
    files = [
        'core/domain/useCase/workoutLogging/HydrateLoggingWorkoutWithCompletedSetsUseCaseTest.kt',
        'core/domain/useCase/workoutLogging/HydrateLoggingWorkoutWithExistingLiftDataUseCaseTest.kt',
        'core/domain/useCase/workoutLogging/HydrateLoggingWorkoutWithPartiallyCompletedSetsUseCaseTest.kt',
        'core/domain/useCase/workoutLogging/UpsertExistingSetResultUseCaseTest.kt',
        'core/domain/useCase/progression/MyoRepSetGoalValidatorTests.kt',
        'core/domain/useCase/progression/WaveLoadingProgressionCalculatorTests.kt'
    ]
    for tf in files:
        filepath = os.path.join(base, tf)
        if not os.path.exists(filepath): continue
        with open(filepath, 'r') as f: content = f.read()

        content = re.sub(r'hadInitialWeightRecommendation\s*=\s*(true|false)\s*,?', '', content)
        content = re.sub(r'repRangePlaceholder\s*=\s*"[^"]*"\s*,?', '', content)
        content = re.sub(r'(?<!initial)weightRecommendation\s*=\s*([^,]+),', r'initialWeightRecommendation = \1, weightRecommendation = \1,', content)

        if 'HydrateLoggingWorkoutWithCompletedSetsUseCaseTest' in tf:
            content = re.sub(r'hydrateLoggingWorkoutWithCompletedSetsUseCase\(listOf\(([^)]+)\),\s*emptyList\(\),\s*microCycle\s*=\s*\d+,\s*\d+\)', r'hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(\1), emptyList())', content)
            content = re.sub(r'hydrateLoggingWorkoutWithCompletedSetsUseCase\(listOf\(([^)]+)\),\s*emptyList\(\),\s*microCycle\s*=\s*\d+,\s*programDeloadWeek\s*=\s*null\)', r'hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(\1), emptyList())', content)
            content = re.sub(r'hydrateLoggingWorkoutWithCompletedSetsUseCase\(listOf\(([^)]+)\),\s*emptyList\(\),\s*microCycle\s*=\s*\d+\)', r'hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(\1), emptyList())', content)
            content = re.sub(r'hydrateLoggingWorkoutWithCompletedSetsUseCase\(listOf\(([^)]+)\),\s*emptyList\(\),\s*microCycle\s*=\s*\d+,\s*programDeloadWeek\s*=\s*4\)', r'hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(\1), emptyList())', content)

            content = re.sub(r'val lift = LoggingWorkoutLift', r'val myLift = LoggingWorkoutLift', content)
            content = re.sub(r'hydrateLoggingWorkoutWithCompletedSetsUseCase\(listOf\(lift\)', r'hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(myLift)', content)

        with open(filepath, 'w') as f: f.write(content)

fix_use_case_tests()

def fix_viewmodels():
    viewmodel_tests = [
        'LabViewModelTest.kt',
        'LiftDetailsViewModelTest.kt',
        'LiftLibraryViewModelTest.kt',
        'WorkoutBuilderViewModelTest.kt',
        'WorkoutHistoryViewModelTest.kt'
    ]

    for tf in viewmodel_tests:
        filepath = os.path.join(base, 'ui/viewmodels', tf)
        if not os.path.exists(filepath): continue
        with open(filepath, 'r') as f: content = f.read()

        if 'LabViewModelTest.kt' in filepath:
            content = re.sub(
                r'val viewModel = LabViewModel\(\s*getProgramsUseCase = getProgramsUseCase,\s*getActiveProgramUseCase = getActiveProgramUseCase,\s*deleteProgramUseCase = deleteProgramUseCase,\s*setActiveProgramUseCase = setActiveProgramUseCase,\s*dispatchers = dispatchers\s*\)',
                r'val viewModel = LabViewModel(getProgramsUseCase = getProgramsUseCase, getActiveProgramUseCase = getActiveProgramUseCase, deleteProgramUseCase = deleteProgramUseCase, setActiveProgramUseCase = setActiveProgramUseCase, generateProgramUseCase = mockk(), saveAsNewProgramUseCase = mockk(), dispatchers = dispatchers)',
                content)

        if 'LiftDetailsViewModelTest.kt' in filepath:
            content = re.sub(
                r'val viewModel = LiftDetailsViewModel\(\s*getLiftUseCase = getLiftUseCase,\s*updateLiftUseCase = updateLiftUseCase,\s*getProgramsUseCase = getProgramsUseCase,\s*getWorkoutsUseCase = getWorkoutsUseCase,\s*liftMetricChartDataCalculator = mockk\(\),\s*dispatchers = dispatchers\s*\)',
                r'val viewModel = LiftDetailsViewModel(getLiftUseCase = getLiftUseCase, updateLiftUseCase = updateLiftUseCase, getProgramsUseCase = getProgramsUseCase, getWorkoutsUseCase = getWorkoutsUseCase, liftMetricChartDataCalculator = mockk(), onMergeLift = mockk(), dispatchers = dispatchers)',
                content)

        if 'LiftLibraryViewModelTest.kt' in filepath:
            content = re.sub(
                r'val viewModel = LiftLibraryViewModel\(\s*getAllLiftsUseCase = getAllLiftsUseCase,\s*createCustomLiftUseCase = createCustomLiftUseCase,\s*hideLiftUseCase = hideLiftUseCase,\s*restoreLiftUseCase = restoreLiftUseCase,\s*deleteCustomLiftUseCase = deleteCustomLiftUseCase,\s*dispatchers = dispatchers\s*\)',
                r'val viewModel = LiftLibraryViewModel(getAllLiftsUseCase = getAllLiftsUseCase, createCustomLiftUseCase = createCustomLiftUseCase, hideLiftUseCase = hideLiftUseCase, restoreLiftUseCase = restoreLiftUseCase, deleteCustomLiftUseCase = deleteCustomLiftUseCase, mergeLiftsUseCase = mockk(), mergeLiftId = -1L, dispatchers = dispatchers)',
                content)

        if 'WorkoutHistoryViewModelTest.kt' in filepath:
            content = content.replace('invoke("any")', 'invoke(any<Long>())')
            content = content.replace('invoke(any())', 'invoke(any<Long>())')

        if 'WorkoutBuilderViewModelTest.kt' in filepath:
            content = re.sub(r'deleteWorkoutLiftUseCase\(any\(\)\)', r'deleteWorkoutLiftUseCase(any<Long>(), any())', content)
            content = re.sub(r'onWorkoutLiftDeleted\(any\(\)\)', r'onWorkoutLiftDeleted(any<Long>(), any())', content)

        with open(filepath, 'w') as f: f.write(content)

fix_viewmodels()


def fix_calculator_tests():
    calculator_tests = [
        'DoubleProgressionCalculatorTests.kt',
        'DynamicDoubleProgressionCalculatorTests.kt',
        'LinearProgressionCalculatorTests.kt',
        'WaveLoadingProgressionCalculatorTests.kt'
    ]
    for tf in calculator_tests:
        filepath = os.path.join(base, 'core/domain/useCase/progression', tf)
        if not os.path.exists(filepath): continue
        with open(filepath, 'r') as f: content = f.read()

        content = re.sub(r',\s*microCycle\s*=\s*\d+', r'', content)
        content = re.sub(r'microCycle\s*=\s*\d+\s*,?', r'', content)
        with open(filepath, 'w') as f: f.write(content)

fix_calculator_tests()


def fix_other_issues():
    f1 = os.path.join(base, 'core/domain/useCase/workoutConfiguration/UpdateWorkoutLiftUseCaseTest.kt')
    if os.path.exists(f1):
        with open(f1, 'r') as f: content = f.read()
        content = re.sub(r'updateWorkoutLiftUseCase\(workoutLift\)', r'updateWorkoutLiftUseCase(workoutLift, programDeloadWeek = 4)', content)
        with open(f1, 'w') as f: f.write(content)

    f2 = os.path.join(base, 'core/domain/useCase/workoutLogging/CancelWorkoutUseCaseTest.kt')
    if os.path.exists(f2):
        with open(f2, 'r') as f: content = f.read()
        content = re.sub(r'CancelWorkoutUseCase\(\s*restTimerInProgressRepository = mockk\(\)', r'CancelWorkoutUseCase(transactionScope = mockk(), restTimerInProgressRepository = mockk()', content)
        with open(f2, 'w') as f: f.write(content)

    f3 = os.path.join(base, 'core/domain/useCase/workoutLogging/GetWorkoutStateFlowUseCaseTest.kt')
    if os.path.exists(f3):
        with open(f3, 'r') as f: content = f.read()
        content = re.sub(r'hydrateLoggingWorkoutWithCompletedSetsUseCase\(any\(\),\s*any\(\),\s*any\(\),\s*any\(\)\)', r'hydrateLoggingWorkoutWithCompletedSetsUseCase(any(), any())', content)
        with open(f3, 'w') as f: f.write(content)

    f4 = os.path.join(base, 'core/data/local/repositories/WorkoutLiftsRepositoryImplTest.kt')
    if os.path.exists(f4):
        with open(f4, 'r') as f: content = f.read()
        content = re.sub(r'val repository = WorkoutLiftsRepositoryImpl\(\s*workoutLiftDao = workoutLiftDao,\s*workoutRepository = workoutRepository,\s*customLiftDao = customLiftDao\s*\)', r'val repository = WorkoutLiftsRepositoryImpl(workoutLiftDao = workoutLiftDao, workoutRepository = workoutRepository, customLiftDao = customLiftDao, syncScheduler = mockk())', content)
        with open(f4, 'w') as f: f.write(content)

fix_other_issues()
