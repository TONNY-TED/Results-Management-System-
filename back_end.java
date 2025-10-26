import java.sql.*;
import java.util.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ResultsManagementSystem {
    // ✅ MySQL configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/resultsdb";
    private static final String DB_USER = "root";      // change if you have another username
    private static final String DB_PASSWORD = "tedd";  // change to your actual password
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";

    // ✅ Use MySQL-friendly table definitions
    private static final String CREATE_STUDENTS =
            "CREATE TABLE IF NOT EXISTS students (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100), " +
                    "student_id VARCHAR(20) UNIQUE, " +
                    "current_semester INT DEFAULT 1, " +
                    "program VARCHAR(50))";

    private static final String CREATE_COURSES =
            "CREATE TABLE IF NOT EXISTS courses (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "course_name VARCHAR(100) UNIQUE)";

    private static final String CREATE_SEMESTERS =
            "CREATE TABLE IF NOT EXISTS semesters (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "semester_number INT UNIQUE)";

    private static final String CREATE_SUBJECTS =
            "CREATE TABLE IF NOT EXISTS subjects (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "subject_name VARCHAR(50), " +
                    "course_id INT, " +
                    "FOREIGN KEY(course_id) REFERENCES courses(id))";

    private static final String CREATE_INSTRUCTORS =
            "CREATE TABLE IF NOT EXISTS instructors (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100), " +
                    "instructor_id VARCHAR(20) UNIQUE)";

    private static final String CREATE_SUBJECT_INSTRUCTORS =
            "CREATE TABLE IF NOT EXISTS subject_instructors (" +
                    "subject_id INT, instructor_id INT, " +
                    "PRIMARY KEY(subject_id, instructor_id), " +
                    "FOREIGN KEY(subject_id) REFERENCES subjects(id), " +
                    "FOREIGN KEY(instructor_id) REFERENCES instructors(id))";

    private static final String CREATE_CLASSES =
            "CREATE TABLE IF NOT EXISTS classes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "day VARCHAR(10), time_slot VARCHAR(20), " +
                    "subject_id INT, instructor_id INT, room VARCHAR(20), semester_number INT, " +
                    "FOREIGN KEY(subject_id) REFERENCES subjects(id), " +
                    "FOREIGN KEY(instructor_id) REFERENCES instructors(id))";

    private static final String CREATE_STUDENT_CLASSES =
            "CREATE TABLE IF NOT EXISTS student_classes (" +
                    "student_id VARCHAR(20), class_id INT, " +
                    "PRIMARY KEY(student_id, class_id), " +
                    "FOREIGN KEY(student_id) REFERENCES students(student_id), " +
                    "FOREIGN KEY(class_id) REFERENCES classes(id))";

    private static final String CREATE_RESULTS =
            "CREATE TABLE IF NOT EXISTS results (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "student_id VARCHAR(20), semester_id INT, subject_id INT, " +
                    "marks DOUBLE, grade VARCHAR(5), " +
                    "FOREIGN KEY(student_id) REFERENCES students(student_id), " +
                    "FOREIGN KEY(semester_id) REFERENCES semesters(id), " +
                    "FOREIGN KEY(subject_id) REFERENCES subjects(id))";

    private static final String CREATE_SUP_EXAMS =
            "CREATE TABLE IF NOT EXISTS sup_exams (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "student_id VARCHAR(20), semester_id INT, subject_id INT, " +
                    "status VARCHAR(10), marks DOUBLE, " +
                    "FOREIGN KEY(student_id) REFERENCES students(student_id), " +
                    "FOREIGN KEY(semester_id) REFERENCES semesters(id), " +
                    "FOREIGN KEY(subject_id) REFERENCES subjects(id))";

    private static final String CREATE_FEE_STRUCTURE =
            "CREATE TABLE IF NOT EXISTS fee_structure (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "program VARCHAR(50), fee_amount DOUBLE, semester INT, due_date DATE, " +
                    "UNIQUE KEY(program, semester))";

    private static final String CREATE_STUDENT_PAYMENTS =
            "CREATE TABLE IF NOT EXISTS student_payments (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "student_id VARCHAR(20), semester_number INT, amount_paid DOUBLE, " +
                    "payment_date DATE, receipt_no VARCHAR(20), " +
                    "FOREIGN KEY(student_id) REFERENCES students(student_id))";

    private Connection conn;

    public ResultsManagementSystem() {
        initDatabase();
    }

    private void initDatabase() {
        try {
            Class.forName(DB_DRIVER);
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();
            stmt.execute(CREATE_STUDENTS);
            stmt.execute(CREATE_COURSES);
            stmt.execute(CREATE_SEMESTERS);
            stmt.execute(CREATE_SUBJECTS);
            stmt.execute(CREATE_INSTRUCTORS);
            stmt.execute(CREATE_SUBJECT_INSTRUCTORS);
            stmt.execute(CREATE_CLASSES);
            stmt.execute(CREATE_STUDENT_CLASSES);
            stmt.execute(CREATE_RESULTS);
            stmt.execute(CREATE_SUP_EXAMS);
            stmt.execute(CREATE_FEE_STRUCTURE);
            stmt.execute(CREATE_STUDENT_PAYMENTS);
            stmt.close();
            System.out.println(" Database initialized successfully with MySQL!\n just to notify me ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



private int insertOrGetStudent(String name, String studentId, String program) {
    try {
        PreparedStatement select = conn.prepareStatement("SELECT id, program FROM students WHERE student_id = ?");
        select.setString(1, studentId);
        ResultSet rs = select.executeQuery();
        if (rs.next()) {
            String existingProgram = rs.getString("program");
            if (existingProgram == null || existingProgram.isEmpty()) {
                PreparedStatement updateProg = conn.prepareStatement("UPDATE students SET program = ? WHERE student_id = ?");
                updateProg.setString(1, program);
                updateProg.setString(2, studentId);
                updateProg.executeUpdate();
            }
            return rs.getInt("id");
        }
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO students (name, student_id, program, current_semester) VALUES (?, ?, ?, 1)");
        pstmt.setString(1, name);
        pstmt.setString(2, studentId);
        pstmt.setString(3, program);
        pstmt.executeUpdate();
        select.setString(1, studentId);
        rs = select.executeQuery();
        rs.next();
        return rs.getInt("id");
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return -1;
}

private int insertOrGetCourse(String courseName) {
    try {
        PreparedStatement select = conn.prepareStatement("SELECT id FROM courses WHERE course_name = ?");
        select.setString(1, courseName);
        ResultSet rs = select.executeQuery();
        if (rs.next()) {
            return rs.getInt("id");
        }
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO courses (course_name) VALUES (?)");
        pstmt.setString(1, courseName);
        pstmt.executeUpdate();
        select.setString(1, courseName);
        rs = select.executeQuery();
        rs.next();
        return rs.getInt("id");
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return -1;
}

private int insertOrGetSemester(int num) {
    try {
        PreparedStatement select = conn.prepareStatement("SELECT id FROM semesters WHERE semester_number = ?");
        select.setInt(1, num);
        ResultSet rs = select.executeQuery();
        if (rs.next()) {
            return rs.getInt("id");
        }
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO semesters (semester_number) VALUES (?)");
        pstmt.setInt(1, num);
        pstmt.executeUpdate();
        select.setInt(1, num);
        rs = select.executeQuery();
        rs.next();
        return rs.getInt("id");
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return -1;
}

private int insertOrGetSubject(String subjectName, int courseId) {
    try {
        PreparedStatement select = conn.prepareStatement("SELECT id FROM subjects WHERE subject_name = ? AND course_id = ?");
        select.setString(1, subjectName);
        select.setInt(2, courseId);
        ResultSet rs = select.executeQuery();
        if (rs.next()) {
            return rs.getInt("id");
        }
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO subjects (subject_name, course_id) VALUES (?, ?)");
        pstmt.setString(1, subjectName);
        pstmt.setInt(2, courseId);
        pstmt.executeUpdate();
        select.setString(1, subjectName);
        select.setInt(2, courseId);
        rs = select.executeQuery();
        rs.next();
        return rs.getInt("id");
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return -1;
}

private int insertOrGetInstructor(String name, String instructorId) {
    try {
        PreparedStatement select = conn.prepareStatement("SELECT id FROM instructors WHERE instructor_id = ?");
        select.setString(1, instructorId);
        ResultSet rs = select.executeQuery();
        if (rs.next()) {
            return rs.getInt("id");
        }
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO instructors (name, instructor_id) VALUES (?, ?)");
        pstmt.setString(1, name);
        pstmt.setString(2, instructorId);
        pstmt.executeUpdate();
        select.setString(1, instructorId);
        rs = select.executeQuery();
        rs.next();
        return rs.getInt("id");
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return -1;
}

// Assign instructor to subject
public void assignInstructorToSubject(int subjectId, int instructorId) {
    try {
        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO subject_instructors (subject_id, instructor_id) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE instructor_id = instructor_id");
        pstmt.setInt(1, subjectId);
        pstmt.setInt(2, instructorId);
        pstmt.executeUpdate();
        System.out.println("Instructor assigned to subject successfully!");
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

// Create class schedule with conflict detection
public void createClassSchedule(String day, String timeSlot, int subjectId, int instructorId, String room, int semesterNum) {
    // Check room conflict
    try {
        PreparedStatement roomCheck = conn.prepareStatement(
                "SELECT COUNT(*) FROM classes WHERE room = ? AND day = ? AND time_slot = ?");
        roomCheck.setString(1, room);
        roomCheck.setString(2, day);
        roomCheck.setString(3, timeSlot);
        ResultSet roomRs = roomCheck.executeQuery();
        roomRs.next();
        if (roomRs.getInt(1) > 0) {
            System.out.println("Conflict: Room " + room + " already booked on " + day + " at " + timeSlot);
            return;
        }

        // Check instructor conflict
        PreparedStatement instCheck = conn.prepareStatement(
                "SELECT COUNT(*) FROM classes WHERE instructor_id = ? AND day = ? AND time_slot = ?");
        instCheck.setInt(1, instructorId);
        instCheck.setString(2, day);
        instCheck.setString(3, timeSlot);
        ResultSet instRs = instCheck.executeQuery();
        instRs.next();
        if (instRs.getInt(1) > 0) {
            System.out.println("Conflict: Instructor already scheduled on " + day + " at " + timeSlot);
            return;
        }

        // Check if instructor is assigned to subject
        PreparedStatement assignCheck = conn.prepareStatement(
                "SELECT COUNT(*) FROM subject_instructors WHERE subject_id = ? AND instructor_id = ?");
        assignCheck.setInt(1, subjectId);
        assignCheck.setInt(2, instructorId);
        ResultSet assignRs = assignCheck.executeQuery();
        assignRs.next();
        if (assignRs.getInt(1) == 0) {
            System.out.println("Error: Instructor not assigned to this subject.");
            return;
        }

        // Create class
        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO classes (day, time_slot, subject_id, instructor_id, room, semester_number) VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
        pstmt.setString(1, day);
        pstmt.setString(2, timeSlot);
        pstmt.setInt(3, subjectId);
        pstmt.setInt(4, instructorId);
        pstmt.setString(5, room);
        pstmt.setInt(6, semesterNum);
        pstmt.executeUpdate();
        ResultSet rs = pstmt.getGeneratedKeys();
        if (rs.next()) {
            int classId = rs.getInt(1);
            System.out.println("Class scheduled successfully! Class ID: " + classId);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

// Allocate student to class
public void allocateStudentToClass(String studentId, int classId) {
    try {
        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO student_classes (student_id, class_id) VALUES (?, ?)");
        pstmt.setString(1, studentId);
        pstmt.setInt(2, classId);
        int rows = pstmt.executeUpdate();
        if (rows > 0) {
            System.out.println("Student allocated to class successfully!");
        } else {
            System.out.println("Allocation failed.");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

// Enter regular exam results
public void enterResults() {
    Scanner scanner = new Scanner(System.in);
    System.out.print("Enter student name: ");
    String name = scanner.nextLine();
    System.out.print("Enter student ID: ");
    String studentId = scanner.nextLine();
    System.out.print("Enter program (if new student): ");
    String program = scanner.nextLine();
    insertOrGetStudent(name, studentId, program);

    System.out.print("Enter semester number: ");
    int semNum = scanner.nextInt();
    scanner.nextLine();
    int semId = insertOrGetSemester(semNum);

    System.out.print("Enter course name: ");
    String courseName = scanner.nextLine();
    int courseId = insertOrGetCourse(courseName);

    System.out.print("Enter subject name: ");
    String subjectName = scanner.nextLine();
    int subjectId = insertOrGetSubject(subjectName, courseId);

    System.out.print("Enter marks (out of 100): ");
    double marks = scanner.nextDouble();
    String grade = computeGrade(marks);

    try (PreparedStatement pstmt = conn.prepareStatement(
            "INSERT INTO results (student_id, semester_id, subject_id, marks, grade) VALUES (?, ?, ?, ?, ?)")) {
        pstmt.setString(1, studentId);
        pstmt.setInt(2, semId);
        pstmt.setInt(3, subjectId);
        pstmt.setDouble(4, marks);
        pstmt.setString(5, grade);
        pstmt.executeUpdate();
        System.out.println("Regular result entered successfully!");
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

// Enter/update supplementary exam
public void enterSUP() {
    Scanner scanner = new Scanner(System.in);
    System.out.print("Enter student ID: ");
    String studentId = scanner.nextLine();
    System.out.print("Enter semester number: ");
    int semNum = scanner.nextInt();
    scanner.nextLine();
    int semId = insertOrGetSemester(semNum);
    System.out.print("Enter subject name: ");
    String subName = scanner.nextLine();
    System.out.print("Enter course name: ");
    String courseName = scanner.nextLine();
    int courseId = insertOrGetCourse(courseName);
    int subId = insertOrGetSubject(subName, courseId);
    System.out.print("Enter SUP marks (out of 100): ");
    double marks = scanner.nextDouble();
    String grade = computeGrade(marks);
    String status = !"F".equals(grade) ? "Cleared" : "Pending";

    try (PreparedStatement supPstmt = conn.prepareStatement(
            "INSERT INTO sup_exams (student_id, semester_id, subject_id, status, marks) VALUES (?, ?, ?, ?, ?)")) {
        supPstmt.setString(1, studentId);
        supPstmt.setInt(2, semId);
        supPstmt.setInt(3, subId);
        supPstmt.setString(4, status);
        supPstmt.setDouble(5, marks);
        supPstmt.executeUpdate();


        // Update or insert in results
        PreparedStatement update = conn.prepareStatement(
                "UPDATE results SET marks = ?, grade = ? WHERE student_id = ? AND semester_id = ? AND subject_id = ?");
        update.setDouble(1, marks);
        update.setString(2, grade);
        update.setString(3, studentId);
        update.setInt(4, semId);
        update.setInt(5, subId);
        int rows = update.executeUpdate();

        if (rows == 0) {
            // Insert new
            PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO results (student_id, semester_id, subject_id, marks, grade) VALUES (?, ?, ?, ?, ?)");
            ins.setString(1, studentId);
            ins.setInt(2, semId);
            ins.setInt(3, subId);
            ins.setDouble(4, marks);
            ins.setString(5, grade);
            ins.executeUpdate();
            System.out.println("SUP result inserted as new. Status: " + status);
        } else {
            System.out.println("SUP result updated successfully! Status: " + status);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

// Register for new semester (auto-increment, add fee if structure exists)
public void registerNewSemester(String studentId) {
    try {
        PreparedStatement pstmt = conn.prepareStatement("SELECT current_semester, program FROM students WHERE student_id = ?");
        pstmt.setString(1, studentId);
        ResultSet rs = pstmt.executeQuery();
        if (!rs.next()) {
            System.out.println("Student not found.");
            return;
        }
        int current = rs.getInt("current_semester");
        String program = rs.getString("program");
        int newSem = current + 1;
        // Update student
        PreparedStatement update = conn.prepareStatement("UPDATE students SET current_semester = ? WHERE student_id = ?");
        update.setInt(1, newSem);
        update.setString(2, studentId);
        update.executeUpdate();
        // Insert semester
        insertOrGetSemester(newSem);
        // If fee structure exists, note outstanding (no auto-payment)
        if (program != null && !program.isEmpty()) {
            PreparedStatement feeCheck = conn.prepareStatement("SELECT fee_amount FROM fee_structure WHERE program = ? AND semester = ?");
            feeCheck.setString(1, program);
            feeCheck.setInt(2, newSem);
            ResultSet feeRs = feeCheck.executeQuery();
            if (feeRs.next()) {
                System.out.println("Registered for semester " + newSem + ". Fee structure found: $" + feeRs.getDouble("fee_amount") + " due.");
            } else {
                System.out.println("Registered for semester " + newSem + ". No fee structure defined yet.");
            }
        } else {
            System.out.println("Registered for semester " + newSem + ". Program not set.");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

// Set fee structure
public void setFeeStructure(String program, double amount, int semester, String dueDateStr) {
    try {
        java.sql.Date dueDate = java.sql.Date.valueOf(dueDateStr);
        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO fee_structure (program, fee_amount, semester, due_date) VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE fee_amount = ?, due_date = ?");
        pstmt.setString(1, program);
        pstmt.setDouble(2, amount);
        pstmt.setInt(3, semester);
        pstmt.setDate(4, dueDate);
        pstmt.setDouble(5, amount);
        pstmt.setDate(6, dueDate);
        pstmt.executeUpdate();
        System.out.println("Fee structure updated for " + program + " semester " + semester);
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

// Record student payment
public void recordPayment(String studentId, int semester, double amount, String receiptNo) {
    try {
        java.sql.Date payDate = new java.sql.Date(new java.util.Date().getTime());
        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO student_payments (student_id, semester_number, amount_paid, payment_date, receipt_no) " +
                        "VALUES (?, ?, ?, ?, ?)");
        pstmt.setString(1, studentId);
        pstmt.setInt(2, semester);
        pstmt.setDouble(3, amount);
        pstmt.setDate(4, payDate);
        pstmt.setString(5, receiptNo);
        pstmt.executeUpdate();
        System.out.println("Payment recorded. Receipt: " + receiptNo);
        // Simulate receipt (no PDF)
        System.out.println("=== SIMULATED RECEIPT ===");
        System.out.println("Student ID: " + studentId);
        System.out.println("Semester: " + semester);
        System.out.println("Amount Paid: $" + new DecimalFormat("#.##").format(amount));
        System.out.println("Date: " + payDate);
        System.out.println("Receipt No: " + receiptNo);
        System.out.println("========================");
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

// Calculate outstanding for a student and semester
public double computeOutstanding(String studentId, int semester) {
    try {
        // Get program
        PreparedStatement progStmt = conn.prepareStatement("SELECT program FROM students WHERE student_id = ?");
        progStmt.setString(1, studentId);
        ResultSet progRs = progStmt.executeQuery();
        if (!progRs.next()) return -1; // Student not found
        String program = progRs.getString("program");

        // Get total fee
        PreparedStatement feeStmt = conn.prepareStatement(
                "SELECT fee_amount FROM fee_structure WHERE program = ? AND semester = ?");
        feeStmt.setString(1, program);
        feeStmt.setInt(2, semester);
        ResultSet feeRs = feeStmt.executeQuery();
        if (!feeRs.next()) return -1; // No fee structure
        double totalFee = feeRs.getDouble("fee_amount");

        // Get total paid
        PreparedStatement paidStmt = conn.prepareStatement(
                "SELECT SUM(amount_paid) FROM student_payments WHERE student_id = ? AND semester_number = ?");
        paidStmt.setString(1, studentId);
        paidStmt.setInt(2, semester);
        ResultSet paidRs = paidStmt.executeQuery();
        paidRs.next();
        double totalPaid = paidRs.getDouble(1);

        return totalFee - totalPaid;
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return -1;
}

// Check for any outstanding fees across all semesters
public boolean hasOutstandingFees(String studentId) {
    try {
        PreparedStatement semStmt = conn.prepareStatement("SELECT current_semester FROM students WHERE student_id = ?");
        semStmt.setString(1, studentId);
        ResultSet semRs = semStmt.executeQuery();
        if (!semRs.next()) return true;
        int currentSem = semRs.getInt("current_semester");

        for (int sem = 1; sem <= currentSem; sem++) {
            double out = computeOutstanding(studentId, sem);
            if (out > 0) {
                return true;
            }
        }
        return false;
    } catch (SQLException e) {
        e.printStackTrace();
        return true;
    }
}

// Generate Invoice (console simulation)
public void generateInvoicePDF(String studentId, int semester) {
    double outstanding = computeOutstanding(studentId, semester);
    if (outstanding < 0) {
        System.out.println("Cannot generate invoice: Invalid data.");
        return;
    }
    System.out.println("=== SIMULATED INVOICE ===");
    System.out.println("Student ID: " + studentId);
    System.out.println("Semester: " + semester);
    System.out.println("Outstanding Amount: $" + new DecimalFormat("#.##").format(outstanding));
    System.out.println("========================");
    System.out.println("Invoice generated (console). For PDF, add iText JAR.");
}

// Finance Reports
public void generateFinanceReports() {
    DecimalFormat df = new DecimalFormat("#.##");
    LocalDate now = LocalDate.now();

    // Total collections
    try (PreparedStatement totalStmt = conn.prepareStatement("SELECT SUM(amount_paid) FROM student_payments")) {
        ResultSet totalRs = totalStmt.executeQuery();
        totalRs.next();
        double totalCollections = totalRs.getDouble(1);
        System.out.println("Total Collections: $" + df.format(totalCollections));
    } catch (SQLException e) {
        e.printStackTrace();
    }

    // Overdue accounts
    System.out.println("\nOverdue Accounts:");
    System.out.println("Student ID\tSemester\tOutstanding\tDue Date");
    System.out.println("-----------------------------------------------------");
    try (PreparedStatement overStmt = conn.prepareStatement(
            "SELECT DISTINCT s.student_id, fs.semester, fs.due_date " +
                    "FROM students s JOIN fee_structure fs ON s.program = fs.program " +
                    "WHERE s.current_semester >= fs.semester")) {
        ResultSet overRs = overStmt.executeQuery();
        while (overRs.next()) {
            String sid = overRs.getString("student_id");
            int sem = overRs.getInt("semester");
            java.sql.Date due = overRs.getDate("due_date");
            if (due.toLocalDate().isBefore(now)) {
                double out = computeOutstanding(sid, sem);
                if (out > 0) {
                    System.out.println(sid + "\t" + sem + "\t\t$" + df.format(out) + "\t\t" + due);
                }
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

private String computeGrade(double marks) {
    if (marks >= 90) return "A";
    else if (marks >= 80) return "B";
    else if (marks >= 70) return "C";
    else if (marks >= 60) return "D";
    else return "F";
}

private double gradeToPoints(String grade) {
    switch (grade) {
        case "A": return 4.0;
        case "B": return 3.0;
        case "C": return 2.0;
        case "D": return 1.0;
        default: return 0.0;
    }
}

public double computeGPA(String studentId, int semesterNum) {
    if (semesterNum == -1) return computeOverallGPA(studentId);
    double totalPoints = 0;
    double totalCredits = 0;
    int semId = insertOrGetSemester(semesterNum);
    try (PreparedStatement pstmt = conn.prepareStatement(
            "SELECT r.marks FROM results r WHERE r.student_id = ? AND r.semester_id = ?")) {
        pstmt.setString(1, studentId);
        pstmt.setInt(2, semId);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            double marks = rs.getDouble("marks");
            String grade = computeGrade(marks);
            double points = gradeToPoints(grade);
            totalPoints += points * 4;
            totalCredits += 4;
        }
        return totalCredits > 0 ? totalPoints / totalCredits : 0;
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return 0;
}

private double computeOverallGPA(String studentId) {
    double totalPoints = 0;
    double totalCredits = 0;
    try (PreparedStatement pstmt = conn.prepareStatement(
            "SELECT r.marks FROM results r WHERE r.student_id = ?")) {
        pstmt.setString(1, studentId);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            double marks = rs.getDouble("marks");
            String grade = computeGrade(marks);
            double points = gradeToPoints(grade);
            totalPoints += points * 4;
            totalCredits += 4;
        }
        return totalCredits > 0 ? totalPoints / totalCredits : 0;
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return 0;
}

private String getSUPStatus(String studentId, int semNum, String subName, String courseName) {
    try {
        int semId = insertOrGetSemester(semNum);
        int courseId = insertOrGetCourse(courseName);
        int subId = insertOrGetSubject(subName, courseId);
        PreparedStatement pstmt = conn.prepareStatement(
                "SELECT status FROM sup_exams se WHERE se.student_id = ? AND se.semester_id = ? AND se.subject_id = ?");
        pstmt.setString(1, studentId);
        pstmt.setInt(2, semId);
        pstmt.setInt(3, subId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            return rs.getString("status");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return "";
}

// Generate transcripts with fee check and historical view
public void generateTranscript(String studentId, int semesterNum) {
    if (hasOutstandingFees(studentId)) {
        System.out.println("Access denied: Outstanding fees pending.");
        return;
    }
    DecimalFormat df = new DecimalFormat("#.##");
    System.out.println("=== Transcript for Student ID: " + studentId +
            (semesterNum == -1 ? " (All Semesters)" : " (Semester " + semesterNum + ")") + " ===");
    String query;
    if (semesterNum == -1) {
        query = "SELECT s.semester_number, c.course_name, sub.subject_name, r.marks, r.grade FROM results r " +
                "JOIN semesters s ON r.semester_id = s.id " +
                "JOIN subjects sub ON r.subject_id = sub.id " +
                "JOIN courses c ON sub.course_id = c.id " +
                "WHERE r.student_id = ? ORDER BY s.semester_number, c.course_name, sub.subject_name";
    } else {
        int semId = insertOrGetSemester(semesterNum);
        query = "SELECT c.course_name, sub.subject_name, r.marks, r.grade FROM results r " +
                "JOIN subjects sub ON r.subject_id = sub.id " +
                "JOIN courses c ON sub.course_id = c.id " +
                "WHERE r.student_id = ? AND r.semester_id = ? ORDER BY c.course_name, sub.subject_name";
    }
    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
        pstmt.setString(1, studentId);
        if (semesterNum != -1) pstmt.setInt(2, insertOrGetSemester(semesterNum));
        ResultSet rs = pstmt.executeQuery();
        Map<Integer, List<String>> semData = new HashMap<>();
        int currentSem = 0;
        while (rs.next()) {
            int sem = semesterNum == -1 ? rs.getInt("semester_number") : semesterNum;
            String line = rs.getString("course_name") + " - " + rs.getString("subject_name") + "\t" +
                    rs.getDouble("marks") + "\t" + rs.getString("grade");
            semData.computeIfAbsent(sem, k -> new ArrayList<>()).add(line);
            currentSem = sem;
        }
        for (int sem : semData.keySet()) {
            System.out.println("\nSemester " + sem + ":");
            System.out.println("Course - Subject\tMarks\tGrade");
            System.out.println("------------------------------------------");
            for (String line : semData.get(sem)) {
                System.out.println(line);
                // SUP status (simplified, assume course/sub known)
                // In full, query by sub
            }
            double gpa = computeGPA(studentId, sem);
            System.out.println("GPA: " + df.format(gpa));
        }
        if (semesterNum == -1) {
            double overall = computeOverallGPA(studentId);
            System.out.println("\nOverall GPA: " + df.format(overall));
        }
        System.out.println("=== End Transcript ===");
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

public void comparePerformance(String studentId, List<Integer> semesters) {
    System.out.println("=== Performance Comparison for Student ID: " + studentId + " ===");
    System.out.println("Semester\tGPA");
    System.out.println("----------------");
    DecimalFormat df = new DecimalFormat("#.##");
    for (int sem : semesters) {
        double gpa = computeGPA(studentId, sem);
        System.out.println(sem + "\t\t" + df.format(gpa));
    }
    System.out.println("=== End Comparison ===");
}

public static void main(String[] args) {
    ResultsManagementSystem rms = new ResultsManagementSystem();
    Scanner scanner = new Scanner(System.in);

    while (true) {
        System.out.println("\n1. Enter Results\n2. Generate Transcript\n3. Compute GPA\n4. Compare Performance\n5. Register New Semester\n6. Enter SUP\n7. Set Fee Structure\n8. Record Payment\n9. Generate Invoice\n10. Finance Reports\n11. Assign Instructor to Subject\n12. Create Class Schedule\n13. Allocate Student to Class\n14. Exit");
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        switch (choice) {
            case 1:
                rms.enterResults();
                break;
            case 2:
                System.out.print("Enter student ID: ");
                String sid = scanner.nextLine();
                System.out.print("Enter semester (0 for all): ");
                int semInput = scanner.nextInt();
                scanner.nextLine();
                rms.generateTranscript(sid, semInput == 0 ? -1 : semInput);
                break;
            case 3:
                System.out.print("Enter student ID: ");
                sid = scanner.nextLine();
                System.out.print("Enter semester (0 for overall): ");
                semInput = scanner.nextInt();
                scanner.nextLine();
                double gpa = rms.computeGPA(sid, semInput == 0 ? -1 : semInput);
                System.out.println("GPA: " + gpa);
                break;
            case 4:
                System.out.print("Enter student ID: ");
                sid = scanner.nextLine();
                System.out.print("Enter semesters (comma-separated): ");
                String[] semStr = scanner.nextLine().split(",");
                List<Integer> sems = new ArrayList<>();
                for (String s : semStr) {
                    sems.add(Integer.parseInt(s.trim()));
                }
                rms.comparePerformance(sid, sems);
                break;
            case 5:
                System.out.print("Enter student ID: ");
                String regSid = scanner.nextLine();
                rms.registerNewSemester(regSid);
                break;
            case 6:
                rms.enterSUP();
                break;
            case 7:
                System.out.print("Enter program: ");
                String prog = scanner.nextLine();
                System.out.print("Enter fee amount: ");
                double amt = scanner.nextDouble();
                System.out.print("Enter semester: ");
                int fsSem = scanner.nextInt();
                scanner.nextLine();
                System.out.print("Enter due date (YYYY-MM-DD): ");
                String dueStr = scanner.nextLine();
                rms.setFeeStructure(prog, amt, fsSem, dueStr);
                break;
            case 8:
                System.out.print("Enter student ID: ");
                String paySid = scanner.nextLine();
                System.out.print("Enter semester: ");
                int paySem = scanner.nextInt();
                System.out.print("Enter amount paid: ");
                double payAmt = scanner.nextDouble();
                scanner.nextLine();
                System.out.print("Enter receipt no: ");
                String recNo = scanner.nextLine();
                rms.recordPayment(paySid, paySem, payAmt, recNo);
                break;
            case 9:
                System.out.print("Enter student ID: ");
                String invSid = scanner.nextLine();
                System.out.print("Enter semester: ");
                int invSem = scanner.nextInt();
                scanner.nextLine();
                rms.generateInvoicePDF(invSid, invSem);
                break;
            case 10:
                rms.generateFinanceReports();
                break;
            case 11:
                // Assign Instructor to Subject
                System.out.print("Enter subject name: ");
                String subName = scanner.nextLine();
                System.out.print("Enter course name: ");
                String courseName = scanner.nextLine();
                int courseId = rms.insertOrGetCourse(courseName);
                int subId = rms.insertOrGetSubject(subName, courseId);
                System.out.print("Enter instructor name: ");
                String instName = scanner.nextLine();
                System.out.print("Enter instructor ID: ");
                String instId = scanner.nextLine();
                int instructorDbId11 = rms.insertOrGetInstructor(instName, instId); // Renamed to avoid conflict
                rms.assignInstructorToSubject(subId, instructorDbId11);
                break;
            case 12:
                // Create Class Schedule
                System.out.print("Enter day (e.g., Monday): ");
                String day = scanner.nextLine();
                System.out.print("Enter time slot (e.g., 10:00-11:00): ");
                String timeSlot = scanner.nextLine();
                System.out.print("Enter subject name: ");
                subName = scanner.nextLine();
                System.out.print("Enter course name: ");
                courseName = scanner.nextLine();
                courseId = rms.insertOrGetCourse(courseName);
                subId = rms.insertOrGetSubject(subName, courseId);
                System.out.print("Enter instructor ID: ");
                instId = scanner.nextLine();
                int instructorDbId12 = -1; // Renamed to avoid conflict
                try {
                    PreparedStatement instSelect = rms.conn.prepareStatement("SELECT id FROM instructors WHERE instructor_id = ?");
                    instSelect.setString(1, instId);
                    ResultSet instRs = instSelect.executeQuery();
                    if (instRs.next()) instructorDbId12 = instRs.getInt("id");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                if (instructorDbId12 == -1) {
                    System.out.println("Instructor not found.");
                    break;
                }
                System.out.print("Enter room: ");
                String room = scanner.nextLine();
                System.out.print("Enter semester number: ");
                int schSem = scanner.nextInt();
                scanner.nextLine();
                rms.createClassSchedule(day, timeSlot, subId, instructorDbId12, room, schSem);
                break;
            case 13:
                // Allocate Student to Class
                System.out.print("Enter student ID: ");
                String allocSid = scanner.nextLine();
                System.out.print("Enter class ID: ");
                int classId = scanner.nextInt();
                scanner.nextLine();
                rms.allocateStudentToClass(allocSid, classId);
                break;
            case 14:
                System.exit(0);
                break;
        }
    }
}
  
}
