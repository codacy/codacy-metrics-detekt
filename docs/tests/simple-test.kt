// #Metrics: {"filename":"src/main/kotlin/com/example/demo/DemoApplication.kt","complexity":2,"loc":17,"cloc":1,"nrMethods":1,"nrClasses":1,"lineComplexities":[]}

package com.example.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
    if(true) {
        print("")
    }
}
