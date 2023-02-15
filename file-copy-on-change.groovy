import org.apache.commons.io.monitor.FileAlterationListenerAdaptor
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver

@Grab("commons-io:commons-io:2.11.0")
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit


if (args.length != 2) {
    println "Incorrect usage, example:"
    println "groovy file-copy-on-change root/path/to/be/monitored root/path/to/copy/to"
    return
}

def monitoredPath = new File(args[0])
def targetPath = new File(args[1])

if (!monitoredPath.exists()) {
    println "${monitoredPath.absolutePath} does not exist!"
    return
}
if (!targetPath.exists()) {
    println "${targetPath.absolutePath} does not exist!"
    return
}

println "Starts watching: ${monitoredPath.absolutePath}"

def timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
char BELL = 7
def observer = new FileAlterationObserver(monitoredPath)
observer.addListener(new FileAlterationListenerAdaptor() {

    @Override
    void onFileChange(File file) {
        def copyPath = new File(targetPath, file.absolutePath - monitoredPath.absolutePath)

        copyPath.bytes = file.bytes

        println "[${LocalTime.now().format(timeFormatter)}] ${file.absolutePath} --> ${copyPath.absolutePath}"
        println BELL
    }
})

def monitor = new FileAlterationMonitor(TimeUnit.SECONDS.toMillis(5))
monitor.addObserver(observer)
monitor.start()