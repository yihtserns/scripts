def cli = new CliBuilder(usage: "unzip-all [options]")
cli.h(longOpt: "help", "Show usage")
cli._(longOpt: "unwrap", "Unzip content directly into current folder instead of inside a subfolder with same name as the zip file.")

def options = cli.parse(args)
if (options.h) {
    println cli.usage()
    return
}


def currentFolder = new File(".")
def zipFiles = currentFolder.listFiles().findAll { it.name.endsWith(".zip") }

def ant = new AntBuilder()
zipFiles.each { zipFile ->
    File destination = options.unwrap
        ? currentFolder
        : new File(currentFolder, zipFile.name.substring(0, zipFile.name.lastIndexOf(".zip")))

    ant.unzip(
        src: zipFile.absolutePath, 
        dest: destination.absolutePath)
}
