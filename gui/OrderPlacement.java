package gui;

import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class OrderPlacement extends JFrame {
    private JTextField quantityField;
    private JComboBox<String> drinkBox, branchBox;
    private JLabel statusLabel;
    private String loggedInUser;

    public OrderPlacement(String username) {
        this.loggedInUser = username;

        setTitle("Place Drink Order");
        setSize(500, 350);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(7, 2, 10, 10));
        getContentPane().setBackground(Color.decode("#FFFACD"));

        add(new JLabel("Logged in as: " + username));

        add(new JLabel("Select Drink:"));
        drinkBox = new JComboBox<>();
        loadDrinks();
        add(drinkBox);

        add(new JLabel("Quantity:"));
        quantityField = new JTextField();
        add(quantityField);

        add(new JLabel("Select Branch:"));
        String[] branches = {"NAIROBI", "NAKURU", "KISUMU", "MOMBASA"};
        branchBox = new JComboBox<>(branches);
        add(branchBox);

        JButton orderBtn = new JButton("Place Order");
        orderBtn.setBackground(Color.decode("#FF6347"));
        orderBtn.setForeground(Color.WHITE);
        orderBtn.addActionListener(e -> placeOrder());
        add(orderBtn);

        JButton historyBtn = new JButton("View Order History");
        historyBtn.setBackground(Color.decode("#32CD32"));
        historyBtn.setForeground(Color.WHITE);
        historyBtn.addActionListener(e -> viewOrderHistory());
        add(historyBtn);

        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setForeground(Color.BLUE);
        add(statusLabel);

        setVisible(true);
    }

    private void loadDrinks() {
        try (Connection conn = adminapp.DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name FROM drinks");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                drinkBox.addItem(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void placeOrder() {
        String drink = (String) drinkBox.getSelectedItem();
        String branch = (String) branchBox.getSelectedItem();
        String quantityText = quantityField.getText();

        if (quantityText.isEmpty() || !quantityText.matches("\\d+")) {
            statusLabel.setText("Enter a valid quantity.");
            return;
        }

        int quantity = Integer.parseInt(quantityText);

        try (Connection conn = adminapp.DBUtil.getConnection()) {
            conn.setAutoCommit(false);

            int drinkId = -1;
            double price = 0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT drink_id, price FROM drinks WHERE name = ?")) {
                ps.setString(1, drink);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    drinkId = rs.getInt("drink_id");
                    price = rs.getDouble("price");
                }
            }

            int branchId = -1;
            try (PreparedStatement ps = conn.prepareStatement("SELECT branch_id FROM branches WHERE branch_name = ?")) {
                ps.setString(1, branch);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    branchId = rs.getInt("branch_id");
                }
            }

            // Check stock level
            int currentStock = 0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT quantity FROM stock_levels WHERE drink_id = ? AND branch_id = ?")) {
                ps.setInt(1, drinkId);
                ps.setInt(2, branchId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    currentStock = rs.getInt("quantity");
                }
            }

            if (currentStock < quantity) {
                JOptionPane.showMessageDialog(this, "Insufficient stock! Only " + currentStock + " available.", "Stock Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double total = price * quantity;

            // Insert order
            String orderSQL = "INSERT INTO orders (user_id, drink_id, quantity, branch_id, total_price) " +
                    "VALUES ((SELECT user_id FROM users WHERE username = ?), ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(orderSQL)) {
                ps.setString(1, loggedInUser);
                ps.setInt(2, drinkId);
                ps.setInt(3, quantity);
                ps.setInt(4, branchId);
                ps.setDouble(5, total);
                ps.executeUpdate();
            }

            // Update stock
            String updateStock = "UPDATE stock_levels SET quantity = quantity - ? WHERE drink_id = ? AND branch_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateStock)) {
                ps.setInt(1, quantity);
                ps.setInt(2, drinkId);
                ps.setInt(3, branchId);
                ps.executeUpdate();
            }

            // Check stock after update
            String checkStock = "SELECT s.quantity, d.min_threshold, d.name FROM stock_levels s JOIN drinks d ON s.drink_id = d.drink_id WHERE s.drink_id = ? AND s.branch_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkStock)) {
                ps.setInt(1, drinkId);
                ps.setInt(2, branchId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int updatedQty = rs.getInt("quantity");
                    int min = rs.getInt("min_threshold");
                    String drinkName = rs.getString("name");

                    if (updatedQty < min) {
                        String message = drinkName + " is below threshold in " + branch;
                        String insertAlert = "INSERT INTO alerts (drink_id, branch_id, alert_type, message) VALUES (?, ?, 'Low Stock', ?)";
                        try (PreparedStatement alertStmt = conn.prepareStatement(insertAlert)) {
                            alertStmt.setInt(1, drinkId);
                            alertStmt.setInt(2, branchId);
                            alertStmt.setString(3, message);
                            alertStmt.executeUpdate();
                        }
                    }
                }
            }

            conn.commit();
            statusLabel.setText("Order placed: " + quantity + " x " + drink + " = Ksh " + total);

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Failed to place order.");
        }
    }

    private void viewOrderHistory() {
        JTextArea textArea = new JTextArea(10, 30);
        textArea.setEditable(false);

        try (Connection conn = adminapp.DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT d.name, o.quantity, b.branch_name, o.total_price " +
                     "FROM orders o " +
                     "JOIN drinks d ON o.drink_id = d.drink_id " +
                     "JOIN branches b ON o.branch_id = b.branch_id " +
                     "WHERE o.user_id = (SELECT user_id FROM users WHERE username = ?)")) {
            ps.setString(1, loggedInUser);
            ResultSet rs = ps.executeQuery();

            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                sb.append("Drink: ").append(rs.getString("name"))
                        .append(", Qty: ").append(rs.getInt("quantity"))
                        .append(", Branch: ").append(rs.getString("branch_name"))
                        .append(", Total: Ksh ").append(rs.getDouble("total_price"))
                        .append("\n");
            }

            if (sb.length() == 0) {
                sb.append("No orders found.");
            }

            textArea.setText(sb.toString());
            JOptionPane.showMessageDialog(this, new JScrollPane(textArea), "Order History", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

