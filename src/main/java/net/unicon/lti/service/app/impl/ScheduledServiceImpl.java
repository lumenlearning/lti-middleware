package net.unicon.lti.service.app.impl;

import net.unicon.lti.service.app.APIDataService;
import net.unicon.lti.service.app.ScheduledService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ScheduledServiceImpl implements ScheduledService {

    static final Logger log = LoggerFactory.getLogger(ScheduledServiceImpl.class);
    private static final DateTimeFormatter dateTimeFormatter= DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    APIDataService apiDataService;

    @Override
    @Scheduled(cron = "${scheduled.deleteoldtokens.cron:0 0 1 * * ?}")
    public void deleteOldTokens(){
        log.info("Deleting Old Tokens :: Starting - {} ", dateTimeFormatter.format(LocalDateTime.now()));
        apiDataService.cleanOldTokens();
        log.info("Deleting Old Tokens :: Ended - {} ", dateTimeFormatter.format(LocalDateTime.now()));
    }
}