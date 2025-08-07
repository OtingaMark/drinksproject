package adminapp;

import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class AdminDashboard extends JFrame {
    private JTable alertTable, reportTable, stockTable;

    public AdminDashboard() {
        setTitle("Admin Dashboard");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set a soft pink background
        getContentPane().setBackground(new Color(255, 240, 245));
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(new Color(255, 192, 203));
        tabs.setForeground(Color.DARK_GRAY);
        tabs.setFont(new Font("Arial", Font.BOLD, 14));

        alertTable = new JTable();
        reportTable = new JTable();
        stockTable = new JTable();

        JScrollPane alertScrollPane = new JScrollPane(alertTable);
        alertScrollPane.getViewport().setBackground(Color.white);

        JScrollPane reportScrollPane = new JScrollPane(reportTable);
        reportScrollPane.getViewport().setBackground(Color.white);

        JScrollPane stockScrollPane = new JScrollPane(stockTable);
        stockScrollPane.getViewport().setBackground(Color.white);

        tabs.addTab("Stock Alerts", alertScrollPane);
        tabs.addTab("Sales Report", reportScrollPane);
        tabs.addTab("Stock Levels", stockScrollPane);

        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setBackground(new Color(255, 182, 193));
        refreshButton.setForeground(Color.BLACK);
        refreshButton.setFont(new Font("Tahoma", Font.PLAIN, 13));
        refreshButton.addActionListener(e -> {
            loadAlerts();
            loadReport();
            loadStockLevels();
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(255, 240, 245));
        topPanel.add(refreshButton, BorderLayout.EAST);
        topPanel.add(new JLabel("  Welcome, Admin"), BorderLayout.WEST);

        add(topPanel, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        loadAlerts();
        loadReport();
        loadStockLevels();

        setVisible(true);
    }

    private void loadAlerts() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT a.alert_type, d.name AS drink, b.branch_name, a.message, a.created_at " +
                     "FROM alerts a " +
                     "JOIN drinks d ON a.drink_id = d.drink_id " +
                     "JOIN branches b ON a.branch_id = b.branch_id")) {

            DefaultTableModel model = new DefaultTableModel(
                    new String[]{"Alert Type", "Drink", "Branch", "Message", "Created At"}, 0);

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("alert_type"),
                        rs.getString("drink"),
                        rs.getString("branch_name"),
                        rs.getString("message"),
                        rs.getTimestamp("created_at")
                });
            }

            alertTable.setModel(model);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadReport() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT b.branch_name, d.name AS drink, SUM(o.quantity) AS total_quantity, SUM(o.total_price) AS total_sales " +
                     "FROM orders o " +
                     "JOIN drinks d ON o.drink_id = d.drink_id " +
                     "JOIN branches b ON o.branch_id = b.branch_id " +
                     "GROUP BY b.branch_name, d.name")) {

            DefaultTableModel model = new DefaultTableModel(
                    new String[]{"Branch", "Drink", "Total Quantity", "Total Sales (Ksh)"}, 0);

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("branch_name"),
                        rs.getString("drink"),
                        rs.getInt("total_quantity"),
                        rs.getDouble("total_sales")
                });
            }

            reportTable.setModel(model);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadStockLevels() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT b.branch_name, d.name AS drink, s.quantity " +
                     "FROM stock_levels s " +
                     "JOIN drinks d ON s.drink_id = d.drink_id " +
                     "JOIN branches b ON s.branch_id = b.branch_id")) {

            DefaultTableModel model = new DefaultTableModel(
                    new String[]{"Branch", "Drink", "Stock Quantity"}, 0);

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("branch_name"),
                        rs.getString("drink"),
                        rs.getInt("quantity")
                });
            }

            stockTable.setModel(model);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new AdminDashboard();
    }
}
