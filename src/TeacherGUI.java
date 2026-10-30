import javax.swing.*;
import java.awt.*;

public class TeacherGUI extends BaseFrame {
    private Teacher currentTeacher;

    public TeacherGUI(Teacher teacher) {
        super("LMS - Teacher Portal (" + teacher.getName() + ")", 400, 320);
        this.currentTeacher = teacher;

        // Profile Details Header
        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Instructor Details"));
        infoPanel.add(new JLabel(" Instructor Name: " + currentTeacher.getName()));
        infoPanel.add(new JLabel(" Institutional Email: " + currentTeacher.getEmail()));
        infoPanel.add(new JLabel(" Faculty Staff ID: " + currentTeacher.getTeacherId()));

        // Action Buttons Grid
        JPanel btnPanel = new JPanel(new GridLayout(3, 1, 8, 8));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JButton btnUpload = new JButton("Upload Course Material");
        JButton btnCreateAsg = new JButton("Create Assignment Blueprint");
        JButton btnGrade = new JButton("Grade Student Submission");

        // Action Listeners - Updated to route file uploads correctly
        btnUpload.addActionListener(e -> UploadMaterial.showDialog(currentTeacher));
        btnCreateAsg.addActionListener(e -> currentTeacher.createAssignmentBlueprint());
        btnGrade.addActionListener(e -> currentTeacher.gradeSubmission());

        btnPanel.add(btnUpload);
        btnPanel.add(btnCreateAsg);
        btnPanel.add(btnGrade);

        add(infoPanel, BorderLayout.NORTH);
        add(btnPanel, BorderLayout.CENTER);
    }
}
