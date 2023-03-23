package io.hackfest;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Singleton;

@Singleton
public class CertificateListener {
    private static final Logger logger = LoggerFactory.getLogger(CertificateListener.class);

    private Watch watch;

    @ConfigProperty(name = "k8s.namespace")
    String k8sNamespace;

    void onStart(@Observes StartupEvent event, KubernetesClient kubernetesClient, CertificateUpdater certificateUpdater) {
        watch = kubernetesClient.secrets()
                .inNamespace(k8sNamespace)
                .withLabel(Constants.POS_EDGE_SECRET_LABEL_KEY, Constants.POS_EDGE_SECRET_LABEL_VALUE)
                .watch(new Watcher<>() {
                    @Override
                    public void eventReceived(Action action, Secret resource) {
                        certificateUpdater.handleUpdate(action, resource);
                    }

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
}
