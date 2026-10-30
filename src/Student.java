import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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

    // Displays assignments and checks if a file BLOB exists in the 'file' column
    public void viewAssignments() {
        String query = "SELECT id, title, due_date, max_points, description, course_id, file FROM assignment";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            String[] columnNames = {"Assignment ID", "Title", "Course ID", "Due Date", "Max Points", "Description", "Has File"};
            DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            while (rs.next()) {
                String id = rs.getString("id");
                String title = rs.getString("title");
                String courseId = rs.getString("course_id");
                String dueDate = rs.getTimestamp("due_date") != null ? rs.getTimestamp("due_date").toString() : "N/A";
                int maxPoints = rs.getInt("max_points");
                String description = rs.getString("description");
                
                // Check if BLOB data exists in the 'file' column
                byte[] blobCheck = rs.getBytes("file");
                String fileStatus = (blobCheck != null && blobCheck.length > 0) ? "Available" : "No File";

                tableModel.addRow(new Object[]{id, title, courseId, dueDate, maxPoints, description, fileStatus});
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
                String title = (String) tableModel.getValueAt(selectedRow, 1);
                String fileStatus = (String) tableModel.getValueAt(selectedRow, 6);

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

    // Helper method to retrieve binary data from the 'file' column and save locally
    private void downloadAssignmentFile(String assignmentId, String title) {
        String query = "SELECT file FROM assignment WHERE id = ?";

        // Generate a safe default file name from assignment title
        String defaultFileName = title.replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf";

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(defaultFileName));

        int userSelection = fileChooser.showSaveDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File targetFile = fileChooser.getSelectedFile();

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, assignmentId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    byte[] fileBytes = rs.getBytes("file");

                    if (fileBytes == null || fileBytes.length == 0) {
                        JOptionPane.showMessageDialog(null, "The file in the database is empty.", "Download Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Save binary BLOB stream to the user's selected file path
                    try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                        fos.write(fileBytes);
                    }

                    JOptionPane.showMessageDialog(null, "Assignment file downloaded successfully!\nSaved to: " + targetFile.getAbsolutePath(), "Download Complete", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Assignment not found in database.", "Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to download file: " + ex.getMessage(), "Download Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void submitAssignment() {
        String assignmentId = JOptionPane.showInputDialog(null, "Enter Assignment ID (e.g., ASG01):", "Submit Assignment", JOptionPane.QUESTION_MESSAGE);
        if (assignmentId == null || assignmentId.trim().isEmpty()) return;

        String content = JOptionPane.showInputDialog(null, "Enter Submission URL/Link:", "Submit Assignment", JOptionPane.QUESTION_MESSAGE);
        if (content == null || content.trim().isEmpty()) return;

        String query = "INSERT INTO submission (id, content, submission_date, status, assignment_id, student_id) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            String submissionId = "SUB" + System.currentTimeMillis();
            java.sql.Timestamp currentDate = new java.sql.Timestamp(System.currentTimeMillis());

            pstmt.setString(1, submissionId);
            pstmt.setString(2, content.trim());
            pstmt.setTimestamp(3, currentDate);
            pstmt.setString(4, "SUBMITTED");
            pstmt.setString(5, assignmentId.trim());
            pstmt.setString(6, this.studentId);

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                JOptionPane.showMessageDialog(null, "Submission saved to database successfully!\nID: " + submissionId, "Success", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void viewGrades() {
        JOptionPane.showMessageDialog(null, "Grades for " + getName() + ":\n• CS101 Lab 1: 95.5 / 100", "Student Grades", JOptionPane.INFORMATION_MESSAGE);
    }

    public void viewCourseMaterials() {
        JOptionPane.showMessageDialog(null, "Course Materials for CS101:\n1. Chapter 1: Basics (PDF)\n2. Lab 1 Guidelines (PDF)", "Course Materials", JOptionPane.INFORMATION_MESSAGE);
    }
}