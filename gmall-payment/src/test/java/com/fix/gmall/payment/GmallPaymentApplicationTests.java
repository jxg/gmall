package com.fix.gmall.payment;

import com.fix.gmall.config.ActiveMQUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPaymentApplicationTests {

    @Autowired
    private ActiveMQUtil activeMQUtil;
    @Test
    public void contextLoads() {
    }

    @Test
    public void testM() throws JMSException {
        Connection connection = activeMQUtil.getConnection();
        connection.start();

        Session session = connection.createSession(true,Session.SESSION_TRANSACTED);

        Queue fix = session.createQueue("fix-test1");
        MessageProducer producer = session.createProducer(fix);
        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
        activeMQTextMessage.setText("我累了");
        producer.send(activeMQTextMessage);

        session.commit();
        producer.close();
        session.close();
        connection.close();


    }
}
