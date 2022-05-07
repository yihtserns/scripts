@Grab('org.apache.pdfbox:pdfbox:2.0.8')
@Grab('com.opencsv:opencsv:4.1')
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.util.regex.Pattern
import com.opencsv.CSVWriter

def cli = new CliBuilder(usage: "${getClass().simpleName} [options]")
cli.h(longOpt: "help", "Show usage")
cli.i(longOpt: "input.folder", args: 1, argName: "Folder path containing PDF files", "e.g. C:\\tmp\\my-savings (defaults to current folder)")
cli.o(longOpt: "output.file", required: true, args: 1, argName: "File path", "e.g. savings.csv")

def options = cli.parse(args)
if (options == null) {
    return
}
if (options.h) {
    println cli.usage()
    return
}

File sourceFolder = options.i ? new File(options.i) : new File(".")
File outputFile = new File(options.o)

def pdfFiles = sourceFolder.listFiles().findAll { it.isFile() && getExtension(it).equalsIgnoreCase("pdf") }
if (pdfFiles.empty) {
    System.err.println("[ERROR] No PDF file found in ${sourceFolder.canonicalPath}")
    return
}

def records = []
pdfFiles.each { pdfFile ->
    def text = PDDocument.load(pdfFile.bytes).withCloseable { new PDFTextStripper().getText(it) }
    def lines = text.split("\r\n")

    def txStartPattern = Pattern.compile("^\\d\\d/\\d\\d/\\d\\d.*")
    def findInfo = { iter ->
        if (!iter.hasNext()) {
            return null
        }

        def line = iter.next()

        while (!line.startsWith("   ") && iter.hasNext()) {
            line = iter.next()
        }

        return line.startsWith("   ") ? line : null
    }

    lines.iterator().with { iter ->
        while (iter.hasNext()) {
            def line = iter.next()

            if (line.matches(txStartPattern)) {
                def record = new TxRecord()
                record.data = line
                findInfo(iter)?.with { record.info1 = it }
                findInfo(iter)?.with { record.info2 = it }
                findInfo(iter)?.with { record.info3 = it }

                records << record
            }

            print "Records found: ${records.size()}\r"
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

    void setInfo1(line) {
        this.info1 = line.trim()
    }

    void setInfo2(line) {
        this.info2 = line.trim()
    }

    void setInfo3(line) {
        this.info3 = line.trim()
    }

    void writeTo(CSVWriter writer) {
        writer.writeNext([] as String[])
    }

    @Override
    String toString() {
        return """
        ${date} (${action}): ${signedAmount} => ${balance}
          - ${info1 ?: 'N/A'}
          - ${info2 ?: 'N/A'}
          - ${info3 ?: 'N/A'}
        """.trim().stripIndent()
    }

    static void writeTo(CSVWriter writer, Collection<TxRecord> records) {
        writer.writeNext(
                ["Date", "Action", "Amount", "Balance", "Info 1", "Info 2", "Info 3"] as String[],
                false)

        records.each {
            writer.writeNext(
                    [it.date, it.action, it.signedAmount, it.balance, it.info1, it.info2, it.info3] as String[],
                    false)
        }
    }
}

def getExtension(File file) {
    def paths = file.name.split("/")
    def fileNameWithExt = paths[-1]

    return fileNameWithExt.substring(fileNameWithExt.indexOf(".") + 1)
}
