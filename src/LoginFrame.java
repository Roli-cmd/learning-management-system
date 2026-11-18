import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginFrame extends BaseFrame {
    private JTextField emailField;
    private JPasswordField passwordField;

    public LoginFrame() {
        super("LMS - User Login", 350, 220);

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        formPanel.add(new JLabel("Email:"));
        emailField = new JTextField("smith@univ.edu");
        formPanel.add(emailField);

        formPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField("teacher123");
        formPanel.add(passwordField);

        JPanel btnPanel = new JPanel();
        JButton loginBtn = new JButton("Login");
        btnPanel.add(loginBtn);

        loginBtn.addActionListener(e -> authenticateUser());

        add(formPanel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void authenticateUser() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Please fill in all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // UPDATED: Added LEFT JOIN for admin table and selected a.admin_id
        String loginQuery = "SELECT u.id, u.name, u.email, u.role, t.teacher_id, s.student_id, a.admin_id " +
                           "FROM user u " +
                           "LEFT JOIN teacher t ON u.id = t.user_id " +
                           "LEFT JOIN student s ON u.id = s.user_id " +
                           "LEFT JOIN admin a ON u.id = a.user_id " +
                           "WHERE u.email = ? AND u.password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(loginQuery)) {

            pstmt.setString(1, email);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String id = rs.getString("id");
                    String name = rs.getString("name");
                    String userEmail = rs.getString("email");
                    String role = rs.getString("role");
                    String teacherId = rs.getString("teacher_id");
                    String studentId = rs.getString("student_id");
                    String adminId = rs.getString("admin_id");

                    showMessage("Login Successful! Welcome, " + name + ".", "Success", JOptionPane.INFORMATION_MESSAGE);
                    this.dispose();

                    SwingUtilities.invokeLater(() -> {
                        try {
                            if ("TEACHER".equalsIgnoreCase(role)) {
                                Teacher teacher = new Teacher(id, name, userEmail, teacherId != null ? teacherId : "T2001");
                                TeacherGUI teacherGUI = new TeacherGUI(teacher);
                                teacherGUI.setVisible(true);

                            } else if ("STUDENT".equalsIgnoreCase(role)) {
                                Student student = new Student(id, name, userEmail, studentId != null ? studentId : "ST01");
                                StudentGUI studentGUI = new StudentGUI(student);
                                studentGUI.setVisible(true);

                            } else if ("ADMIN".equalsIgnoreCase(role)) {
                                // NEW: Launch Admin Dashboard
                                Admin admin = new Admin(id, name, userEmail, adminId != null ? adminId : "A3001");
                                AdminFrame adminFrame = new AdminFrame(admin);
                                adminFrame.setVisible(true);
                            } 
							
							else {
                                showMessage("Unknown user role: " + role, "Notice", JOptionPane.WARNING_MESSAGE);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showMessage("Error opening dashboard: " + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
                        }
                    });

                } else {
                    showMessage("Invalid Email or Password!", "Login Error", JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showMessage("Database Error: " + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}