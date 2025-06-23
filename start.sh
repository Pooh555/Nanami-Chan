#!/bin/bash

# Start backend service in a new Kitty terminal
(
  kitty -e bash -c "
    echo 'Starting backend service...'
    ./gradlew clean build
    java -jar app/build/libs/nanami.jar 2>/dev/null
    echo 'Backend service finished or exited. Press Enter to close this terminal.'
    read -p '' # Keep the terminal open until user presses Enter
  " &
)

# Start frontend service in a new Kitty terminal
(
  kitty -e bash -c "
    echo 'Starting frontend service...'
    cd frontend/TypeScript/frontend/src || { echo 'Error: Could not change to frontend directory.'; exit 1; }
    echo 'Building frontend...'
    npm run build

    if [ $? -ne 0 ]; then
      echo 'Frontend build failed. Press Enter to close this terminal.'
      read -p ''
      exit 1
    fi

    echo 'Serving frontend...'
    npm run serve
    echo 'Frontend service finished or exited. Press Enter to close this terminal.'
    read -p '' # Keep the terminal open until user presses Enter
  " &
)

echo "Both backend and frontend services are attempting to start in new Kitty terminals."
echo "You can close this script's terminal now, or it will exit automatically."