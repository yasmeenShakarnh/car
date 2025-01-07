import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/carcompany";  // تغيير اسم قاعدة البيانات إذا لزم الأمر
    private static final String USER = "root";  // اسم المستخدم لقاعدة البيانات
    private static final String PASSWORD = "";  // كلمة المرور الخاصة بك

    // الحصول على الاتصال بقاعدة البيانات
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}