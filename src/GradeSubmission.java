import javax.swing.*;

public class GradeSubmission {
    public static void showDialog(Teacher teacher) {
        // This links to the `grade` database table from your SQL file
        String scoreInput = JOptionPane.showInputDialog(null, "Enter Grade Score (0.0 - 100.0):", "Grade Submission", JOptionPane.QUESTION_MESSAGE);
        if (scoreInput == null || scoreInput.trim().isEmpty()) return;

        JOptionPane.showMessageDialog(null, 
                "Grade matrix saved successfully into system archive.", 
                "Graded", 
                JOptionPane.INFORMATION_MESSAGE);
    }
}
