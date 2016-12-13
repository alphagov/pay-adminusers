package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.User;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;

@Entity
@Table(name = "users")
@SequenceGenerator(name = "users_id_seq", sequenceName = "users_id_seq", allocationSize = 1)
public class UserEntity extends AbstractEntity {

    @Column(name = "username") //also our externalId
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "email")
    private String email;

    @Column(name = "gateway_account_id")
    private String gatewayAccountId;

    @Column(name = "otp_key")
    private String otpKey;

    @Column(name = "telephone_number")
    private String telephoneNumber;

    @Column(name = "disabled")
    private Boolean disabled = FALSE;

    @Column(name = "login_counter")
    private Integer loginCount = 0;

    @ManyToMany(fetch = FetchType.EAGER, targetEntity = RoleEntity.class)
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
    private List<RoleEntity> roles = new ArrayList<>();

    public UserEntity() {
        //for jpa
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    public void setGatewayAccountId(String gatewayAccountId) {
        this.gatewayAccountId = gatewayAccountId;
    }

    public String getOtpKey() {
        return otpKey;
    }

    public void setOtpKey(String otpKey) {
        this.otpKey = otpKey;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public void setTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    public Boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Integer getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(Integer loginCount) {
        this.loginCount = loginCount;
    }

    public List<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleEntity> roles) {
        this.roles = roles;
    }

    /**
     * Note: this constructor will not copy <b>id</b> from the User model. It will always assign a new one internally (by JPA)
     *
     * @param user
     * @return persistable UserEntity object not bounded to entity manager
     */
    public static UserEntity from(User user) {
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(user.getUsername());
        userEntity.setPassword(user.getPassword());
        userEntity.setEmail(user.getEmail());
        userEntity.setOtpKey(user.getOtpKey());
        userEntity.setTelephoneNumber(user.getTelephoneNumber());
        userEntity.setGatewayAccountId(user.getGatewayAccountId());
        userEntity.setLoginCount(user.getLoginCount());
        userEntity.setDisabled(user.isDisabled());
        userEntity.setRoles(user.getRoles().stream().map(RoleEntity::new).collect(Collectors.toList()));
        return userEntity;
    }

    public User toUser() {
        User user = User.from(getId(), username, password, email, gatewayAccountId, otpKey, telephoneNumber);
        user.setLoginCount(loginCount);
        user.setDisabled(disabled);
        user.setRoles(roles.stream().map(roleEntity -> roleEntity.toRole()).collect(Collectors.toList()));
        return user;
    }
}
