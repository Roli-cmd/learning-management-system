import javax.swing.*;
import java.awt.*;

public class StudentGUI extends BaseFrame {
    private Student currentStudent;

    public StudentGUI(Student student) {
        super("LMS - Student Portal (" + student.getName() + ")", 400, 400);
        this.currentStudent = student;

        // Profile Details Header
        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Student Details"));
        infoPanel.add(new JLabel(" Name: " + currentStudent.getName()));
        infoPanel.add(new JLabel(" Email: " + currentStudent.getEmail()));
        infoPanel.add(new JLabel(" Student ID: " + currentStudent.getStudentId()));

        // Action Buttons Grid (Updated to 5 rows to fit "View Assignments")
        JPanel btnPanel = new JPanel(new GridLayout(5, 1, 8, 8));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JButton btnJoin = new JButton("Join Class");
        JButton btnMaterials = new JButton("View Course Materials");
        JButton btnViewAssignments = new JButton("View Assignments");
        JButton btnSubmit = new JButton("Submit Assignment");
        JButton btnGrades = new JButton("View Grades");

        // Action Listeners
        btnJoin.addActionListener(e -> currentStudent.joinClass());
        btnMaterials.addActionListener(e -> currentStudent.viewCourseMaterials());
        btnViewAssignments.addActionListener(e -> currentStudent.viewAssignments());
        btnSubmit.addActionListener(e -> currentStudent.submitAssignment());
        btnGrades.addActionListener(e -> currentStudent.viewGrades());

        btnPanel.add(btnJoin);
        btnPanel.add(btnMaterials);
        btnPanel.add(btnViewAssignments);
        btnPanel.add(btnSubmit);
        btnPanel.add(btnGrades);

        add(infoPanel, BorderLayout.NORTH);
        add(btnPanel, BorderLayout.CENTER);
    }
}