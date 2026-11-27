import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminFrame extends BaseFrame {
    private Admin admin;

    public AdminFrame(Admin admin) {
        super("Admin Dashboard - " + admin.getName(), 700, 550);
        this.admin = admin;

        // --- Top Panel (Welcome & Logout) ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel welcomeLabel = new JLabel("Welcome, Admin: " + admin.getName() + " (" + admin.getAdminId() + ")");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JButton logoutBtn = new JButton("Logout");
        styleOrangeButton(logoutBtn); // Apply orange style
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        topPanel.add(welcomeLabel, BorderLayout.WEST);
        topPanel.add(logoutBtn, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // --- Center Panel (Actions) ---
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JButton viewLinkBtn = new JButton("View Class Link");
        viewLinkBtn.setPreferredSize(new Dimension(220, 45));
        styleOrangeButton(viewLinkBtn); // Apply orange style
        viewLinkBtn.addActionListener(e -> JoinClasses.openClassLinkAsAdmin());

        JButton courseLinksBtn = new JButton("View Course Links");
        courseLinksBtn.setPreferredSize(new Dimension(220, 45));
        styleOrangeButton(courseLinksBtn); // Apply orange style
        courseLinksBtn.addActionListener(e -> showCourseLinksTable());

        // NEW: Upload Material Button for Admin
        JButton uploadMaterialBtn = new JButton("Upload Material");
        uploadMaterialBtn.setPreferredSize(new Dimension(220, 45));
        styleOrangeButton(uploadMaterialBtn); // Apply orange style
        uploadMaterialBtn.addActionListener(e -> openMaterialManagerAsAdmin());

        JLabel infoLabel1 = new JLabel("Look up a course by ID (e.g. C101) to open its class link.");
        JLabel infoLabel2 = new JLabel("Or view all course links and click the link column.");
        JLabel infoLabel3 = new JLabel("Select a course to upload and manage materials.");
        for (JLabel l : new JLabel[]{infoLabel1, infoLabel2, infoLabel3}) {
            l.setFont(new Font("Arial", Font.PLAIN, 12));
            l.setForeground(Color.GRAY);
        }

        gbc.gridx = 0; gbc.gridy = 0; centerPanel.add(viewLinkBtn, gbc);
        gbc.gridy = 1; centerPanel.add(infoLabel1, gbc);
        
        gbc.gridy = 2; centerPanel.add(courseLinksBtn, gbc);
        gbc.gridy = 3; centerPanel.add(infoLabel2, gbc);

        gbc.gridy = 4; centerPanel.add(uploadMaterialBtn, gbc);
        gbc.gridy = 5; centerPanel.add(infoLabel3, gbc);

        add(centerPanel, BorderLayout.CENTER);
    }

    // --- Helper method to style buttons with orange ---
    private void styleOrangeButton(JButton btn) {
        btn.setBackground(Color.ORANGE);           // Orange background
        btn.setForeground(Color.WHITE);            // White text for good contrast
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setFocusPainted(false);                // Remove focus outline
        btn.setOpaque(true);                       // Required to show background color
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Hand cursor on hover
    }

    // Admin flow to open the material manager
    private void openMaterialManagerAsAdmin() {
        String courseInput = JOptionPane.showInputDialog(this,
                "Enter Course ID or Code to manage materials:",
                "Manage Materials", JOptionPane.QUESTION_MESSAGE);
                
        if (courseInput == null || courseInput.trim().isEmpty()) return;

        String query = "SELECT id, name FROM course WHERE id = ? OR code = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, courseInput.trim());
            pstmt.setString(2, courseInput.trim());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String cId = rs.getString("id");
                    String cName = rs.getString("name");
                    // Open the shared ManageMaterialsFrame!
                    new ManageMaterialsFrame(cId, cName, "ADMIN").setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this, "Course not found.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ... (Keep your existing showCourseLinksTable() and openLink() methods below) ...
    private void showCourseLinksTable() {
        JDialog dialog = new JDialog(this, "Course Class Links", true);
        dialog.setSize(800, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        String[] columns = {"Course ID", "Code", "Course Name", "Class Link"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 3) {
                    openLink((String) model.getValueAt(row, 3));
                }
            }
        });

        dialog.add(new JScrollPane(table), BorderLayout.CENTER);
        
        JButton closeBtn = new JButton("Close");
        styleOrangeButton(closeBtn); // Apply orange style to the Close button too
        closeBtn.addActionListener(e -> dialog.dispose());
        
        JPanel bottom = new JPanel();
        bottom.add(closeBtn);
        dialog.add(bottom, BorderLayout.SOUTH);

        String sql = "SELECT id, code, name, class_link FROM course ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("id"), rs.getString("code"), 
                        rs.getString("name"), rs.getString("class_link")
                });
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        dialog.setVisible(true);
    }

    private void openLink(String link) {
        if (link == null || link.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No link available.", "No Link", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(link.trim()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not open link.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}