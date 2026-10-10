import java.security.MessageDigest
import java.sql.DriverManager
import java.time.LocalDate
import kotlin.use

object DbManager {
    private val connection = DriverManager.getConnection("jdbc:sqlite:HW1.db")


    fun addClass(c:Classs){
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
        var result: Boolean
        connection.createStatement().use { statement ->
             result= statement.executeQuery(
                "SELECT password_h FROM User WHERE userName = '$u.userName'"
            ).getString("password_h")==hash(password)
        }
        connection.close()
        return result
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

    fun checkLogin(username:String, passwordH:String):Boolean{
        var result: Boolean
        connection.createStatement().use { statement ->
            result = statement.executeQuery(
                "SELECT password_h FROM User WHERE userName = '$username'"
            ).getString("password_h")==passwordH
        }
        connection.close()
        return result
    }

    fun userExists(username:String):Boolean{
        var result: Boolean
        connection.createStatement().use { statement ->
            result=statement.executeQuery(
                "SELECT userName FROM User WHERE userName = '$username'"
            ).getString("userName")!=null
        }
        connection.close()
        return result
    }

    fun addUser(u:User, ):User{
        val id = generateId(u)
        u.userId = id
        when (u) {
            is Teacher -> {
                val insertQuery = """
                    INSERT INTO "Teacher"
                        (teacherId, regYear)
                    VALUES (?,?)
                    """.trimIndent()
                connection.prepareStatement(insertQuery).use { statement ->
                    statement.setString(1, u.userId)
                    statement.setInt(2, LocalDate.now().year)
                    statement.executeUpdate()
                }
            }

            is Student -> {
                val insertQuery = """
                    INSERT INTO Student
                        (studentId, studyYear, gpa, speciality)
                    VALUES (?,?,?,?)
                    """.trimIndent()
                connection.prepareStatement(insertQuery).use { statement ->
                    statement.setString(1, u.userId)
                    statement.setInt(2, u.studyYear)
                    statement.setDouble(3, u.gpa)
                    statement.setString(4, u.speciality)
                    statement.executeUpdate()
                }
            }

            is Manager -> {
                val insertQuery = """
                    INSERT INTO Manager
                        (managerId, speciality)
                    VALUES (?,?,?,?)
                    """.trimIndent()
                connection.prepareStatement(insertQuery).use { statement ->
                    statement.setString(1, u.userId)
                    statement.setString(4, u.speciality)
                    statement.executeUpdate()
                }
            }
        }

        val insertQuery = """
        INSERT INTO "User"
            (userId, username, password_h)
        VALUES (?, ?, ?)
        """.trimIndent()
        connection.prepareStatement(insertQuery).use { statement ->
            statement.setString(1, u.userId)
            statement.setString(2, u.userName)
            statement.setString(3, hash(u.passwordH))
            statement.setString(4, u.fio)
            statement.executeUpdate()
        }
        connection.close()
        return u
    }


    fun generateId(u: User): String {
        val year = LocalDate.now().year
        val shortYear = year - 2000

        val (table, idColumn, prefix, suffixLength) = when (u) {
            is Teacher -> listOf("Teacher", "teacherId", "${shortYear}EMP", "3")
            is Student -> listOf("Student", "studentId", "${shortYear}BD", "4")
            is Manager -> listOf("Manager", "managerId", "${shortYear}MNG", "3")
            else -> throw IllegalArgumentException("Unsupported user type")
        }

        val query = """
        SELECT COALESCE(
            MAX(CAST(SUBSTR(t.id, -${suffixLength.toInt()}) AS INTEGER)), 0
        )
        FROM "$table" t
        JOIN "User" usr ON usr.userId = t.id
        WHERE usr.regYear = ?
    """.trimIndent()
            .replace("t.id", "t.$idColumn")

        val lastId = connection.prepareStatement(query).use { statement ->
            statement.setInt(1, year)
            statement.executeQuery().use { rs ->
                rs.next()
                rs.getInt(1)
            }
        }
        connection.close()
        return "$prefix${(lastId + 1).toString().padStart(suffixLength.toInt(), '0')}"
    }

    fun addUserRequest(fio:String, type:UserType){
        val insertQuery = """
        INSERT INTO "ManagerRequest"
            (fio, userType, requestStatus)
        VALUES (?, ?, ?)
        """.trimIndent()
        connection.prepareStatement(insertQuery).use { statement ->
            statement.setString(1, fio)
            statement.setString(2, type.name)
            statement.setString(3, RequestStatus.Pending.name)
            statement.executeUpdate()
        }
    }

    fun hash(input: String): String =
        MessageDigest.getInstance("SHA-256").digest(input.encodeToByteArray()).toHexString()
}
enum class UserType{
    Teacher,
    Student,
    Manager
}
enum class RequestStatus{
    Pending,
    Approved,
    Rejected
}