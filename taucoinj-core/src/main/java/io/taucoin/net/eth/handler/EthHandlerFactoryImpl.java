package io.taucoin.net.eth.handler;

import io.taucoin.net.eth.EthVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Default factory implementation
 *
 * @author Mikhail Kalinin
 * @since 20.08.2015
 */
@Component
public class EthHandlerFactoryImpl implements EthHandlerFactory {

    @Autowired
    private ApplicationContext ctx;

    @Override
    public EthHandler create(EthVersion version) {
        switch (version) {
            case V60:   return ctx.getBean(Eth60.class);
            case V61:   return ctx.getBean(Eth61.class);
            case V62:   return ctx.getBean(Eth62.class);
            default:    throw new IllegalArgumentException("Eth " + version + " is not supported");
        }
    }
}