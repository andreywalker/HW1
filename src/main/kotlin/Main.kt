
import kotlin.system.exitProcess

fun main() {
    CLI.banner("VERY_NEW_CLI_WSP")
    CLI.printk("For your educational needs")

    var failedAttempts = 0

    while (true) {
        val response = CLI.requestOption(
            "Authentication",
            listOf(
                Triple("Sign in", 's', { login() }),
                Triple("Register", 'r', { reg() }),
                Triple("Exit", 'e', { AuthResponse.Exit })
            ),
            's'
        )

        when (response) {
            is AuthResponse.LoggedIn -> {
                failedAttempts = 0
                CLI.printk("Login successful.")

                mainMenu(response.userName)

                CLI.printk("You have been logged out.")
            }

            AuthResponse.UserNotFound -> {
                failedAttempts++
                CLI.printk("Account not found.")
            }

            AuthResponse.WrongPassword -> {
                failedAttempts++
                CLI.printk("Wrong password.")
            }

            AuthResponse.RegistrationSubmitted -> {
                CLI.printk(
                    "Registration submitted. Awaiting manager approval."
                )
            }

            AuthResponse.RegistrationFailed -> {
                CLI.printk("Registration failed.")
            }

            AuthResponse.Exit -> quit()
        }

        if (failedAttempts >= 3) {
            CLI.printk("Maximum number of failed sign-in attempts reached.")
            quit()
        }
    }
}



fun login(): AuthResponse {
    CLI.section("Sign in")

    val userName = CLI.requestInput("Username").trim()
    val password = CLI.requestPassword("Password")

    if (!DbManager.userExists(userName)) {
        return AuthResponse.UserNotFound
    }

    val passwordHash = DbManager.hash(password)
    CLI.printk("'"+password+"'")
    CLI.printk(passwordHash)
    if (!DbManager.checkLogin(userName, passwordHash)) {
        return AuthResponse.WrongPassword
    }

    return AuthResponse.LoggedIn(userName)
}


fun reg(): AuthResponse {
    CLI.section("Registration")

    var fio: String

    while (true) {
        val input = CLI.requestInput(
            "Full name (surname, name, patronymic)"
        ).trim()

        if (input.isNotBlank() && input.matches(Regex("[A-Za-z ]+"))) {
            fio = input
            break
        }

        CLI.printk("Invalid name. Use only Latin letters and spaces.")
    }

    val speciality = CLI.requestInput(
        "Enter your speciality"
    ).trim()

    val userType = CLI.requestOption(
        "Choose user type",
        listOf(
            Triple("Teacher", 't', { UserType.Teacher }),
            Triple("Student", 's', { UserType.Student }),
            Triple("Manager", 'm', { UserType.Manager })
        ),
        's'
    )

    repeat(3) {
        val password = CLI.requestPassword("Password")
        val confirmation = CLI.requestPassword("Confirm password")

        if (password.isBlank()) {
            CLI.printk("Password cannot be empty.")
        } else if (password != confirmation) {
            CLI.printk("Passwords do not match. Try again.")
        } else {
            return try {
                DbManager.addUserRequest(fio, userType, password, speciality)
                AuthResponse.RegistrationSubmitted
            } catch (e: Exception) {
                CLI.printk("Could not submit registration.")
                CLI.printk(e.message.toString())
                AuthResponse.RegistrationFailed
            }
        }
    }

    return AuthResponse.RegistrationFailed
}

fun mainMenu(userName: String) {
    while (true) {
        CLI.section("Main menu")
        val userType = DbManager.getUserType(userName)
        when (userType) {
            UserType.Manager -> {
                var result = CLI.requestOption(
                    "Main menu",
                    listOf(
                        Triple(
                            "Users' requests",
                            'r',
                            { usersRequests() }
                        ),
                        Triple(
                            "Set course",
                            's',
                            {
                                createCourse()
                            }
                        ),
                        Triple(
                            "Set class",
                            'c',
                            {
                                createClasss()
                                //AuthResponse.Failed(AuthResult.EXIT)
                            }
                        ),
                        Triple(
                            "User info",
                            'i',
                            {
                                info(userName)
                                //AuthResponse.Failed(AuthResult.EXIT)
                            }
                        ),
                        Triple(
                            "Exit",
                            'e',
                            {
                                quit()
                                //AuthResponse.Failed(AuthResult.EXIT)
                            }
                        )
                    ),
                    default = 'i'
                )
            }

            UserType.Teacher -> TODO()
            UserType.Student -> TODO()
            UserType.UNDEFINED -> TODO()
        }
    }
}

fun usersRequests(){
    CLI.section("Requests for users registration")
    CLI.requestPagedOptions("Choose the request", DbManager.getUserRequests(),::userRequest )

}

