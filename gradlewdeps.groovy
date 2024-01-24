if (args.length == 0) {
    throw new IllegalArgumentException("groovy gradlewdeps <comma delimited module names>")
}
def moduleNames = args[0].split(",")

moduleNames.each { moduleName ->
    new File("${moduleName}/build").with {
        if (!it.exists()) {
            if (!it.mkdir()) {
                throw new IllegalArgumentException("Unable to create ${it.absolutePath}")
            }
        }
    }

    String command = "gradlew.bat dependencies -p ${moduleName} --configuration runtimeClasspath > ${moduleName}/build/dependencies.txt"
    
    println "Running: ${command}"
    def process = command.execute()

    addShutdownHook {
        process.destroy()
        process.waitFor()
    }
    process.waitForProcessOutput(System.out, System.err)
    println(new File("${moduleName}/build/dependencies.txt").absolutePath)
    
}
