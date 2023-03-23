package io.hackfest;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.transaction.Transactional;

@Singleton
public class CertificateUpdater {
    private static final Logger logger = LoggerFactory.getLogger(CertificateUpdater.class);

    @Transactional
    public void onAdded(PosDeviceEntity device, Secret secret) {
        logger.info("New certificate for deviceId {} created", device.id);
        device.iotCertificate = secret.getData().get("tls.crt");
    }

    @Transactional
    public void onModified(PosDeviceEntity device, Secret secret) {
        logger.info("Existing certificate for deviceId {} modified", device.id);
        device.iotCertificate = secret.getData().get("tls.crt");
    }

    @Transactional
    public void onDeleted(PosDeviceEntity device, Secret secret) {
        logger.info("Existing certificate for deviceId {} deleted", device.id);
        device.iotCertificate = null;
    }
}
