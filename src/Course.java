import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Course {
    private String id;
    private String name;
    private String code;
    private String description;

    public Course(String id, String name, String code, String description) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getDescription() { return description; }

    // =========================================================
    // STUDENT ENROLLMENT (used by JoinClasses.joinClass)
    // =========================================================
    public static boolean enrollStudent(String studentId, String courseIdOrCode) {
        // 1. Get course details and class_link directly from the course table
        String findCourseQuery = "SELECT id, name, class_link FROM course WHERE id = ? OR code = ?";

        // 2. Check if the student is already enrolled
        String checkEnrollmentQuery = "SELECT * FROM course_enrollments WHERE student_id = ? AND course_id = ?";

        // 3. Insert new enrollment
        String insertEnrollmentQuery = "INSERT INTO course_enrollments (student_id, course_id) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {

            // Step 1: Resolve course ID, name, and link
            String targetCourseId = null;
            String targetCourseName = null;
            String classLink = null;

            try (PreparedStatement pstmt = conn.prepareStatement(findCourseQuery)) {
                pstmt.setString(1, courseIdOrCode);
                pstmt.setString(2, courseIdOrCode);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        targetCourseId = rs.getString("id");
                        targetCourseName = rs.getString("name");
                        classLink = rs.getString("class_link");
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "Course or class not found for ID/Code: " + courseIdOrCode,
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }

            // Step 2: Check if already enrolled
            try (PreparedStatement pstmt = conn.prepareStatement(checkEnrollmentQuery)) {
                pstmt.setString(1, studentId);
                pstmt.setString(2, targetCourseId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        // Already enrolled - show the link
                        showClassLinkDialog("You are already enrolled in course: ",
                                targetCourseName, targetCourseId, classLink);
                        return false;
                    }
                }
            }

            // Step 3: Insert new enrollment record
            try (PreparedStatement pstmt = conn.prepareStatement(insertEnrollmentQuery)) {
                pstmt.setString(1, studentId);
                pstmt.setString(2, targetCourseId);

                int rowsInserted = pstmt.executeUpdate();
                if (rowsInserted > 0) {
                    // Successfully enrolled - show the link
                    showClassLinkDialog("Successfully enrolled in course: ",
                            targetCourseName, targetCourseId, classLink);
                    return true;
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Database Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

        return false;
    }

    // =========================================================
    // ADMIN LOOKUP (used by JoinClasses.openClassLinkAsAdmin)
    // Admin only reads course.class_link - no enrollment write.
    // =========================================================
    public static void showClassLink(String courseIdOrCode) {
        String query = "SELECT id, name, class_link FROM course WHERE id = ? OR code = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, courseIdOrCode);
            pstmt.setString(2, courseIdOrCode);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String courseId   = rs.getString("id");
                    String courseName = rs.getString("name");
                    String classLink  = rs.getString("class_link");

                    showClassLinkDialog("Course: ", courseName, courseId, classLink);
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Course not found for ID/Code: " + courseIdOrCode,
                            "Not Found", JOptionPane.WARNING_MESSAGE);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Database Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================
    // SHARED DIALOG: shows class link + Open / Copy buttons
    // Used by students, teachers and admins
    // =========================================================
    public static void showClassLinkDialog(String prefix, String courseName, String courseId, String classLink) {
        // If no link exists for the course
        if (classLink == null || classLink.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    prefix + courseName + " [" + courseId + "]\n\n" +
                            "No class link is available for this course.",
                    "Course Details", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Build the dialog UI
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(new JLabel("<html>" + prefix + "<b>" + courseName + "</b> [" + courseId + "]" +
                        "<br><br><b>Class Link:</b></html>"),
                BorderLayout.NORTH);

        JTextField linkField = new JTextField(classLink);
        linkField.setEditable(false);
        panel.add(linkField, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCopy = new JButton("Copy Link");
        JButton btnOpen = new JButton("Open Link");

        btnCopy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(classLink), null);
            JOptionPane.showMessageDialog(panel, "Link copied to clipboard!",
                    "Copied", JOptionPane.INFORMATION_MESSAGE);
        });

        btnOpen.addActionListener(e -> {
            try {
                if (Desktop.isDesktopSupported() &&
                        Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(classLink.trim()));
                } else {
                    JOptionPane.showMessageDialog(panel,
                            "Cannot open browser automatically.\n\nLink:\n" + classLink,
                            "Class Link", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel,
                        "Could not open link: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttons.add(btnCopy);
        buttons.add(btnOpen);
        panel.add(buttons, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(null, panel,
                "Course Details", JOptionPane.INFORMATION_MESSAGE);
    }
}