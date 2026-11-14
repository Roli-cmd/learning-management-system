import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class GradeSubmission {

    public static void showDialog(Teacher teacher) {
        JDialog dialog = new JDialog((Frame) null,
                "Grade Student Submissions (" + teacher.getTeacherId() + ")", true);
        dialog.setSize(900, 500);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout(10, 10));

        // ---- Table of students enrolled in this teacher's courses ----
        String[] columns = {"Student ID", "Course ID", "Course Name", "Current Grade", "Feedback", "Graded On"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);

        loadStudentsForTeacher(model, teacher.getTeacherId());

        // ---- Buttons ----
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton btnGrade   = new JButton("Grade Selected Student");
        JButton btnRefresh = new JButton("Refresh");
        JButton btnClose   = new JButton("Close");

        buttonPanel.add(btnGrade);
        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnClose);

        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // ---- Actions ----
        btnGrade.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Please select a student to grade.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String studentId = (String) model.getValueAt(row, 0);
            String courseId  = (String) model.getValueAt(row, 1);
            gradeStudent(dialog, model, teacher.getTeacherId(), studentId, courseId);
        });

        btnRefresh.addActionListener(e -> loadStudentsForTeacher(model, teacher.getTeacherId()));
        btnClose.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private static void loadStudentsForTeacher(DefaultTableModel model, String teacherId) {
        model.setRowCount(0);

        // UPDATED SQL: Removed the WHERE teacher filter so ALL enrollments are shown.
        // Teachers can now see and grade any student in the system.
        String sql =
            "SELECT ce.student_id, ce.course_id, c.name AS course_name, " +
            "       g.grade, g.feedback, g.graded_date " +
            "FROM course_enrollments ce " +
            "JOIN course c ON ce.course_id = c.id " +
            "LEFT JOIN grade g " +
            "     ON g.student_id = ce.student_id AND g.course_id = ce.course_id " +
            "ORDER BY ce.course_id, ce.student_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // No parameters needed anymore since we removed the WHERE clause
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String studentId = rs.getString("student_id");
                    String courseId  = rs.getString("course_id");
                    String courseName = rs.getString("course_name");

                    double gradeVal = rs.getDouble("grade");
                    String gradeStr = rs.wasNull() ? "Not Graded" : String.format("%.2f", gradeVal);

                    String feedback = rs.getString("feedback");
                    Timestamp ts    = rs.getTimestamp("graded_date");
                    String gradedOn = ts == null ? "" : ts.toString();

                    model.addRow(new Object[]{
                            studentId, courseId, courseName,
                            gradeStr,
                            feedback == null ? "" : feedback,
                            gradedOn
                    });
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Database Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void gradeStudent(JDialog parent, DefaultTableModel model,
                                     String teacherId, String studentId, String courseId) {

        // find current row to pre-fill
        int row = -1;
        for (int i = 0; i < model.getRowCount(); i++) {
            if (studentId.equals(model.getValueAt(i, 0)) && courseId.equals(model.getValueAt(i, 1))) { 
                row = i; 
                break; 
            }
        }
        
        String currentGradeStr = row >= 0 ? (String) model.getValueAt(row, 3) : "";
        if(currentGradeStr.equals("Not Graded")) currentGradeStr = "";
        
        String currentFeedback = row >= 0 ? (String) model.getValueAt(row, 4) : "";

        JTextField txtStudent = new JTextField(studentId);
        txtStudent.setEditable(false);
        JTextField txtCourse  = new JTextField(courseId);
        txtCourse.setEditable(false);
        JTextField txtScore   = new JTextField(currentGradeStr);
        JTextArea  txtFeedback = new JTextArea(currentFeedback, 4, 25);
        txtFeedback.setLineWrap(true);
        txtFeedback.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.add(new JLabel("Student ID:"));
        form.add(txtStudent);
        form.add(new JLabel("Course ID:"));
        form.add(txtCourse);
        form.add(new JLabel("Grade (0.00 - 100.00) [leave blank to clear]:"));
        form.add(txtScore);
        form.add(new JLabel("Feedback:"));
        form.add(new JScrollPane(txtFeedback));

        int result = JOptionPane.showConfirmDialog(parent, form,
                "Grade Student Submission",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        String gradeText = txtScore.getText().trim();
        String feedback = txtFeedback.getText().trim();
        
        boolean isClearing = gradeText.isEmpty();
        double score = 0.0;

        // ---- validate score if not clearing ----
        if (!isClearing) {
            try {
                score = Double.parseDouble(gradeText);
                if (score < 0 || score > 100) {
                    JOptionPane.showMessageDialog(parent,
                            "Grade must be between 0.00 and 100.00.",
                            "Invalid Grade", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parent,
                        "Please enter a valid numeric grade (e.g., 85.5) or leave blank to clear.",
                        "Invalid Grade", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // UPDATED SQL: Check based ONLY on student and course to prevent duplicate errors
        String checkSql = "SELECT 1 FROM grade WHERE student_id = ? AND course_id = ?";
        
        // UPDATED SQL: We update the teacher_id to whoever is currently grading it
        String updateSql = "UPDATE grade SET teacher_id = ?, grade = ?, feedback = ?, graded_date = ? " +
                           "WHERE student_id = ? AND course_id = ?";
                           
        String insertSql = "INSERT INTO grade (teacher_id, grade, feedback, graded_date, student_id, course_id) " +
                           "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {

            boolean exists = false;
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, studentId);
                ps.setString(2, courseId);
                try (ResultSet rs = ps.executeQuery()) {
                    exists = rs.next();
                }
            }

            if (exists) {
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, teacherId); // Overwrite with current teacher
                    if (isClearing) {
                        ps.setNull(2, java.sql.Types.FLOAT);
                        ps.setNull(3, java.sql.Types.VARCHAR);
                        ps.setNull(4, java.sql.Types.TIMESTAMP);
                    } else {
                        ps.setDouble(2, score);
                        ps.setString(3, feedback);
                        ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    }
                    ps.setString(5, studentId);
                    ps.setString(6, courseId);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, teacherId);
                    if (isClearing) {
                        ps.setNull(2, java.sql.Types.FLOAT);
                        ps.setNull(3, java.sql.Types.VARCHAR);
                        ps.setNull(4, java.sql.Types.TIMESTAMP);
                    } else {
                        ps.setDouble(2, score);
                        ps.setString(3, feedback);
                        ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    }
                    ps.setString(5, studentId);
                    ps.setString(6, courseId);
                    ps.executeUpdate();
                }
            }

            String successMsg = isClearing ? 
                "Grade cleared successfully (marked as Not Graded)." : 
                "Grade for student " + studentId + " saved successfully!\nScore: " + String.format("%.2f", score);

            JOptionPane.showMessageDialog(parent, successMsg, "Success", JOptionPane.INFORMATION_MESSAGE);

            // refresh table
            loadStudentsForTeacher(model, teacherId);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                    "Error saving grade: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}