package server;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    public void write(String message) throws IOException {
        os.write(message);
        os.newLine();
        os.flush();
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
                            write("LOGIN_SUCCESS");
                            write("1");
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
            String databaseUrl = "jdbc:sqlite:" + File.separator + File.separator + config.db_path;

            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Establish the connection to the SQLite database
            try (Connection connection = DriverManager.getConnection(databaseUrl)) {
                String query = "SELECT id, noi_dung, linh_vuc_id, phuong_an_a, phuong_an_b, phuong_an_c, phuong_an_d, dap_an FROM cau_hoi";

                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    try (ResultSet resultSet = statement.executeQuery()) {
                        JSONArray jsonArray = new JSONArray();

                        while (resultSet.next()) {
                            JSONObject jsonObject = new JSONObject();

                            jsonObject.put("id", resultSet.getInt("id"));
                            jsonObject.put("noi_dung", resultSet.getString("noi_dung"));
                            jsonObject.put("linh_vuc_id", resultSet.getInt("linh_vuc_id"));
                            jsonObject.put("phuong_an_a", resultSet.getString("phuong_an_a"));
                            jsonObject.put("phuong_an_b", resultSet.getString("phuong_an_b"));
                            jsonObject.put("phuong_an_c", resultSet.getString("phuong_an_c"));
                            jsonObject.put("phuong_an_d", resultSet.getString("phuong_an_d"));
                            jsonObject.put("dap_an", resultSet.getString("dap_an"));

                            jsonArray.put(jsonObject);
                        }

                        // Shuffle the JSON array
                        List<Object> jsonList = jsonArray.toList();

                        Collections.shuffle(jsonList);

                        // Send only the first 15 elements to the client
                        JSONArray trimmedArray = new JSONArray(jsonList.subList(0, 15));

                        // Convert the trimmed array to a JSON string
                        String jsonStr = trimmedArray.toString();

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

    private void handleRegisterRequest() throws IOException {
        // TODO: Implement the registration logic here
        // This is just a placeholder
        String response = "OK: Register request received";
        write(response);
    }
}