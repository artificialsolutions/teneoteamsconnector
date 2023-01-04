# Teneo Microsoft Teams Connector

This Java Connector connects Microsoft Teams to a Teneo-built Virtual Assistant (VA) so the Teams messenger acts as a frontend to the Teneo engine (the VA backend). This way, users can chat via Teams with a Teneo engine instead of a real person. One instance of this connector can serve multiple users talking to one published Teneo engine simultaneously.

## Description of the software

Teneo Microsoft Teams Connector is a Java console application. No firewalls, etc., should prevent its communication with Microsoft Bot API.

The functioning of the connector is illustrated in the following functional diagram:

![Functional diagram](README-imgs/FunctionalDiagram.png)

The sequence of the steps depicted in the diagram is as follows:

1. User submits their message into Teams messenger.

2. User's input is submitted to the connector by the Microsoft teams backend. The connector creates a `TurnContext` object (an instance of `com.microsoft.bot.builder.TurnContext`) for the given user interaction and generates a so-called bridge session ID (BSID) consisting the account's object ID within Azure Active Directory (AAD) and the channel ID for the user or bot on this channel. The connector then checks via its singleton bridge object (an instance of `com.artificialsolutions.teamsconnector.TeneoBot`) if there already exists a session (an instance of `com.artificialsolutions.teamsconnector.TeneoBot.BridgeSession`) identified via this BSID. If no such session exists, it is created and its timeout countdown is started. If it already exists, it is returned and its timeout countdown is restarted. Each session object has its own instance of Teneo engine client (`com.artificialsolutions.teneoengine.TeneoEngineClient`) to talk to Teneo engine.

3. The connector submits the user input to the Teneo engine client.

4. The Teneo engine client forwards the user's message to the Teneo engine with a POST request.

5. The Teneo engine client receives Virtual Assistant's answer in the response.

6. Virtual Assistant's answer is passed to the `TurnContext` object obtained at step 2.

7. The `TurnContext` object submits the answer to the Microsoft Bot API.

8. The answer is displayed to the user.

## Prerequisites

### Teneo Engine

Your bot needs to be published and you need to know the Engine URL.

### Java

