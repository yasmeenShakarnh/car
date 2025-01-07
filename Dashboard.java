import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import java.sql.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Dashboard extends Application {
    private VBox dashboardLayout;
    private HBox headerLayout;
    private BorderPane mainLayout;
    private Label contentLabel;
    private TextField searchField;
    private Button addButton, updateButton, searchButton;
    private TableView<ObservableList<String>> dataTable;
    private ObservableList<ObservableList<String>> tableData;
    private String selectedTable;

    private ObservableList<String> phoneNumbers = FXCollections.observableArrayList();
    private ObservableList<String> vins = FXCollections.observableArrayList();
    private ObservableList<String> zipCodes = FXCollections.observableArrayList();

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) {
        contentLabel = new Label("Select a table to manage.");

        searchField = new TextField();
        searchField.setPromptText("Search...");
        searchButton = new Button("Search");
        addButton = new Button("Add");
        updateButton = new Button("Update");
        searchButton.setOnAction(e -> searchAction());
        addButton.setOnAction(e -> openAddForm());
        updateButton.setOnAction(e -> updateAction());

        HBox actionLayout = new HBox(10, searchField, searchButton, addButton, updateButton);
        actionLayout.setPadding(new Insets(10));

        ListView<String> menuList = new ListView<>();
        menuList.getItems().addAll("cars", "customers", "employees", "orders", "payments", "services");
        menuList.setStyle("-fx-font-size: 20px; -fx-padding: 10px;");
        menuList.setOnMouseClicked(event -> {
            selectedTable = menuList.getSelectionModel().getSelectedItem();
            showContent(selectedTable);
        });

        ImageView menuImageView = new ImageView(new Image("file:C:\\Users\\Mohammad\\eclipse-workspace\\Sql-project\\src\\car.png")); // ضع مسار الصورة
        menuImageView.setFitWidth(200); 
        menuImageView.setPreserveRatio(true); 

        VBox menuBox = new VBox(10, menuImageView, menuList);
        menuBox.setPadding(new Insets(10));

        Button reportButton = new Button("Generate Reports");
        reportButton.setStyle("-fx-background-color: #673AB7; -fx-text-fill: white; -fx-font-size: 20px; -fx-padding: 5px;     -fx-font-weight: bold;\r\n"
        		+ "");
        reportButton.setOnAction(e -> openReportWindow());
        HBox reportButtonLayout = new HBox(10, reportButton);
        reportButtonLayout.setPadding(new Insets(10));

        // تخطيط واجهة المستخدم
        dashboardLayout = new VBox(20, contentLabel, actionLayout, new Label("Data Table:"));
        dashboardLayout.setPadding(new Insets(20));
        dataTable = new TableView<>();
        dashboardLayout.getChildren().addAll(dataTable, reportButtonLayout);

        // الهيدر
        headerLayout = new HBox(10, new Label("Car Dealership Management"));
        headerLayout.setPadding(new Insets(10));
        headerLayout.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        // التخطيط الرئيسي
        mainLayout = new BorderPane();
        mainLayout.setTop(headerLayout);
        mainLayout.setLeft(menuBox);
        mainLayout.setCenter(dashboardLayout);

        Scene scene = new Scene(mainLayout, 800, 500);
        scene.getStylesheets().add(getClass().getResource("style1.css").toExternalForm());

        primaryStage.setTitle("Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showContent(String tableName) {
        contentLabel.setText("Managing " + tableName);
        loadTableData(tableName);
    }


    private void loadTableData(String tableName) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT * FROM " + tableName;
            ResultSet rs = conn.createStatement().executeQuery(query);
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            dataTable.getColumns().clear();
            tableData = FXCollections.observableArrayList();

            for (int i = 1; i <= columnCount; i++) {
                final int columnIndex = i;
                TableColumn<ObservableList<String>, String> column = new TableColumn<>(metaData.getColumnName(i));
                column.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(columnIndex - 1)));
                dataTable.getColumns().add(column);
            }

            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i));
                }
                tableData.add(row);
            }
            dataTable.setItems(tableData);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void openAddForm() {
        if (selectedTable != null) {
            Stage addStage = new Stage();
            VBox addLayout = new VBox(10);
            addLayout.setPadding(new Insets(10));
            String[] columnNames = getColumnNames(selectedTable);
            TextField[] fields = new TextField[columnNames.length];

            for (int i = 0; i < columnNames.length; i++) {
                Label fieldLabel = new Label("Enter " + columnNames[i]);
                fields[i] = new TextField();
                fields[i].setPromptText("Enter " + columnNames[i]);
                addLayout.getChildren().addAll(fieldLabel, fields[i]);
            }

            Button saveButton = new Button("Save");
            saveButton.setOnAction(e -> {
                if (validateInputs(fields, columnNames)) {
                    insertNewData(selectedTable, fields);
                    addStage.close();
                }
            });

            addLayout.getChildren().add(saveButton);
            Scene addScene = new Scene(addLayout, 300, 400);
            addStage.setScene(addScene);
            addStage.setTitle("Add Data");
            addStage.show();
        } else {
            showErrorMessage("No Table Selected", "Please select a table to add data.");
        }
    }

    private boolean validateInputs(TextField[] fields, String[] columnNames) {
        for (int i = 0; i < columnNames.length; i++) {
            String inputValue = fields[i].getText().trim();

            if (inputValue.isEmpty()) {
                showErrorMessage("Input Error", "The " + columnNames[i] + " field cannot be empty.");
                return false;
            }
            if (!validateFieldType(columnNames[i], inputValue)) {
                return false;
            }
            if (!validateUniqueValue(columnNames[i], inputValue)) {
                return false;
            }
        }
        return true;
    }

    private boolean validateFieldType(String columnName, String value) {
        switch (columnName.toLowerCase()) {
            case "phone":
            case "zipcode":
            case "year":
            case "price":
            case "cost":
            case "quantity":
            case "totalprice":
            case "amount":
            case "salary": 
                if (!value.matches("\\d+")) {
                    showErrorMessage("Input Error", columnName + " must be numeric.");
                    return false;
                }
                break;
            case "firstname":
            case "lastname":
            case "make":
            case "model":
            case "position":
                if (!value.matches("[a-zA-Z\\s]+")) {
                    showErrorMessage("Input Error", columnName + " must contain only letters and spaces.");
                    return false;
                }
                break;
            case "email":
                if (!isValidEmail(value)) {
                    showErrorMessage("Input Error", "Invalid email format.");
                    return false;
                }
                break;
            default:
                break;
        }
        return true;
    }
    private boolean validateUniqueValue(String columnName, String value) {
        if (columnName.equalsIgnoreCase("Phone") && !isUniquePhoneNumber(value)) {
            showErrorMessage("Unique Value Error", "The phone number must be unique.");
            return false;
        }
        if (columnName.equalsIgnoreCase("VIN") && !isUniqueVIN(value)) {
            showErrorMessage("Unique Value Error", "The VIN must be unique.");
            return false;
        }
        if (columnName.equalsIgnoreCase("ZipCode") && !isUniqueZipCode(value)) {
            showErrorMessage("Unique Value Error", "The Zip Code must be unique.");
            return false;
        }
        return true;
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }
    private boolean isUniquePhoneNumber(String phone) {
        if (phoneNumbers.contains(phone)) {
            return false;
        } else {
            phoneNumbers.add(phone);
            return true;
        }
    }
    private boolean isUniqueVIN(String vin) {
        if (vins.contains(vin)) {
            return false; 
        } else {
            vins.add(vin); 
            return true;
        }
    }
    private boolean isUniqueZipCode(String zipcode) {
        if (zipCodes.contains(zipcode)) {
            return false;
        } else {
            zipCodes.add(zipcode); 
            return true;
        }
    }

    private void insertNewData(String tableName, TextField[] fields) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            StringBuilder query = new StringBuilder("INSERT INTO " + tableName + " (");
            for (int i = 0; i < fields.length; i++) {
                query.append(getColumnNames(tableName)[i]);
                if (i < fields.length - 1) query.append(", ");
            }
            query.append(") VALUES (");
            for (int i = 0; i < fields.length; i++) {
                query.append("?"); 
                if (i < fields.length - 1) query.append(", ");
            }
            query.append(")");

            PreparedStatement pstmt = conn.prepareStatement(query.toString());
            for (int i = 0; i < fields.length; i++) {
                pstmt.setString(i + 1, fields[i].getText());
            }
            pstmt.executeUpdate();
            showSuccessMessage("Data added successfully.");
            loadTableData(tableName); // Refresh table data after insertion
        } catch (SQLException e) {
            showErrorMessage("Database Error", e.getMessage());
        }
    }

    private String[] getColumnNames(String tableName) {
        switch (tableName) {
            case "cars":
                return new String[]{"CarID", "Make", "Model", "Year", "Price", "Stock", "VIN"};
            case "customers":
                return new String[]{"CustomerID", "FirstName", "LastName", "Email", "Phone", "Address", "City", "State", "ZipCode"};
            case "employees":
                return new String[]{"EmployeeID", "FirstName", "LastName", "Position", "Salary", "HireDate"};
            case "orders":
                return new String[]{"OrderID","OrderDate", "CarID", "CustomerID", "EmployeeID", "Quantity", "TotalPrice"};
            case "payments":
                return new String[]{"PaymentID", "OrderID", "PaymentDate", "PaymentMethod", "Amount"};
            case "services":
                return new String[]{"ServiceID", "CarID", "CustomerID","ServiceDate ","ServiceDescription", "Cost"};
            default:
                return new String[]{}; // Empty array in case of unrecognized table name
        }
    }

    private void searchAction() {
        String searchText = searchField.getText().trim();

        if (searchText.isEmpty()) {
            showErrorMessage("Search Error", "Please enter a search term.");
            return;
        }

        if (selectedTable != null) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String columnNames[] = getColumnNames(selectedTable);
                StringBuilder query = new StringBuilder("SELECT * FROM " + selectedTable + " WHERE ");

                // بناء الاستعلام بناءً على نوع العمود
                for (int i = 0; i < columnNames.length; i++) {
                    if (isNumericColumn(selectedTable, columnNames[i])) {
                        // البحث عن تطابق دقيق للأرقام
                        query.append(columnNames[i]).append(" = ? ");
                    } else {
                        // البحث عن تطابق دقيق للنصوص (حروف كبيرة وصغيرة)
                        query.append("LOWER(").append(columnNames[i]).append(") = ? ");
                    }

                    if (i < columnNames.length - 1) {
                        query.append("OR ");
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(query.toString())) {
                    // تعيين القيم للبحث في الأعمدة بناءً على النوع
                    for (int i = 0; i < columnNames.length; i++) {
                        if (isNumericColumn(selectedTable, columnNames[i])) {
                            stmt.setString(i + 1, searchText); // دقة الأرقام
                        } else {
                            stmt.setString(i + 1, searchText.toLowerCase()); 
                        }
                    }

                    ResultSet rs = stmt.executeQuery();
                    loadSearchResults(rs);
                }
            } catch (SQLException e) {
                showErrorMessage("Database Error", e.getMessage());
            }
        } else {
            showErrorMessage("No Table Selected", "Please select a table to search.");
        }
    }

    private boolean isNumericColumn(String tableName, String columnName) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, tableName);
                stmt.setString(2, columnName);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String dataType = rs.getString("DATA_TYPE").toLowerCase();
                    return dataType.equals("int") || dataType.equals("bigint") || dataType.equals("decimal") || dataType.equals("float") || dataType.equals("double");
                }
            }
        }
        return false;
    }

    private void loadSearchResults(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        dataTable.getColumns().clear(); // ة
        tableData = FXCollections.observableArrayList(); 

        for (int i = 1; i <= columnCount; i++) {
            final int columnIndex = i;
            TableColumn<ObservableList<String>, String> column = new TableColumn<>(metaData.getColumnName(i));
            column.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(columnIndex - 1)));
            dataTable.getColumns().add(column);
        }

        while (rs.next()) {
            ObservableList<String> row = FXCollections.observableArrayList();
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getString(i));
            }
            tableData.add(row);
        }
        dataTable.setItems(tableData);

        if (tableData.isEmpty()) {
            dataTable.setPlaceholder(new Label("No matching results"));
        } else {
            dataTable.setPlaceholder(null);
        }
    }

    private void updateAction() {
        if (dataTable.getSelectionModel().getSelectedItem() == null) {
            showErrorMessage("Update Error", "Please select a row to update.");
            return;
        }

        ObservableList<String> selectedRow = dataTable.getSelectionModel().getSelectedItem();

        Stage updateStage = new Stage();
        VBox updateLayout = new VBox(10);
        updateLayout.setPadding(new Insets(10));
        String[] columnNames = getColumnNames(selectedTable);
        TextField[] fields = new TextField[columnNames.length];

        for (int i = 0; i < columnNames.length; i++) {
            Label fieldLabel = new Label(columnNames[i]);
            fields[i] = new TextField(selectedRow.get(i)); 
            updateLayout.getChildren().addAll(fieldLabel, fields[i]);
        }

        Button saveButton = new Button("Save Changes");
        saveButton.setOnAction(e -> {
            if (validateInputs(fields, columnNames)) {
                updateExistingData(selectedTable, selectedRow, fields);
                updateStage.close();
            }
        });

        updateLayout.getChildren().add(saveButton);
        Scene updateScene = new Scene(updateLayout, 400, 400);
        updateStage.setScene(updateScene);
        updateStage.setTitle("Update Data");
        updateStage.show();
    }
    private void updateExistingData(String tableName, ObservableList<String> selectedRow, TextField[] fields) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            StringBuilder query = new StringBuilder("UPDATE " + tableName + " SET ");
            for (int i = 0; i < fields.length; i++) {
                query.append(getColumnNames(tableName)[i]).append(" = ?");
                if (i < fields.length - 1) query.append(", ");
            }
            query.append(" WHERE ");
            query.append(getColumnNames(tableName)[0]).append(" = ?");

            PreparedStatement pstmt = conn.prepareStatement(query.toString());
            for (int i = 0; i < fields.length; i++) {
                pstmt.setString(i + 1, fields[i].getText());
            }
            pstmt.setString(fields.length + 1, selectedRow.get(0)); 
            pstmt.executeUpdate();
            showSuccessMessage("Data updated successfully.");
            loadTableData(tableName);
        } catch (SQLException e) {
            showErrorMessage("Database Error", e.getMessage());
        }
    }    


    private void openReportWindow() {
        Stage reportStage = new Stage();
        VBox reportLayout = new VBox(10);
        reportLayout.setPadding(new Insets(10));
        reportLayout.setId("reportLayout");

        ComboBox<String> reportType = new ComboBox<>();
        reportType.getItems().addAll(
            "Services for Car/Customer", 
            "Sales by Employee", 
            "Payments by Customer", 
            "Monthly/Quarterly Revenue", 
            "Service Frequency", 
            "Service Costs History"
        );
        reportType.setPromptText("Select Report Type");

        TextField carIdField = new TextField();
        carIdField.setPromptText("Enter CarID ");
        carIdField.setVisible(false);

        TextField customerIdField = new TextField();
        customerIdField.setPromptText("Enter CustomerID ");
        customerIdField.setVisible(false);

        TextField employeeIdField = new TextField();
        employeeIdField.setPromptText("Enter EmployeeID ");
        employeeIdField.setVisible(false);

        ChoiceBox<String> periodChoiceBox = new ChoiceBox<>();
        periodChoiceBox.getItems().addAll("Monthly", "Quarterly");
        periodChoiceBox.setValue("Select Period Type");
        periodChoiceBox.setVisible(false);

        reportType.setOnAction(e -> {
            String selectedReport = reportType.getValue();
            carIdField.setVisible(false);
            customerIdField.setVisible(false);
            employeeIdField.setVisible(false);
            periodChoiceBox.setVisible(false);

            switch (selectedReport) {
                case "Services for Car/Customer":
                    carIdField.setVisible(true);
                    customerIdField.setVisible(true);
                    break;
                case "Sales by Employee":
                    employeeIdField.setVisible(true);
                    break;
                case "Payments by Customer":
                    customerIdField.setVisible(true);
                    break;
                case "Service Costs History":
                    carIdField.setVisible(true);
                    break;
                case "Monthly/Quarterly Revenue":
                    periodChoiceBox.setVisible(true);
                    break;
                default:
                    break;
            }
        });

        Button generateButton = new Button("Generate");
        generateButton.setOnAction(e -> {
            String selectedReport = reportType.getValue();
            String periodType = periodChoiceBox.getValue();
            Integer carId = carIdField.getText().isEmpty() ? null : Integer.valueOf(carIdField.getText());
            Integer customerId = customerIdField.getText().isEmpty() ? null : Integer.valueOf(customerIdField.getText());
            Integer employeeId = employeeIdField.getText().isEmpty() ? null : Integer.valueOf(employeeIdField.getText());

            generateReport(selectedReport, carId, customerId, employeeId, periodType);
        });

        reportLayout.getChildren().addAll(reportType, carIdField, customerIdField, employeeIdField, periodChoiceBox, generateButton);
        Scene scene = new Scene(reportLayout, 400, 200);
        scene.getStylesheets().add(getClass().getResource("style1.css").toExternalForm());
        reportStage.setScene(scene);
        reportStage.setTitle("Generate Reports");
        reportStage.show();
    }

    private void generateReport(String reportType, Integer carId, Integer customerId, Integer employeeId, String periodType) {
        if (reportType == null || periodType == null) {
            showErrorMessage("Report Error", "Please select both report type and period type (Monthly or Quarterly).");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "";

            if ("Services for Car/Customer".equals(reportType)) {
                if (carId != null && customerId == null) {
                    query = "SELECT cars.CarID, cars.Make, cars.Model, services.ServiceDate, services.ServiceDescription, services.Cost " +
                            "FROM cars " +
                            "JOIN services ON cars.CarID = services.CarID " +
                            "WHERE cars.CarID = ?";
                } else if (customerId != null && carId == null) {
                    query = "SELECT customers.CustomerID, customers.FirstName, customers.LastName, services.ServiceDate, services.ServiceDescription, services.Cost " +
                            "FROM customers " +
                            "JOIN services ON customers.CustomerID = services.CustomerID " +
                            "WHERE customers.CustomerID = ?";
                } else if (carId != null && customerId != null) {
                    query = "SELECT cars.CarID, cars.Make, cars.Model, customers.CustomerID, customers.FirstName, customers.LastName, services.ServiceDate, services.ServiceDescription, services.Cost " +
                            "FROM cars " +
                            "JOIN services ON cars.CarID = services.CarID " +
                            "JOIN customers ON customers.CustomerID = services.CustomerID " +
                            "WHERE cars.CarID = ? AND customers.CustomerID = ?";
                } else {
                    query = "SELECT cars.CarID, cars.Make, cars.Model, customers.CustomerID, customers.FirstName, customers.LastName, services.ServiceDate, services.ServiceDescription, services.Cost " +
                            "FROM cars " +
                            "JOIN services ON cars.CarID = services.CarID " +
                            "LEFT JOIN customers ON customers.CustomerID = services.CustomerID";
                }
            } else if ("Sales by Employee".equals(reportType)) {
                if (employeeId != null) {
                    query = "SELECT employees.EmployeeID, employees.FirstName, employees.LastName, orders.OrderID, orders.OrderDate, orders.TotalPrice " +
                            "FROM employees " +
                            "JOIN orders ON employees.EmployeeID = orders.EmployeeID " +
                            "WHERE employees.EmployeeID = ?";
                } else {
                    query = "SELECT employees.EmployeeID, employees.FirstName, employees.LastName, orders.OrderID, orders.OrderDate, orders.TotalPrice " +
                            "FROM employees " +
                            "JOIN orders ON employees.EmployeeID = orders.EmployeeID";
                }
            } else if ("Payments by Customer".equals(reportType)) {
                if (customerId != null) {
                    query = "SELECT payments.PaymentMethod, payments.Amount, orders.OrderID, orders.OrderDate, customers.FirstName " +
                            "FROM payments " +
                            "JOIN orders ON payments.OrderID = orders.OrderID " +
                            "JOIN customers ON orders.CustomerID = customers.CustomerID " +
                            "WHERE customers.CustomerID = ?";
                } else {
                    query = "SELECT payments.PaymentMethod, payments.Amount, orders.OrderID, orders.OrderDate, customers.FirstName " +
                            "FROM payments " +
                            "JOIN orders ON payments.OrderID = orders.OrderID " +
                            "JOIN customers ON orders.CustomerID = customers.CustomerID";
                }
            } else if ("Monthly/Quarterly Revenue".equals(reportType)) {
                if ("Monthly".equals(periodType)) {
                    query = "SELECT DATE_FORMAT(ServiceDate, '%Y-%m') AS Month, ServiceDescription, SUM(Cost) AS Revenue " +
                            "FROM services " +
                            "GROUP BY Month, ServiceDescription";
                } else if ("Quarterly".equals(periodType)) {
                    query = "SELECT CASE " +
                            "WHEN EXTRACT(MONTH FROM ServiceDate) BETWEEN 1 AND 3 THEN 'Q1' " +
                            "WHEN EXTRACT(MONTH FROM ServiceDate) BETWEEN 4 AND 6 THEN 'Q2' " +
                            "WHEN EXTRACT(MONTH FROM ServiceDate) BETWEEN 7 AND 9 THEN 'Q3' " +
                            "WHEN EXTRACT(MONTH FROM ServiceDate) BETWEEN 10 AND 12 THEN 'Q4' " +
                            "END AS Quarter, " +
                            "ServiceDescription, SUM(Cost) AS Revenue " +
                            "FROM services " +
                            "GROUP BY Quarter, ServiceDescription";
                } else {
                    showErrorMessage("Report Error", "Please select a valid period type (Monthly or Quarterly).");
                    return;
                }
            } else if ("Service Frequency".equals(reportType)) {
                query = "SELECT cars.Model, COUNT(services.ServiceID) AS ServiceCount " +
                        "FROM services " +
                        "JOIN cars ON services.CarID = cars.CarID " +
                        "GROUP BY cars.Model";
            } else if ("Service Costs History".equals(reportType)) {
                if (carId != null) {
                    query = "SELECT cars.CarID, cars.Make, cars.Model, services.ServiceDate, services.ServiceDescription, services.Cost " +
                            "FROM services " +
                            "JOIN cars ON services.CarID = cars.CarID " +
                            "WHERE cars.CarID = ?";
                } else {
                    query = "SELECT cars.CarID, cars.Make, cars.Model, services.ServiceDate, services.ServiceDescription, services.Cost " +
                            "FROM services " +
                            "JOIN cars ON services.CarID = cars.CarID";
                }
            } else {
                showErrorMessage("Report Error", "Invalid report type.");
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                int paramIndex = 1;
                if ("Services for Car/Customer".equals(reportType)) {
                    if (carId != null && customerId == null) {
                        stmt.setInt(paramIndex++, carId);
                    } else if (customerId != null && carId == null) {
                        stmt.setInt(paramIndex++, customerId);
                    } else if (carId != null && customerId != null) {
                        stmt.setInt(paramIndex++, carId);
                        stmt.setInt(paramIndex++, customerId);
                    }
                } else if ("Sales by Employee".equals(reportType)) {
                    if (employeeId != null) {
                        stmt.setInt(paramIndex++, employeeId);
                    }
                } else if ("Payments by Customer".equals(reportType)) {
                    if (customerId != null) {
                        stmt.setInt(paramIndex++, customerId);
                    }
                } else if ("Service Costs History".equals(reportType)) {
                    if (carId != null) {
                        stmt.setInt(paramIndex++, carId);
                    }
                }

                ResultSet rs = stmt.executeQuery();
                displayReportData(rs);

            }
        } catch (SQLException e) {
            showErrorMessage("Database Error", e.getLocalizedMessage());
        }
    }

    private void displayReportData(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        dataTable.getColumns().clear();
        tableData = FXCollections.observableArrayList();

        for (int i = 1; i <= columnCount; i++) {
            final int columnIndex = i;
            TableColumn<ObservableList<String>, String> column = new TableColumn<>(metaData.getColumnName(i));
            column.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(columnIndex - 1)));
            dataTable.getColumns().add(column);
        }

        while (rs.next()) {
            ObservableList<String> row = FXCollections.observableArrayList();
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getString(i));
            }
            tableData.add(row);
        }
        dataTable.setItems(tableData);
    }
    private void showErrorMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }
}