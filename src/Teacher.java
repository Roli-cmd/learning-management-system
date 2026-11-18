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
        UploadMaterial.showDialog(this);
    }

    public void gradeSubmission() {
        GradeSubmission.showDialog(this);
    }
    
    // NEW METHODS
    public void joinClass() {
        TeacherJoinClass.joinClass(this);
    }

    public void editClassLink() {
        EditClassLink.showDialog(this);
    }
}