package adminapp;

import java.io.*;
import java.net.Socket;
import java.sql.*;

public class ClientOrderHandler implements Runnable {
    private Socket clientSocket;

    public ClientOrderHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            Connection conn = DBUtil.getConnection()
        ) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received from branch: " + inputLine);
                String[] parts = inputLine.split(",");
                if (parts.length != 4) {
                    out.println("Invalid order format");
                    continue;
                }

                String drinkName = parts[0].trim();
                int quantity = Integer.parseInt(parts[1].trim());
                String branchName = parts[2].trim();
                String username = parts[3].trim();

                int drinkId = getDrinkId(conn, drinkName);
                int branchId = getBranchId(conn, branchName);
                int userId = getUserId(conn, username);
                double price = getDrinkPrice(conn, drinkId);
                double total = price * quantity;

                if (drinkId == -1 || branchId == -1 || userId == -1) {
                    out.println("Invalid order data");
                    continue;
                }

                insertOrder(conn, userId, drinkId, quantity, branchId, total);
                updateStock(conn, drinkId, branchId, quantity);
                checkAndAlert(conn, drinkId, branchId);
                out.println("Order received and processed.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getDrinkId(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT drink_id FROM drinks WHERE name = ?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("drink_id") : -1;
        }
    }

    private int getBranchId(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT branch_id FROM branches WHERE branch_name = ?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("branch_id") : -1;
        }
    }

    private int getUserId(Connection conn, String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM users WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("user_id") : -1;
        }
    }

    private double getDrinkPrice(Connection conn, int drinkId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT price FROM drinks WHERE drink_id = ?")) {
            ps.setInt(1, drinkId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble("price") : 0;
        }
    }

    private void insertOrder(Connection conn, int userId, int drinkId, int quantity, int branchId, double total) throws SQLException {
        String sql = "INSERT INTO orders (user_id, drink_id, quantity, branch_id, total_price) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, drinkId);
            ps.setInt(3, quantity);
            ps.setInt(4, branchId);
            ps.setDouble(5, total);
            ps.executeUpdate();
        }
    }

    private void updateStock(Connection conn, int drinkId, int branchId, int quantityOrdered) throws SQLException {
        String sql = "UPDATE stock_levels SET quantity = quantity - ? WHERE drink_id = ? AND branch_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantityOrdered);
            ps.setInt(2, drinkId);
            ps.setInt(3, branchId);
            ps.executeUpdate();
        }
    }

    private void checkAndAlert(Connection conn, int drinkId, int branchId) throws SQLException {
        int quantity = 0;
        int threshold = 0;

        try (PreparedStatement ps = conn.prepareStatement("SELECT quantity FROM stock_levels WHERE drink_id = ? AND branch_id = ?")) {
            ps.setInt(1, drinkId);
            ps.setInt(2, branchId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                quantity = rs.getInt("quantity");
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT min_threshold FROM drinks WHERE drink_id = ?")) {
            ps.setInt(1, drinkId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                threshold = rs.getInt("min_threshold");
            }
        }

        if (quantity < threshold) {
            String message = "Low stock alert for Drink ID " + drinkId + " at Branch ID " + branchId;
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO alerts (drink_id, branch_id, alert_type, message) VALUES (?, ?, 'LOW_STOCK', ?)")) {
                ps.setInt(1, drinkId);
                ps.setInt(2, branchId);
                ps.setString(3, message);
                ps.executeUpdate();
            }
        }
    }
}

