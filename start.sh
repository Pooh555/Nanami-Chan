#!/bin/bash

# Define the frontend URL
FRONTEND_URL="http://localhost:5000/"

# Function to open the URL based on OS
open_url() {
  local url="$1"

  echo "Attempting to open $url in default browser..."

  if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    if command -v xdg-open >/dev/null; then
      xdg-open "$url" &>/dev/null &
    elif command -v gio >/dev/null; then
      gio open "$url" &>/dev/null &
    # Check for WSL
    elif [[ "$(uname -r)" =~ "Microsoft" ]]; then
      /mnt/c/Windows/explorer.exe "$url" &>/dev/null &
    else
      echo "No suitable command (xdg-open, gio, explorer.exe) found to open URL on Linux/WSL."
    fi
  # MacOS
  elif [[ "$OSTYPE" == "darwin"* ]]; then
    open "$url" &>/dev/null &
  # Windows (Git Bash / Cygwin / MSYS)
  elif [[ "$OSTYPE" == "cygwin"* || "$OSTYPE" == "msys"* || "$OSTYPE" == "win32"* ]]; then
    start "" "$url" &>/dev/null &
  else
    echo "Unsupported OS: $OSTYPE. Please open $url manually."
  fi
}

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

    if [ \$? -ne 0 ]; then
      echo 'Frontend build failed. Press Enter to close this terminal.'
      read -p ''
      exit 1
    fi

    echo 'Serving frontend...'
    npm run serve
    echo 'Frontend service finished or exited. Press Enter to close this terminal.'
    read -p ''
  " &
)

sleep 5
open_url "$FRONTEND_URL"
echo "Both backend and frontend services are attempting to start in new Kitty terminals."
echo "You can close this script's terminal now, or it will exit automatically."