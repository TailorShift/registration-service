package io.hackfest;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.transaction.Transactional;

import java.util.Base64;

import static io.hackfest.Constants.DEVICE_ID_LABEL_KEY;

@Singleton
public class CertificateUpdater {
    private static final Logger logger = LoggerFactory.getLogger(CertificateUpdater.class);

    @Transactional
    public void handleUpdate(Watcher.Action action, Secret resource) {
        String deviceId = resource.getMetadata().getLabels().getOrDefault(DEVICE_ID_LABEL_KEY, "undefined");

        PosDeviceEntity.findByDeviceId(deviceId)
                .ifPresentOrElse(
                        device -> {
                            switch (action) {
                                case ADDED -> onAdded(device, resource);
                                case MODIFIED -> onModified(device, resource);
                                case DELETED -> onDeleted(device, resource);
                                default -> logger.trace("Irrelevant action {}", action);
                            }
                        },
                        () -> logger.warn("DeviceId {} not present in database", deviceId)
                );
    }

    private void onAdded(PosDeviceEntity device, Secret secret) {
        logger.info("New certificate for deviceId {} created", device.id);
        String base64Cert = secret.getData().get("tls.crt");
        device.iotCertificate = new String(Base64.getDecoder().decode(base64Cert));
    }

    private void onModified(PosDeviceEntity device, Secret secret) {
        logger.info("Existing certificate for deviceId {} modified", device.id);
        String base64Cert = secret.getData().get("tls.crt");
        device.iotCertificate = new String(Base64.getDecoder().decode(base64Cert));
    }

    private void onDeleted(PosDeviceEntity device, Secret secret) {
        logger.info("Existing certificate for deviceId {} deleted", device.id);
        device.iotCertificate = null;
    }
}
