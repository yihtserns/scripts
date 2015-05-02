import groovy.io.FileType
import java.util.jar.JarFile
import java.util.Locale

File.metaClass.listFilesRecurse = { FileType type, Closure<Boolean> matchesFile ->
    def files = []
    delegate.eachFileRecurse(type) {
        if (matchesFile(it)) {
            files << it
        }
    }
    
    return files
}

def cli = new CliBuilder(usage: "${getClass().simpleName} [options]")
cli.h(longOpt: 'help', 'Show usage')
cli._(longOpt: 'class', args: 1, argName: 'Fully qualified class name', 'e.g. groovy.grape.GrapeIvy')
cli._(longOpt: 'exact', args: 1, argName: 'File path', 'e.g. groovy/grape/GrapeIvy.class, META-INF/MANIFEST.MF')
cli._(longOpt: 'contains', args: 1, argName: 'File path', 'e.g. GrapeIvy, MANIFEST.MF')
cli.ci(longOpt: 'case.insensitive', 'Default: Case sensitive')

def options = cli.parse(args)
if (options.h) {
    println cli.usage()
    return
}

Closure<Boolean> matcher
if (options.class) {
    String exactFilename = options.class.replaceAll('\\.', '/') + ".class"
    matcher = { it == exactFilename }
} else if (options.exact) {
    String exactFilename = options.exact
    matcher = { it == exactFilename }
} else if (options.contains) {
    String partialFilename = options.contains
    matcher = { it.contains(partialFilename) }
} else {
    println cli.usage()
    return
}
if (options.ci) {
    def filenameMatcher = matcher
    matcher = { filenameMatcher(it.toLowerCase(Locale.ENGLISH)) }
}

def currentDir = new File('.')
def jarFiles = currentDir
                .listFilesRecurse(FileType.FILES, { it.name.endsWith('.jar') })
                .collect { new JarFile(it) }
                    
def total = jarFiles.size()

def result = []
int counter = 0
jarFiles.each {
    def foundEntries = it.entries()
                            .findAll { !it.isDirectory() }
                            .findAll { matcher(it.name) }
                            .collect { it.name }

    if (!foundEntries.isEmpty()) {
        result << new Result(file: it, entryNames: foundEntries)
    }
    counter++
    print "\r                                                                 \r"
    int progress = (counter/total) * 100
    print "Progress: ${progress}% (${counter}/${total})"
}

print "\r                                                                 \r"
result.each {
    println "${it.file.name}"
    it.entryNames.each {
        println "    ${it}"
    }
}



class Result {
    JarFile file
    List<String> entryNames
}