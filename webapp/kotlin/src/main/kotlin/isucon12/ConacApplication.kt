package isucon12

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ConacApplication

fun main(args: Array<String>) {
    runApplication<ConacApplication>(*args)
}
