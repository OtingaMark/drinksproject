package gui;

import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class SignupForm extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public SignupForm() {
        setTitle("Sign Up");
        setSize(300, 200);
        setLayout(new GridLayout(3, 2));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Color.decode("#FFF0F5"));

        add(new JLabel("Username:"));
        usernameField = new JTextField();
        add(usernameField);

        add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        add(passwordField);

        JButton signupButton = new JButton("Sign Up");
        signupButton.setBackground(Color.decode("#FFB6C1"));
        signupButton.setForeground(Color.BLACK);
        add(signupButton);

        signupButton.addActionListener(e -> registerUser());

        setVisible(true);
    }

    private void registerUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill in all fields.");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/drink_sales", "root", "");
             PreparedStatement check = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
             PreparedStatement insert = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)")) {

            check.setString(1, username);
            ResultSet rs = check.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Username already exists.");
                return;
            }

            insert.setString(1, username);
            insert.setString(2, password);
            insert.executeUpdate();

            JOptionPane.showMessageDialog(this, "Sign up successful!");
            dispose();
            new LoginForm();

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
        }
    }
}

