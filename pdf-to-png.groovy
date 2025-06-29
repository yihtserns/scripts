@Grab("org.apache.pdfbox:pdfbox-tools:3.0.0")
import org.apache.pdfbox.tools.PDFToImage

def pdfFiles = new File(".").listFiles().findAll { it.isFile() && getExtension(it).equalsIgnoreCase("pdf") }
pdfFiles.each { file ->
    new PDFToImage(imageFormat: "png", infile: file).call()
    println "Converted ${file.absolutePath} to .png"
}

static def getExtension(File file) {
    def paths = file.name.split("/")
    def fileNameWithExt = paths[-1]

    return fileNameWithExt.substring(fileNameWithExt.indexOf(".") + 1)
}