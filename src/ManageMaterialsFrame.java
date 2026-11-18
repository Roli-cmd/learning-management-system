import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ManageMaterialsFrame extends JFrame {
    private String courseId;
    private String courseName;
    private String ownerId; // Teacher ID or "ADMIN"
    private JTable table;
    private DefaultTableModel model;

    public ManageMaterialsFrame(String courseId, String courseName, String ownerId) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.ownerId = ownerId;

        setTitle("Manage Course Materials - " + courseName + " [" + courseId + "]");
        setSize(850, 450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- Top Panel (Action Buttons) ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        
        JButton uploadBtn = new JButton("Upload New Material");
        uploadBtn.setFont(new Font("Arial", Font.BOLD, 14));
        uploadBtn.addActionListener(e -> {
            UploadMaterial.uploadDirectlyToCourse(courseId, courseName);
            loadMaterials(); // Refresh table after upload
        });
        
        // NEW: View/Download Button
        JButton viewBtn = new JButton("View / Download File");
        viewBtn.setFont(new Font("Arial", Font.BOLD, 14));
        viewBtn.addActionListener(e -> viewSelectedMaterial());
        
        topPanel.add(uploadBtn);
        topPanel.add(viewBtn);
        add(topPanel, BorderLayout.NORTH);

        // --- Center Panel (Table) ---
        String[] columns = {"Material ID", "Title", "Type", "Course ID", "Description", "Has File"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table = new JTable(model);
        table.setRowHeight(24);
        
        // Optional: Double-click a row to view the file instantly
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    viewSelectedMaterial();
                }
            }
        });
        
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- Bottom Panel (Close) ---
        JPanel bottomPanel = new JPanel();
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        bottomPanel.add(closeBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        loadMaterials();
    }

    private void loadMaterials() {
        model.setRowCount(0);
        String query = "SELECT id, title, type, course_id, description, file FROM material WHERE course_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    byte[] fileData = rs.getBytes("file");
                    String hasFile = (fileData != null && fileData.length > 0) ? "Yes" : "No";
                    
                    model.addRow(new Object[]{
                            rs.getString("id"),
                            rs.getString("title"),
                            rs.getString("type"),
                            rs.getString("course_id"),
                            rs.getString("description"),
                            hasFile
                    });
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading materials: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================
    // NEW METHOD: Fetch file from DB, save to temp, and open
    // =========================================================
    private void viewSelectedMaterial() {
        int selectedRow = table.getSelectedRow();
        
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a material from the list first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String materialId = (String) model.getValueAt(selectedRow, 0);
        String title = (String) model.getValueAt(selectedRow, 1);
        String type = (String) model.getValueAt(selectedRow, 2);

        String query = "SELECT file FROM material WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, materialId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    byte[] fileBytes = rs.getBytes("file");
                    
                    if (fileBytes == null || fileBytes.length == 0) {
                        JOptionPane.showMessageDialog(this, "No file was uploaded for this material.", "No File", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    
                    // Create a temporary file
                    String fileExtension = type.equalsIgnoreCase("OTHER") ? ".dat" : "." + type.toLowerCase();
                    // Sanitize the title to use as a filename
                    String safeTitle = title.replaceAll("[^a-zA-Z0-9.-]", "_");
                    
                    File tempFile = File.createTempFile("LMS_" + safeTitle + "_", fileExtension);
                    tempFile.deleteOnExit(); // Automatically delete when Java program closes
                    
                    // Write bytes to the temp file
                    Files.write(tempFile.toPath(), fileBytes);
                    
                    // Open the file using the system's default application
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(tempFile);
                    } else {
                        JOptionPane.showMessageDialog(this, "File saved to: " + tempFile.getAbsolutePath(), "Saved", JOptionPane.INFORMATION_MESSAGE);
                    }
                    
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error opening file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}