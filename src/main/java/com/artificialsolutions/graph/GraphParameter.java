package com.artificialsolutions.graph;

import java.util.function.Function;
import java.util.Arrays;

import com.microsoft.graph.models.User;

/**
 * This enumeration defines available fields for Graph API calls. It includes some of the fields described
 * <a href="https://learn.microsoft.com/en-us/powershell/module/microsoft.graph.users/update-mguser">here</a>.
 */
public enum GraphParameter {

    USERNAME("UserPrincipalName", "userPrincipalName", x -> x.userPrincipalName),
    GIVENNAME("GivenName", "givenName", x -> x.givenName),
    SURNAME("Surname", "surname", x -> x.surname),
    MAIL("Mail", "email", x -> x.mail),
    DEPARTMENT("Department", "department", x -> x.department),
    EMPLOYEEID("EmployeeId", "employeeId", x -> x.employeeId),
    AGEGROUP("AgeGroup", "ageGroup", x -> x.ageGroup),
    CITY("City", "city", x -> x.city),
    COMPANYNAME("CompanyName", "companyName", x -> x.companyName),
    CONSENTPROVIDEDFORMINOR("ConsentProvidedForMinor", "consentProvidedForMinor", x -> x.consentProvidedForMinor),
    COUNTRY("Country", "country", x -> x.country),
    DISPLAYNAME("DisplayName", "displayName", x -> x.displayName),
    EMPLOYEETYPE("EmployeeType", "employeeType", x -> x.employeeType),
    EXTERNALUSERSTATE("ExternalUserState", "externalUserState", x -> x.externalUserState),
    FAXNUMBER("FaxNumber", "faxNumber", x -> x.faxNumber),
    JOBTITLE("JobTitle", "jobTitle", x -> x.jobTitle),
    LEGALAGEGROUPCLASSIFICATION("LegalAgeGroupClassification", "legalAgeGroupClassification",
            x -> x.legalAgeGroupClassification),
    MAILNICKNAME("MailNickname", "mailNickname", x -> x.mailNickname),
    MOBILEPHONE("MobilePhone", "mobilePhone", x -> x.mobilePhone),
    OFFICELOCATION("OfficeLocation", "officeLocation", x -> x.officeLocation),
    POSTALCODE("PostalCode", "postalCode", x -> x.postalCode),
    PREFERREDLANGUAGE("PreferredLanguage", "preferredLanguage", x -> x.preferredLanguage),
    STATE("State", "state", x -> x.state),
    STREETADDRESS("StreetAddress", "streetAddress", x -> x.streetAddress),
    USERTYPE("UserType", "userType", x -> x.userType),
    USAGELOCATION("UsageLocation", "usageLocation", x -> x.usageLocation),
    MYSITE("MySite", "mySite", x -> x.mySite),
    ABOUTME("AboutMe", "aboutMe", x -> x.aboutMe),
    PREFERREDNAME("PreferredName", "preferredName", x -> x.preferredName);

    /**
     * The name of the field as per
     * <a href="https://learn.microsoft.com/en-us/powershell/module/microsoft.graph.users/update-mguser">this description</a>.
     */
    private final String name;
    
    /**
     * The name of the Teneo engine request parameter corresponding to this field.
     */
    private final String teneoName;
    
    /**
     * The function getting the value of the {@code User} object property corresponding to this field.
     */
    private final Function<User, Object> propertyFunction;

    /**
     * Creates an instance of this class.
     *  
     * @param name the name of the field as per
     * <a href="https://learn.microsoft.com/en-us/powershell/module/microsoft.graph.users/update-mguser">this description</a>.
     * @param teneoName the name of the Teneo engine request parameter corresponding to this field.
     * @param propertyFunction the function getting the value of the {@code User} object property corresponding to this field.   
     */
    private GraphParameter(String name, String teneoName, Function<User, Object> propertyFunction) {
        this.name = name;
        this.teneoName = teneoName;
        this.propertyFunction = propertyFunction;
    }

    /**
     * Gets the name of this field.
     * 
     * @return the name of the field as per
     * <a href="https://learn.microsoft.com/en-us/powershell/module/microsoft.graph.users/update-mguser">this description</a>.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the name of request parameter corresponding to this field, to be used in requests to Teneo engine.
     * The value of this parameter will depend on the user.
     * 
     * @return the name of request parameter to be used in requests to Teneo engine.
     */
    public String getTeneoName() {
        return teneoName;
    }

    /**
     * Checks if the given user has a value for this field.
     * 
     * @param u the user.
     * 
     * @return {@code true} if the given user has a value for this field, {@code false} otherwise. 
     */
    public boolean hasValue(User u) {
        return propertyFunction.apply(u) != null;
    }

    /**
     * Returns the value of this field for the given user.
     * 
     * @param u the user.
     * 
     * @return the value of this field for the given user.
     */
    public Object getValue(User u) {
        return propertyFunction.apply(u);
    }

    /**
     * Returns the {@code GraphParameter} instance by its name. The name is case-insensitive.
     * 
     * @param name the name of the {@code GraphParameter} instance.
     *  
     * @return the {@code GraphParameter} instance.
     */
    public static GraphParameter findByName(final String name) {
        return Arrays.stream(values()).filter(value -> value.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
}
