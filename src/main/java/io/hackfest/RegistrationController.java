package io.hackfest;

import io.fabric8.certmanager.api.model.v1.*;
import io.fabric8.certmanager.client.DefaultCertManagerClient;
import io.fabric8.certmanager.client.NamespacedCertManagerClient;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.hackfest.error.ApiException;
import io.hackfest.error.ErrorCode;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.List;
import java.util.Map;

import static io.hackfest.Constants.*;

@Path("/registration")
public class RegistrationController {
    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);
    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(name = "k8s.namespace")
    String k8sNamespace;

    @GET
    @Path("/devices")
    public List<Secret> listDevices() {
        try (NamespacedCertManagerClient certManagerClient = new DefaultCertManagerClient()) {
            return certManagerClient.v1().certificates()
                    .inNamespace(k8sNamespace)
                    .withLabels(Map.of(POS_EDGE_SECRET_LABEL_KEY, POS_EDGE_SECRET_LABEL_VALUE))
                    .list()
                    .getItems()
                    .stream()
                    .map(cert -> cert.getMetadata().getName())
                    .map(certName -> kubernetesClient.secrets().inNamespace(k8sNamespace).withName(certName).get())
                    .toList();
        }
    }

    @POST
    @Path("/devices")
    public Secret registerDevice(
            @FormParam("deviceId") String deviceId
    ) throws InterruptedException {
        PosDeviceEntity device = PosDeviceEntity.findByDeviceId(deviceId)
                .orElseThrow(() -> ApiException.badRequest(ErrorCode.DEVICE_ID_UNKNOWN, deviceId));

        String deviceName = POS_EDGE_NAME_PREFIX + deviceId;
        String dnsName = "device-" + deviceId + "." + DNS_ROOT;

        try (NamespacedCertManagerClient certManagerClient = new DefaultCertManagerClient()) {
            Certificate certificate = new CertificateBuilder()
                    .withMetadata(
                            new ObjectMetaBuilder()
                                    .withName(deviceName)
                                    .withNamespace(k8sNamespace)
                                    .withLabels(Map.of(
                                            POS_EDGE_SECRET_LABEL_KEY, POS_EDGE_SECRET_LABEL_VALUE,
                                            DEVICE_ID_LABEL_KEY, deviceId
                                    ))
                                    .build()
                    )
                    .withSpec(
                            new CertificateSpecBuilder()
                                    .withSecretName(deviceName)
                                    .withIsCA(false)
                                    .withSecretTemplate(
                                            new CertificateSecretTemplateBuilder()
                                                    .withLabels(Map.of(
                                                            POS_EDGE_SECRET_LABEL_KEY, POS_EDGE_SECRET_LABEL_VALUE,
                                                            DEVICE_ID_LABEL_KEY, deviceId
                                                    ))
                                                    .build()
                                    )
                                    .withKeystores(
                                            new CertificateKeystoresBuilder()
                                                    .withJks(
                                                            new JKSKeystoreBuilder()
                                                                    .withCreate(true)
                                                                    .withNewPasswordSecretRef(K8S_KEYSTORE_PASSWORD_SECRET_KEY, K8S_KEYSTORE_PASSWORD_SECRET_NAME)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .withNewPrivateKey("RSA", "PKCS1", null, 2048)
                                    .withUsages("server auth", "client auth")
                                    .withDnsNames(dnsName)
                                    .withNewIssuerRef(null, "Issuer", K8S_CA_ISSUER)
                                    .build()
                    )
                    .build();

            logger.info("Creating certificate for device {}", deviceId);
            certManagerClient.v1().certificates()
                    .inNamespace(k8sNamespace)
                    .resource(certificate)
                    .createOrReplace();

            logger.info("Waiting for certificate secrets of device {}", deviceId);
            while (!kubernetesClient.secrets().inNamespace(k8sNamespace)
                    .withName(deviceName)
                    .isReady()) {
                Thread.sleep(100);
            }

            Secret secret = kubernetesClient.secrets().inNamespace(k8sNamespace)
                    .withName(deviceName)
                    .get();

            return secret;
        }
    }
}
