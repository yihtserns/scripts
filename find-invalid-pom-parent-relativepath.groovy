import groovy.util.XmlSlurper
import groovy.io.FileType

def currentFolder = new File('.')

def pomFiles = []
currentFolder.eachFileRecurse(FileType.FILES) { file ->
  if (file.name == 'pom.xml') {
    pomFiles << file
  }
}

def parser = new XmlSlurper()
pomFiles
  .grep {
    try {
      def pom = parser.parse(it)
      String parentPomLocation = pom.parent.relativePath.text()
      return !parentPomLocation.empty && !parentPomLocation.endsWith('pom.xml')
    } catch (org.xml.sax.SAXParseException ex) {
      // Ignore invalid POM
      return false
    }
  }
  .each {
    println it.canonicalPath
  }
