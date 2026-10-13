
import kotlin.system.exitProcess

object CLI {

    fun printk(s: String) {
        println(s)
    }

    fun printk(values: Array<Pair<String, String>>) {
        for ((key, value) in values) {
            println("$key: $value")
        }
    }

    fun <T> printk(
        values: Map<String, () -> T>,
        default: String
    ) {
        for ((key, _) in values) {
            val suffix = if (key == default) " (default)" else ""
            println("$key$suffix")
        }
    }

    fun banner(title: String) {
        println()
        println("==================================================")
        println(title)
        println("==================================================")
    }

    fun section(title: String) {
        println()
        println("--- $title ---")
    }

    fun pause(message: String = "Press Enter to continue...") {
        print("\n$message")
        readlnOrNull() ?: error("Input stream closed")
    }

    fun goodbye(message: String = "Goodbye!") {
        println(message)
    }

    fun requestInput(message: String): String {
        while (true) {
            print("$message: ")
            val input = readlnOrNull()
                ?: error("Input stream closed")

            if (input.isNotBlank()) {
                return input.trim()
            }

            println("Input cannot be empty. Try again.")
        }
    }

    fun requestPassword(message: String = "Enter password"): String {
        val console = System.console()

        if (console != null) {
            while (true) {
                val password = console.readPassword("$message: ")
                    ?.concatToString()
                    ?: error("Input stream closed")

                if (password.isNotBlank()) {
                    return password
                }

                println("Password cannot be empty. Try again.")
            }
        }
        return requestInput(message)
    }

    fun <T> requestOption(
        request: String,
        options: List<Triple<String, Char, () -> T>>,
        default: Char
    ): T = select(request, options, default)

    private fun <T> select(
        request: String,
        options: List<Triple<String, Char, () -> T>>,
        default: Char
    ): T {
        require(options.isNotEmpty()) {
            "Options cannot be empty"
        }

        val keys = options.map { it.second.lowercaseChar() }

        require(keys.distinct().size == keys.size) {
            "Option shortcuts must be unique (case-insensitive)"
        }

        val defaultIndex = keys.indexOf(default.lowercaseChar())

        require(defaultIndex >= 0) {
            "Default shortcut '$default' does not exist in options"
        }

        while (true) {
            section(request)

            options.forEachIndexed { index, (label, shortcut, _) ->
                val key = if (index == defaultIndex) {
                    shortcut.uppercaseChar()
                } else {
                    shortcut.lowercaseChar()
                }

                val suffix = if (index == defaultIndex) {
                    " (default)"
                } else {
                    ""
                }

                println("  $key - $label$suffix")
            }

            val prompt = options.mapIndexed { index, option ->
                val key = option.second
                (
                        if (index == defaultIndex) {
                            key.uppercaseChar()
                        } else {
                            key.lowercaseChar()
                        }
                        ).toString()
            }.joinToString("/")

            print("\nSelect an option [$prompt]: ")

            val input = readlnOrNull()?.trim()
                ?: error("Input stream closed")

            // Enter selects the default option.
            if (input.isEmpty()) {
                return options[defaultIndex].third()
            }

            if (input.length == 1) {
                val selectedIndex =
                    keys.indexOf(input[0].lowercaseChar())

                if (selectedIndex >= 0) {
                    return options[selectedIndex].third()
                }
            }

            println("Invalid option. Enter one of: $prompt")
        }
    }

    fun requestNumberedOption(
        request: String,
        options: Array<Pair<String, () -> String>>
    ): String {
        require(options.isNotEmpty()) {
            "Options cannot be empty"
        }

        while (true) {
            section(request)

            options.forEachIndexed { index, option ->
                println("  ${index + 1}. ${option.first}")
            }

            print("\nEnter option number [1-${options.size}]: ")

            val input = readlnOrNull()?.trim()
                ?: error("Input stream closed")
            when(input){
                "e","exit"->{
                    quit()
                }
            }

            val number = input.toIntOrNull()

            if (number != null && number in 1..options.size) {
                return options[number - 1].second()
            }

            println("Invalid option. Enter a number from 1 to ${options.size}.")
        }
    }

    fun <E, T> requestPagedOptions(
        request: String,
        options: Array<E>,
        action: (E) -> T,
        pageSize: Int = 5
    ): T {
        require(pageSize > 0) {
            "Page size must be positive"
        }



        var page = 0
        val totalPages = (options.size + pageSize - 1) / pageSize

        while (true) {
            val start = page * pageSize
            val end = minOf(start + pageSize, options.size)
            val visibleCount = end - start

            section("$request (Page ${page + 1}/$totalPages)")

            for (index in start until end) {
                println("  ${index - start + 1}. ${options[index]}")
            }

            println()

            if (page < totalPages - 1) {
                println("  n - Next page")
            }

            if (page > 0) {
                println("  p - Previous page")
            }

            println("  b - Back to main menu")

            print("\nSelect an option: ")

            val input = readlnOrNull()?.trim()?.lowercase()
                ?: error("Input stream closed")

            when (input) {
                "n", "next" -> {
                    if (page < totalPages - 1) {
                        page++
                    } else {
                        println("You are already on the last page.")
                    }
                }

                "p", "prev", "previous" -> {
                    if (page > 0) {
                        page--
                    } else {
                        println("You are already on the first page.")
                    }
                }

                "b", "back" -> return action(options[0])

                else -> {
                    val number = input.toIntOrNull()

                    if (number != null && number in 1..visibleCount) {
                        return action(options[start + number - 1])
                    }

                    println(
                        "Invalid option. Enter a number, " +
                                "n/next, p/previous, or b/back."
                    )
                }
            }
        }
    }



    fun requestConfirmation(
        request: String,
        default: Boolean = true
    ): Boolean {
        val prompt = if (default) "Y/n" else "y/N"

        while (true) {
            print("$request [$prompt]: ")

            val input = readlnOrNull()?.trim()?.lowercase()
                ?: error("Input stream closed")

            when (input) {
                "" -> return default
                "y", "yes" -> return true
                "n", "no" -> return false
                else -> println("Enter y or n.")
            }
        }
    }
}
