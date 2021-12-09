package scheduler.model;

import scheduler.db.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Date;
import java.sql.SQLException;

public class Appointments {
    private int ID;
    private String PatientName;
    private String CaregiverName;
    private String Vaccine;
    private Date Time;

    public Appointments() {
    }

    public Appointments(int ID, String PatientName, String CaregiverName, String Vaccine, Date Time) {
        this.ID = ID;
        this.PatientName = PatientName;
        this.CaregiverName = CaregiverName;
        this.Vaccine = Vaccine;
        this.Time = Time;
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAppointment = "INSERT INTO Appointments VALUES(?,?,?,?,?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAppointment);
            statement.setInt(1, this.ID);
            statement.setString(2, this.Vaccine);
            statement.setString(3, this.PatientName);
            statement.setString(4, this.CaregiverName);
            statement.setDate(5, this.Time);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }
}
