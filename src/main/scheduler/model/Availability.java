package scheduler.model;

import scheduler.db.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;

public class Availability {
    private Date Time;
    private String username;

    public Availability() {
    }

    public Availability(String username, Date Time) {
        this.username = username;
        this.Time = Time;
    }

    public static String caregiverAssign(Date time) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        //randomly assign a caregiver
        String randomAssign = "SELECT TOP 1 Username From Availabilities WHERE Time = ? and isAvailable = ? ORDER BY NEWID()";
        try {
            PreparedStatement statement = con.prepareStatement(randomAssign);
            statement.setDate(1, time);
            statement.setInt(2, 1);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.wasNull()) {
                System.out.println("Sorry. There is no available care giver on that date " + time);
                return null;
            }
            while (resultSet.next()) {
                String name = resultSet.getString("Username");
                return name;
            }
            return null;
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }

    }

    public static ArrayList<String> getList(Date time) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        ArrayList<String> res = new ArrayList<>();

        String get = "SELECT Username From Availabilities WHERE Time = ? and isAvailable = ?";
        try {
            PreparedStatement statement = con.prepareStatement(get);
            statement.setDate(1, time);
            statement.setInt(2, 1);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String name = resultSet.getString("Username");
                //Add every Caregiver from the resultSet to the created ArrayList res
                res.add(name);
            }
            //if ArrayList res has nothing in it, return null; otherwise return the ArrayList res
            return res.size() == 0 ? null : res;
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }

    }

    public static void updateAvailability(Date time, String CaregiverName) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        // isAvailable means when vaccine appointment is available, isAvailable = 1, otherwise isAvailable = 0
        String update = "UPDATE Availabilities SET isAvailable = ? WHERE Username = ? and Time = ?";
        try {
            PreparedStatement statement = con.prepareStatement(update);
            statement.setInt(1, 0);
            statement.setString(2, CaregiverName);
            statement.setDate(3, time);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }
}
