# FileTableMaker

Java console record manager.

- **Full app**: versioning, save/load, undo/redo, input validation (`app/` package)
- **Lite demo**: minimal CRUD (`demo/` package)

## Requirements
- JDK 17+ (works on 21 too)

## Run (IntelliJ)
- Open as a project.
- Right-click `src/app/FileTableMaker.java` → **Run** (full app).
- Right-click `src/demo/FileTableMaker.java` → **Run** (lite).

## Run (command line)
```bash
# from repo root

# Full app
javac -d out src/app/*.java
java -cp out app.FileTableMaker

# Lite
javac -d out src/demo/FileTableMaker.java
java -cp out demo.FileTableMaker
