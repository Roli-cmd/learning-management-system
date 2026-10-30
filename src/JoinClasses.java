import javax.swing.*;

public class JoinClasses {

    // This method handles the logic that used to be inside Student
    public static void joinClass(Student student) {
        String courseInput = JOptionPane.showInputDialog(null, 
                "Enter Course ID (e.g., C101) or Course Code (e.g., CS101):", 
                "Join Class", 
                JOptionPane.QUESTION_MESSAGE);

        if (courseInput != null && !courseInput.trim().isEmpty()) {
            // We pass the student's ID retrieved via its getter method
            Course.enrollStudent(student.getStudentId(), courseInput.trim());
        }
    }
}
