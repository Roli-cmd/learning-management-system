import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CreateAssignment {

    public static void showDialog(Teacher teacher) {
        JDialog dialog = new JDialog((Frame) null, "Manage Assignment Blueprints (" + teacher.getTeacherId() + ")", true);
        dialog.setSize(800, 450);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout(10, 10));

        String[] columnNames = {"ID", "Title", "Course ID", "Due Date", "Description", "Has File"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table);

        loadTeacherAssignments(tableModel, teacher.getTeacherId());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton btnAdd = new JButton("Add Assignment");
        JButton btnEdit = new JButton("Edit Selected");
        JButton btnRefresh = new JButton("Refresh");

        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnRefresh);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> showAssignmentForm(dialog, tableModel, teacher.getTeacherId(), null));

        btnEdit.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "Please select an assignment to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String assignmentId = (String) tableModel.getValueAt(selectedRow, 0);
            showAssignmentForm(dialog, tableModel, teacher.getTeacherId(), assignmentId);
        });

        btnRefresh.addActionListener(e -> loadTeacherAssignments(tableModel, teacher.getTeacherId()));

        dialog.setVisible(true);
    }

    private static void loadTeacherAssignments(DefaultTableModel model, String teacherId) {
        model.setRowCount(0);
        String query = "SELECT id, title, course_id, due_date, description, file FROM assignment WHERE teacher_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, teacherId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String id = rs.getString("id");
                String title = rs.getString("title");
                String courseId = rs.getString("course_id");
                String dueDate = rs.getTimestamp("due_date") != null ? rs.getTimestamp("due_date").toString() : "";
                String description = rs.getString("description");
                byte[] blobCheck = rs.getBytes("file");
                String fileStatus = (blobCheck != null && blobCheck.length > 0) ? "Yes" : "No";

                model.addRow(new Object[]{id, title, courseId, dueDate, description, fileStatus});
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void showAssignmentForm(JDialog parentDialog, DefaultTableModel tableModel, String teacherId, String editAssignmentId) {
        boolean isEdit = (editAssignmentId != null);

        JTextField txtId = new JTextField(isEdit ? editAssignmentId : "ASG" + String.format("%02d", (int)(Math.random() * 90 + 10)));
        if (isEdit) txtId.setEditable(false);

        JTextField txtTitle = new JTextField();
        JTextField txtCourseId = new JTextField("C101");
        JTextField txtDueDate = new JTextField("2026-10-15 23:59:59");
        JTextArea txtDescription = new JTextArea(4, 20);

        JLabel lblFileSelected = new JLabel("No file selected");
        JButton btnChooseFile = new JButton("Choose File (Optional)");
        final File[] selectedFile = {null};

        btnChooseFile.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(parentDialog) == JFileChooser.APPROVE_OPTION) {
                selectedFile[0] = fileChooser.getSelectedFile();
                lblFileSelected.setText(selectedFile[0].getName());
            }
        });

        if (isEdit) {
            String fetchQuery = "SELECT title, course_id, due_date, description FROM assignment WHERE id = ? AND teacher_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(fetchQuery)) {
                pstmt.setString(1, editAssignmentId);
                pstmt.setString(2, teacherId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    txtTitle.setText(rs.getString("title"));
                    txtCourseId.setText(rs.getString("course_id"));
                    txtDueDate.setText(rs.getTimestamp("due_date") != null ? rs.getTimestamp("due_date").toString() : "");
                    txtDescription.setText(rs.getString("description"));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 8, 8));
        formPanel.add(new JLabel("Assignment ID:"));
        formPanel.add(txtId);
        formPanel.add(new JLabel("Title:"));
        formPanel.add(txtTitle);
        formPanel.add(new JLabel("Course ID:"));
        formPanel.add(txtCourseId);
        formPanel.add(new JLabel("Due Date (YYYY-MM-DD HH:MM:SS):"));
        formPanel.add(txtDueDate);
        formPanel.add(new JLabel("Description:"));
        formPanel.add(new JScrollPane(txtDescription));
        formPanel.add(btnChooseFile);
        formPanel.add(lblFileSelected);

        int result = JOptionPane.showConfirmDialog(parentDialog, formPanel, 
                isEdit ? "Edit Assignment Blueprint" : "Create Assignment Blueprint", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            saveAssignmentToDatabase(teacherId, isEdit, txtId.getText().trim(), txtTitle.getText().trim(), 
                    txtCourseId.getText().trim(), txtDueDate.getText().trim(), 
                    txtDescription.getText().trim(), selectedFile[0]);
            loadTeacherAssignments(tableModel, teacherId);
        }
    }

    private static void saveAssignmentToDatabase(String teacherId, boolean isEdit, String id, String title, String courseId, String dueDate, String description, File file) {
        String sql;
        if (isEdit) {
            sql = (file != null) 
                ? "UPDATE assignment SET title = ?, course_id = ?, due_date = ?, description = ?, file = ? WHERE id = ? AND teacher_id = ?" 
                : "UPDATE assignment SET title = ?, course_id = ?, due_date = ?, description = ? WHERE id = ? AND teacher_id = ?";
        } else {
            sql = "INSERT INTO assignment (title, course_id, due_date, description, file, id, teacher_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, title);
            pstmt.setString(2, courseId);
            pstmt.setTimestamp(3, java.sql.Timestamp.valueOf(dueDate));
            pstmt.setString(4, description);

            byte[] fileBytes = (file != null) ? Files.readAllBytes(file.toPath()) : null;

            if (isEdit) {
                if (fileBytes != null) {
                    pstmt.setBytes(5, fileBytes);
                    pstmt.setString(6, id);
                    pstmt.setString(7, teacherId);
                } else {
                    pstmt.setString(5, id);
                    pstmt.setString(6, teacherId);
                }
            } else {
                if (fileBytes != null) {
                    pstmt.setBytes(5, fileBytes);
                } else {
                    pstmt.setNull(5, java.sql.Types.BLOB);
                }
                pstmt.setString(6, id);
                pstmt.setString(7, teacherId);
            }

            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(null, "Assignment saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error saving assignment: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}