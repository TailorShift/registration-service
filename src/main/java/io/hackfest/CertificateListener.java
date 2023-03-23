package io.hackfest;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import static io.hackfest.Constants.DEVICE_ID_LABEL_KEY;
import static io.hackfest.Constants.K8S_NAMESPACE;

@ApplicationScoped
public class CertificateListener {
    private static final Logger logger = LoggerFactory.getLogger(CertificateListener.class);

    private Watch watch;

    void onStart(@Observes StartupEvent event, KubernetesClient kubernetesClient) {
        watch = kubernetesClient.secrets()
                .inNamespace(K8S_NAMESPACE)
                .withLabel(Constants.POS_EDGE_SECRET_LABEL_KEY, Constants.POS_EDGE_SECRET_LABEL_VALUE)
                .watch(new Watcher<>() {
                    @Override
                    public void eventReceived(Action action, Secret resource) {
                        String deviceId = resource.getMetadata().getLabels().getOrDefault(DEVICE_ID_LABEL_KEY, "undefined");

                        PosDeviceEntity.findByDeviceId(deviceId)
                                .ifPresentOrElse(
                                        device -> {
                                            switch (action) {
                                                case ADDED -> onAdded(deviceId, resource);
                                                case MODIFIED -> onModified(deviceId, resource);
                                                case DELETED -> onDeleted(deviceId, resource);
                                                default -> logger.trace("Irrelevant action {}", action);
                                            }
                                        },
                                        () -> logger.warn("DeviceId {} not present in database", deviceId)
                                );
c                    }

                    @Override
                    public void onClose() {
                        logger.info("Watch gracefully closed");
                    }

                    @Override
                    public void onClose(WatcherException e) {
                        logger.error("Watch error received: {}", e.getMessage(), e);
                    }
                });

    }

    private void onAdded(PosDeviceEntity device, Secret secret) {
        logger.info("New certificate for deviceId {} created", device.id);
        device.iotCertificate = secret.getData().get("tls.crt");
    }

    private void onModified(PosDeviceEntity device, Secret secret) {
        logger.info("Existing certificate for deviceId {} modified", device.id);
        device.iotCertificate = secret.getData().get("tls.crt");
    }

    private void onDeleted(PosDeviceEntity device, Secret secret) {
        logger.info("Existing certificate for deviceId {} deleted", device.id);
        device.iotCertificate = null;
    }
}
