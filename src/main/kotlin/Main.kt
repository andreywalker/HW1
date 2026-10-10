import CLI.requestOption
import java.lang.Class
import kotlin.system.exitProcess

fun main() {
    lateinit var user:User
    println("Welcome to VERY_NEW_WSP!")
    for (i in 1..3) {
        val signInResult = CLI.requestOption(
            "Sign in or Regista?",
            mapOf("Sign in" to ::login, "Regista" to ::reg),
            "Sign in"
        )
        if (signInResult == "logged in") {
            break
        } else if (signInResult == "wrong password") {
            CLI.printk("Wrong password, try again")
            continue
        } else if (signInResult == "no user") {
            val signInResult = CLI.requestOption(
                "Such user does not exist. Maybe you need to regista first",
                mapOf("Try again" to { "try again" }, "Regista" to ::reg),
                "Try again"
            )
            if (signInResult == "try again") {
                continue
            } else {
                break
            }
        }else if(signInResult=="reg fault"){
            CLI.requestOption("You exceeded the limit of wrong password enter attempts. Quit?",mapOf("OK" to ::quit),"OK")
        } else if(signInResult=="reg success"){
            CLI.requestOption("Your registration request is pending. Wait for manager's approval. Quit?",mapOf("OK" to ::quit),"OK")
        } else {
            CLI.printk("Something went wrong")
        }
    }
}
fun login():String{
    val username = CLI.requestInput("Enter Username")
    val passwordH = DbManager.hash(CLI.requestInput("Enter Password"))
    return if (DbManager.checkLogin(username, passwordH)) "logged in"
    else{
        if(DbManager.userExists(username)) "wrong password"
        else "no user"
    }
}
fun reg():String{
    val fio = CLI.requestInput("Enter your full name (surname, name, patronym)")
    val options: Map<String, () -> UserType> = mapOf(
        "Teacher" to { UserType.Teacher },
        "Student" to { UserType.Student },
        "Manager" to { UserType.Manager }
    )
    val userType = requestOption(
        "Choose user type:",
        options,
        "Student"
    )
    for (i in 1..3) {
        val passwordH = DbManager.hash(CLI.requestInput("Enter Password"))
        if (DbManager.hash(CLI.requestInput("Reenter Password")) == passwordH) {
            DbManager.addUserRequest(fio, userType)
            return "reg success"
        } else {
            CLI.printk("Passwords do not match, Try again")
        }
    }
    return "reg fault"


}
fun createObject(className: String): Any {
    val clazz = Class.forName("com.example.$className")
    return clazz.getDeclaredConstructor().newInstance()
}
fun quit():String{
    exitProcess(0)
    return "ok"
}

