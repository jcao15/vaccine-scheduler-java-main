package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Availability;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.*;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;
    private static String[] commands = {"create_patient", "create_caregiver", "login_patient", "login_caregiver", "search_caregiver_schedule", "reserve", "" +
            "upload_availability", "cancel", "add_doses", "show_appointments", "logout", "quit"};

    public static void main(String[] args) throws SQLException {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        /*
        //instantiating a connection manager class
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        //example 1: getting all records in the vaccine table
        PreparedStatement getAllVaccines = con.prepareStatement("SELECT * FROM Vaccines");
        ResultSet rs = getAllVaccines.executeQuery();
        while (rs.next()) {
            System.out.println("id: " + rs.getLong(1) + ", available_doses: " + rs.getInt(2) +
                    ", name: " + rs.getString(3) + ", required_doses: " + rs.getString(4));
        }
        */

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout();
            } else if (operation.equals("quit")) {
                System.out.println("Thank you for using the CSE414 Appointment Reservation System. Have a wonderful Day!");
                return;
            } else {
                System.out.println("Invalid operation name!");
                continue;
            }
        }

    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        if (tokens.length != 3) {
            System.out.println("Please try again! ");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        //check if the password is Strong / Moderate / Weak
        if (isStrongPassword(password) == true) {
            System.out.print("Your password is Strong!\n");
        } else {
            System.out.println("A. At least 8 characters");
            System.out.println("B. A mixture of both uppercase and lowercase letters");
            System.out.println("C. A mixture of letters and numbers");
            System.out.println("D. Inclusion of at least one special character, from ???!???, ???@???, ???#???, ????\"");
            System.out.println("Please try again!");
            return;
        }
        //generate salt and hash randomly
        //password stored in hash code format
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the Patient
        try {
            currentPatient = new Patient.PatientSetter(username, salt, hash).build();
            currentPatient.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    /*
     * Password Checker
     * At least 8 characters.
     * A mixture of both uppercase and lowercase letters.
     * A mixture of letters and numbers.
     * Inclusion of at least one special character, from ???!???, ???@???, ???#???, ???????.
     */
    private static boolean isStrongPassword(String password) {
        int n = password.length();
        boolean isStrong = false;
        boolean hasLower = false, hasUpper = false,
                hasDigit = false, specialChar = false, hasLetter = false;
        Set<Character> set = new HashSet<Character>(Arrays.asList('!', '@', '#', '?'));
        for (char i : password.toCharArray()) {
            if (Character.isLowerCase(i))
                hasLower = true;
            if (Character.isUpperCase(i))
                hasUpper = true;
            if (Character.isDigit(i))
                hasDigit = true;
            if (set.contains(i))
                specialChar = true;
            if (Character.isLetter(i))
                hasLetter = true;

        }

        // Strength of password
        if (hasDigit && hasLower && hasUpper && specialChar
                && hasLetter && (n >= 8)) {
            isStrong = true;

        } else if ((hasLower || hasUpper || specialChar)
                && (n >= 6)) {
            isStrong = false;
            System.out.print("Your password is Moderate. But still need a bit stronger. \n");
        } else {
            isStrong = false;
            System.out.print("Your password is Weak. Pleases follow the password guideline as shown below to create a strong passward: \n");
        }
        return isStrong;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        //check 1: tokens exist
        /*if(tokens == null||tokens.length == 0){
        return}*/
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        /*
        if(!userName unique(username){
        return;}
         */

        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        // check 1: if someone's already logged-in, they need to log out first
        if (currentPatient != null || currentCaregiver != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }

        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in. Please check your username and password.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Patient logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in. Please check your username and password.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2
        //search_caregiver_schedule <date>
        /*Output the username for the caregivers that are available for the date,
        along with the number of available doses left for each vaccine.
        */
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login your account first.");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Invalid inputs. Please try again. Input should be a date in format YYYY-MM-DD.");
            return;
        }
        try {
            Vaccine.VaccineGetter vg = new Vaccine.VaccineGetter();
            Availability getter = new Availability();
            Date time = Date.valueOf(tokens[1]);
            ArrayList<Vaccine> availableVaccineInfo = null;
            ArrayList<String> caregiversNameList = null;

            availableVaccineInfo = vg.getAll();
            caregiversNameList = getter.getList(time);

            if (caregiversNameList == null || availableVaccineInfo == null) {
                System.out.println("Unfortunately, there is no available caregiver schedule and vaccine info right now.");
                return;
            }
            System.out.println("Here is the caregivers list that are available for the date: \n" + tokens[1]);
            //loop through the arraylist of caregiver name list, print the available caregiver name one line by one line

            for (String n : caregiversNameList) {
                System.out.println("Caregiver: " + n);
            }
            System.out.println("\n----------------------------------------------------------\n");
            System.out.println("Vaccine information are Listed below: \n");
            //loop through the arraylist of available vaccine and doses number;
            for (Vaccine a : availableVaccineInfo) {
                System.out.println(a);
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when fetching available vaccine information");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid Date input. Valid Date format should be YYYY-MM-DD. Please try again.");
            return;
        }
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
         /*
           reserve <date> <vaccine>
            Patients perform this operation to reserve an appointment.
            You will be randomly assigned a caregiver for the reservation on that date.
            Output the assigned caregiver and the appointment ID for the reservation.
         */
        if (currentPatient == null) {
            System.out.println("Please login your Patient account first.");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Invalid input. Please try again");
            return;
        }

        try {
            Date time = Date.valueOf(tokens[1]);
            String vaccineName = tokens[2];
            String caregiverName = Availability.caregiverAssign(time);
            System.out.println(caregiverName);
            Vaccine.VaccineGetter vcc = new Vaccine.VaccineGetter(vaccineName);
            Vaccine vaccine = vcc.get();
            //if caregiverAssign return null which means caregiverName is null
            //Or if getAvailableDoses return 0, both cases mean no available appointment could be reserved
            if (caregiverName == null || vaccine.getAvailableDoses() == 0) {
                System.out.println("Unfortunately, there is no available appointment right now. Please come back to check again later.");
                return;
            }
            currentPatient.makeReservation(time, caregiverName, vaccine);
        } catch (SQLException ex) {
            /***
             for (Throwable e : ex) {
             if (e instanceof SQLException) {
             if (ignoreSQLException(
             ((SQLException)e).
             getSQLState()) == false) {

             e.printStackTrace(System.err);
             System.err.println("SQLState: " +
             ((SQLException)e).getSQLState());

             System.err.println("Error Code: " +
             ((SQLException)e).getErrorCode());

             System.err.println("Message: " + e.getMessage());

             Throwable t = ex.getCause();
             while(t != null) {
             System.out.println("Cause: " + t);
             t = t.getCause();
             }
             }
             }
             }
             ***/

            System.out.println("Ops, something wrong happened with your reservation...");
            ex.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid date input.  Please try again.");
            return;
        }

    }

    /***
     public static boolean ignoreSQLException(String sqlState) {

     if (sqlState == null) {
     System.out.println("The SQL state is not defined!");
     return false;
     }

     // X0Y32: Jar file already exists in schema
     if (sqlState.equalsIgnoreCase("X0Y32"))
     return true;

     // 42Y55: Table already exists in schema
     if (sqlState.equalsIgnoreCase("42Y55"))
     return true;

     return false;
     }
     ***/

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date! Valid Date format should be YYYY-MM-DD.");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
        /*
        show_appointments
        Output the scheduled appointments for the current user (both patients and caregivers).
        For caregivers, you should print the appointment ID, vaccine name, date, and patient name.
        For patients, you should print the appointment ID, vaccine name, date, and caregiver name.
        */

        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please log in your account.");
            return;
        }
        if (tokens.length != 1) {
            System.out.println("Invalid input. Please try again.");
            return;
        }
        try {
            if (currentCaregiver != null) {
                currentCaregiver.showAppointments();
            }
            if (currentPatient != null) {
                currentPatient.showAppointments();
            }
        } catch (SQLException e) {
            System.out.println("There is an SQL exception error.");
            e.printStackTrace();
        }
    }

    private static void logout() {
        // TODO: Part 2
        if (currentCaregiver != null) {
            currentCaregiver = null;
            System.out.println("You have been successfully logged out your caregiver account! ");
        }
        if (currentPatient != null) {
            currentPatient = null;
            System.out.println("You have been successfully logged out your patient account! ");
        }

    }
}
