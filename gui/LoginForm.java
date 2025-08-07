package gui;

import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class LoginForm extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginForm() {
        setTitle("Login");
        setSize(300, 200);
        setLayout(new GridLayout(4, 2));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Color.decode("#E6E6FA"));

        add(new JLabel("Username:"));
        usernameField = new JTextField();
        add(usernameField);

        add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        add(passwordField);

        JButton loginButton = new JButton("Login");
        loginButton.setBackground(Color.decode("#FF69B4"));
        loginButton.setForeground(Color.WHITE);

        JButton signupButton = new JButton("Sign Up");
        signupButton.setBackground(Color.decode("#87CEFA"));
        signupButton.setForeground(Color.BLACK);

        add(loginButton);
        add(signupButton);

        loginButton.addActionListener(e -> authenticate());
        signupButton.addActionListener(e -> {
            dispose();
            new SignupForm();
        });

        setVisible(true);
    }

    private void authenticate() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/drink_sales", "root", "");
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {

            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                dispose();
                new OrderPlacement(username);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials.");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database connection error.");
        }
    }

    public static void main(String[] args) {
        new LoginForm();
    }
}
