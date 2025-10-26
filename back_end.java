import java.sql.*;
import java.util.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// Updated Results Management System in Java
// Full Combined System: Results, Finance, Class Management
// Features: Enter results/SUPs, transcripts/GPA, semester registration, fee structure/payments/outstanding, scheduling with conflicts, allocations
// PDF generation removed to avoid iText dependency; use console output instead. Add iText JAR for PDFs.

public class ResultsManagementSystem {
    private static final String DB_URL = "jdbc:h2:mem:resultsdb";
    private static final String DB_DRIVER = "org.h2.Driver";

    private static final String CREATE_STUDENTS =
            "CREATE TABLE students (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100), student_id VARCHAR(20), " +
                    "current_semester INT DEFAULT 1, program VARCHAR(50), PRIMARY KEY(student_id))";

    private static final String CREATE_COURSES =
            "CREATE TABLE courses (id INT PRIMARY KEY AUTO_INCREMENT, course_name VARCHAR(100))";

    private static final String CREATE_SEMESTERS =
            "CREATE TABLE semesters (id INT PRIMARY KEY AUTO_INCREMENT, semester_number INT UNIQUE)";

    private static final String CREATE_SUBJECTS =
            "CREATE TABLE subjects (id INT PRIMARY KEY AUTO_INCREMENT, subject_name VARCHAR(50), course_id INT, " +
                    "FOREIGN KEY(course_id) REFERENCES courses(id))";

    private static final String CREATE_INSTRUCTORS =
            "CREATE TABLE instructors (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100), instructor_id VARCHAR(20) UNIQUE)";

    private static final String CREATE_SUBJECT_INSTRUCTORS =
            "CREATE TABLE subject_instructors (subject_id INT, instructor_id INT, PRIMARY KEY(subject_id, instructor_id), " +
                    "FOREIGN KEY(subject_id) REFERENCES subjects(id), FOREIGN KEY(instructor_id) REFERENCES instructors(id))";

    private static final String CREATE_CLASSES =
            "CREATE TABLE classes (id INT PRIMARY KEY AUTO_INCREMENT, day VARCHAR(10), time_slot VARCHAR(20), " +
                    "subject_id INT, instructor_id INT, room VARCHAR(20), semester_number INT, " +
                    "FOREIGN KEY(subject_id) REFERENCES subjects(id), FOREIGN KEY(instructor_id) REFERENCES instructors(id))";

    private static final String CREATE_STUDENT_CLASSES =
            "CREATE TABLE student_classes (student_id VARCHAR(20), class_id INT, PRIMARY KEY(student_id, class_id), " +
                    "FOREIGN KEY(student_id) REFERENCES students(student_id), FOREIGN KEY(class_id) REFERENCES classes(id))";

    private static final String CREATE_RESULTS =
            "CREATE TABLE results (id INT PRIMARY KEY AUTO_INCREMENT, student_id VARCHAR(20), " +
                    "semester_id INT, subject_id INT, marks DOUBLE, grade VARCHAR(5), " +
                    "FOREIGN KEY(student_id) REFERENCES students(student_id), " +
                    "FOREIGN KEY(semester_id) REFERENCES semesters(id), " +
                    "FOREIGN KEY(subject_id) REFERENCES subjects(id))";

    private static final String CREATE_SUP_EXAMS =
            "CREATE TABLE sup_exams (id INT PRIMARY KEY AUTO_INCREMENT, student_id VARCHAR(20), " +
                    "semester_id INT, subject_id INT, status VARCHAR(10), marks DOUBLE, " +
                    "FOREIGN KEY(student_id) REFERENCES students(student_id), " +
                    "FOREIGN KEY(semester_id) REFERENCES semesters(id), " +
                    "FOREIGN KEY(subject_id) REFERENCES subjects(id))";

    private static final String CREATE_FEE_STRUCTURE =
            "CREATE TABLE fee_structure (id INT PRIMARY KEY AUTO_INCREMENT, program VARCHAR(50), " +
                    "fee_amount DOUBLE, semester INT, due_date DATE, PRIMARY KEY(program, semester))";

    private static final String CREATE_STUDENT_PAYMENTS =
            "CREATE TABLE student_payments (id INT PRIMARY KEY AUTO_INCREMENT, student_id VARCHAR(20), " +
                    "semester_number INT, amount_paid DOUBLE, payment_date DATE, receipt_no VARCHAR(20), " +
                    "FOREIGN KEY(student_id) REFERENCES students(student_id))";

    private Connection conn;

    public ResultsManagementSystem() {
        initDatabase();
    }

    private void initDatabase() {
        try {
            Class.forName(DB_DRIVER);
            conn = DriverManager.getConnection(DB_URL, "sa", "tedd");
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
