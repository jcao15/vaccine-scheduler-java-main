package scheduler.model;

import scheduler.db.ConnectionManager;
import scheduler.util.Util;

import java.sql.*;
import java.util.Arrays;
import java.util.UUID;

public class Patient {
    private final String username;
    private final byte[] salt;
    private final byte[] hash;

    private Patient(PatientSetter setter) {
        this.username = setter.username;
        this.salt = setter.salt;
        this.hash = setter.hash;
    }

    private Patient(PatientGetter getter) {
        this.username = getter.username;
        this.salt = getter.salt;
        this.hash = getter.hash;
    }

    public String getUsername() {
        return username;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getHash() {
        return hash;
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addPatient = "INSERT INTO Patients VALUES (?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addPatient);
            statement.setString(1, this.username);
            statement.setBytes(2, this.salt);
            statement.setBytes(3, this.hash);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void makeReservation(Date time, String caregiverName, Vaccine vaccine) throws SQLException {
        if (!checkReservedOrNot(time)) {
            System.out.println("Hi, It seems like you have already reserved a vaccine appointment. Please note that you can only reserve one vaccine per time.");
            return;
        }
        Availability.updateAvailability(time, caregiverName);
        vaccine.decreaseAvailableDoses(1);
        int ID = Math.abs(generateUniqueId());
        String userName = this.username;
        String vccName = vaccine.getVaccineName();
//        System.out.println(ID);
//        System.out.println(userName);
//        System.out.println(caregiverName);
//        System.out.println(vccName);
//        System.out.println(time);
        Appointments apt = new Appointments(ID, userName, caregiverName, vccName, time);
        apt.saveToDB();
        System.out.println("Congratulations! You have successfully made a vaccine Reservation.");
        System.out.println("Your assigned Caregiver Name is: " + caregiverName);
        System.out.println("Your assigned Appointment ID is: " + ID);
    }

    public int generateUniqueId() {
        //generate a random 4 digit unique id.
        UUID idOne = UUID.randomUUID();
        String str = "" + idOne;
        int uid = str.hashCode();
        String filterStr = "" + uid;
        str = filterStr.replaceAll("-", "");
        return Integer.parseInt(str);
    }

    public boolean checkReservedOrNot(Date time) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String check = "SELECT PatientName FROM Appointments WHERE PatientName = ? and Time = ?";
        try {
            PreparedStatement statement = con.prepareStatement(check);
            statement.setString(1, this.username);
            statement.setDate(2, time);
            ResultSet set = statement.executeQuery();
            while (set.next()) {
                return false;
            }
            return true;
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void showAppointments() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getAvailability = "SELECT ID, Vaccine, PatientName, CaregiverName, Time FROM Appointments WHERE PatientName = ?";
        try {
            PreparedStatement statement = con.prepareStatement(getAvailability);
            statement.setString(1, this.username);
            ResultSet resultSet = statement.executeQuery();
            //determines whether the last column read had a Null value
            if (resultSet.wasNull()) {
                System.out.println("No appointments exist");
                return;
            }
            System.out.println("The appointments are listed as below: ");
            while (resultSet.next()) {
                int ID = resultSet.getInt("ID");
                String Vaccine = resultSet.getString("Vaccine");
                String caregiverName = resultSet.getString("CaregiverName");
                Date time = resultSet.getDate("Time");
                System.out.println("Appointment ID: " + ID + "| Vaccine Name: " + Vaccine + "| Date: " + time + "| Caregiver Name: " + caregiverName);
            }
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class PatientSetter {
        private final String username;
        private final byte[] salt;
        private final byte[] hash;

        public PatientSetter(String username, byte[] salt, byte[] hash) {
            this.username = username;
            this.salt = salt;
            this.hash = hash;
        }

        public Patient build() {
            return new Patient(this);
        }
    }

    public static class PatientGetter {
        private final String username;
        private final String password;
        private byte[] salt;
        private byte[] hash;

        public PatientGetter(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public Patient get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getPatient = "SELECT Salt, Hash FROM Patients WHERE Username = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getPatient);
                statement.setString(1, this.username);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    byte[] salt = resultSet.getBytes("Salt");
                    byte[] hash = Util.trim(resultSet.getBytes("Hash"));

                    byte[] calculatedHash = Util.generateHash(password, salt);
                    if (!Arrays.equals(hash, calculatedHash)) {
                        return null;
                    } else {
                        this.salt = salt;
                        this.hash = hash;
                        return new Patient(this);
                    }
                }
                return null;
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }
}
