package com.fix.gmall.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;

public class ActiveMQUtil {

    PooledConnectionFactory pooledConnectionFactory = null;
    public  void init(String brokerUrl) {
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        pooledConnectionFactory = new PooledConnectionFactory(activeMQConnectionFactory);
        pooledConnectionFactory.setExpiryTimeout(2000);
        pooledConnectionFactory.setReconnectOnException(true);

        pooledConnectionFactory.setMaxConnections(5);
    }
    public Connection getConnection(){
        Connection connection  = null;

        try {
            connection = pooledConnectionFactory.createConnection();
        } catch (JMSException e) {
            e.printStackTrace();
        }
        return connection;
    }


    }
