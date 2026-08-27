package lk.sliit.identityissuanceservice.User_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "CitizenAccount", schema = "dbo")
public class CitizenAccount {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    public CitizenAccount() {
    }

    public CitizenAccount(Integer userId) {
        this.userId = userId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}
