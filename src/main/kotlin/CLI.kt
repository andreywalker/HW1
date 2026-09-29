import java.util.Scanner
import kotlin.text.ifEmpty

object CLI {
    private val scanner = Scanner(System.`in`)

    fun print(s: String) {
        println(s)
    }

    fun <T> print(values: Array<Pair<T, String>>) {
        for (value in values) {
            println("${value.first}: ${value.second}")
        }
    }

    fun printRequest(s:String){
        print("$s: ")
    }

    fun requestOption(request:String, options: Array<Pair<String, String>>, default: String): String {
        print(options)
        printRequest(request)
        val input = scanner.nextLine()
        return input.ifEmpty { default }
    }

    fun requestNumberedOption(request:String, options: Array<Pair<Int, String>>): Int {
        print(options)
        printRequest(request)
        return scanner.nextInt().also {
            scanner.nextLine()
        }
    }

    fun requestInput(message:String): String {
        print(message)
        val input = scanner.nextLine()
        return input.ifEmpty { requestInput(message) }
    }

}