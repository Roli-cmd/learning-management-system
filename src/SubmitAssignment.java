import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class SubmitAssignment {

    public static void showDialog(Student student) {
        JDialog dialog = new JDialog((Frame) null, "Submit Assignment", true);
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout(10, 10));

        // Main Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Student Info (Read-only)
        JTextField txtStudent = new JTextField(student.getName() + " (" + student.getStudentId() + ")");
        txtStudent.setEditable(false);
        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Student:"), gbc);
        gbc.gridx = 1; formPanel.add(txtStudent, gbc);

        // Assignment ID Input
        JTextField txtAssignmentId = new JTextField();
        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Assignment ID (e.g., ASG01):"), gbc);
        gbc.gridx = 1; formPanel.add(txtAssignmentId, gbc);

        // Submission Content Input
        JTextArea txtContent = new JTextArea();
        txtContent.setLineWrap(true);
        txtContent.setWrapStyleWord(true);
        JScrollPane scrollContent = new JScrollPane(txtContent);
        scrollContent.setPreferredSize(new Dimension(300, 150));
        
        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Your Submission:"), gbc);
        gbc.gridx = 1; formPanel.add(scrollContent, gbc);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSubmit = new JButton("Submit");
        JButton btnCancel = new JButton("Cancel");
        btnPanel.add(btnSubmit);
        btnPanel.add(btnCancel);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        // Actions
        btnCancel.addActionListener(e -> dialog.dispose());

        btnSubmit.addActionListener(e -> {
            String assignmentId = txtAssignmentId.getText().trim();
            String content = txtContent.getText().trim();

            if (assignmentId.isEmpty() || content.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in both the Assignment ID and your submission content.", "Missing Info", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean success = saveSubmission(student.getStudentId(), assignmentId, content);
            if (success) {
                dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }

    private static boolean saveSubmission(String studentId, String assignmentId, String content) {
        // 1. Fetch assignment details to verify it exists and get the teacher_id/course_id
        String findAssignmentQuery = "SELECT teacher_id, course_id FROM assignment WHERE id = ?";
        
        // 2. Verify the student is enrolled in that course
        String checkEnrollmentQuery = "SELECT 1 FROM course_enrollments WHERE student_id = ? AND course_id = ?";
        
        // 3. Check if submission already exists (to update instead of throwing a duplicate error)
        String checkSubmissionQuery = "SELECT 1 FROM submission WHERE student_id = ? AND assignment_id = ?";
        
        // 4. Insert or Update
        String insertQuery = "INSERT INTO submission (teacher_id, content, submission_date, status, assignment_id, student_id) VALUES (?, ?, ?, 'Submitted', ?, ?)";
        String updateQuery = "UPDATE submission SET content = ?, submission_date = ?, status = 'Submitted' WHERE student_id = ? AND assignment_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            
            String teacherId = null;
            String courseId = null;

            // Step 1: Verify Assignment
            try (PreparedStatement ps = conn.prepareStatement(findAssignmentQuery)) {
                ps.setString(1, assignmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        teacherId = rs.getString("teacher_id");
                        courseId = rs.getString("course_id");
                    } else {
                        JOptionPane.showMessageDialog(null, "Assignment ID not found. Please check the ID and try again.", "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }

            // Step 2: Verify Enrollment
            try (PreparedStatement ps = conn.prepareStatement(checkEnrollmentQuery)) {
                ps.setString(1, studentId);
                ps.setString(2, courseId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(null, "You are not enrolled in the course for this assignment (" + courseId + ").", "Access Denied", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }

            // Step 3 & 4: Check if already submitted, then Insert or Update
            boolean exists = false;
            try (PreparedStatement ps = conn.prepareStatement(checkSubmissionQuery)) {
                ps.setString(1, studentId);
                ps.setString(2, assignmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    exists = rs.next();
                }
            }

            Timestamp now = new Timestamp(System.currentTimeMillis());

            if (exists) {
                int confirm = JOptionPane.showConfirmDialog(null, 
                        "You have already submitted this assignment. Do you want to overwrite it?", 
                        "Overwrite Submission", JOptionPane.YES_NO_OPTION);
                
                if (confirm == JOptionPane.YES_OPTION) {
                    try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
                        ps.setString(1, content);
                        ps.setTimestamp(2, now);
                        ps.setString(3, studentId);
                        ps.setString(4, assignmentId);
                        ps.executeUpdate();
                        JOptionPane.showMessageDialog(null, "Submission updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        return true;
                    }
                } else {
                    return false; // User chose not to overwrite
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(insertQuery)) {
                    ps.setString(1, teacherId);
                    ps.setString(2, content);
                    ps.setTimestamp(3, now);
                    ps.setString(4, assignmentId);
                    ps.setString(5, studentId);
                    ps.executeUpdate();
                    JOptionPane.showMessageDialog(null, "Assignment submitted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
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