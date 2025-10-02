package app;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileTableMaker {
    private static final Scanner in = new Scanner(System.in);
    private static Map<Integer, String> recordMap = new LinkedHashMap<>();
    private static Stack<HashMap<Integer, String>> undoStack = new Stack<>();
    private static Stack<HashMap<Integer, String>> redoStack = new Stack<>();
    private static boolean recordsChanged = false;
    private static String currentFileName = null;
    private static final String VERSION_FOLDER = "versions";

    public static void main(String[] args) {
        File versionDir = new File(VERSION_FOLDER);
        if (!versionDir.exists()) versionDir.mkdir();

        while (true) {
            System.out.println("\n--- FileTableMaker ---");
            System.out.println("1. View Records");
            System.out.println("2. Add Record");
            System.out.println("3. Delete Record");
            System.out.println("4. Update Record");
            System.out.println("5. Move (Swap) Record");
            System.out.println("6. Clear Records");
            System.out.println("7. Save Records");
            System.out.println("8. Load Records");
            System.out.println("9. List Saved Files");
            System.out.println("10. Undo");
            System.out.println("11. Redo");
            System.out.println("12. Exit");

            int choice = getValidNumber("Enter choice: ", 1, 12);
            switch (choice) {
                case 1 -> viewRecords();
                case 2 -> addRecord();
                case 3 -> deleteRecord();
                case 4 -> updateRecord();
                case 5 -> moveRecord();
                case 6 -> clearRecords();
                case 7 -> saveRecords();
                case 8 -> loadRecords();
                case 9 -> listSavedFiles();
                case 10 -> undo();
                case 11 -> redo();
                case 12 -> {
                    if (recordsChanged && getYNConfirm("Unsaved changes exist. Save before exiting?")) {
                        saveRecords();
                    }
                    System.out.println("Exiting.");
                    return;
                }
            }
        }
    }

    private static void viewRecords() {
        if (recordMap.isEmpty()) {
            System.out.println("No records to display.");
            return;
        }
        System.out.println("--- Records ---");
        recordMap.forEach((key, value) -> System.out.println(key + ": " + value));
    }

    private static void addRecord() {
        System.out.print("Enter record content: ");
        String content = in.nextLine().trim();
        int key = getNextAvailableKey();
        pushUndoState();
        recordMap.put(key, content);
        recordsChanged = true;
        System.out.println("Record added with key " + key + ".");
    }

    private static void deleteRecord() {
        if (recordMap.isEmpty()) {
            System.out.println("No records to delete.");
            return;
        }
        viewRecords();
        int key = getValidNumber("Enter key of the record to delete: ", 1, Integer.MAX_VALUE);
        if (!recordMap.containsKey(key)) {
            System.out.println("Record not found.");
            return;
        }
        System.out.println("Record to delete: " + key + ": " + recordMap.get(key));
        if (getYNConfirm("Delete this record?")) {
            pushUndoState();
            recordMap.remove(key);
            recordsChanged = true;
            System.out.println("Record deleted.");
        }
    }

    private static void updateRecord() {
        if (recordMap.isEmpty()) {
            System.out.println("No records to update.");
            return;
        }
        viewRecords();
        int key = getValidNumber("Enter key of the record to update: ", 1, Integer.MAX_VALUE);
        if (!recordMap.containsKey(key)) {
            System.out.println("Record not found.");
            return;
        }
        System.out.println("Current record: " + key + ": " + recordMap.get(key));
        System.out.print("Enter new content: ");
        String content = in.nextLine().trim();
        System.out.println("Preview: " + key + ": " + content);
        if (getYNConfirm("Update this record?")) {
            pushUndoState();
            recordMap.put(key, content);
            recordsChanged = true;
            System.out.println("Record updated.");
        }
    }

    private static void moveRecord() {
        if (recordMap.size() < 2) {
            System.out.println("Not enough records to move.");
            return;
        }
        viewRecords();
        int fromKey = getValidNumber("Enter key of the record to move: ", 1, Integer.MAX_VALUE);
        int toKey = getValidNumber("Enter destination key to swap with: ", 1, Integer.MAX_VALUE);
        if (!recordMap.containsKey(fromKey) || !recordMap.containsKey(toKey)) {
            System.out.println("One or both keys not found.");
            return;
        }
        System.out.println("Preview: " + fromKey + ": " + recordMap.get(fromKey)
                + " <--> " + toKey + ": " + recordMap.get(toKey));
        if (getYNConfirm("Proceed with move (swap)?")) {
            pushUndoState();
            String temp = recordMap.get(fromKey);
            recordMap.put(fromKey, recordMap.get(toKey));
            recordMap.put(toKey, temp);
            recordsChanged = true;
            System.out.println("Records swapped.");
        }
    }

    private static void clearRecords() {
        if (recordMap.isEmpty()) {
            System.out.println("No records to clear.");
            return;
        }
        if (getYNConfirm("Clear all records?")) {
            pushUndoState();
            recordMap.clear();
            recordsChanged = true;
            System.out.println("All records cleared.");
        }
    }

    private static void saveRecords() {
        System.out.print("Enter filename to save: ");
        String filename = in.nextLine().trim();
        if (Files.exists(Path.of(filename))) {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Path versionPath = Path.of(VERSION_FOLDER, filename + "_" + timestamp + ".bak");
            try {
                Files.copy(Path.of(filename), versionPath);
                System.out.println("Previous version saved to " + versionPath);
            } catch (IOException e) {
                System.out.println("Versioning failed: " + e.getMessage());
            }
        }
        try (PrintWriter out = new PrintWriter(filename)) {
            recordMap.forEach((k, v) -> out.println(k + "=" + v));
            currentFileName = filename;
            recordsChanged = false;
            System.out.println("Records saved to " + filename);
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }

    private static void loadRecords() {
        System.out.print("Enter filename to load: ");
        String filename = in.nextLine().trim();
        if (!Files.exists(Path.of(filename))) {
            System.out.println("File not found.");
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            pushUndoState();
            recordMap.clear();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    recordMap.put(Integer.parseInt(parts[0]), parts[1]);
                }
            }
            currentFileName = filename;
            recordsChanged = false;
            System.out.println("Records loaded from " + filename);
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error loading file: " + e.getMessage());
        }
    }

    private static void listSavedFiles() {
        try {
            Files.list(Path.of(".")).filter(Files::isRegularFile).forEach(p -> System.out.println(p.getFileName()));
        } catch (IOException e) {
            System.out.println("Failed to list files.");
        }
    }

    private static void undo() {
        if (undoStack.isEmpty()) {
            System.out.println("Nothing to undo.");
            return;
        }
        redoStack.push(new HashMap<>(recordMap));
        recordMap = undoStack.pop();
        recordsChanged = true;
        System.out.println("Undo performed.");
    }

    private static void redo() {
        if (redoStack.isEmpty()) {
            System.out.println("Nothing to redo.");
            return;
        }
        undoStack.push(new HashMap<>(recordMap));
        recordMap = redoStack.pop();
        recordsChanged = true;
        System.out.println("Redo performed.");
    }

    private static void pushUndoState() {
        undoStack.push(new HashMap<>(recordMap));
        redoStack.clear();
    }

    private static int getNextAvailableKey() {
        int key = 1;
        while (recordMap.containsKey(key)) {
            key++;
        }
        return key;
    }

    private static int getValidNumber(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = in.nextLine().trim();
            try {
                int number = Integer.parseInt(input);
                if (number >= min && number <= max) return number;
                else System.out.println("Enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    private static boolean getYNConfirm(String prompt) {
        while (true) {
            System.out.print(prompt + " [Y/N]: ");
            String response = in.nextLine().trim().toUpperCase();
            if (response.equals("Y")) return true;
            else if (response.equals("N")) return false;
            else System.out.println("Please enter Y or N.");
        }
    }
}