fun userRequest(ur:UserRequest){
    CLI.section("Request for ${ur.fio}")
    var result=CLI.requestOption("${ur.fio} is applying for ${ur.userType} registration",listOf(Triple("Approve", 'a',{"approved"}),Triple("Reject", 'r',{"rejected"}),Triple("Back", 'b',{"back"})),default = 'r')
    when(result){
        "approved"->{
            CLI.section("Username assignment")
            var defaultUsername=""
            defaultUsername = when(ur.userType) {
                UserType.Teacher -> {
                    ur.fio.split(" ")[1][0] +"."+ ur.fio.split(" ")[0]
                }

                UserType.Student -> {
                    ur.fio.split(" ")[1][0] +"_"+ ur.fio.split(" ")[0]
                }

                UserType.Manager -> {
                    ur.fio.split(" ")[1][0] +"-"+ ur.fio.split(" ")[0]
                }

                UserType.UNDEFINED -> {
                    ur.fio.split(" ")[1][0] +"."+ ur.fio.split(" ")[0]
                }
            }
            defaultUsername=defaultUsername.lowercase()
            result = CLI.requestOption("Assign default username? (${defaultUsername})", listOf(Triple("Yes", 'y',{"yes"}),Triple("No", 'n',{"no"}), Triple("Back", 'b',{"back"})),default = 'y')
            when(result){
                "yes"->{
                }
                "no"->{
                    defaultUsername = CLI.requestInput("Enter username for ${ur.fio}")

                }
                "back"->{ return}
            }
            when(ur.userType) {
                UserType.Teacher -> DbManager.addUser(Teacher(defaultUsername,"",ur.passwordH, ur.fio))
                UserType.Student -> DbManager.addUser(Student(defaultUsername,"",ur.passwordH, ur.fio, speciality = ur.speciality))
                UserType.Manager -> DbManager.addUser((Manager(defaultUsername,"",ur.passwordH, ur.fio, speciality = ur.speciality)))
                UserType.UNDEFINED -> TODO()
            }
            DbManager.setUserRequestStatus(ur.id, RequestStatus.Approved)
            CLI.printk("User added successfully")
        }
        "rejected"->{
            DbManager.setUserRequestStatus(ur.id,RequestStatus.Rejected)
        }
        "back"->{
            return
        }
    }

}

fun createCourse(){
    CLI.section("Create course")
    val courseName = CLI.requestInput("Enter course name")
    val courseId = CLI.requestInput("Enter course id")
    val credits = CLI.requestInput("Enter course credits")
    val description = CLI.requestInput("Enter course description")
    DbManager.addCourse(Course(courseId,courseName,credits.toInt(),description))
}

fun manageClasss(){

}

fun manageClassses(classs:Classs){

}

fun createClasss(){
    CLI.section("Create class")
    val course:Course = CLI.requestPagedOptions(
        "Choose the course for the class",
        DbManager.getCourses(),
        { course:Course ->course}
    )
    val weekday = CLI.requestOption(
        "Choose week day",
        listOf(
            Triple("Monday", 'm', { "Monday" }),
            Triple("Tuesday", 't', { "Tuesday" }),
            Triple("Wednesday", 'w', { "Wednesday" }),
            Triple("Thursday", 'h', { "Thursday" }),
            Triple("Friday", 'f', { "Friday" }),
            Triple("Saturday", 's', { "Saturday" }),
            Triple("Sunday", 'u', { "Sunday" }),
            Triple(
                "Exit",
                'e',
                {
                    quit()
                    //AuthResponse.Failed(AuthResult.EXIT)
                }
            )
        ),
        'm'
    )
    val timeslot = CLI.requestNumberedOption(
        "Choose timeslot",
        arrayOf(
            Pair("8:00-10:00", { "8:00-10:00" }),
            Pair("10:00-12:00", { "10:00-12:00" }),
            Pair("12:00-14:00", { "12:00-14:00" }),
            Pair("14:00-16:00", { "14:00-16:00" }),
            Pair("16:00-18:00", { "16:00-18:00" }),
            Pair("18:00-20:00", { "18:00-20:00" }),
            Pair("20:00-22:00", { "20:00-22:00" })
        )
    )
    val room = CLI.requestInput("Enter room number").toInt()

    val teacherList:Array<Teacher> = DbManager.getTeachersForTime(weekday,timeslot)
    if (teacherList.isEmpty()){
        CLI.printk("No teachers available for this time")
        return
    }
    CLI.requestPagedOptions(
        "Choose the teacher",
        teacherList,
        {addTeacherToClass(timeslot=timeslot, weekday=weekday, room=room, courseId=course.courseId, t=it)}
    )
    return

}

fun addTeacherToClass(t:Teacher, timeslot:String, weekday:String, room:Int, courseId:String){
    DbManager.addClass(Classs(teacher = t, weekday = weekday, time = timeslot, room = room, course = DbManager.getCourse(courseId)))
    CLI.printk("Class added successfully")
    return

}

fun info(userName:String){
    val userType = DbManager.getUserType(userName)
    when (userType) {
        UserType.Manager -> {
            CLI.section("Manager info")
        }

        UserType.Teacher -> TODO("get user info from db according to type")
        UserType.Student -> TODO()
        UserType.UNDEFINED -> TODO()
    }
}

fun quit(): Nothing {
    CLI.goodbye()
    DbManager.close()
    exitProcess(0)
}
sealed class AuthResponse {
    data class LoggedIn(val userName: String) : AuthResponse()

    data object UserNotFound : AuthResponse()
    data object WrongPassword : AuthResponse()
    data object RegistrationSubmitted : AuthResponse()
    data object RegistrationFailed : AuthResponse()
    data object Exit : AuthResponse()
}