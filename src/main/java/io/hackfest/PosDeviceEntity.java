package io.hackfest;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Optional;

@Entity
@Table(name = "pos_devices")
public class PosDeviceEntity extends PanacheEntityBase {
    @Id
    public Long id;

    public Long shopId;

    public String serial;

    public String iotCertificate;

    public static Optional<PosDeviceEntity> findByDeviceId(String deviceId){
        return find("serial", deviceId).singleResultOptional();
    }
}
