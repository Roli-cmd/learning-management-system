import javax.swing.*;

public class JoinClasses {

    // For Students (keeps enrolling them in the DB)
    public static void joinClass(Student student) {
        String courseInput = JOptionPane.showInputDialog(null,
                "Enter Course ID (e.g., C101) or Course Code (e.g., CS101):",
                "Join Class",
                JOptionPane.QUESTION_MESSAGE);

        if (courseInput != null && !courseInput.trim().isEmpty()) {
            Course.enrollStudent(student.getStudentId(), courseInput.trim());
        }
    }

    // For Admins (just opens the link, bypasses the enrollment FK constraint)
    public static void openClassLinkAsAdmin() {
        String courseInput = JOptionPane.showInputDialog(null,
                "Enter Course ID (e.g., C101) or Course Code (e.g., CS101) to view link:",
                "View Class Link",
                JOptionPane.QUESTION_MESSAGE);

        if (courseInput != null && !courseInput.trim().isEmpty()) {
            Course.showClassLink(courseInput.trim());
        }
    }
}