#!/bin/bash

# Start backend service in a new Kitty terminal
(
  kitty -e bash -c "
    sudo archlinux-java set java-24-openjdk
    echo 'Starting backend service...'
    ./gradlew clean build
    java -jar app/build/libs/nanami.jar 2>/dev/null
    echo 'Backend service finished or exited. Press Enter to close this terminal.'
    read -p '' # Keep the terminal open until user presses Enter
  " &
)