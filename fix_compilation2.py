import re
import os

base = 'app/src/test/java/com/browntowndev/liftlab'

filepath = os.path.join(base, 'core/domain/useCase/workoutLogging/HydrateLoggingWorkoutWithCompletedSetsUseCaseTest.kt')
with open(filepath, 'r') as f: content = f.read()

# Revert myLift back to lift, because we replaced global matches and broke local scopes.
content = content.replace('myLift', 'lift')

# The only place where lift clashes is usually inside a test.
# Let's just fix the method call explicitly.
content = re.sub(r'hydrateLoggingWorkoutWithCompletedSetsUseCase\(listOf\(lift\)', r'hydrateLoggingWorkoutWithCompletedSetsUseCase(listOf(lift)', content) # unchanged

with open(filepath, 'w') as f: f.write(content)
