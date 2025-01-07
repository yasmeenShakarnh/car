import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.sql.*;

public class LoginApp extends Application {
    private TextField usernameField;
    private PasswordField passwordField;
    private Label messageLabel;
    private ImageView userIconView;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            usernameField = new TextField();
            usernameField.setPromptText("Enter Username");
            usernameField.setStyle("-fx-font-size: 14px; -fx-padding: 10px; -fx-background-radius: 25px; -fx-background-color: rgba(255, 255, 255, 0.3); -fx-border-color: rgba(255, 255, 255, 0.8); -fx-border-radius: 25px;");
            
            passwordField = new PasswordField();
            passwordField.setPromptText("Enter Password");
            passwordField.setStyle("-fx-font-size: 14px; -fx-padding: 10px; -fx-background-radius: 25px; -fx-background-color: rgba(255, 255, 255, 0.3); -fx-border-color: rgba(255, 255, 255, 0.8); -fx-border-radius: 25px;");
            
            String userIconPath = "file:///C:/Users/Mohammad/eclipse-workspace/Sql-project/src/user_icon2.png";
            Image userIcon = new Image(userIconPath);
            userIconView = new ImageView(userIcon);
            userIconView.setFitWidth(150);
            userIconView.setFitHeight(150);

            messageLabel = new Label();
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
            messageLabel.setTextAlignment(TextAlignment.CENTER);
            messageLabel.setVisible(false);

            Button loginButton = new Button("Login");
            loginButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px; "
                    + "-fx-background-radius: 25px; -fx-border-color: #2980b9; -fx-border-radius: 25px; "
                    + "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 10, 0.5, 2, 2);");

            String imagePath = "file:///C:/Users/Mohammad/eclipse-workspace/Sql-project/src/car1.png";
            Image backgroundImage = new Image(imagePath);
            BackgroundImage myBg = new BackgroundImage(backgroundImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER, BackgroundSize.DEFAULT);
            Background background = new Background(myBg);

            VBox layout = new VBox(20, userIconView, usernameField, passwordField, loginButton, messageLabel);
            layout.setPadding(new Insets(40));
            layout.setBackground(background);
            layout.setAlignment(Pos.CENTER);

            loginButton.setOnAction(e -> {
                try {
                    handleLogin(primaryStage);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            });

            Scene scene = new Scene(layout, 500, 400);
            primaryStage.setTitle("Login");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean validateLogin(String username, String password) {
        String query = "SELECT * FROM user_account WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void handleLogin(Stage primaryStage) throws Exception {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (validateLogin(username, password)) {
            Dashboard dashboard = new Dashboard();
            primaryStage.close();
            dashboard.start(new Stage());
        } else {
            messageLabel.setText("Login failed! Incorrect username or password.");
            messageLabel.setVisible(true);

            shakeAnimation(usernameField);
            shakeAnimation(passwordField);
        }
    }

    private void shakeAnimation(Control control) {
        Timeline shakeAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, e -> control.setTranslateX(0)),
            new KeyFrame(Duration.millis(50), e -> control.setTranslateX(10)),
            new KeyFrame(Duration.millis(100), e -> control.setTranslateX(-10)),
            new KeyFrame(Duration.millis(150), e -> control.setTranslateX(10)),
            new KeyFrame(Duration.millis(200), e -> control.setTranslateX(0))
        );
        shakeAnimation.setCycleCount(1);
        shakeAnimation.play();
    }
}
