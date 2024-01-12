package server;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class ServerThread implements Runnable {
    private Socket socketOfServer;
    private int clientNumber;
    private BufferedReader is;
    private BufferedWriter os;
    private boolean isClosed;

    public BufferedReader getIs() {
        return is;
    }

    public BufferedWriter getOs() {
        return os;
    }

    public int getClientNumber() {
        return clientNumber;
    }

    public ServerThread(Socket socketOfServer, int clientNumber) {
        this.socketOfServer = socketOfServer;
        this.clientNumber = clientNumber;
        System.out.println("Server thread number " + clientNumber + " Started");
        isClosed = false;
    }
    public void write(String message) throws IOException {
        os.write(message);
        os.newLine();
        os.flush();
    }

    @Override
    public void run() {
        try {
            // Open input and output streams on the server socket
            is = new BufferedReader(new InputStreamReader(socketOfServer.getInputStream()));
            os = new BufferedWriter(new OutputStreamWriter(socketOfServer.getOutputStream()));
            System.out.println("Thread " + clientNumber + " started successfully");
            write("get-id" + "," + this.clientNumber);
//            Server.serverThreadBus.sendOnlineList();
//            Server.serverThreadBus.mutilCastSend("global-message" + "," + "---Client " + this.clientNumber + " has logged in---");

            String message;
            while (!isClosed) {
                message = is.readLine();
                if (message == null) {
                    break;
                }

//                String[] messageSplit = message.split(",");


                // Check the message type sent by the client
//                String messageType = messageSplit[0];
                String messageType = message;
                switch (messageType) {
                    case "GET /login":
                        handleLoginRequest();
                        break;
                    case "GET /data":
                        handleDataRequest();
                        break;
                    case "GET /register":
                        handleRegisterRequest();
                        break;
                    case "GET /log":
                        handleLogRequest();
                        break;
                    case "GET /findingCompetitor":
                        handleFindingCompetitorRequest();
                        break;
                    default:
                        // If the message type is not recognized, the server continues listening
                        break;
                }
            }
        } catch (IOException e) {
            isClosed = true;
            Server.serverThreadBus.remove(clientNumber);
            System.out.println("[Client " + this.clientNumber + "] - Exited");
//            Server.serverThreadBus.sendOnlineList();
//            Server.serverThreadBus.mutilCastSend("global-message" + "," + "---Client " + this.clientNumber + " has exited---");
        }
    }

    private void handleFindingCompetitorRequest() throws IOException {
        String response = "Find competitor request";
        System.out.printf("[Client %d] - %s\n", this.clientNumber, response);
        // finding competitor online in stack
        String tmp_userID = is.readLine();
        System.out.printf("[Client %d] - UserID %s\n", this.clientNumber, tmp_userID);
        Server.serverThreadBus.add2WaitingList(tmp_userID);
        Server.serverThreadBus.send2AllCompetitor("Found");
    }

    private void handleLogRequest() {
        try {
//
            String userID = is.readLine();
            String lastQuest = is.readLine();
            String begin = is.readLine();
            String end = is.readLine();
            String timeUsage = is.readLine();
            String listQuestionsStr = is.readLine();
            System.out.println(userID);
            System.out.println(begin);
            System.out.println(end);
            System.out.println(listQuestionsStr);

            String databaseUrl = "jdbc:sqlite:" + File.separator + File.separator + config.db_path;
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            // Establish the connection to the SQLite database
            Connection connection = DriverManager.getConnection(databaseUrl);
            String query = "INSERT INTO play_log (user_id, score, time_usage, last_question, questions_play_history, begin, end, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            PreparedStatement statement_2 = connection.prepareStatement(query);
            statement_2.setString(1, userID);
            statement_2.setString(2, lastQuest);
            statement_2.setString(3, timeUsage);
            statement_2.setString(4, lastQuest);
            statement_2.setString(5, listQuestionsStr);
            statement_2.setString(6, begin);
            statement_2.setString(7, end);
            statement_2.executeUpdate();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }



    private void handleLoginRequest() throws IOException {
        // TODO: Implement the login logic here
        // This is just a placeholder
        String response = "Login request";
        System.out.printf("[Client %d] - %s\n", this.clientNumber, response);
        String username = is.readLine();
        String password = is.readLine();

        System.out.printf("Username: %s - Password: %s\n", username, password);
        try {
            // Specify the path to the SQLite database file
            String databaseUrl = "jdbc:sqlite:" + File.separator + File.separator + config.db_path;

            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Establish the connection to the SQLite database
            try (Connection connection = DriverManager.getConnection(databaseUrl)) {
                String query = "SELECT id FROM nguoi_choi WHERE ten_dang_nhap = ? AND mat_khau = ?";

                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, username);
                    statement.setString(2, password);

                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            // The login is successful
                            int userId = resultSet.getInt("id");
                            write("LOGIN_SUCCESS");
                            write(String.valueOf(userId));
                        } else {
                            // Invalid username or password
                            write("LOGIN_FAILURE");
                            write("0");
                        }
                    }
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            // Handle any errors that occur during the database connection or query execution
            e.printStackTrace();
        }
    }


    private void handleDataRequest() throws IOException {
        String response = "Data request";
        System.out.printf("[Client %d] - %s\n", this.clientNumber, response);

        try {
            // Specify the path to the SQLite database file
            String databaseUrl = "jdbc:sqlite:" + File.separator + File.separator + config.questionsDB_path;

            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            JSONArray jsonArray = new JSONArray();

            // Establish the connection to the SQLite database
            try (Connection connection = DriverManager.getConnection(databaseUrl)) {
                String query = "select  * from Tbl_question WHERE diff_id = 1 ORDER BY RANDOM()  LIMIT 3";

                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    try (ResultSet resultSet = statement.executeQuery()) {

                        while (resultSet.next()) {
                            JSONObject jsonObject = new JSONObject();

                            jsonObject.put("id", resultSet.getInt("_id"));
                            jsonObject.put("noi_dung", Decrypt(resultSet.getString("Question")));
                            jsonObject.put("phuong_an_a", resultSet.getString("ANSW1"));
                            jsonObject.put("phuong_an_b", resultSet.getString("ANSW2"));
                            jsonObject.put("phuong_an_c", resultSet.getString("ANSW3"));
                            jsonObject.put("phuong_an_d", resultSet.getString("ANSW4"));
                            jsonObject.put("dap_an", convert2ABCD(resultSet.getString("ANSWT")));
                            jsonObject.put("path", resultSet.getString("Qpath"));

                            jsonArray.put(jsonObject);
                        }

                    }
                }

                query = "select  * from Tbl_question WHERE diff_id = 2 ORDER BY RANDOM()  LIMIT 5";

                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    try (ResultSet resultSet = statement.executeQuery()) {

                        while (resultSet.next()) {
                            JSONObject jsonObject = new JSONObject();

                            jsonObject.put("id", resultSet.getInt("_id"));
                            jsonObject.put("noi_dung", Decrypt(resultSet.getString("Question")));
                            jsonObject.put("phuong_an_a", resultSet.getString("ANSW1"));
                            jsonObject.put("phuong_an_b", resultSet.getString("ANSW2"));
                            jsonObject.put("phuong_an_c", resultSet.getString("ANSW3"));
                            jsonObject.put("phuong_an_d", resultSet.getString("ANSW4"));
                            jsonObject.put("dap_an", convert2ABCD(resultSet.getString("ANSWT")));
                            jsonObject.put("path", resultSet.getString("Qpath"));

                            jsonArray.put(jsonObject);
                        }

                    }
                }

                query = "select  * from Tbl_question WHERE diff_id = 3 ORDER BY RANDOM()  LIMIT 7";

                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    try (ResultSet resultSet = statement.executeQuery()) {

                        while (resultSet.next()) {
                            JSONObject jsonObject = new JSONObject();

                            jsonObject.put("id", resultSet.getInt("_id"));
                            jsonObject.put("noi_dung", Decrypt(resultSet.getString("Question")));
                            jsonObject.put("phuong_an_a", resultSet.getString("ANSW1"));
                            jsonObject.put("phuong_an_b", resultSet.getString("ANSW2"));
                            jsonObject.put("phuong_an_c", resultSet.getString("ANSW3"));
                            jsonObject.put("phuong_an_d", resultSet.getString("ANSW4"));
                            jsonObject.put("dap_an", convert2ABCD(resultSet.getString("ANSWT")));
                            jsonObject.put("path", resultSet.getString("Qpath"));

                            jsonArray.put(jsonObject);
                        }

                        // Convert the trimmed array to a JSON string
                        String jsonStr = jsonArray.toString();
                        // Send the JSON string to the client
                        write(jsonStr);
                    }
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            // Handle any errors that occur during the database connection or query execution
            e.printStackTrace();
        }
    }

    private String convert2ABCD(String answt) {
        String[] abcd = {"A", "B", "C", "D"};
        return abcd[Integer.parseInt(answt) - 1];
    }

    private String Decrypt(String question) {
        String lastfirst = question.substring(question.length() - 10, question.length()) + question.substring(0, question.length() - 10);
        byte[] decodedBytes = Base64.getDecoder().decode(lastfirst);
        return new String(decodedBytes);
    }

    private void handleRegisterRequest() throws IOException {
        String response = "Register request";
        System.out.printf("[Client %d] - %s\n", this.clientNumber, response);
        String username = is.readLine();
        String password = is.readLine();

        try {
            String databaseUrl = "jdbc:sqlite:" + File.separator + File.separator + config.db_path;
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            // Establish the connection to the SQLite database
            try (Connection connection = DriverManager.getConnection(databaseUrl)) {
                // check user exist
                String query = "SELECT id FROM nguoi_choi WHERE ten_dang_nhap = ?";
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, username);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            // user exist
                            write("0");
                        } else {
                            write("1");
                            String question = is.readLine();
                            String answer = is.readLine();
                            query = "INSERT INTO nguoi_choi (ten_dang_nhap, mat_khau, cau_hoi, cau_tra_loi, created_at, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                            PreparedStatement statement_2 = connection.prepareStatement(query);
                            statement_2.setString(1, username);
                            statement_2.setString(2, password);
                            statement_2.setString(3, question);
                            statement_2.setString(4, answer);
                            statement_2.executeUpdate();
                        }
                    }
                }

            }


        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}