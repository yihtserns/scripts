def cli = new CliBuilder(usage: 'checksum [options] <file>')
cli.h(longOpt: 'help', 'Show usage')
cli.a(longOpt: 'algorithm', args: 1, argName: 'name', 'e.g. md5, sha-1, sha-256, sha-512 (default: md5, unless `verify` is provided then it tries to auto-detect)')
cli.v(longOpt: 'verify', args: 1, argName: 'checksum', 'Compare results with this expected checksum')

def options = cli.parse(args)
if (options.h || !options.arguments()) {
    println cli.usage()
    return
}

def file = new File(options.arguments()[0])
def algorithm = options.a
def expectedChecksum = options.v

if (!algorithm) {
    if (expectedChecksum) {
        switch (expectedChecksum.length()) {
            case 32:
                algorithm = "md5"
                break
            case 40:
                algorithm = "sha-1"
                break
            case 64:
                algorithm = "sha-256"
                break
            case 128:
                algorithm = "sha-512"
                break
            default:
                println "Failed to auto-detect algorithm based on the '--verify' length, please specify '--algorithm'"
                println cli.usage()
                return
        }
    } else {
        algorithm = "md5"
    }
}
println "[${algorithm}]"

def ant = new AntBuilder()
ant.checksum(file: file, property: 'checksum', algorithm: algorithm)
def generatedChecksum = ant.project.properties.checksum
if (expectedChecksum) {
    assert generatedChecksum.toUpperCase() == expectedChecksum.toUpperCase()
	
    println 'Correct!'
} else {
    println generatedChecksum
}
