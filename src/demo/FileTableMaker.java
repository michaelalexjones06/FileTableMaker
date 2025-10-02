package demo;
import java.util.*;

public class FileTableMaker {
    private static final Scanner in = new Scanner(System.in);
    private static final Map<Integer, String> records = new LinkedHashMap<>();
    private static int nextId = 1;

    public static void main(String[] args) {
        while (true) {
            System.out.print("(A)dd (U)pdate (D)elete (S)how (Q)uit: ");
            String choice = in.nextLine().trim().toUpperCase();
            switch (choice) {
                case "A": add(); break;
                case "U": update(); break;
                case "D": delete(); break;
                case "S": show(); break;
                case "Q": System.out.println("Bye!"); return;
                default: System.out.println("Try A/U/D/S/Q");
            }
        }
    }

    private static void add() {
        System.out.print("Enter record text: ");
        String text = in.nextLine().trim();
        if (text.isEmpty()) { System.out.println("Nothing added."); return; }
        records.put(nextId, text);
        System.out.println("Added id " + nextId);
        nextId++;
    }

    private static void update() {
        int id = askId();
        if (!records.containsKey(id)) { System.out.println("No such id."); return; }
        System.out.println("Current: " + records.get(id));
        System.out.print("New text: ");
        String text = in.nextLine().trim();
        if (!text.isEmpty()) { records.put(id, text); System.out.println("Updated."); }
    }

    private static void delete() {
        int id = askId();
        if (records.remove(id) != null) System.out.println("Deleted.");
        else System.out.println("No such id.");
    }

    private static void show() {
        if (records.isEmpty()) { System.out.println("(no records)"); return; }
        records.forEach((id, text) -> System.out.println(id + ": " + text));
    }

    private static int askId() {
        System.out.print("Enter id: ");
        while (true) {
            String s = in.nextLine().trim();
            try { return Integer.parseInt(s); }
            catch (NumberFormatException e) { System.out.print("Enter a number: "); }
        }
    }
}
