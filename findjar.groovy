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
cli._(longOpt: 'manifest', args: 1, argName: 'Keyword', 'Search for a text in MANIFEST.MF')
cli.ci(longOpt: 'case.insensitive', 'Default: Case sensitive')

def options = cli.parse(args)
if (options.h) {
    println cli.usage()
    return
}

Closure<String> norm = { return options.ci ? it.toLowerCase(Locale.ENGLISH) : it }
Closure<String> findJarEntries = { Closure<String> matchEntryName, JarFile f ->
    return f.entries()
                .findAll { !it.isDirectory() }
                .findAll { matchEntryName(it.name) }
                .collect { it.name }
}

Closure<Boolean> matcher
if (options.class) {
    String exactFilename = options.class.replaceAll('\\.', '/') + ".class"
    matcher = findJarEntries.curry { norm(it) == norm(exactFilename) }
} else if (options.exact) {
    String exactFilename = options.exact
    matcher = findJarEntries.curry { norm(it) == norm(exactFilename) }
} else if (options.contains) {
    String partialFilename = options.contains
    matcher = findJarEntries.curry { norm(it).contains(norm(partialFilename)) }
} else if (options.manifest) {
    String keyword = options.manifest
    matcher = { JarFile f ->
        if (norm(f.manifest?.mainAttributes.toString()).contains(norm(keyword))) {
            return ['MANIFEST.MF']
        }
    }
} else {
    println cli.usage()
    return
}

def currentDir = new File('.')
def files = currentDir.listFilesRecurse(FileType.FILES, { it.name.endsWith('.jar') })

def total = files.size()

def result = []
int counter = 0
files.each {
    def jarFile = new JarFile(it)
    def foundEntries = matcher(jarFile) ?: []

    if (!foundEntries.isEmpty()) {
        result << new Result(file: it, entryNames: foundEntries)
    }
    counter++
    int progress = (counter/total) * 100
    print "Progress: ${progress}% (${counter}/${total})                       \r"
}

// Clear progress bar
print "\r                                                                 \r"

result.each {
    println "${it.file.absolutePath}"
    it.entryNames.each {
        println "    ${it}"
    }
}


class Result {
    def file
    List<String> entryNames
}
