import javax.swing.*;
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

    /**
     * Enrolls a student into a course using the Course ID or Course Code,
     * saving the record into the database table `course_enrollments`.
     */
    public static boolean enrollStudent(String studentId, String courseIdOrCode) {
        String findCourseQuery = "SELECT id, name FROM course WHERE id = ? OR code = ?";
        String checkEnrollmentQuery = "SELECT * FROM course_enrollments WHERE student_id = ? AND course_id = ?";
        String insertEnrollmentQuery = "INSERT INTO course_enrollments (student_id, course_id) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            
            // Step 1: Resolve the course ID from either the ID (e.g., C101) or Code (e.g., CS101)
            String targetCourseId = null;
            String targetCourseName = null;

            try (PreparedStatement pstmt = conn.prepareStatement(findCourseQuery)) {
                pstmt.setString(1, courseIdOrCode);
                pstmt.setString(2, courseIdOrCode);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        targetCourseId = rs.getString("id");
                        targetCourseName = rs.getString("name");
                    } else {
                        JOptionPane.showMessageDialog(null, "Course or class not found for ID/Code: " + courseIdOrCode, "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }

            // Step 2: Check if student is already enrolled
            try (PreparedStatement pstmt = conn.prepareStatement(checkEnrollmentQuery)) {
                pstmt.setString(1, studentId);
                pstmt.setString(2, targetCourseId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        JOptionPane.showMessageDialog(null, "You are joining  course or class in " + targetCourseName + " (" + targetCourseId + ").", "Notice", JOptionPane.WARNING_MESSAGE);
                        return false;
                    }
                }
            }

            // Step 3: Insert new enrollment record into `course_enrollments`
            try (PreparedStatement pstmt = conn.prepareStatement(insertEnrollmentQuery)) {
                pstmt.setString(1, studentId);
                pstmt.setString(2, targetCourseId);

                int rowsInserted = pstmt.executeUpdate();
                if (rowsInserted > 0) {
                    JOptionPane.showMessageDialog(null, "Successfully enrolled in course: " + targetCourseName + " [" + targetCourseId + "]", "Enrollment Successful", JOptionPane.INFORMATION_MESSAGE);
                    return true;
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        return false;
    }
}