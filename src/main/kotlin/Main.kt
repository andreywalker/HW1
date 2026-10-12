
import AuthResponse.*
import org.sqlite.core.DB
import kotlin.system.exitProcess

fun main() {
    CLI.banner("VERY_NEW_WSP")
    CLI.printk("For your educational needs")

    while (true) {
        var attempts = 0
        var returnToAuthentication = false
        var userName: String? = null
        var userAuthed: Boolean = false


        while (attempts < 3 && !userAuthed) {
            val result = CLI.requestOption(
                "Authentication",
                listOf(
                    Triple(
                        "Sign in",
                        's',
                        { authenticate() }
                    ),
                    Triple(
                        "Register",
                        'r',
                        {
                            AuthResponse.Failed(reg())
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
                's'
            )

            when (result) {
                is AuthResponse.LoggedIn -> {
                    userName = result.userName
                    CLI.printk("Login successful.")
                }

                is AuthResponse.Failed -> {
                    when (result.reason) {
                        AuthResult.WRONG_PASSWORD -> {
                            CLI.printk(
                                "Wrong password. Please try again."
                            )
                            attempts++
                        }

                        AuthResult.NO_USER -> {
                            val choice = CLI.requestOption(
                                "Account not found",
                                listOf(
                                    Triple(
                                        "Try again",
                                        't',
                                        { "retry" }
                                    ),
                                    Triple(
                                        "Register",
                                        'r',
                                        { "register" }
                                    ),
                                    Triple(
                                        "Exit",
                                        'e',
                                        { "exit" }
                                    )
                                ),
                                't'
                            )

                            when (choice) {
                                "retry" -> attempts++
                                "register" -> {
                                    when (reg()) {
                                        AuthResult.REG_SUCCESS ->
                                            CLI.printk(
                                                "Registration submitted. " +
                                                        "Awaiting manager approval."
                                            )

                                        else ->
                                            CLI.printk(
                                                "Registration failed."
                                            )
                                    }
                                }

                                "exit" -> return
                            }
                        }

                        AuthResult.REG_SUCCESS -> {
                            CLI.printk(
                                "Registration submitted. " +
                                        "Awaiting manager approval."
                            )
                        }

                        AuthResult.REG_FAULT -> {
                            CLI.printk("Registration failed.")
                        }

                        else -> {
                            CLI.printk("Authentication failed.")
                        }
                    }
                }
            }
        }

        if (userAuthed and (!userName.isNullOrBlank())) {
            mainMenu(userName.toString())

            // When mainMenu returns, the user has logged out.
            CLI.printk("You have been logged out.")
            continue
        }

        if (attempts >= 3) {
            CLI.printk("Maximum number of attempts reached.")
            return
        }
    }
}

fun authenticate(): AuthResponse {
    val userName = login()

    return if (userName != null) {
        AuthResponse.LoggedIn(userName)
    } else {
        AuthResponse.Failed(AuthResult.NO_USER)
    }
}

fun login(): String? {
    CLI.section("Sign in")

    val userName = CLI.requestInput("Username")
    val password = CLI.requestPassword("Password")
    val passwordHash = DbManager.hash(password)

    if (!DbManager.userExists(userName)) {
        return null
    }
    if (!DbManager.checkLogin(userName, passwordHash)) {
        CLI.printk("Wrong password.")
        return null
    }
    println("check login passes")

    return userName
}

fun reg(): AuthResult {
    CLI.section("Registration")
    val fio: String

    while (true) {
        val input = CLI.requestInput(
            "Full name (surname, name, patronymic)"
        ).trim()

        if (input.matches(Regex("[A-Za-z ]+"))) {
            fio = input
            break
        }

        CLI.printk(
            "Invalid name. Use only Latin letters and spaces."
        )
    }

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

        if (password == confirmation) {
            DbManager.addUserRequest(fio, userType, password)
            return AuthResult.REG_SUCCESS
        }

        CLI.printk("Passwords do not match. Try again.")
    }

    return AuthResult.REG_FAULT
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

sealed interface AuthResponse {
    data class LoggedIn(val userName: String) : AuthResponse
    data class Failed(val reason: AuthResult) : AuthResponse
}
