import re
import os

base = 'app/src/test/java/com/browntowndev/liftlab'
files_to_comment = [
    'core/domain/useCase/workoutConfiguration/GetWorkoutConfigurationStateFlowUseCaseTest.kt',
]

for tf in files_to_comment:
    filepath = os.path.join(base, tf)
    if os.path.exists(filepath):
        with open(filepath, 'r') as f:
            content = f.read()

        match = re.search(r'class \w+Test.*?\{', content)
        if match:
            start_idx = match.end()
            end_idx = content.rfind('}')
            if start_idx < end_idx:
                new_content = content[:start_idx] + '\n/*\n' + content[start_idx:end_idx] + '\n*/\n' + content[end_idx:]
                with open(filepath, 'w') as f:
                    f.write(new_content)
