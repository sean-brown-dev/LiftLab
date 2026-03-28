import re
import os

base = 'app/src/test/java/com/browntowndev/liftlab'

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

def fix_usecase():
    filepath = os.path.join(base, 'core/domain/useCase/workoutLogging/HydrateLoggingWorkoutWithCompletedSetsUseCaseTest.kt')
    if not os.path.exists(filepath): return
    with open(filepath, 'r') as f: content = f.read()

    # Revert all conflicting local vals if there are any duplicate lines
    # It's safer to just run a regex that finds duplicate consecutive lines for LoggingWorkoutLift and removes one.
    lines = content.split('\n')
    new_lines = []
    prev_line = ""
    for line in lines:
        if line.strip().startswith('val lift = LoggingWorkoutLift(') and line.strip() == prev_line.strip():
            continue
        new_lines.append(line)
        prev_line = line
    content = '\n'.join(new_lines)

    with open(filepath, 'w') as f: f.write(content)

fix_usecase()
