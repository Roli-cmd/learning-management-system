import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TeacherGUI extends BaseFrame {
    private Teacher currentTeacher;

    public TeacherGUI(Teacher teacher) {
        super("LMS - Teacher Portal (" + teacher.getName() + ")", 400, 450); 
        this.currentTeacher = teacher;

        // --- COLOR THEME ---
        Color orangeColor = new Color(255, 140, 0); // Vibrant Orange for buttons
        Color bgColor = new Color(245, 245, 245);   // Light gray background

        // Set main window background
        this.getContentPane().setBackground(bgColor);

        // --- Profile Details Header ---
        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        infoPanel.setBackground(bgColor); // Match background
        infoPanel.setBorder(BorderFactory.createTitledBorder("Instructor Details"));
        infoPanel.add(new JLabel(" Instructor Name: " + currentTeacher.getName()));
        infoPanel.add(new JLabel(" Institutional Email: " + currentTeacher.getEmail()));
        infoPanel.add(new JLabel(" Faculty Staff ID: " + currentTeacher.getTeacherId()));

        // --- Action Buttons Grid ---
        JPanel btnPanel = new JPanel(new GridLayout(5, 1, 8, 8)); 
        btnPanel.setBackground(bgColor); // Match background
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JButton btnJoinClass = new JButton("Join Class");
        JButton btnEditLink = new JButton("Edit Class Link");
        JButton btnUpload = new JButton("Upload Course Material");
        JButton btnCreateAsg = new JButton("Create Assignment Blueprint");
        JButton btnGrade = new JButton("Grade Student Submission");

        // Array to easily style all buttons at once
        JButton[] buttons = {btnJoinClass, btnEditLink, btnUpload, btnCreateAsg, btnGrade};
        
        for (JButton btn : buttons) {
            btn.setBackground(orangeColor);           // Orange background
            btn.setForeground(Color.WHITE);           // White text for contrast
            btn.setFont(new Font("Arial", Font.BOLD, 13)); // Bold font
            btn.setFocusPainted(false);               // Remove dotted outline
            btn.setOpaque(true);                      // Ensure color renders
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Hand cursor on hover
            btnPanel.add(btn);                        // Add to panel
        }

        // Action Listeners
        btnJoinClass.addActionListener(e -> currentTeacher.joinClass());
        btnEditLink.addActionListener(e -> currentTeacher.editClassLink());
        
        // THIS IS THE CORRECTED LINE FOR UPLOADING MATERIALS
        btnUpload.addActionListener(e -> openManageMaterials());

        btnCreateAsg.addActionListener(e -> currentTeacher.createAssignmentBlueprint());
        btnGrade.addActionListener(e -> currentTeacher.gradeSubmission());

        add(infoPanel, BorderLayout.NORTH);
        add(btnPanel, BorderLayout.CENTER);
    }
    
    // =========================================================
    // NEW METHOD: Handles opening the ManageMaterialsFrame
    // =========================================================
    private void openManageMaterials() {
        String courseInput = JOptionPane.showInputDialog(this, 
                "Enter Course ID or Code to manage materials:", 
                "Manage Materials", JOptionPane.QUESTION_MESSAGE);
                
        if (courseInput == null || courseInput.trim().isEmpty()) return;

        String query = "SELECT id, name FROM course WHERE (id = ? OR code = ?) AND teacher_id = ? LIMIT 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, courseInput.trim());
            pstmt.setString(2, courseInput.trim());
            pstmt.setString(3, currentTeacher.getTeacherId()); // Uses currentTeacher
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String cId = rs.getString("id");
                    String cName = rs.getString("name");
                    
                    // Opens the SAME window the Admin uses
                    new ManageMaterialsFrame(cId, cName, currentTeacher.getTeacherId()).setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this, 
                            "Course not found or you are not the instructor.", 
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                    "Database Error: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}