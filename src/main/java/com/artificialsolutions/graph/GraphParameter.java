package com.artificialsolutions.graph;

import java.util.function.Function;
import java.util.Arrays;

import com.microsoft.graph.models.User;

/**
 * This enumeration defines available fields for Graph API call
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
    PREFERREDNAME("PreferredName", "preferredName", x -> x.preferredName),
    ;

    private final String name;
    private final String teneoName;
    private final Function<User, Object> propertyFunction;

    private GraphParameter(String name, String teneoName, Function<User, Object> propertyFunction) {
        this.name = name;
        this.teneoName = teneoName;
        this.propertyFunction = propertyFunction;
    }

    public String getName() {
        return name;
    }

    public String getTeneoName() {
        return teneoName;
    }

    public boolean hasValue(User u) {
        return propertyFunction.apply(u) != null;
    }

    public Object getValue(User u) {
        return propertyFunction.apply(u);
    }

    public static GraphParameter findByName(final String name) {
        return Arrays.stream(values()).filter(value -> value.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
}
