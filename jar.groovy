@Grab('org.codehaus.groovy:groovy-eclipse-batch:2.0.0-01')
@Grab('org.slf4j:slf4j-simple:1.5.8')
import java.util.jar.*
import org.eclipse.osgi.util.*
import org.slf4j.*
import groovy.xml.*
import static org.eclipse.osgi.util.ManifestElement.*

def log = LoggerFactory.getLogger('')

def out = { int indentMultiplier, list ->
    final String indent = '    '
    list.each {
        println "${indent*indentMultiplier}${it}"
    }
}

def cli = new CliBuilder(
    usage:'jar [options] <file>',
    header:'Options:',
)
cli.with {
    mf(longOpt:'manifest', "List jar's manifest entries")
    d(longOpt:'debug', "Show stacktrace")
    h(longOpt:'help', "Show usage")
    _(longOpt:'install', args:1, argName:'artifactInfo', "Installs jar into maven repo using the given artifact info (gradle syntax)")
}
def options = cli.parse(args)

if (options.help) {
    println cli.usage()
    return
}

if (options.arguments().empty) {
    throw new IllegalArgumentException("Please specify the jar file")
}
def jar = new JarFile(options.arguments()[0])

def showManifest = { ->
    jar.manifest.mainAttributes.each { header, value ->
        println header.toString()
        def attributes = parseHeader(header.toString(), value)
        attributes.each { attribute ->
            out 1, [attribute.value]
            attribute.keys.each { key ->
                out 2, [key]
                out 3, attribute.getAttributes(key)
            }
            attribute.directiveKeys.each { key ->
                out 2, [key]
                attribute.getDirectives(key).each {
                    out 3, it.split(',')
                }
            }
        }
        println ''
    }
}

def installIntoMaven = { String artifactInfo ->
    String[] tokens = artifactInfo.split(':')
    if (tokens.length < 3) {
        throw new IllegalArgumentException("Need at least the Group ID, Artifact ID and Artifact Version (e.g. groupId:artifactId:version")
    }
    String mavenInstallCommand = "cmd /c mvn install:install-file -Dfile=\"${jar.name}\""
    switch (tokens.length) {
        case 4:
            mavenInstallCommand += " -Dclassifier=${tokens[3]}"
        case 3:
            mavenInstallCommand += " -DgroupId=${tokens[0]}"
            mavenInstallCommand += " -DartifactId=${tokens[1]}"
            mavenInstallCommand += " -Dversion=${tokens[2]}"
            mavenInstallCommand += " -Dpackaging=jar"
            break
    }
    
    log.info("Installing jar using command '{}'", mavenInstallCommand)
    println mavenInstallCommand.execute().text
}

try {
    if (options.manifest) {
        showManifest()
    }
    if (options.install) {
        installIntoMaven(options.install)
    }

} catch (ex) {
    if (options.debug) {
        log.error("An error has occured", ex)
    } else {
        log.error("An error has occurred, turn on the debug switch (-d or --debug) to view the stack trace")
    }
    println cli.usage()
}
