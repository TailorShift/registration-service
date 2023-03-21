package io.hackfest;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

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
                        switch (action) {
                            case ADDED -> logger.info("New certificate created");
                            case MODIFIED -> logger.info("Existing certificate modified");
                            case DELETED -> logger.info("Existing certificate deleted");
                        }
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
