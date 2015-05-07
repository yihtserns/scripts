@Grab('org.ccil.cowan.tagsoup:tagsoup:1.2.1')
import org.ccil.cowan.tagsoup.Parser
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import groovy.util.XmlSlurper

String json = 'https://search.maven.org/solrsearch/select?q=g:%22ch.qos.logback%22+AND+a:%22logback-parent%22+AND+p:%22pom%22&rows=1000&core=gav'.toURL().text

def result = new JsonSlurper().parseText(json)
def logbackVersions = result.response.docs.collect { it.v }.reverse()

def slf4jVersion2LogbackVersions = [:]
slf4jVersion2LogbackVersions.getMetaClass().getOrDefault = { String key ->
    def values = delegate.get(key)
    if (values == null) {
        values = new TreeSet()
        delegate.put(key, values)
    }
    return values
}

logbackVersions.each { logbackVersion ->
    print "Processing POM for Logback version: ${logbackVersion}\r"
    
    String xml = "https://repo1.maven.org/maven2/ch/qos/logback/logback-parent/${logbackVersion}/logback-parent-${logbackVersion}.pom".toURL().text
    def pom = new XmlSlurper(new Parser()).parseText(xml)
    def slf4jVersion = pom.properties.'slf4j.version'.text()
    
    slf4jVersion2LogbackVersions.getOrDefault(slf4jVersion) << logbackVersion
}
// Clear progress report
print "                                                                          \r"

def file = new File(getClass().simpleName + ".json")
file.text = new JsonBuilder(slf4jVersion2LogbackVersions).toPrettyString()
println "Report generated: " + file.absolutePath