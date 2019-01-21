package io.taucoin.facade;

import io.taucoin.config.DefaultConfig;
import io.taucoin.config.NoAutoscan;
import io.taucoin.config.SystemProperties;
import org.ethereum.net.eth.EthVersion;
import org.ethereum.net.shh.ShhHandler;

import org.ethereum.net.swarm.bzz.BzzHandler;
import io.taucoin.util.BuildInfo;
import io.taucoin.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Roman Mandeleil
 * @since 13.11.2014
 */
@Component
public class TaucoinFactory {

    private static final Logger logger = LoggerFactory.getLogger("general");
    public static ApplicationContext context = null;

    public static Taucoin createTaucoin() {
        return createTaucoin((Class) null);
    }

    public static Taucoin createTaucoin(Class userSpringConfig) {
        return createTaucoin(SystemProperties.CONFIG, userSpringConfig);
    }

    public static Taucoin createTaucoin(SystemProperties config, Class userSpringConfig) {

        logger.info("Running {},  core version: {}-{}", config.genesisInfo(), config.projectVersion(), config.projectVersionModifier());
        BuildInfo.printInfo();

        if (config.databaseReset()){
            FileUtil.recursiveDelete(config.databaseDir());
            logger.info("Database reset done");
        }

        return userSpringConfig == null ? createTaucoin(new Class[] {DefaultConfig.class}) :
                createTaucoin(DefaultConfig.class, userSpringConfig);
    }

    public static Taucoin createTaucoin(Class ... springConfigs) {

        if (logger.isInfoEnabled()) {
            StringBuilder versions = new StringBuilder();
            for (EthVersion v : EthVersion.supported()) {
                versions.append(v.getCode()).append(", ");
            }
            versions.delete(versions.length() - 2, versions.length());
            logger.info("capability eth version: [{}]", versions);
        }
        logger.info("capability shh version: [{}]", ShhHandler.VERSION);
        logger.info("capability bzz version: [{}]", BzzHandler.VERSION);

        context = new AnnotationConfigApplicationContext(springConfigs);
        return context.getBean(Taucoin.class);
    }
}
