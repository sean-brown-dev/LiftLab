#!/bin/bash
find app/src/main/java/com/browntowndev/liftlab/ui -type f -name "*.kt" -print0 | while IFS= read -r -d '' file; do
  if grep -q "contentDescription = null" "$file"; then
    echo "Found contentDescription = null in $file"
  fi
done
