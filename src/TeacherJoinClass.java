import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TeacherJoinClass {
    public static void joinClass(Teacher teacher) {
        String courseInput = JOptionPane.showInputDialog(null, 
                "Enter Course ID (e.g., C101) or Course Code (e.g., CS101) to join:", 
                "Join Class", 
                JOptionPane.QUESTION_MESSAGE);

        if (courseInput != null && !courseInput.trim().isEmpty()) {
            assignTeacherToCourse(teacher.getTeacherId(), courseInput.trim());
        }
    }

    private static void assignTeacherToCourse(String teacherId, String courseIdOrCode) {
        // 1. Update query to also select the class_link
        String findCourseQuery = "SELECT id, name, class_link FROM course WHERE id = ? OR code = ?";
        String updateTeacherQuery = "UPDATE course SET teacher_id = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement findStmt = conn.prepareStatement(findCourseQuery)) {

            findStmt.setString(1, courseIdOrCode);
            findStmt.setString(2, courseIdOrCode);

            try (ResultSet rs = findStmt.executeQuery()) {
                if (rs.next()) {
                    String courseId = rs.getString("id");
                    String courseName = rs.getString("name");
                    String classLink = rs.getString("class_link"); // 2. Retrieve the link

                    try (PreparedStatement updateStmt = conn.prepareStatement(updateTeacherQuery)) {
                        updateStmt.setString(1, teacherId);
                        updateStmt.setString(2, courseId);
                        
                        int rows = updateStmt.executeUpdate();
                        if (rows > 0) {
                            // 3. Call the course dialog to show the link to the teacher
                            Course.showClassLinkDialog("Successfully joined class: ", courseName, courseId, classLink);
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Course not found for ID/Code: " + courseIdOrCode, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}