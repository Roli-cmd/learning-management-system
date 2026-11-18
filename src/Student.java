import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class Student extends User {
    private String studentId;

    public Student(String id, String name, String email, String studentId) {
        super(id, name, email, "STUDENT");
        this.studentId = studentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void joinClass() {
        JoinClasses.joinClass(this);
    }

    // =====================================================================
    // VIEW ASSIGNMENTS
    // =====================================================================
    public void viewAssignments() {
        String query = "SELECT id, title, due_date, description, course_id, file FROM assignment";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            String[] columnNames = {"Assignment ID", "Title", "Course ID", "Due Date", "Description", "Has File"};
            DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            while (rs.next()) {
                String id          = rs.getString("id");
                String title       = rs.getString("title");
                String courseId    = rs.getString("course_id");
                String dueDate     = rs.getTimestamp("due_date") != null ? rs.getTimestamp("due_date").toString() : "N/A";
                String description = rs.getString("description");

                byte[] blobCheck  = rs.getBytes("file");
                String fileStatus = (blobCheck != null && blobCheck.length > 0) ? "Available" : "No File";

                tableModel.addRow(new Object[]{id, title, courseId, dueDate, description, fileStatus});
            }

            if (tableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(null, "No assignments currently available.", "Assignments", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JTable table = new JTable(tableModel);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setFillsViewportHeight(true);

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(new Dimension(750, 250));

            JButton btnDownload = new JButton("Download Selected Assignment File");

            JPanel containerPanel = new JPanel(new BorderLayout(10, 10));
            containerPanel.add(scrollPane, BorderLayout.CENTER);
            containerPanel.add(btnDownload, BorderLayout.SOUTH);

            btnDownload.addActionListener(e -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(null, "Please select an assignment from the list to download.", "No Selection", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String assignmentId = (String) tableModel.getValueAt(selectedRow, 0);
                String title        = (String) tableModel.getValueAt(selectedRow, 1);
                String fileStatus   = (String) tableModel.getValueAt(selectedRow, 5);

                if ("No File".equals(fileStatus)) {
                    JOptionPane.showMessageDialog(null, "No file is attached to this assignment.", "File Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                downloadAssignmentFile(assignmentId, title);
            });

            JOptionPane.showMessageDialog(null, containerPanel, "View & Download Assignments", JOptionPane.PLAIN_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =====================================================================
    // DOWNLOAD ASSIGNMENT
    // =====================================================================
    private void downloadAssignmentFile(String assignmentId, String title) {
        String query = "SELECT file FROM assignment WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, assignmentId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(null, "Assignment not found in database.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                byte[] fileBytes = rs.getBytes("file");
                if (fileBytes == null || fileBytes.length == 0) {
                    JOptionPane.showMessageDialog(null, "The file in the database is empty.", "Download Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String extension    = detectExtension(fileBytes);
                String safeName     = title.replaceAll("[^a-zA-Z0-9.-]", "_");
                String defaultName  = safeName + extension;

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new File(defaultName));

                if (fileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return;
                File targetFile = fileChooser.getSelectedFile();

                try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                    fos.write(fileBytes);
                }

                JOptionPane.showMessageDialog(null, "Assignment file downloaded successfully!\nSaved to: " + targetFile.getAbsolutePath(), "Download Complete", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to download file: " + ex.getMessage(), "Download Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String detectExtension(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return ".bin";
        if (bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) return ".pdf";
        if (bytes[0] == 0x50 && bytes[1] == 0x4B && bytes[2] == 0x03 && bytes[3] == 0x04) return ".zip";
        if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return ".png";
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) return ".jpg";
        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46) return ".gif";
        if (bytes[0] >= 0x20 && bytes[0] < 0x7F) return ".txt";
        return ".bin";
    }

    // =====================================================================
    // SUBMIT ASSIGNMENT (FIXED SQL AND ADDED TEACHER_ID FETCH)
    // =====================================================================
    public void submitAssignment() {
        String assignmentId = JOptionPane.showInputDialog(null, "Enter Assignment ID (e.g., ASG01):", "Submit Assignment", JOptionPane.QUESTION_MESSAGE);
        if (assignmentId == null || assignmentId.trim().isEmpty()) return;

        // 1. Verify the assignment exists and get the teacher_id to prevent DB crash
        String teacherId = null;
        String courseId = null;
        String checkAsgQuery = "SELECT teacher_id, course_id FROM assignment WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(checkAsgQuery)) {
            pstmt.setString(1, assignmentId.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    teacherId = rs.getString("teacher_id");
                    courseId = rs.getString("course_id");
                } else {
                    JOptionPane.showMessageDialog(null, "Assignment ID not found. Please check the ID.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Check if the student is already enrolled in this course
        String checkEnrollQuery = "SELECT 1 FROM course_enrollments WHERE student_id = ? AND course_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(checkEnrollQuery)) {
            pstmt.setString(1, this.studentId);
            pstmt.setString(2, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(null, "You are not enrolled in the course for this assignment (" + courseId + ").", "Access Denied", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        } catch (Exception ex) {
             JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
             return;
        }

        // 3. Prompt for submission content (URL/Link)
        String content = JOptionPane.showInputDialog(null, "Enter Submission URL/Link:", "Submit Assignment", JOptionPane.QUESTION_MESSAGE);
        if (content == null || content.trim().isEmpty()) return;

        // 4. Insert into database (Corrected columns based on your screenshot)
        String insertQuery = "INSERT INTO submission (teacher_id, content, submission_date, status, assignment_id, student_id) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

            java.sql.Timestamp currentDate = new java.sql.Timestamp(System.currentTimeMillis());

            pstmt.setString(1, teacherId);
            pstmt.setString(2, content.trim());
            pstmt.setTimestamp(3, currentDate);
            pstmt.setString(4, "SUBMITTED");
            pstmt.setString(5, assignmentId.trim());
            pstmt.setString(6, this.studentId);

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                JOptionPane.showMessageDialog(null, "Assignment submitted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =====================================================================
    // VIEW GRADES (Fixed to actually query the database!)
    // =====================================================================
    public void viewGrades() {
        String query = "SELECT c.name AS course_name, c.code AS course_code, g.grade, g.feedback, g.graded_date " +
                       "FROM grade g " +
                       "JOIN course c ON g.course_id = c.id " +
                       "WHERE g.student_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, this.studentId);
            ResultSet rs = pstmt.executeQuery();

            String[] columnNames = {"Course Code", "Course Name", "Grade", "Feedback", "Graded Date"};
            DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            while (rs.next()) {
                double gradeVal = rs.getDouble("grade");
                String gradeStr = rs.wasNull() ? "Not Graded" : String.format("%.2f", gradeVal);
                Timestamp ts = rs.getTimestamp("graded_date");
                String dateStr = ts == null ? "N/A" : ts.toString();

                tableModel.addRow(new Object[]{
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        gradeStr,
                        rs.getString("feedback"),
                        dateStr
                });
            }

            if (tableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(null, "You have no grades recorded yet.", "My Grades", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JTable table = new JTable(tableModel);
            table.setFillsViewportHeight(true);
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(new Dimension(600, 200));

            JOptionPane.showMessageDialog(null, scrollPane, "My Grades", JOptionPane.PLAIN_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =====================================================================
    // VIEW COURSE MATERIALS
    // =====================================================================
    public void viewCourseMaterials() {
        String query = "SELECT id, title, type, course_id, description, file FROM material";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            String[] columnNames = {"Material ID", "Title", "Type", "Course ID", "Description", "Has File"};
            DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            while (rs.next()) {
                String id          = rs.getString("id");
                String title       = rs.getString("title");
                String type        = rs.getString("type");
                String courseId    = rs.getString("course_id");
                String description = rs.getString("description");

                byte[] blobCheck  = rs.getBytes("file");
                String fileStatus = (blobCheck != null && blobCheck.length > 0) ? "Available" : "No File";

                tableModel.addRow(new Object[]{id, title, type, courseId, description, fileStatus});
            }

            if (tableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(null, "No course materials currently available.", "Course Materials", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JTable table = new JTable(tableModel);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setFillsViewportHeight(true);

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(new Dimension(750, 250));

            JButton btnDownload = new JButton("Download Selected Material");

            JPanel containerPanel = new JPanel(new BorderLayout(10, 10));
            containerPanel.add(scrollPane, BorderLayout.CENTER);
            containerPanel.add(btnDownload, BorderLayout.SOUTH);

            btnDownload.addActionListener(e -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(null, "Please select a material from the list to download.", "No Selection", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String materialId = (String) tableModel.getValueAt(selectedRow, 0);
                String title      = (String) tableModel.getValueAt(selectedRow, 1);
                String type       = (String) tableModel.getValueAt(selectedRow, 2);
                String fileStatus = (String) tableModel.getValueAt(selectedRow, 5);

                if ("No File".equals(fileStatus)) {
                    JOptionPane.showMessageDialog(null, "No file is attached to this material.", "File Not Found", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                downloadMaterialFile(materialId, title, type);
            });

            JOptionPane.showMessageDialog(null, containerPanel, "View & Download Course Materials", JOptionPane.PLAIN_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =====================================================================
    // DOWNLOAD COURSE MATERIAL
    // =====================================================================
    private void downloadMaterialFile(String materialId, String title, String type) {
        String query = "SELECT file FROM material WHERE id = ?";

        String extension = ".bin";
        if (type != null && !type.trim().isEmpty()) {
            extension = "." + type.trim().toLowerCase();
        }

        String defaultFileName = title.replaceAll("[^a-zA-Z0-9.-]", "_") + extension;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(defaultFileName));

        if (fileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return;
        File targetFile = fileChooser.getSelectedFile();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, materialId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(null, "Material not found in database.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                byte[] fileBytes = rs.getBytes("file");
                if (fileBytes == null || fileBytes.length == 0) {
                    JOptionPane.showMessageDialog(null, "The file in the database is empty.", "Download Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                    fos.write(fileBytes);
                }

                JOptionPane.showMessageDialog(null, "Course material downloaded successfully!\nSaved to: " + targetFile.getAbsolutePath(), "Download Complete", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to download file: " + ex.getMessage(), "Download Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}