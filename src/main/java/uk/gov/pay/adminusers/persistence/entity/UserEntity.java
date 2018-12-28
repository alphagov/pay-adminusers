package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.app.util.RandomIdGenerator;
import uk.gov.pay.adminusers.model.CreateUserRequest;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceRole;
import uk.gov.pay.adminusers.model.User;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@Entity
@Table(name = "users")
@SequenceGenerator(name = "users_id_seq", sequenceName = "users_id_seq", allocationSize = 1)
public class UserEntity extends AbstractEntity {

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "email")
    private String email;

    @Column(name = "otp_key")
    private String otpKey;

    @Column(name = "telephone_number")
    private String telephoneNumber;

    @Column(name = "disabled")
    private Boolean disabled = Boolean.FALSE;

    @Column(name = "login_counter")
    private Integer loginCounter = 0;

    @Column(name = "features")
    private String features;

    @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST, targetEntity = ServiceRoleEntity.class)
    private final List<ServiceRoleEntity> servicesRoles = new ArrayList<>();

    // TODO: Change column from 'camelCase' to 'snake_case'. These columns were created through Sequelize.
    @Column(name = "\"createdAt\"")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime createdAt;

    @Column(name = "\"updatedAt\"")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime updatedAt;

    @Column(name = "session_version", columnDefinition = "int default 0")
    private Integer sessionVersion;

    @Column(name = "second_factor", nullable = false)
    @Convert(converter = SecondFactorMethodConverter.class)
    private SecondFactorMethod secondFactor;

    @Column(name = "provisional_otp_key")
    private String provisionalOtpKey;

    @Column(name = "provisional_otp_key_created_at")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime provisionalOtpKeyCreatedAt;

    public UserEntity() {
        //for jpa
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
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
        return this.servicesRoles.get(0).getService().getGatewayAccountId().getGatewayAccountId();
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

    public String getFeatures() { return features; }

    public void setFeatures(String features) { this.features = features; }

    public List<RoleEntity> getRoles() {
        return servicesRoles.isEmpty() ? newArrayList() : singletonList(servicesRoles.get(0).getRole());
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

    public SecondFactorMethod getSecondFactor() {
        return secondFactor;
    }

    public void setSecondFactor(SecondFactorMethod secondFactor) {
        this.secondFactor = secondFactor;
    }

    public String getProvisionalOtpKey() {
        return provisionalOtpKey;
    }

    public void setProvisionalOtpKey(String provisionalOtpKey) {
        this.provisionalOtpKey = provisionalOtpKey;
    }

    public ZonedDateTime getProvisionalOtpKeyCreatedAt() {
        return provisionalOtpKeyCreatedAt;
    }

    public void setProvisionalOtpKeyCreatedAt(ZonedDateTime provisionalOtpKeyCreatedAt) {
        this.provisionalOtpKeyCreatedAt = provisionalOtpKeyCreatedAt;
    }

    /**
     * Note: this constructor will not copy <b>id</b> from the User model. It will always assign a new one internally (by JPA)
     *
     * @param user
     * @return persistable UserEntity object not bounded to entity manager
     */
    public static UserEntity from(User user) {
        UserEntity userEntity = new UserEntity();
        userEntity.setExternalId(user.getExternalId());
        userEntity.setUsername(user.getUsername());
        userEntity.setPassword(user.getPassword());
        userEntity.setEmail(user.getEmail());
        userEntity.setOtpKey(user.getOtpKey());
        userEntity.setTelephoneNumber(user.getTelephoneNumber());
        userEntity.setSecondFactor(user.getSecondFactor());
        userEntity.setProvisionalOtpKey(user.getProvisionalOtpKey());
        userEntity.setProvisionalOtpKeyCreatedAt(user.getProvisionalOtpKeyCreatedAt());
        userEntity.setLoginCounter(user.getLoginCounter());
        userEntity.setFeatures(user.getFeatures());
        userEntity.setDisabled(user.isDisabled());
        userEntity.setSessionVersion(user.getSessionVersion());
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("UTC"));
        userEntity.setCreatedAt(timeNow);
        userEntity.setUpdatedAt(timeNow);
        return userEntity;
    }

    /**
     * Creates UserEntity object from CreateUserRequest object
     *
     * @param createUserRequest
     * @return persistable UserEntity object not bounded to entity manager
     */
    public static UserEntity from(CreateUserRequest createUserRequest) {
        UserEntity userEntity = new UserEntity();
        userEntity.setExternalId(RandomIdGenerator.randomUuid());
        userEntity.setUsername(createUserRequest.getUsername());
        userEntity.setPassword(createUserRequest.getPassword());
        userEntity.setEmail(createUserRequest.getEmail());
        userEntity.setOtpKey(createUserRequest.getOtpKey());
        userEntity.setTelephoneNumber(createUserRequest.getTelephoneNumber());
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setLoginCounter(0);
        userEntity.setFeatures(createUserRequest.getFeatures());
        userEntity.setDisabled(Boolean.FALSE);
        userEntity.setSessionVersion(0);
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("UTC"));
        userEntity.setCreatedAt(timeNow);
        userEntity.setUpdatedAt(timeNow);
        return userEntity;
    }

    public User toUser() {

        List<String> gatewayAccountIds = newArrayList();
        List<Service> services = newArrayList();
        List<ServiceRole> serviceRoles = newArrayList();

        if (!this.servicesRoles.isEmpty()) {
            services = this.servicesRoles.stream()
                    .map(serviceRole -> serviceRole.getService().toService()).collect(toList());
            serviceRoles = this.servicesRoles.stream().map(serviceRoleEntity -> serviceRoleEntity.toServiceRole()).collect(toList());
        }

        User user = User.from(getId(), externalId, username, password, email, gatewayAccountIds, services, otpKey, telephoneNumber,
                serviceRoles, features, secondFactor, provisionalOtpKey, provisionalOtpKeyCreatedAt);
        user.setLoginCounter(loginCounter);
        user.setDisabled(disabled);
        user.setSessionVersion(sessionVersion);
        user.setRoles(this.getRoles().stream().map(RoleEntity::toRole).collect(toList()));

        return user;
    }

    public void addServiceRole(ServiceRoleEntity serviceRole) {
        serviceRole.setUser(this);
        this.servicesRoles.add(serviceRole);
    }

    @Deprecated // Use external Id version
    public Optional<ServiceRoleEntity> getServicesRole(Integer serviceId) {
        return servicesRoles.stream().filter(serviceRoleEntity -> serviceId.equals(serviceRoleEntity.getService().getId())).findFirst();
    }

    public Optional<ServiceRoleEntity> getServicesRole(String serviceExternalId) {
        return servicesRoles.stream().filter(serviceRoleEntity -> serviceExternalId.equals(serviceRoleEntity.getService().getExternalId())).findFirst();
    }

    public boolean canInviteUsersTo(Integer serviceId) {
        Optional<ServiceRoleEntity> serviceRole = this.getServicesRole(serviceId);
        return serviceRole.isPresent() &&
                serviceRole.get().getRole().isAdmin() &&
                serviceRole.get().getService().getId().equals(serviceId);
    }

    public void remove(ServiceRoleEntity serviceRole) {
        servicesRoles.remove(serviceRole);
    }

    public List<ServiceRoleEntity> getServicesRoles() {
        return servicesRoles;
    }
}
