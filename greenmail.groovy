@Grab("com.icegreen:greenmail:2.0.0")
@Grab("com.sun.activation:jakarta.activation:2.0.1")
@Grab("org.slf4j:slf4j-simple:1.7.36")
import com.icegreen.greenmail.mail.MailAddress
import com.icegreen.greenmail.mail.MovingMessage
import com.icegreen.greenmail.user.GreenMailUser
import com.icegreen.greenmail.user.MessageDeliveryHandler
import com.icegreen.greenmail.user.UserException
import com.icegreen.greenmail.util.ServerSetup
import jakarta.mail.MessagingException

import jakarta.mail.BodyPart
import jakarta.mail.Multipart
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import com.icegreen.greenmail.util.GreenMail

import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG")

String base64Icon = "iVBORw0KGgoAAAANSUhEUgAAABQAAAASCAYAAABb0P4QAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAKMSURBVDhPlZPBaxNREMa/9zappi0hFlM9FLFIQQRRa61U6cFeFBQvBRW8eBYPHvwDvNiDN6knwaMnpeBF8SIqBBSxtHgpxRwqErGtrW2z2WSzu8+ZN7tJ1lbQIdP3Mm/m976Z16hCoYC/WfPKWQSXThnTvQswBnrLQ+bFrMo8K8UZ221HYDg6hMaNCYN+OVORAWJXtODHOrqevFX60xd73mkpoCGAf+uiCY8MSCCKCKBktcB0zFn4BufxK6V+bkk+mQVyS83JMQTnT/D9tj0VF6ehHOMU1VLNq373WTkvPwKeD9U7MQz/+jljNCfERQQEfaSIYtACikIbB13QGoOh82zW1mRnSkqNzD4086/n8ObybRzdf4Cy/88Mgea/lnHh6TTGJ09CVwcMhq4No+ytwPd9m/AvxnlBEKBWq6GaD3HmzijcQzSCw8vTLcKgm8eD5hiKxSIcx4mj2y0MQ3u567q4V3iPxa41moRgUkA2EylcXT+Im3tGkMvloBQ9SGwRzZNVeZ6H53oBM/lF+HbobdsG5Bfkdvr8bkxVj+PYvkForS2s0WhgyVvF/b4PWM54cb4YT4rfKAXkIE3BAu1KGePVftztOm2Bj3rmUOqptNpj4508ONfFQAHoGJSAeZX47tDB3l4KZAP607YEJCs7ZdsNFfEXdmMTeM9AUVtTTSy5ISqbFLQgg5BGl3aJaSlKVAlY1Eqh3CyX1Elgec3gF40voGJx+vexMGWdFIqydvFOnj5frRlU6Ofr0w8nNJpcIaA4ewxkBe1WE0Cn0lQntG+Q2u+bETbqUdwyOylM5iSeLuoEJ/v2I1CbtN+oG6y4EanlllmhL220Ff7pyTl5Cswx2bPaFXq0esPgNxRpKN+8uMbUAAAAAElFTkSuQmCC"
def bytesIcon = base64Icon.decodeBase64()

def trayIcon = new TrayIcon(Toolkit.defaultToolkit.createImage(bytesIcon), "GreenMail").tap {
    imageAutoSize = true
}
SystemTray.systemTray.add(trayIcon)
def notify = { String title, String message ->
    trayIcon.displayMessage(title, message, TrayIcon.MessageType.NONE)
    Thread.sleep(1000)
}

def emailFolder = new File("/tmp/greenmail").tap {
    if (!it.exists()) {
        assert it.mkdirs()
    }
    // We want empty folder on start
    it.listFiles()*.delete()
}
def saveMail = { String address, MovingMessage email ->
    def baseFileName = "${email.message.sentDate.time}-${address}"

    def emailFile
    int count = 0
    do {
        emailFile = new File(emailFolder, "${baseFileName}-${count++}.md")
    } while (emailFile.exists())

    def lines = [
            "**From**: ${email.message.from}",
            "**To**: ${email.message.allRecipients}",
            "**Subject**: ${email.message.subject}",
    ]

    if (email.message.content instanceof Multipart) {
        def multipart = (Multipart) email.message.content
        multipart.count.times { i ->
            def part = multipart.getBodyPart(i)
            if (part.isMimeType("text/html")) {
                lines << "**Body**: ${part.content}"
            } else if (part.isMimeType("application/pdf")) {
                new File(emailFolder, "${emailFile.name}-${part.fileName}-${i}.pdf").bytes = part.inputStream.bytes
            } else {
                throw new UnsupportedOperationException("Unhandled content type: ${part.contentType}")
            }
        }
    } else {
        lines << "**Body**: ${email.message.content}"
    }

    emailFile.setText(lines.join("\r\n").stripIndent().trim(), StandardCharsets.UTF_8.name())
}

def greenMail = new GreenMail(ServerSetup.SMTP.createCopy("0.0.0.0"))
greenMail.setUser("nobody", "none") // TODO:
greenMail.userManager.messageDeliveryHandler = new MessageDeliveryHandler() {
    private Map<String, GreenMailUser> email2User = new ConcurrentHashMap<>()

    @Override
    synchronized GreenMailUser handle(MovingMessage msg, MailAddress mailAddress) throws MessagingException, UserException {
        def normalizedEmailAddress = mailAddress.email.trim().toLowerCase(Locale.ROOT)

        notify(mailAddress.email, msg.message.subject)
        saveMail(normalizedEmailAddress, msg)

        return email2User.computeIfAbsent(normalizedEmailAddress, { email ->
            return greenMail.userManager.createUser(email, email, email)
        })
    }
}

greenMail.start()
notify("GreenMail", "Started")

addShutdownHook { greenMail.stop() }
