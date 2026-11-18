import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class EditClassLink {
    public static void showDialog(Teacher teacher) {
        String courseInput = JOptionPane.showInputDialog(null,
                "Enter Course ID or Code to edit the class link:",
                "Edit Class Link",
                JOptionPane.QUESTION_MESSAGE);

        if (courseInput == null || courseInput.trim().isEmpty()) return;

        String courseIdOrCode = courseInput.trim();
        
        // Verify teacher owns this course and get current link
        String checkQuery = "SELECT id, name, class_link FROM course WHERE (id = ? OR code = ?) AND teacher_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(checkQuery)) {
            
            pstmt.setString(1, courseIdOrCode);
            pstmt.setString(2, courseIdOrCode);
            pstmt.setString(3, teacher.getTeacherId());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String courseId = rs.getString("id");
                    String courseName = rs.getString("name");
                    String currentLink = rs.getString("class_link");

                    String newLink = JOptionPane.showInputDialog(null,
                            "Editing link for: " + courseName + " [" + courseId + "]\n\n" +
                            "Enter the new Class Link (Zoom, Google Meet, Teams, etc.):",
                            currentLink == null ? "" : currentLink);

                    if (newLink != null) { // User clicked OK (can be empty string to clear)
                        updateLink(conn, courseId, newLink.trim());
                    }
                } else {
                    JOptionPane.showMessageDialog(null, 
                            "Course not found, or you are not the assigned instructor for this course.", 
                            "Access Denied", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void updateLink(Connection conn, String courseId, String newLink) throws Exception {
        String updateQuery = "UPDATE course SET class_link = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
            if (newLink.isEmpty()) {
                pstmt.setNull(1, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(1, newLink);
            }
            pstmt.setString(2, courseId);
            pstmt.executeUpdate();
            
            JOptionPane.showMessageDialog(null, "Class link updated successfully!", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}