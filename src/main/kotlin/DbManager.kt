import java.security.MessageDigest
import java.sql.DriverManager
import kotlin.use

object DbManager {
    private val connection = DriverManager.getConnection("jdbc:sqlite:HW1.db")


    fun addClass(c:Class){
        val insertQuery = """
        INSERT INTO "Class"
            (classId, time, room, courseId, teacherId)
        VALUES (?, ?, ?, ?)
        """.trimIndent()
        var courseId:String;
        var teacherId:String;
        connection.createStatement().use { statement ->
            val result = statement.executeQuery(
                "SELECT courseId FROM Course WHERE courseName = '$c.courseName'"
            )
            courseId = result.getString("courseId")
        }
        connection.createStatement().use { statement ->
            val result = statement.executeQuery(
                "SELECT teacherId FROM Teacher WHERE teacherId = SELECT userId FROM User " +
                        "WHERE firstName = '$c.teacher.firstName' AND surName = '$c.teacher.surName'"
            )
            teacherId = result.getString("teacherId")
        }

        connection.prepareStatement(insertQuery).use { statement ->
            statement.setString(1, c.classId)
            statement.setString(2, c.time)
            statement.setInt(3, c.room)
            statement.setString(4, courseId)
            statement.setString(4, teacherId)
            statement.executeUpdate()
        }

        connection.close()
    }
    fun comparePassword(password:String, u: User):Boolean{
        connection.createStatement().use { statement ->
            val result = statement.executeQuery(
                "SELECT password_h FROM User WHERE userName = '$u.userName'"
            )
            return result.getString("password_h")==hash(password)
        }
    }
    fun updatePassword(password:String, u: User){
        val updateQuery = """
        UPDATE User
        SET password_h = ?
        WHERE userName = ?
        """.trimIndent()
        connection.prepareStatement(updateQuery).use { statement ->
            statement.setString(1, hash(password))
            statement.setString(2, u.userName)
            statement.executeUpdate()
        }
        connection.close()
    }
    private fun hash(input: String): String =
        MessageDigest.getInstance("SHA-256").digest(input.encodeToByteArray()).toHexString()
}