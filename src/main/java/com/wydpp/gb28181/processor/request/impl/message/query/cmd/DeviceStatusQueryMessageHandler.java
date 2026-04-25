package com.wydpp.gb28181.processor.request.impl.message.query.cmd;

import com.wydpp.gb28181.bean.SipDevice;
import com.wydpp.gb28181.bean.SipPlatform;
import com.wydpp.gb28181.commander.ISIPCommander;
import com.wydpp.gb28181.processor.request.SIPRequestProcessorParent;
import com.wydpp.gb28181.processor.request.impl.message.IMessageHandler;
import com.wydpp.gb28181.processor.request.impl.message.query.QueryMessageHandler;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.header.FromHeader;
import javax.sip.message.Response;

@Component
public class DeviceStatusQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final Logger logger = LoggerFactory.getLogger(DeviceStatusQueryMessageHandler.class);
    private final String cmdType = "DeviceStatus";

    @Autowired
    private QueryMessageHandler queryMessageHandler;

    @Autowired
    private ISIPCommander sipCommander;

    @Override
    public void afterPropertiesSet() throws Exception {
        queryMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, SipDevice sipDevice, Element rootElement) {
    }

    @Override
    public void handForPlatform(RequestEvent evt, SipPlatform sipPlatform, SipDevice sipDevice, Element rootElement) {
        logger.info("接收到DeviceStatus查询消息");
        try {
            responseAck(evt, Response.OK);
        } catch (Exception e) {
            logger.error("回复DeviceStatus 200失败", e);
        }
        FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
        Element snElement = rootElement.element("SN");
        String sn = snElement.getText();
        sipCommander.deviceStatusResponse(sipPlatform, sipDevice, sn, fromHeader.getTag());
    }
}
