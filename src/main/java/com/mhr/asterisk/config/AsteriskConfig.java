package com.mhr.asterisk.config;


import java.io.IOException;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.asteriskjava.fastagi.AgiServerThread;
import org.asteriskjava.fastagi.DefaultAgiServer;
import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.ManagerConnectionState;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.response.ManagerResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Configuration
public class AsteriskConfig {

	private ManagerConnection managerConnection;
	private DefaultAsteriskServer asteriskServer;

	@Value("${asterisk.config.host}")
	private String host;

	@Value("${asterisk.config.username}")
	private String username;

	@Value("${asterisk.config.password}")
	private String password;

	@Value("${asterisk.config.port}")
	private int port;

	@Value("${asterisk.config.timeout}")
	private long asteriskTimeout;

    private DefaultAgiServer agiServer;

    private static AgiServerThread agiServerThread;
    
    @PostConstruct
	public void start() {
		try {
			log.info("start connect asterisk ...");
			ManagerConnectionFactory factory = new ManagerConnectionFactory(host,port,username,password);
			managerConnection = factory.createManagerConnection();
			asteriskServer = new DefaultAsteriskServer(managerConnection);
			asteriskServer.initialize();
	        log.info("Created connection to the Asterisk server, connection status: " + managerConnection.getState());
	        
	        log.info("Start the Agi server for Asterisk");
	        agiServer = new DefaultAgiServer();
	        agiServerThread = new AgiServerThread(agiServer);
	        agiServerThread.startup();
	        
	        callOrginator();
	        
		}catch (Exception e) {
			log.error(e.getMessage());
		}
	}
    
    public ManagerResponse callOrginator() throws IOException,  AuthenticationFailedException, TimeoutException {
    	
    	OriginateAction originateAction = new OriginateAction();
    	
    	originateAction.setChannel("SIP/boby");
    	originateAction.setContext("LocalSets");
    	originateAction.setExten("6000");
//    	originateAction.setCallerId("6001");
    	originateAction.setPriority(1);
    	originateAction.setActionId(UUID.randomUUID().toString());
    	
    	
    	ManagerResponse originateResponse = managerConnection.sendAction(originateAction,30000);
    	log.info(originateResponse.getResponse());
    	return originateResponse;
    }
    
    
    @PreDestroy
    public void stop() throws Exception{
    	if(managerConnection.getState().equals(ManagerConnectionState.CONNECTED) || managerConnection.getState().equals(ManagerConnectionState.RECONNECTING)) {
            managerConnection.logoff();
    	}
        asteriskServer.shutdown();
    }
	
}
