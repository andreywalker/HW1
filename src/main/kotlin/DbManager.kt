import java.security.MessageDigest
import java.sql.DriverManager
import java.time.LocalDate
import java.util.Date
import kotlin.use

object DbManager {
    private val connection = DriverManager.getConnection("jdbc:sqlite:HW1.db")
    init {
        if (!userExists("admin")){
            addUser(Manager("admin","admin",hash("12345"),"admin","admin"))
        }
        CLI.printk("DB initialised")
    }

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

        //connection.close()
    }

    fun addCourse(c:Course){
        require(
        connection.createStatement().use { statement ->
            statement.executeQuery(
                "SELECT * FROM Course WHERE courseId = '$c.courseId'"
            ).getString("courseId")==null
        }
        ){
            "Course with this id already exists"
        }
        require(
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT * FROM Course WHERE courseName = '${c.courseName}'"
                ).getString("courseName")==null
            }
        ){
            "Course with this name already exists"
        }

        val insertQuery = """
        INSERT INTO "Course"
            (courseId, courseName, credits, description)
        VALUES (?, ?, ?, ?)
        """.trimIndent()
        connection.prepareStatement(insertQuery).use { statement ->
            statement.setString(1, c.courseId)
            statement.setString(2, c.courseName)
            statement.setInt(3, c.credits)
            statement.setString(4, c.description)
            statement.executeUpdate()
        }
        //connection.close()
    }


    fun comparePassword(password:String, u: User):Boolean{
        var result: Boolean
        connection.createStatement().use { statement ->
             result= statement.executeQuery(
                "SELECT password_h FROM User WHERE userName = '$u.userName'"
            ).getString("password_h")==hash(password)
        }
        //connection.close()
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
        //connection.close()
    }

    fun checkLogin(username:String, passwordH:String):Boolean{
        var result: Boolean
        connection.createStatement().use { statement ->
            result = statement.executeQuery(
                "SELECT password_h FROM User WHERE userName = '$username'"
            ).getString("password_h")==passwordH
        }
        //connection.close()
        return result
    }

    fun userExists(username:String):Boolean{
        var result: Boolean
        connection.createStatement().use { statement ->
            result=statement.executeQuery(
                "SELECT userName FROM User WHERE userName = '$username'"
            ).getString("userName")!=null
        }
        //connection.close()
        return result
    }

    fun addUser(u:User, ):User{
        val id = generateId(u)
        u.userId = id
        var userType =UserType.UNDEFINED
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
                userType=UserType.Teacher
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
                userType=UserType.Student
            }

            is Manager -> {
                val insertQuery = """
                    INSERT INTO Manager
                        (managerId, speciality)
                    VALUES (?,?)
                    """.trimIndent()
                connection.prepareStatement(insertQuery).use { statement ->
                    statement.setString(1, u.userId)
                    statement.setString(2, u.speciality)
                    statement.executeUpdate()
                }
                userType=UserType.Manager
            }
        }

        val insertQuery = """
        INSERT INTO "User"
            (userId, username, password_h, fio, userType, regYear)
        VALUES (?, ?, ?,?,?,?)
        """.trimIndent()
        connection.prepareStatement(insertQuery).use { statement ->
            statement.setString(1, u.userId)
            statement.setString(2, u.userName)
            statement.setString(3, hash(u.passwordH))
            statement.setString(4, u.fio)
            statement.setString(5, userType.name)
            statement.setString(6, LocalDate.now().year.toString())
            statement.executeUpdate()
        }
        //connection.close()
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
        //connection.close()
        return "$prefix${(lastId + 1).toString().padStart(suffixLength.toInt(), '0')}"
    }

    fun addUserRequest(fio:String, type:UserType, password: String){
        val insertQuery = """
        INSERT INTO "ManagerRequest"
            (fio, userType, requestStatus, regDate, password_h)
        VALUES (?, ?, ?,?,?)
        """.trimIndent()
        connection.prepareStatement(insertQuery).use { statement ->
            statement.setString(1, fio)
            statement.setString(2, type.name)
            statement.setString(3, RequestStatus.Pending.name)
            statement.setString(4, LocalDate.now().toString())
            statement.setString(5, hash(password))
            statement.executeUpdate()
        }
    }

    fun getUserType( userName:String): UserType {
        var result: UserType
        connection.createStatement().use { statement ->
            result= UserType.valueOf(statement.executeQuery(
                "SELECT userType FROM User WHERE userName = '$userName'"
            ).getString("userType"))
        }
        return result
    }

    fun getUserRequests():Array<UserRequest>{
        var result: Array<UserRequest>
        connection.createStatement().use { statement ->
            result = statement.executeQuery(
                "SELECT * FROM ManagerRequest WHERE requestStatus = 'Pending'"
            ).use { rs ->
                val requests = mutableListOf<UserRequest>()

                while (rs.next()) {
                    requests.add(
                        UserRequest(
                            id= rs.getInt("id"),
                            fio= rs.getString("fio"),
                            userType= UserType.valueOf(rs.getString("userType")),
                            requestStatus= RequestStatus.valueOf(rs.getString("requestStatus")),
                            regDate= LocalDate.parse(rs.getString("regDate")),
                            passwordH= rs.getString("password_h"),
                            speciality= rs.getString("speciality")
                        )
                    )
                }

                requests.toTypedArray()
            }
        }
        return result
    }

    fun setUserRequestStatus(id:Int, status:RequestStatus){
        val updateQuery = """
        UPDATE ManagerRequest
        SET requestStatus = ?
        WHERE id = ?
        """.trimIndent()
        connection.prepareStatement(updateQuery).use { statement ->
            statement.setString(1, status.name)
            statement.setInt(2, id)
            statement.executeUpdate()
        }
    }

    fun close() {
        connection.close()
    }

    fun hash(input: String): String =
        MessageDigest.getInstance("SHA-256").digest(input.encodeToByteArray()).toHexString()
}
enum class UserType{
    Teacher,
    Student,
    Manager,
    UNDEFINED
}
enum class RequestStatus{
    Pending,
    Approved,
    Rejected
}
enum class AuthResult {
    LOGGED_IN,
    WRONG_PASSWORD,
    NO_USER,
    REG_SUCCESS,
    REG_FAULT,
    RETRY,
    EXIT
}

data class UserRequest(
    val id:Int,
    val fio:String,
    val userType:UserType,
    val requestStatus:RequestStatus,
    val regDate: LocalDate,
    val passwordH: String,
    val speciality: String)