import javax.swing.JOptionPane;

public class Teacher extends User {
    private String teacherId;

    public Teacher(String id, String name, String email, String teacherId) {
        super(id, name, email, "TEACHER");
        this.teacherId = teacherId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void createAssignment() {
        CreateAssignment.showDialog(this);
    }

    public void createAssignmentBlueprint() {
        CreateAssignment.showDialog(this);
    }

    public void uploadMaterial() {
        JOptionPane.showMessageDialog(null, "Upload Material feature.", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public void gradeSubmission() {
        JOptionPane.showMessageDialog(null, "Grade Submission feature.", "Info", JOptionPane.INFORMATION_MESSAGE);
    }
	public void uploadMaterial() {
    // Replace the simple alert message option with the actual functional dialog window
    UploadMaterial.showDialog(this);
}
}