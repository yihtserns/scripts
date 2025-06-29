/**
 * <h2>Usage examples</h2>
 * <code>
 *  C:\tmp> groovy path/to/maybank-pdf-to-csv.groovy --input.folder path/to/pdf/folder --output.file path/to/result.csv
 *
 *  C:\path\to\pdf\folder> groovy path/to/maybank-pdf-to-csv.groovy --output.file path/to/result.csv
 * </code>
 */
@Grab('org.apache.pdfbox:pdfbox:2.0.8')
@Grab('com.opencsv:opencsv:4.1')
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.util.regex.Pattern
import com.opencsv.CSVWriter

def cli = new CliBuilder(usage: "${getClass().simpleName} [options]")
cli.h(longOpt: "help", "Show usage")
cli.i(longOpt: "input.folder", required:true, args: 1, argName: "Folder path containing PDF files", "e.g. C:\\tmp\\my-savings")
cli.o(longOpt: "output.file", required: true, args: 1, argName: "File path", "e.g. savings.csv")

def options = cli.parse(args)
if (options == null) {
    return
}
if (options.h) {
    println cli.usage()
    return
}

File sourceFolder = new File(options.i)
File outputFile = new File(options.o)

def pdfFiles = sourceFolder.listFiles().findAll { it.isFile() && getExtension(it).equalsIgnoreCase("pdf") }
if (pdfFiles.empty) {
    System.err.println("[ERROR] No PDF file found in ${sourceFolder.canonicalPath}")
    return
}

def txStartPattern = Pattern.compile("^\\d\\d/\\d\\d/\\d\\d.*")
def records = []
pdfFiles.each { pdfFile ->
    def text = PDDocument.load(pdfFile.bytes).withCloseable { new PDFTextStripper().getText(it) }
    def lines = text.split("\r\n") as List

    def currentRecord = null
    for (line in lines) {
        print "Records found: ${records.size()}\r"

        if (line.matches(txStartPattern)) {
            currentRecord = new TxRecord()
            currentRecord.data = line

            records << currentRecord

            continue
        }

        if (line.startsWith("   ") && currentRecord != null) {
            currentRecord.addInfo(line)

            continue
        }
    }
}

def csvFile = outputFile
new CSVWriter(new FileWriter(csvFile)).withCloseable { writer ->
    TxRecord.writeTo(writer, records)
}

println "Successfully converted to CSV at ${csvFile.canonicalPath}"

class TxRecord {

    String date
    String action
    String signedAmount
    String balance
    List<String> infos = []
    String info1
    String info2
    String info3

    void setData(line) {
        try {
            def entries = line.trim().split(" ")

            this.date = entries[0]
            this.balance = entries[-1]

            entries[-2].with {
                def sign = it.substring(it.length() - 1)
                def amount = it.substring(0, it.length() - 1)

                this.signedAmount = sign + amount
            }

            this.action = entries[1..-3].join(" ")

        } catch (ex) {
            throw new IllegalArgumentException("Failed to parse ${line}", ex)
        }
    }

    void addInfo(String line) {
        infos << line.trim()
    }

    String getInfo(int index) {
        return infos.size() <= index ? null : infos.get(index)
    }

    void writeTo(CSVWriter writer) {
        writer.writeNext([] as String[])
    }

    @Override
    String toString() {
        def sb = new StringBuilder("${date} (${action}): ${signedAmount} => ${balance}\r\n")
        infos.each {
            sb.append(" - ${it}\r\n")
        }
        return sb.toString()
    }

    static void writeTo(CSVWriter writer, Collection<TxRecord> records) {
        writer.writeNext(
                ["Date", "Action", "Amount", "Balance", "Info 1", "Info 2", "Info 3"] as String[],
                false)

        records.each {
            writer.writeNext(
                    [it.date, it.action, it.signedAmount, it.balance, it.getInfo(0), it.getInfo(1), it.getInfo(2)] as String[],
                    false)
        }
    }
}

def getExtension(File file) {
    def paths = file.name.split("/")
    def fileNameWithExt = paths[-1]

    return fileNameWithExt.substring(fileNameWithExt.indexOf(".") + 1)
}
