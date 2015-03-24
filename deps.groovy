@Grab('org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-impl-maven:2.1.1')
import org.jboss.shrinkwrap.resolver.api.maven.Maven

def cli = new CliBuilder(usage: "${getClass().name} [options]")
cli.h(longOpt: 'help', 'Show usage')
cli.a(longOpt: 'artifact', args: 1, argName: 'name', required: true, 'e.g. commons-io:commons-io:2.4')
cli.f(longOpt: 'folder', args: 1, argName: 'name', 'Copy jars into folder')
cli.z(longOpt: 'zip', args: 1, argName: 'filename', 'Zip jars up')

def options = cli.parse(args)
if (!options) {
    return
}
if (options.h) {
    println cli.usage()
    return
}

def artifact = options.a
def folder = options.f
def zipFile = options.z

// default action is print, shouldn't if other action invoked?
boolean print = !folder && !zipFile

def files = Maven.resolver().resolve(artifact).withTransitivity().asFile()
def ant = new AntBuilder()

if (folder) {
    files.each { file ->
        ant.copy(file: file, todir: folder)
    }
}
if (zipFile) {
    ant.zip(destfile: zipFile) {
        files.each { file ->
            fileset(dir: file.parentFile, includes: file.name)
        }
    }
}

if (print) {
    files.each { println it.absolutePath }
}
