import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UploadMaterial {

    public static void showDialog(Teacher teacher) {
        JDialog dialog = new JDialog((Frame) null,
                "Manage Course Materials (" + teacher.getTeacherId() + ")", true);
        dialog.setSize(850, 500);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout(10, 10));

        String[] columnNames = {"Material ID", "Title", "Type", "Course ID", "Description", "Has File"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table);

        loadAllMaterials(tableModel);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton btnAdd = new JButton("Upload New Material");
        JButton btnRefresh = new JButton("Refresh");

        btnPanel.add(btnAdd);
        btnPanel.add(btnRefresh);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> showMaterialForm(dialog, tableModel, teacher));
        btnRefresh.addActionListener(e -> loadAllMaterials(tableModel));

        dialog.setVisible(true);
    }

    // UPDATED: Removed the teacher_id filter so ALL uploaded materials show up
    private static void loadAllMaterials(DefaultTableModel model) {
        model.setRowCount(0);
        String query = "SELECT id, title, type, course_id, description, file " +
                       "FROM material " +
                       "ORDER BY course_id, title";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String id = rs.getString("id");
                String title = rs.getString("title");
                String type = rs.getString("type");
                String courseId = rs.getString("course_id");
                String description = rs.getString("description");
                byte[] fileBytes = rs.getBytes("file");
                String fileStatus = (fileBytes != null && fileBytes.length > 0) ? "Yes" : "No";

                model.addRow(new Object[]{id, title, type, courseId, description, fileStatus});
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void showMaterialForm(JDialog parentDialog, DefaultTableModel tableModel, Teacher teacher) {
        JDialog formDialog = new JDialog(parentDialog, "Upload New Material", true);
        formDialog.setSize(400, 400);
        formDialog.setLocationRelativeTo(parentDialog);
        formDialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 15));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField txtId = new JTextField("MAT" + System.currentTimeMillis());
        txtId.setEditable(false);
        formPanel.add(new JLabel("Material ID:"));
        formPanel.add(txtId);

        JTextField txtTitle = new JTextField();
        formPanel.add(new JLabel("Title:"));
        formPanel.add(txtTitle);

        // Select Course Dropdown
        JComboBox<String> cbCourse = new JComboBox<>();
        loadAllCourses(cbCourse); 
        formPanel.add(new JLabel("Select Course:"));
        formPanel.add(cbCourse);

        JTextField txtType = new JTextField("PDF");
        formPanel.add(new JLabel("File Type:"));
        formPanel.add(txtType);

        JTextArea txtDescription = new JTextArea(3, 20);
        txtDescription.setLineWrap(true);
        formPanel.add(new JLabel("Description:"));
        formPanel.add(new JScrollPane(txtDescription));

        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        JButton btnChooseFile = new JButton("Choose File");
        JLabel lblFileSelected = new JLabel("No file selected");
        filePanel.add(btnChooseFile, BorderLayout.WEST);
        filePanel.add(lblFileSelected, BorderLayout.CENTER);

        final File[] selectedFile = {null};
        btnChooseFile.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(formDialog) == JFileChooser.APPROVE_OPTION) {
                selectedFile[0] = fileChooser.getSelectedFile();
                lblFileSelected.setText(selectedFile[0].getName());
                String name = selectedFile[0].getName();
                int dot = name.lastIndexOf('.');
                if (dot > 0) {
                    txtType.setText(name.substring(dot + 1).toUpperCase());
                }
            }
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnOk = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");
        bottomPanel.add(btnOk);
        bottomPanel.add(btnCancel);

        formDialog.add(formPanel, BorderLayout.CENTER);
        formDialog.add(filePanel, BorderLayout.NORTH);
        formDialog.add(bottomPanel, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> formDialog.dispose());

        btnOk.addActionListener(e -> {
            if (txtTitle.getText().trim().isEmpty() || cbCourse.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(formDialog, "Title and Course are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (selectedFile[0] == null) {
                JOptionPane.showMessageDialog(formDialog, "Please select a file to upload.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String selectedCourse = (String) cbCourse.getSelectedItem();
            String courseId = selectedCourse.split(" - ")[0];

            saveMaterialToDatabase(formDialog, tableModel, teacher.getTeacherId(),
                    txtId.getText(), txtTitle.getText().trim(), txtType.getText().trim(),
                    courseId, txtDescription.getText().trim(), selectedFile[0]);
        });

        formDialog.setVisible(true);
    }

    private static void loadAllCourses(JComboBox<String> cbCourse) {
        String sql = "SELECT id, code FROM course"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
             
            while (rs.next()) {
                cbCourse.addItem(rs.getString("id") + " - " + rs.getString("code"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void saveMaterialToDatabase(JDialog formDialog, DefaultTableModel tableModel, String teacherId,
                                               String id, String title, String type, String courseId,
                                               String description, File file) {
        String sql = "INSERT INTO material (id, title, type, course_id, description, file) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            byte[] fileBytes = Files.readAllBytes(file.toPath());

            pstmt.setString(1, id);
            pstmt.setString(2, title);
            pstmt.setString(3, type);
            pstmt.setString(4, courseId);
            pstmt.setString(5, description);
            pstmt.setBytes(6, fileBytes);

            pstmt.executeUpdate();
            
            JOptionPane.showMessageDialog(formDialog, "Material uploaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            formDialog.dispose();
            
            // Refresh the table
            loadAllMaterials(tableModel);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(formDialog, "Error saving material: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}