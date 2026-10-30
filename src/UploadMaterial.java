import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class UploadMaterial extends JDialog {
    private Teacher currentTeacher;
    private JTable materialTable;
    private DefaultTableModel tableModel;

    public UploadMaterial(Frame owner, Teacher teacher) {
        super(owner, "Manage Course Materials (" + teacher.getTeacherId() + ")", true);
        this.currentTeacher = teacher;

        setSize(950, 450); // Slightly expanded width to account for the description column
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        // 1. Updated Columns array to explicitly show "Description" inside the UI grid matrix view
        String[] columnNames = {"Material ID", "Title", "File Type", "Course Code", "Subject Name", "Description"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Grid elements remain view-only
            }
        };

        materialTable = new JTable(tableModel);
        materialTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        materialTable.getTableHeader().setReorderingAllowed(false);
        materialTable.setRowHeight(24); 

        JScrollPane scrollPane = new JScrollPane(materialTable);
        add(scrollPane, BorderLayout.CENTER);

        // 2. Control Action Buttons Panel matching your layout style
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        JButton btnUploadNew = new JButton("Upload New Material");
        JButton btnRefresh = new JButton("Refresh Matrix");

        btnPanel.add(btnUploadNew);
        btnPanel.add(btnRefresh);
        add(btnPanel, BorderLayout.SOUTH);

        // 3. Register Event Trigger Actions
        btnUploadNew.addActionListener(e -> selectCourseAndUploadFile());
        btnRefresh.addActionListener(e -> loadUploadedMaterials());

        // Initial loading sequence execution to populate existing items
        loadUploadedMaterials();
    }

    public static void showDialog(Teacher teacher) {
        UploadMaterial dialog = new UploadMaterial(null, teacher);
        dialog.setVisible(true);
    }

    // Updated Method: Reads the rows along with descriptions from the database material table
    private void loadUploadedMaterials() {
        tableModel.setRowCount(0);

        // Query joining material and course tables—updated to fetch m.description
        String query = "SELECT m.id, m.title, m.type, m.description, c.code, c.name " +
                       "FROM material m " +
                       "INNER JOIN course c ON m.course_id = c.id " +
                       "WHERE c.teacher_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, currentTeacher.getTeacherId());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String matId = rs.getString("id");
                    String title = rs.getString("title");
                    String type = rs.getString("type");
                    String description = rs.getString("description"); // Extracted column data
                    String courseCode = rs.getString("code");
                    String courseName = rs.getString("name");

                    // Adds matching data properties sequentially into the rows of your UI model framework
                    tableModel.addRow(new Object[]{matId, title, type, courseCode, courseName, description});
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching materials: " + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Refactored Method: Prompts for data components and handles stream insertion plumbing pipelines
    private void selectCourseAndUploadFile() {
        // Step A: Load courses assigned to this teacher to populate a choice selector dropdown
        ArrayList<String> courseIds = new ArrayList<>();
        ArrayList<String> courseLabels = new ArrayList<>();
        
        String courseQuery = "SELECT id, code, name FROM course WHERE teacher_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(courseQuery)) {
            
            pstmt.setString(1, currentTeacher.getTeacherId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    courseIds.add(rs.getString("id"));
                    courseLabels.add(rs.getString("code") + " - " + rs.getString("name"));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        if (courseIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You have no courses assigned to upload material to!", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Step B: Prompt user to choose the target course
        JComboBox<String> courseBox = new JComboBox<>(courseLabels.toArray(new String[0]));
        int response = JOptionPane.showConfirmDialog(this, courseBox, "Select Target Course/Subject", JOptionPane.OK_CANCEL_OPTION);
        if (response != JOptionPane.OK_OPTION) return;

        String targetCourseId = courseIds.get(courseBox.getSelectedIndex());

        // Step C: Open a structured input grid panel to gather the Title and Description together cleanly
        JTextField txtTitle = new JTextField();
        JTextArea txtDescription = new JTextArea(3, 20);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(txtDescription);

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.add(new JLabel("Material Title:"));
        inputPanel.add(txtTitle);
        inputPanel.add(new JLabel("Description:"));
        inputPanel.add(descScrollPane);

        int detailsResponse = JOptionPane.showConfirmDialog(this, inputPanel, "Enter Material Details", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (detailsResponse != JOptionPane.OK_OPTION) return;

        String title = txtTitle.getText().trim();
        String description = txtDescription.getText().trim();
        
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Material Title cannot be empty!", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Step D: Trigger Document File Chooser Explorer Window
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Document File to Upload to LMS Database");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Documents (*.pdf, *.txt, *.docx, *.pptx)", "pdf", "txt", "docx", "pptx"));

        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File selectedFile = fileChooser.getSelectedFile();
        String fileName = selectedFile.getName();

        // Infer extension type formatting natively
        String type = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            type = fileName.substring(dotIndex + 1).toUpperCase();
        }

        // Step E: Stream file bytes straight down the database table pipe with description included
        String insertQuery = "INSERT INTO material (id, title, type, description, file, course_id) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery);
             FileInputStream fis = new FileInputStream(selectedFile)) {

            String materialId = "MAT" + System.currentTimeMillis();

            pstmt.setString(1, materialId);
            pstmt.setString(2, title);
            pstmt.setString(3, type);
            pstmt.setString(4, description); // Assigned description value mapping parameter
            pstmt.setBinaryStream(5, fis, (int) selectedFile.length()); // Saves file into your database blob safely
            pstmt.setString(6, targetCourseId);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "File uploaded successfully to database system!", "Operation Success", JOptionPane.INFORMATION_MESSAGE);
                loadUploadedMaterials(); // Auto-refresh the view matrix grid immediately
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database Processing Failure: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
