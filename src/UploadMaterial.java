import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class UploadMaterial {

    // ==========================================
    // Called directly from the ManageMaterialsFrame
    // ==========================================
    public static void uploadDirectlyToCourse(String courseId, String courseName) {
        // 1. Title
        String title = JOptionPane.showInputDialog(null,
                "Course: " + courseName + " [" + courseId + "]\n\nEnter material title:",
                "Material Title", JOptionPane.QUESTION_MESSAGE);
        if (title == null || title.trim().isEmpty()) return;
        title = title.trim();

        // 2. Type
        String[] types = {"PDF", "DOCX", "DOC", "PPTX", "PPT", "XLSX", "XLS", "TXT", "IMAGE", "VIDEO", "ZIP", "OTHER"};
        String type = (String) JOptionPane.showInputDialog(null,
                "Select material type:", "Material Type",
                JOptionPane.QUESTION_MESSAGE, null, types, types[0]);
        if (type == null) return;

        // 3. Description
        String description = JOptionPane.showInputDialog(null,
                "Enter description (optional):", "Material Description", JOptionPane.QUESTION_MESSAGE);
        if (description == null) description = "";
        description = description.trim();

        // 4. File
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select material file");
        FileNameExtensionFilter filter = buildFilterForType(type);
        if (filter != null) chooser.setFileFilter(filter);

        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (file == null || !file.exists()) return;

        // 5. Read & Insert
        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(file.toPath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Could not read file.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String newId = "MAT" + System.currentTimeMillis();
        String insertQuery = "INSERT INTO material (id, title, type, file, course_id, description) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

            pstmt.setString(1, newId);
            pstmt.setString(2, title);
            pstmt.setString(3, type);
            pstmt.setBytes(4, fileBytes);
            pstmt.setString(5, courseId);
            pstmt.setString(6, description);

            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(null, "Material uploaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // Helper: Filter file extensions
    // ==========================================
    private static FileNameExtensionFilter buildFilterForType(String type) {
        if (type == null) return null;
        switch (type) {
            case "PDF":   return new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf");
            case "DOCX":  return new FileNameExtensionFilter("Word Documents (*.docx)", "docx");
            case "DOC":   return new FileNameExtensionFilter("Word Documents (*.doc)", "doc");
            case "PPTX":  return new FileNameExtensionFilter("PowerPoint (*.pptx)", "pptx");
            case "PPT":   return new FileNameExtensionFilter("PowerPoint (*.ppt)", "ppt");
            case "XLSX":  return new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx");
            case "XLS":   return new FileNameExtensionFilter("Excel (*.xls)", "xls");
            case "TXT":   return new FileNameExtensionFilter("Text Files (*.txt)", "txt");
            case "IMAGE": return new FileNameExtensionFilter("Images", "png", "jpg", "jpeg", "gif");
            case "VIDEO": return new FileNameExtensionFilter("Videos", "mp4", "avi", "mkv");
            case "ZIP":   return new FileNameExtensionFilter("Archives", "zip", "rar", "7z");
            default:      return null;
        }
    }
}