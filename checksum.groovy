def cli = new CliBuilder(usage: 'checksum [options] <file>')
cli.h(longOpt: 'help', 'Show usage')
cli.a(longOpt: 'algorithm', args: 1, argName: 'name', 'e.g. sha, sha-1, sha-512 (default: md5)')
cli.v(longOpt: 'verify', args: 1, argName: 'checksum', 'Compare results with this expected checksum')

def options = cli.parse(args)
if (options.h || !options.arguments()) {
	println cli.usage()
	return
}

def file = new File(options.arguments()[0])
def algorithm = options.a?: 'md5'
def expectedChecksum = options.v

def ant = new AntBuilder()
ant.checksum(file: file, property: 'checksum', algorithm: algorithm)
def generatedChecksum = ant.project.properties.checksum
if (expectedChecksum) {
	assert generatedChecksum == expectedChecksum
	
	println 'Correct!'
} else {
	println generatedChecksum
}
