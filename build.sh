#!/bin/bash

SOURCE_FILE="API_keys.java"
DEST_DIR="app/src/main/java/keys"
DEST_FILE="$DEST_DIR/$SOURCE_FILE"

# Check if the source file exists
if [ ! -f "dummy_files/$SOURCE_FILE" ]; then
  echo "Error: Source file 'dummy_files/$SOURCE_FILE' not found."
  exit 1
fi

# Create the destination directory if it doesn't exist
mkdir -p "$DEST_DIR"

# Check if the destination file already exists
if [ -f "$DEST_FILE" ]; then
  echo "Destination file '$DEST_FILE' already exists. Doing nothing."
else
  # Copy the file if it doesn't exist
  cp "dummy_files/$SOURCE_FILE" "$DEST_FILE"
  echo "File '$SOURCE_FILE' copied to '$DEST_FILE'."
fi