To run the connector you need [Java version 17](https://www.oracle.com/java/technologies/downloads/#java17) or higher.

To run the connector locally, [ngrok](https://ngrok.com/) is preferred to make the connector available via HTTPS on port 3978.
Clone this repository:

``` bash
git clone https://github.com/artificialsolutions/teneoteamsconnector.git
```

## Setting up a bot in Azure

An Azure account with an active subscription is required. [Create an account for free here](https://azure.microsoft.com/free/?utm_source=campaign&utm_campaign=vscode-tutorial-app-service-extension&mktingSource=vscode-tutorial-app-service-extension).

Access [https://portal.azure.com](https://portal.azure.com) and follow the instructions under "Create the resource" from steps 1-10 [here](https://learn.microsoft.com/en-us/azure/bot-service/abs-quickstart?view=azure-bot-service-4.0&tabs=multitenant#create-the-resource) to create a bot.

Once created, go to the bot's configuration and add the connector's public URL in the field **Messaging endpoint** in the format `https://YOUR-PUBLIC-DOMAIN.eu.ngrok/api/messages` where `YOUR-PUBLIC-DOMAIN` is the forwarding url ngrok generates for your connector.

## Configuring the connector

The application is configured in the `application.properties` file (to be found in `src\main\resources` in the source code). The following configuration properties are implemented.

| Property                             | Notes                                                        |
| ------------------------------------ | ------------------------------------------------------------ |
| `server.port`                        | The port the connector is available on localhost (for example, 3978) |
| `MicrosoftAppType`                   | By default "multitenant" for Java client                     |
| `MicrosoftAppId`                     | Can be found under the bot configuration as  'Microsoft App ID' |
| `MicrosoftAppPassword`               | Can be found under 'Certificates & Secrets' section. It's the value of the new client secret you generate |
| `MicrosoftTenantId`                  | Not applicable. Leave this blank for a multi-tenant bot      |
| `microsoft.graph.request.params`     | The user-related request parameters to be added to Teneo engine requests: `UserPrincipalName`, `GivenName`, `Surname`, `Mail`, `Department`, `EmployeeId`, `AgeGroup`, `City`, `CompanyName`, `ConsentProvidedForMinor`, `Country`, `DisplayName`, `EmployeeType`, `ExternalUserState`, `FaxNumber`, `JobTitle`, `LegalAgeGroupClassification`, `MailNickname`, `MobilePhone`, `OfficeLocation`, `PostalCode`, `PreferredLanguage`, `State`, `StreetAddress`, `UserType`, `UsageLocation`, `MySite`, `AboutMe`, `PreferredName` as per [https://learn.microsoft.com/en-us/powershell/module/microsoft.graph.users/update-mguser](https://learn.microsoft.com/en-us/powershell/module/microsoft.graph.users/update-mguser) |
| `teneo.engine.endpointUrl`           | The Teneo engine url for your bot                            |
| `teneo.engine.connectTimeoutMillis`  | The timeout to connect with Teneo engine, in milliseconds    |
| `teneo.engine.responseTimeoutMillis` | The timeout to wait for Teneo engine responses, in milliseconds |
| `bridge.sessionTimeoutMillis`        | The timeout for the sessions created by the bridge, in milliseconds; it is recommendable to have it slightly longer then the session timeout of Teneo engine, which is normally 10 minutes (600 seconds, 600000 milliseconds) |
| `bridge.maxParallelSessions`         | The maximum number of simultaneous sessions for the bridge; this number can be kept high (tens of thousands), although not too high since its purpose is to reduce the risk or the application running out of memory if the number of session increases too much |
| `application.explicitData`           | The Boolean value indicating if some error and debug information should be added to requests sent both to Teneo engine and displayed to users in Teams. This property is not obligatory and defaults to `false`. It should only be set to `true` for testing and troubleshooting |

Regarding the logger configuration (file `log4j2.json` in `src\main\resources` in the source code), in order to test the application it is highly recommended to have it on the `debug` or `trace` level. If you have it on those sensitivity levels, it might log some PII, like user BSIDs, user inputs, etc. Thus it should be set to have less sensitivity in production (`info` or `warn` for example).

### Channels

A channel is how your bot communicates with your application, in this case Microsoft Teams.

* Select your bot in Azure. Under settings, click on Channels, then click on "Available Channels" and select Microsoft Teams.
* Read and Accept the Channel Publication Terms.
* Select "Microsoft Teams Commercial (most common)".
* Click "Apply".
* Once enabled, you can close this tab and the channel should be available under Channels along with Web Chat.

To open in Teams, hit the link under Actions, "Open in Teams". You will be prompted to allow Teams to be opened. Once you agree, you will see your bot in your Teams Chat panel.

### Teneo Solution configuration

#### Data received by Teneo engine from Connector

The requests received by Teneo engine contain the following parameters:

* `viewtype`, value: `tieapi`
* `channel`, value: `Teams`
* `userinput`, value: the input text (if the user submitted it)

Additionally, the request will contain all the parameters/values available via the `com.microsoft.bot.schema.Activity.getValue()` provided this call returned a `Map` instance. Moreover, the user-related parameters configured in `microsoft.graph.request.params` will also be added.

#### Data returned by Teneo engine (Teneo solution) to Connector

Teneo engine normally returns a text as its answer. This text is then displayed in Teams to the user. If [adaptive cards]((https://learn.microsoft.com/en-us/microsoftteams/platform/task-modules-and-cards/cards/cards-reference#adaptive-card)) should be returned, they should be placed in the output parameter `msbotframework` as a well formed JSON. [Splitting answers into 'bubbles'](https://www.teneo.ai/resource/channels/teneo-web-chat#message-types_splitting-answers-into-bubbles) is also supported via the output parameter `outputTextSegmentIndexes`.

### Try out your Bot

* Compile this connector:

``` bash
javac ./src/main/java/com/artificialsolutions/teamsconnector/Application.java
```

* Run this connector:

``` bash
java ./src/main/java/com/artificialsolutions/teamsconnector/Application.java
```

* Send a message through Microsoft Teams and your bot will respond to your chat!
