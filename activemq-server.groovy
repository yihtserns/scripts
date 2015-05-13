@Grab('org.slf4j:slf4j-simple:1.7.7')
@Grab('org.apache.activemq:activemq-core:5.7.0')
import org.apache.activemq.broker.BrokerService
import org.apache.activemq.command.ActiveMQQueue

def broker = new BrokerService()
broker.with {
    addConnector('tcp://localhost:61616')
    destinations = [new ActiveMQQueue('Q1')]
    start()
}
