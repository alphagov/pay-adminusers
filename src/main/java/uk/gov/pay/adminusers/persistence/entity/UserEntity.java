package uk.gov.pay.adminusers.persistence.entity;

import com.google.common.collect.ImmutableList;
import uk.gov.pay.adminusers.model.User;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private Integer loginCounter = 0;

    @ManyToMany(fetch = FetchType.EAGER, targetEntity = RoleEntity.class)
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
    private List<RoleEntity> roles = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY, targetEntity = ServiceEntity.class)
    @JoinTable(name = "users_services", joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "service_id", referencedColumnName = "id"))
    private List<ServiceEntity> services = new ArrayList<>();

    // TODO: Change column from 'camelCase' to 'snake_case'. These columns were created through Sequelize.
    @Column(name = "\"createdAt\"")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime createdAt;

    @Column(name = "\"updatedAt\"")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime updatedAt;

    @Column(name = "session_version", columnDefinition="int default 0")
    private Integer sessionVersion;

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
        return this.services.get(0).getGatewayAccount().getGatewayAccountId();
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

    public Integer getLoginCounter() {
        return loginCounter;
    }

    public void setLoginCounter(Integer loginCount) {
        this.loginCounter = loginCount;
    }

    public List<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleEntity> roles) {
        this.roles = roles;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getSessionVersion() {
        return sessionVersion;
    }

    public void setSessionVersion(Integer sessionVersion) {
        this.sessionVersion = sessionVersion;
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
        userEntity.setLoginCounter(user.getLoginCounter());
        userEntity.setDisabled(user.isDisabled());
        userEntity.setSessionVersion(user.getSessionVersion());
        userEntity.setRoles(user.getRoles().stream().map(RoleEntity::new).collect(Collectors.toList()));
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("UTC"));
        userEntity.setCreatedAt(timeNow);
        userEntity.setUpdatedAt(timeNow);
        return userEntity;
    }

    public User toUser() {
        User user = User.from(getId(), username, password, email, this.getGatewayAccountId(), otpKey, telephoneNumber);
        user.setLoginCounter(loginCounter);
        user.setDisabled(disabled);
        user.setSessionVersion(sessionVersion);
        List<String> gatewayAccountIds = this.services.stream()
                .map(service -> service.getGatewayAccounts().stream()
                        .map(gatewayAccountEntity -> gatewayAccountEntity.getGatewayAccountId())
                        .collect(Collectors.toList()))
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());
        user.setGatewayAccountIds(gatewayAccountIds);
        user.setRoles(roles.stream().map(roleEntity -> roleEntity.toRole()).collect(Collectors.toList()));
        return user;
    }

    public List<ServiceEntity> getServices() {
        return ImmutableList.copyOf(this.services);
    }

    public void addService(ServiceEntity service) {
        this.services.add(service);
    }

    public void removeService(ServiceEntity service) {
        this.services.remove(service);
    }
}
