package app;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FileListMaker
{
    private static ArrayList<String> myArrList = new ArrayList<>();
    private static Scanner in = new Scanner(System.in);
    private static boolean listChanged = false;
    private static String currentFileName = "list.txt";
    private static Stack<ArrayList<String>> undoStack = new Stack<>();
    private static Stack<ArrayList<String>> redoStack = new Stack<>();
    private static final int UNDO_STACK_LIMIT = 10;

    private static void pushUndoState() {
        if (undoStack.size() >= UNDO_STACK_LIMIT) {
            undoStack.remove(0); // Remove the oldest state
        }
        undoStack.push(new ArrayList<>(myArrList)); // Deep copy of the current state
        redoStack.clear(); // Clear redo stack when a new action happens
    }

    private static void viewUndoRedoStackStatus() {
        System.out.println("Undo stack size: " + undoStack.size());
        System.out.println("Redo stack size: " + redoStack.size());
    }

    public static void main(String[] args) {
        boolean running = true;

        while (running) {
            String choice = SafeInput.getRegExString(in,
                    "Enter A (add), D (delete), I (insert), U (update), M (move), V (view), S (save), O (open), C (clear), Q (quit), VL (load version), Z (undo), Y (redo), LF (list files)",
                    "(?i)A|D|I|U|M|V|S|O|C|Q|VL|Z|Y|LF");

            if (choice.equalsIgnoreCase("A")) addItem();
            else if (choice.equalsIgnoreCase("D")) deleteItem();
            else if (choice.equalsIgnoreCase("I")) insertItem();
            else if (choice.equalsIgnoreCase("U")) updateItem();
            else if (choice.equalsIgnoreCase("M")) moveItem();
            else if (choice.equalsIgnoreCase("V")) viewList();
            else if (choice.equalsIgnoreCase("S")) saveListToFile();
            else if (choice.equalsIgnoreCase("O")) openListFromFile();
            else if (choice.equalsIgnoreCase("C")) clearList();
            else if (choice.equalsIgnoreCase("VL")) loadVersionedFile();
            else if (choice.equalsIgnoreCase("Z")) undoLastChange();  // Undo action
            else if (choice.equalsIgnoreCase("Y")) redoLastChange(); // Redo action
            else if (choice.equalsIgnoreCase("LF")) listSavedFiles();
            else if (choice.equalsIgnoreCase("Q")) running = !quitProgram();
            else System.out.println("Invalid option. Try again.");

            // Optionally show undo/redo stack sizes
            viewUndoRedoStackStatus();
        }
    }


    private static void addItem() {
        System.out.print("Enter item to add: ");
        String item = in.nextLine().trim();

        if (myArrList.contains(item)) {
            System.out.println("Item already exists. Not adding.");
            return;
        }

        pushUndoState(); // Save current state for undo

        ArrayList<String> previewList = new ArrayList<>(myArrList);
        previewList.add(item);
        System.out.println("\nPreview of the list with new item:");
        for (int i = 0; i < previewList.size(); i++) {
            System.out.println((i + 1) + ". " + previewList.get(i));
        }

        if (SafeInput.getYNConfirm(in, "\nAdd this item?")) {
            myArrList.add(item);
            System.out.println("Item added.");
            listChanged = true;
        } else {
            System.out.println("Add cancelled.");
        }
    }

    private static void deleteItem() {
        if (myArrList.isEmpty()) {
            System.out.println("No items to delete.");
            return;
        }

        pushUndoState(); // Save current state for undo

        viewList();
        int index = SafeInput.getRangedInt(in, "Enter item number to delete: ", 1, myArrList.size()) - 1;
        String item = myArrList.get(index);

        ArrayList<String> previewList = new ArrayList<>(myArrList);
        previewList.remove(index);

        System.out.println("\nPreview of the list after removing item\"" + item + "\":");
        for (int i = 0; i < previewList.size(); i++) {
            System.out.println((i + 1) + ". " + previewList.get(i));
        }

        if (SafeInput.getYNConfirm(in, "\nDelete item '" + item + "'?")) {
            myArrList.remove(index);
            System.out.println("Item deleted.");
            listChanged = true;
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private static void insertItem() {
        System.out.print("Enter new item description: ");
        String newItem = in.nextLine().trim();

        if (myArrList.contains(newItem)) {
            System.out.println("Item already exists. Not inserting.");
            return;
        }

        pushUndoState(); // Save current state for undo

        int index;
        if (myArrList.isEmpty()) {
            index = 0;
        } else {
            viewList();
            index = getValidNumber("Enter item number to insert before or after: ", 1, myArrList.size()) - 1;
            String relativeItem = myArrList.get(index);
            String pos = SafeInput.getRegExString(in,
                    "Do you want to insert ABOVE or BELOW \"" + relativeItem + "\"? (A/B): ",
                    "[AaBb]");
            index = pos.equalsIgnoreCase("B") ? index + 1 : index;
        }

        ArrayList<String> previewList = new ArrayList<>(myArrList);
        previewList.add(index, newItem);

        System.out.println("\nPreview of the list after insert:");
        for (int i = 0; i < previewList.size(); i++) {
            System.out.println((i + 1) + ". " + previewList.get(i));
        }

        if (SafeInput.getYNConfirm(in, "\nInsert new item?")) {
            myArrList.add(index, newItem);
            System.out.println("Item inserted.");
            listChanged = true;
        } else {
            System.out.println("Insertion cancelled.");
        }
    }

    private static void updateItem() {
        if (myArrList.isEmpty())
        {
            System.out.println("No items to update.");
            return;
        }

        pushUndoState(); // Save current state for undo

        viewList();
        int index = getValidNumber("Enter the number of the item to update: ", 1, myArrList.size()) - 1;
        String oldItem = myArrList.get(index);

        System.out.print("Enter new description for item \"" + oldItem + "\": ");
        String newItem = in.nextLine().trim();

        if (newItem.isEmpty()) {
            System.out.println("New item cannot be empty. Update cancelled.");
            return;
        }

        if (myArrList.contains(newItem)) {
            System.out.println("Item \"" + newItem + "\" already exists in the list. Update cancelled.");
            return;
        }

        // Preview change
        ArrayList<String> previewList = new ArrayList<>(myArrList);
        previewList.set(index, newItem);

        System.out.println("\nPreview of the list after update:");
        for (int i = 0; i < previewList.size(); i++) {
            System.out.println((i + 1) + ". " + previewList.get(i));
        }

        boolean confirm = SafeInput.getYNConfirm(in, "\nUpdate this item?");
        if (confirm) {
            myArrList.set(index, newItem);
            System.out.println("Item updated.");
            listChanged = true;
        } else {
            System.out.println("Update cancelled.");
        }
    }

    private static void moveItem() {
        if (myArrList.size() < 2)
        {
            System.out.println("Need at least 2 items to perform a move.");
            return;
        }

        pushUndoState(); // Save current state for undo

        viewList();
        int fromIndex = getValidNumber("Enter the number of the item to move: ", 1, myArrList.size()) - 1;
        String itemToMove = myArrList.get(fromIndex);

        // Remove temporarily to simplify index math
        myArrList.remove(fromIndex);

        viewList();

        int toIndex = getValidNumber("Enter the number of the item to move ABOVE or BELOW: ", 1, myArrList.size()) - 1;
        String targetItem = myArrList.get(toIndex);

        String position = SafeInput.getRegExString(in,
                "Do you want to move \"" + itemToMove + "\" ABOVE or BELOW \"" + targetItem + "\"? (A/B): ",
                "[AaBb]");

        if (position.equalsIgnoreCase("B")) {
            toIndex++;
        }

        // Adjust if original item was before new insert point (because list shrunk by 1)
        if (fromIndex < toIndex) {
            toIndex--;
        }

        // Preview: Create a copy to simulate the new list
        ArrayList<String> previewList = new ArrayList<>(myArrList);
        previewList.add(toIndex, itemToMove);

        System.out.println("\nPreview of the list after move:");
        for (int i = 0; i < previewList.size(); i++) {
            System.out.println((i + 1) + ". " + previewList.get(i));
        }

        boolean confirm = SafeInput.getYNConfirm(in, "\nConfirm  move?");
        if (confirm) {
            // Commit the move
            myArrList.add(toIndex, itemToMove);
            System.out.println("Move confirmed.");
            listChanged = true;
        } else {
            // Cancel: re-insert at original location
            myArrList.add(fromIndex, itemToMove);
            System.out.println("Move cancelled. List restored.");
        }
    }

    private static void undoLastChange() {
        if (undoStack.isEmpty()) {
            System.out.println("Nothing to undo.");
            return;
        }
        redoStack.push(new ArrayList<>(myArrList)); // Save the current state for redo
        myArrList = undoStack.pop();
        listChanged = true;
        System.out.println("Undo successful.");
    }

    private static void redoLastChange() {
        if (redoStack.isEmpty()) {
            System.out.println("Nothing to redo.");
            return;
        }
        undoStack.push(new ArrayList<>(myArrList)); // Push the new state to undo stack
        myArrList = redoStack.pop();
        listChanged = true;
        System.out.println("Redo successful.");
    }

    private static void viewList()
    {
        if (myArrList.isEmpty())
        {
            System.out.println("List is empty.");
        }
        else
        {
            System.out.println("\nCurrent List:");
            for (int i = 0; i < myArrList.size(); i++)
            {
                System.out.println((i + 1) + ". " + myArrList.get(i));
            }
        }
    }

    private static void saveListToFile() {
        if (currentFileName == null || currentFileName.isEmpty()) {
            currentFileName = getFileName();

            if (!currentFileName.toLowerCase().endsWith(".txt")) {
                currentFileName += ".txt";
            }
        }

        versionCurrentFile(); // <--- ADD THIS LINE BEFORE OVERWRITING

        try (PrintWriter writer = new PrintWriter(currentFileName)) {
            for (String item : myArrList) {
                writer.println(item);
            }
            listChanged = false;
            System.out.println("List saved to '" + currentFileName + "'");
        } catch (IOException e) {
            System.out.println("Error saving list: " + e.getMessage());
        }
    }


    private static void versionCurrentFile() {
        if (currentFileName == null) return;

        File original = new File(currentFileName);
        if (!original.exists()) return;

        String baseName = currentFileName.replace(".txt", "");
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String versionedName = baseName + "_v" + timestamp + ".txt";

        File versionedFile = new File(versionedName);

        try {
            Files.copy(original.toPath(), versionedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Backup saved as: " + versionedName);
        } catch (IOException e) {
            System.out.println("Failed to create version backup: " + e.getMessage());
        }
    }

    private static File[] listVersionsForCurrentFile() {
    if (currentFileName == null) {
        System.out.println("No list is currently loaded.");
        return null;
    }

    String baseName = currentFileName.replace(".txt", "");
    File dir = new File(".");
    File[] versions = dir.listFiles((d, name) -> name.startsWith(baseName + "_v") && name.endsWith(".txt"));

    if (versions == null || versions.length == 0) {
        System.out.println("No versions found for '" + currentFileName + "'");
        return null;
    }

    Arrays.sort(versions, Comparator.comparing(File::getName).reversed()); // Show latest first
    return versions;
    }

    private static void loadVersionedFile() {
        File[] versions = listVersionsForCurrentFile();
        if (versions == null) return;

        System.out.println("\nAvailable versions:");
        for (int i = 0; i < versions.length; i++) {
            System.out.printf("%d. %s\n", i + 1, versions[i].getName());
        }

        pushUndoState();

        int choice = getValidNumber("Enter the number of the version to load: ", 1, versions.length);
        File selectedFile = versions[choice - 1];

        try (Scanner fileIn = new Scanner(selectedFile)) {
            ArrayList<String> tempList = new ArrayList<>();
            while (fileIn.hasNextLine()) {
                tempList.add(fileIn.nextLine().trim());
            }

            System.out.println("\nPreview of version:");
            for (int i = 0; i < tempList.size(); i++) {
                System.out.println((i + 1) + ". " + tempList.get(i));
            }

            if (SafeInput.getYNConfirm(in, "\nReplace current list with this version?")) {
                myArrList = tempList;
                listChanged = true;
                System.out.println("List restored from version: " + selectedFile.getName());
            } else {
                System.out.println("Restore cancelled.");
            }

        } catch (IOException e) {
            System.out.println("Error reading version file: " + e.getMessage());
        }
    }

    private static void openListFromFile() {
        if (!promptToSaveIfNeeded()) {
            System.out.println("Operation cancelled.");
            return;
        }

        File dir = new File(".");
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".txt"));

        if (files == null || files.length == 0) {
            System.out.println("No saved lists found.");
            return;
        }

        System.out.println("\nAvailable list files:");
        for (int i = 0; i < files.length; i++) {
            System.out.printf("%d. %s\n", i + 1, files[i].getName());
        }
        int choice = getValidNumber("Enter the number of the file to load: ", 1, files.length);
        File selectedFile = files[choice - 1];
        currentFileName = selectedFile.getName();

        try (Scanner fileIn = new Scanner(selectedFile)) {
            ArrayList<String> tempList = new ArrayList<>();
            while (fileIn.hasNextLine()) {
                tempList.add(fileIn.nextLine().trim());
            }

            System.out.println("\nPreview of loaded list:");
            for (int i = 0; i < tempList.size(); i++) {
                System.out.println((i + 1) + ". " + tempList.get(i));
            }

            if (SafeInput.getYNConfirm(in, "\nLoad this list?")) {
                myArrList = tempList;
                listChanged = false;
                System.out.println("List loaded successfully from '" + currentFileName + "'.");
            } else {
                System.out.println("Load cancelled.");
            }

        } catch (IOException e) {
            System.out.println("Error reading from file: " + e.getMessage());
        }
    }

    private static void listSavedFiles()
    {
        File dir = new File(".");
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".txt"));

        if (files == null || files.length == 0) {
            System.out.println("No saved lists found.");
        } else {
            System.out.println("Saved lists:");
            for (File file : files) {
                System.out.println("- " + file.getName());
            }
        }
    }

    private static void clearList() {
        if (myArrList.isEmpty()) {
            System.out.println("List is already empty.");
            return;
        }

        if (!promptToSaveIfNeeded()) {
            System.out.println("Clear cancelled.");
            return;
        }

        pushUndoState(); // Save current state for undo

        System.out.println("\nCurrent list:");
        viewList();

        if (SafeInput.getYNConfirm(in, "\nAre you sure you want to clear the entire list? This cannot be undone.")) {
            myArrList.clear();
            System.out.println("List cleared.");
            listChanged = true;
        } else {
            System.out.println("Clear operation cancelled.");
        }
    }

    private static boolean quitProgram()
    {
        if (!promptToSaveIfNeeded()) {
            System.out.println("Exit cancelled.");
            return false;
        }
        return SafeInput.getYNConfirm(in, "Are you sure you want to quit?");
    }

    private static boolean promptToSaveIfNeeded()
    {
        if (!listChanged) return true;

        System.out.println("\nYou have unsaved changes.");
        if (SafeInput.getYNConfirm(in, "Would you like to save your list first?")) {
            saveListToFile();
            return !listChanged;
        }

        return SafeInput.getYNConfirm(in, "Are you sure you don't want to save?");
    }

    private static int getValidNumber(String prompt, int min, int max)
    {
        int num = -1;
        while (num < min || num > max)
        {
            System.out.print(prompt);
            String input = in.nextLine().trim();
            if (input.matches("\\d+"))
            { // Ensures input is only digits
                num = Integer.parseInt(input);
                if (num < min || num > max)
                {
                    System.out.println("Invalid number. Please enter a number between " + min + " and " + max + ".");
                }
            }
            else
            {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
        return num;
    }

    private static String getFileName()
    {
        System.out.print("Enter the file name to " + "save" + " (without extension): ");
        String name = in.nextLine().trim();

        if (!name.endsWith(".txt")) {
            name += ".txt";
        }

        return name;
    }

    private static String ensureTxtExtension(String name) {
        return name.toLowerCase().endsWith(".txt") ? name : name + ".txt";
    }

}