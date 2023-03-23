package io.hackfest;

import io.fabric8.certmanager.api.model.v1.Certificate;
import io.fabric8.certmanager.api.model.v1.CertificateBuilder;
import io.fabric8.certmanager.api.model.v1.CertificateKeystoresBuilder;
import io.fabric8.certmanager.api.model.v1.CertificateList;
import io.fabric8.certmanager.api.model.v1.CertificateSecretTemplateBuilder;
import io.fabric8.certmanager.api.model.v1.CertificateSpecBuilder;
import io.fabric8.certmanager.api.model.v1.JKSKeystoreBuilder;
import io.fabric8.certmanager.client.DefaultCertManagerClient;
import io.fabric8.certmanager.client.NamespacedCertManagerClient;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Map;

import static io.hackfest.Constants.DEVICE_ID_LABEL_KEY;
import static io.hackfest.Constants.DNS_ROOT;
import static io.hackfest.Constants.K8S_CA_ISSUER;
import static io.hackfest.Constants.K8S_KEYSTORE_PASSWORD_SECRET_KEY;
import static io.hackfest.Constants.K8S_KEYSTORE_PASSWORD_SECRET_NAME;
import static io.hackfest.Constants.K8S_NAMESPACE;
import static io.hackfest.Constants.POS_EDGE_NAME_PREFIX;
import static io.hackfest.Constants.POS_EDGE_SECRET_LABEL_KEY;
import static io.hackfest.Constants.POS_EDGE_SECRET_LABEL_VALUE;

@Path("/registration")
public class RegistrationController {
    @Inject
    private KubernetesClient kubernetesClient;

    @GET
    @Path("/devices")
    public List<Secret> listDevices() {
        try (NamespacedCertManagerClient certManagerClient = new DefaultCertManagerClient()) {
            return certManagerClient.v1().certificates()
                    .inNamespace(K8S_NAMESPACE)
                    .withLabels(Map.of(POS_EDGE_SECRET_LABEL_KEY, POS_EDGE_SECRET_LABEL_VALUE))
                    .list()
                    .getItems()
                    .stream()
                    .map(cert -> cert.getMetadata().getName())
                    .map(certName -> kubernetesClient.secrets().inNamespace(K8S_NAMESPACE).withName(certName).get())
                    .toList();
        }
    }

    @POST
    @Path("/devices")
    public Secret registerDevice(
            @FormParam("deviceId") String deviceId
    ) throws InterruptedException {
        PosDeviceEntity device = PosDeviceEntity.findByDeviceId(deviceId)
                .orElseThrow(() -> new WebApplicationException("DeviceId unknown", 404));


        String deviceName = POS_EDGE_NAME_PREFIX + deviceId;
        String dnsName = "device-" + deviceId + "." + DNS_ROOT;

        StringBuilder resultBuilder = new StringBuilder();

        try (NamespacedCertManagerClient certManagerClient = new DefaultCertManagerClient()) {
            Certificate certificate = new CertificateBuilder()
                    .withMetadata(
                            new ObjectMetaBuilder()
                                    .withName(deviceName)
                                    .withNamespace(K8S_NAMESPACE)
                                    .withLabels(Map.of(
                                            POS_EDGE_SECRET_LABEL_KEY, POS_EDGE_SECRET_LABEL_VALUE,
                                            DEVICE_ID_LABEL_KEY, deviceId
                                    ))
                                    .build()
                    )
                    .withSpec(
                            new CertificateSpecBuilder()
                                    .withSecretName(deviceName)
//                                    .withCommonName("example.com")
                                    .withIsCA(false)
                                    .withSecretTemplate(
                                            new CertificateSecretTemplateBuilder()
                                                    .withLabels(Map.of(POS_EDGE_SECRET_LABEL_KEY, POS_EDGE_SECRET_LABEL_VALUE))
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

            while (!kubernetesClient.secrets().inNamespace(K8S_NAMESPACE)
                    .withName(deviceName)
                    .isReady()) {
                Thread.sleep(100);
            }

            Secret secret = kubernetesClient.secrets().inNamespace(K8S_NAMESPACE)
                    .withName(deviceName)
                    .get();

            return secret;
        }
    }
}
