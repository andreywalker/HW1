import java.util.Scanner
import kotlin.text.ifEmpty

object CLI {
    private val scanner = Scanner(System.`in`)

    fun printk(s: String) {
        println(s)
    }

    fun printk(values: Array<Pair<String, ()->String>>, default:Int = 1) {
        for ((first, second) in values) {
            if (first == values[default].first)println("[Default] $first: $second")
            else println("$first: $second")
        }
    }

    /*fun printk(values: Map<String, ()->String >, default: String) {
        for ((first, second) in values) {
            if (first == default)println("[Default] $first: $second")
            else println("$first: $second")
        }
    }*/
    fun <T> printk(values: Map<String, () -> T>, default: String) {
        for ((name, _) in values) {
            if (name == default) println("[Default] $name")
            else println(name)
        }
    }

    fun printRequest(s:String){
        printk("$s: ")
    }

    /*fun requestOption(request:String, options: Map<String, ()->String>, default: String): String {
        printRequest(request)
        printk(options, default)
        val input = scanner.nextLine()
        return options[input]?.invoke()
            ?: options[default]!!.invoke()
    }*/

    //generic overload
    fun <T> requestOption(
        request: String,
        options: Map<String, () -> T>,
        default: String
    ): T {
        printRequest(request)
        printk(options, default)

        val input = scanner.nextLine().trimIndent()
        return options[input]?.invoke()
            ?: options[default]!!.invoke()
    }

    fun requestNumberedOption(request:String, options: Array<Pair<String, () -> String>>): String {
        printk(options,0)
        printRequest(request)
        val input = scanner.nextLine()
        return if (input.isEmpty()) {
            options[0].second()
        } else {
            options[input.toInt()].second()
        }
    }

    fun requestInput(message:String): String {
        print(message)
        val input = scanner.nextLine().trimIndent()
        return input.ifEmpty { requestInput(message) }
    }

}
